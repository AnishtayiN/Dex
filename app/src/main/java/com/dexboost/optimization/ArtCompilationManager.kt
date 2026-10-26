package com.dexboost.optimization

import com.dexboost.domain.model.CompilationMode
import com.dexboost.domain.model.CommandOutcome
import com.dexboost.domain.model.CommandResult
import com.dexboost.shizuku.PrivilegedCommandExecutor
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Runs ART compilation commands for a batch of apps, one app at a time,
 * in the order given.
 *
 * Callers must invoke [runBatch] from a background dispatcher
 * (Dispatchers.Default or Dispatchers.IO); the manager itself does not
 * switch dispatchers, so that coroutine cancellation and the caller's
 * test dispatcher both behave predictably. The executor performs the
 * actual privileged I/O and must support coroutine cancellation so an
 * in-flight command can be abandoned.
 *
 * Cancellation: [cancel] sets a flag that is checked between apps and
 * also cancels the coroutine currently running the batch, which aborts
 * an in-flight executor call. Items interrupted this way are marked
 * [QueueStatus.CANCELLED]. When cancellation was requested via [cancel],
 * [runBatch] returns normally after marking the remaining items; if the
 * batch coroutine is cancelled externally (without [cancel]), the
 * [CancellationException] is rethrown after the queue state is updated.
 *
 * History recording: [onRecord] is invoked for every item that produced
 * a result — executed items and invalid package names — but not for
 * items that were never started because the batch was cancelled.
 * Invalid packages are never passed to the executor; they are marked
 * DONE with a synthetic failed result (exit code -1).
 *
 * Concurrency: only one batch may run at a time. A second [runBatch]
 * call while one is running throws [IllegalStateException] immediately
 * and does not disturb the running batch.
 *
 * Counts: [QueueState.completedCount] counts DONE items whose result
 * interprets to [CommandOutcome.CONFIRMED]; [QueueState.failedCount]
 * counts DONE items with any other outcome except
 * [CommandOutcome.CANCELLED]; [QueueState.remainingCount] counts items
 * still QUEUED or RUNNING.
 *
 * No outcome here is presented as a guaranteed performance gain: an
 * exit code of 0 only confirms the command itself completed.
 */
