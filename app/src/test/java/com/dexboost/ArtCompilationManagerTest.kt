package com.dexboost

import com.dexboost.domain.model.CompilationMode
import com.dexboost.domain.model.CommandResult
import com.dexboost.optimization.ArtCompilationManager
import com.dexboost.shizuku.PrivilegedCommandExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests for [ArtCompilationManager] with a fake executor.
 *
 * NOTE: this test compiles against the `PrivilegedCommandExecutor`
 * interface created by the Shizuku agent in `com.dexboost.shizuku`
 * (suspend fun execute(command: List<String>, timeoutMs: Long):
 * CommandResult). Until that file exists, this test file — and the
 * manager itself — will not compile; that is an integration dependency,
 * not a defect in this code.
 */
class ArtCompilationManagerTest {

    class FakeExecutor(var result: CommandResult) : PrivilegedCommandExecutor {
        val commands: MutableList<List<String>> = mutableListOf()

        /** Overrides [result] per command when set. */
        var resultProvider: (suspend (List<String>) -> CommandResult)? = null

        /** Invoked on every execute call, before the result is returned. */
        var onExecute: (() -> Unit)? = null

        override suspend fun execute(command: List<String>, timeoutMs: Long): CommandResult {
            commands.add(command)
            onExecute?.invoke()
            return resultProvider?.invoke(command) ?: result
        }
    }

    private fun successResult(): CommandResult = CommandResult(
        exitCode = 0,
        stdout = "Success",
        stderr = "",
        timedOut = false,
        durationMs = 5L,
    )

    private fun failedResult(): CommandResult = CommandResult(
        exitCode = 1,
        stdout = "",
        stderr = "Something went wrong",
        timedOut = false,
        durationMs = 5L,
    )

    @Test
    fun runsSequentiallyInOrder() = runBlocking {
        val fake = FakeExecutor(successResult())
        val manager = ArtCompilationManager(fake)
        manager.runBatch(
            listOf("com.example.a", "com.example.b", "com.example.c"),
            CompilationMode.SPEED,
        )
        assertEquals(
            listOf(
                listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.a"),
                listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.b"),
                listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.c"),
            ),
            fake.commands,
        )
        val state = manager.queueState.value
        assertEquals(3, state.items.size)
        assertTrue(state.items.all { it.status == ArtCompilationManager.QueueStatus.DONE })
        assertEquals(null, state.currentPackage)
        assertEquals(3, state.completedCount)
        assertEquals(0, state.failedCount)
        assertEquals(0, state.remainingCount)
    }

    @Test
    fun duplicatesAreSkipped() = runBlocking {
        val fake = FakeExecutor(successResult())
        val manager = ArtCompilationManager(fake)
        manager.runBatch(
            listOf("com.example.a", "com.example.a", "com.example.b", "com.example.b"),
            CompilationMode.VERIFY,
        )
        assertEquals(2, fake.commands.size)
        assertEquals(2, manager.queueState.value.items.size)
        assertEquals(2, manager.queueState.value.completedCount)
    }

    @Test
    fun invalidPackageNeverReachesExecutor() = runBlocking {
        val fake = FakeExecutor(successResult())
        val manager = ArtCompilationManager(fake)
        val recorded = mutableListOf<String>()
        manager.runBatch(
            listOf("com.example.good", "bad name", "com.example.good2"),
            CompilationMode.SPEED,
            onRecord = { packageName, _, _ -> recorded.add(packageName) },
        )
        assertEquals(2, fake.commands.size)
        assertTrue(fake.commands.none { it.contains("bad name") })
        val state = manager.queueState.value
        assertEquals(3, state.items.size)
        val invalid = state.items[1]
        assertEquals("bad name", invalid.packageName)
        assertEquals(ArtCompilationManager.QueueStatus.DONE, invalid.status)
        assertEquals(-1, invalid.result?.exitCode)
        assertEquals("Invalid package name", invalid.result?.stderr)
        assertEquals(2, state.completedCount)
        assertEquals(1, state.failedCount)
        assertTrue(recorded.contains("bad name"))
    }

    @Test
    fun completedAndFailedCountsReflectOutcomes() = runBlocking {
        val fake = FakeExecutor(successResult())
        fake.resultProvider = { command ->
            if (command.last() == "com.example.bad") failedResult() else successResult()
        }
        val manager = ArtCompilationManager(fake)
        manager.runBatch(
            listOf("com.example.good", "com.example.bad", "com.example.good2"),
            CompilationMode.SPEED_PROFILE,
        )
        val state = manager.queueState.value
        assertEquals(2, state.completedCount)
        assertEquals(1, state.failedCount)
        assertEquals(0, state.remainingCount)
    }

    @Test
    fun cancellationBetweenItemsCancelsRemaining() = runBlocking {
        lateinit var manager: ArtCompilationManager
        val fake = FakeExecutor(successResult())
        // Cancel as soon as the first app executes; the batch must
        // stop before the second app and mark it CANCELLED.
        fake.onExecute = { manager.cancel() }
        manager = ArtCompilationManager(fake)

        val job = launch {
            manager.runBatch(
                listOf("com.example.a", "com.example.b", "com.example.c"),
                CompilationMode.SPEED,
            )
        }
        job.join()
        assertEquals(1, fake.commands.size)
        val state = manager.queueState.value
        assertEquals(3, state.items.size)
        assertEquals(ArtCompilationManager.QueueStatus.DONE, state.items[0].status)
        assertEquals(ArtCompilationManager.QueueStatus.CANCELLED, state.items[1].status)
        assertEquals(ArtCompilationManager.QueueStatus.CANCELLED, state.items[2].status)
        assertEquals(1, state.completedCount)
        // Cancelled items are neither completed nor failed.
        assertEquals(0, state.failedCount)
        assertEquals(0, state.remainingCount)
    }

    @Test
    fun secondBatchWhileRunningThrows() = runBlocking {
        val gate = CompletableDeferred<CommandResult>()
        val fake = FakeExecutor(successResult())
        fake.resultProvider = { gate.await() }
        val manager = ArtCompilationManager(fake)

        val job = launch {
            manager.runBatch(listOf("com.example.a"), CompilationMode.SPEED)
        }
        // Wait until the first batch is actually inside the executor.
        while (fake.commands.isEmpty()) {
            delay(10)
        }
        try {
            manager.runBatch(listOf("com.example.b"), CompilationMode.SPEED)
            fail("Expected IllegalStateException for a concurrent batch")
        } catch (expected: IllegalStateException) {
            // Expected: only one batch may run at a time.
        }
        gate.complete(successResult())
        job.join()
        assertEquals(1, fake.commands.size)
        assertEquals(1, manager.queueState.value.completedCount)
    }

    @Test
    fun resetModeIsUsedForWholeBatch() = runBlocking {
        val fake = FakeExecutor(successResult())
        val manager = ArtCompilationManager(fake)
        manager.runBatch(listOf("com.example.a"), CompilationMode.RESET)
        assertEquals(
            listOf(listOf("cmd", "package", "compile", "--reset", "com.example.a")),
            fake.commands,
        )
    }
}