class ArtCompilationManager(
    private val executor: PrivilegedCommandExecutor,
) {

    /** Lifecycle of one item in a batch. */
    enum class QueueStatus {
        QUEUED,
        RUNNING,
        DONE,
        CANCELLED,
    }

    /** One app in a batch, with its latest status and result, if any. */
    data class QueueItem(
        val packageName: String,
        val mode: CompilationMode,
        val status: QueueStatus,
        val result: CommandResult?,
    )

    /** Observable snapshot of the current (or most recent) batch. */
    data class QueueState(
        val items: List<QueueItem>,
        val currentPackage: String?,
        val completedCount: Int,
        val failedCount: Int,
        val remainingCount: Int,
    )

    private val _queueState = MutableStateFlow(
        QueueState(
            items = emptyList(),
            currentPackage = null,
            completedCount = 0,
            failedCount = 0,
            remainingCount = 0,
        ),
    )

    /** Current queue snapshot; updated as each item starts and finishes. */
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private val running = AtomicBoolean(false)
    private val cancelRequested = AtomicBoolean(false)

    @Volatile
    private var currentJob: Job? = null

    /**
     * Runs [mode] for each package in [packages], sequentially and in
     * order. Duplicates within the batch are skipped (only the first
     * occurrence runs). Invalid package names become DONE items with a
     * synthetic failed result and are never sent to the executor.
     *
     * @throws IllegalStateException if another batch is already running.
     */
    suspend fun runBatch(
        packages: List<String>,
        mode: CompilationMode,
        timeoutMsPerApp: Long = 300_000,
        onRecord: (packageName: String, mode: CompilationMode, result: CommandResult) -> Unit = { _, _, _ -> },
    ) {
        if (!running.compareAndSet(false, true)) {
            throw IllegalStateException("An ArtCompilationManager batch is already running")
        }
        cancelRequested.set(false)
        currentJob = currentCoroutineContext()[Job]
        try {
            val uniquePackages = packages.distinct()
            val items = uniquePackages.map { packageName ->
                if (PackageNameValidator.isValid(packageName)) {
                    QueueItem(packageName, mode, QueueStatus.QUEUED, null)
                } else {
                    QueueItem(
                        packageName = packageName,
                        mode = mode,
                        status = QueueStatus.DONE,
                        result = CommandResult(
                            exitCode = -1,
                            stdout = "",
                            stderr = "Invalid package name",
                            timedOut = false,
                            durationMs = 0L,
                        ),
                    )
                }
            }.toMutableList()
            emit(items)

            // Invalid entries already have a result; record them, but
            // never send them to the executor.
            for (item in items) {
                if (item.status == QueueStatus.DONE && item.result != null) {
                    onRecord(item.packageName, mode, item.result)
                }
            }

            for (index in items.indices) {
                if (cancelRequested.get()) {
                    markRemainingCancelled(items, fromIndex = index)
                    emit(items)
                    return
                }
                val item = items[index]
                if (item.status != QueueStatus.QUEUED) continue

                items[index] = item.copy(status = QueueStatus.RUNNING)
                emit(items)

                val result = try {
                    executor.execute(
                        ArtCommandBuilder.build(item.packageName, mode),
                        timeoutMsPerApp,
                    )
                } catch (e: CancellationException) {
                    // Interrupted in flight: mark this and everything
                    // still queued as cancelled, then honour the source
                    // of the cancellation.
                    items[index] = item.copy(
                        status = QueueStatus.CANCELLED,
                        result = cancelledResult(),
                    )
                    markRemainingCancelled(items, fromIndex = index + 1)
                    emit(items)
                    if (cancelRequested.get()) {
                        return
                    } else {
                        throw e
                    }
                }

                if (result.cancelled) {
                    // The executor reports cancellation by returning a
                    // cancelled result instead of throwing (see the
                    // PrivilegedCommandExecutor contract). Treat that
                    // as an interruption of the whole batch, not as a
                    // per-app failure.
                    items[index] = item.copy(status = QueueStatus.CANCELLED, result = result)
                    markRemainingCancelled(items, fromIndex = index + 1)
                    emit(items)
                    val jobCancelled = currentCoroutineContext()[Job]?.isCancelled == true
                    if (!cancelRequested.get() && jobCancelled) {
                        throw CancellationException("Batch coroutine was cancelled")
                    }
                    return
                }

                items[index] = item.copy(status = QueueStatus.DONE, result = result)
                emit(items)
                onRecord(item.packageName, mode, result)
            }
        } finally {
            currentJob = null
            running.set(false)
        }
    }

    /**
     * Requests cancellation of the running batch. The flag stops the
     * batch between apps, and cancelling the running coroutine's job
     * also abandons an in-flight executor call. Has no effect when no
     * batch is running.
     */
    fun cancel() {
        cancelRequested.set(true)
        currentJob?.cancel()
    }

    private fun cancelledResult(): CommandResult = CommandResult(
        exitCode = null,
        stdout = "",
        stderr = "",
        timedOut = false,
        durationMs = 0L,
        cancelled = true,
    )

    private fun markRemainingCancelled(items: MutableList<QueueItem>, fromIndex: Int) {
        for (i in fromIndex until items.size) {
            val item = items[i]
            if (item.status == QueueStatus.QUEUED || item.status == QueueStatus.RUNNING) {
                items[i] = item.copy(
                    status = QueueStatus.CANCELLED,
                    result = item.result ?: cancelledResult(),
                )
            }
        }
    }

    private fun emit(items: List<QueueItem>) {
        val completed = items.count {
            it.status == QueueStatus.DONE && it.result?.interpret() == CommandOutcome.CONFIRMED
        }
        val failed = items.count {
            it.status == QueueStatus.DONE &&
                it.result != null &&
                it.result.interpret() != CommandOutcome.CONFIRMED &&
                it.result.interpret() != CommandOutcome.CANCELLED
        }
        _queueState.value = QueueState(
            items = items.toList(),
            currentPackage = items.firstOrNull { it.status == QueueStatus.RUNNING }?.packageName,
            completedCount = completed,
            failedCount = failed,
            remainingCount = items.count {
                it.status == QueueStatus.QUEUED || it.status == QueueStatus.RUNNING
            },
        )
    }
}
