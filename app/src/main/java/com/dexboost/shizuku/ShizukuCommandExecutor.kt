package com.dexboost.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import com.dexboost.domain.model.CommandResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * [PrivilegedCommandExecutor] backed by a Shizuku UserService.
 *
 * In Shizuku API 13.1.5 `Shizuku.newProcess` is private and cannot be
 * called by apps; the supported path for privileged work is
 * `Shizuku.bindUserService`, which starts [PrivilegedCommandService]
 * in a process running with the Shizuku server's ADB/shell-level
 * privilege (not root). This executor binds that service for each
 * execution, performs one Binder transaction, and unbinds. The
 * command itself is an argv list produced by the engine layer; there
 * is no shell string anywhere here, and the service independently
 * re-validates the command shape before running it.
 *
 * Timeout semantics are honest throughout: if the service reports a
 * timeout, or if this side times out waiting for the bind or the
 * transaction, the returned result has `timedOut = true` and a null
 * exit code, meaning the outcome is unknown — never success, never
 * failure. Cancellation reports cancelled, outcome unknown.
 */
class ShizukuCommandExecutor(
    private val manager: ShizukuManager,
    private val context: Context,
) : PrivilegedCommandExecutor {

    override suspend fun execute(command: List<String>, timeoutMs: Long): CommandResult {
        val startMs = SystemClock.elapsedRealtime()
        fun elapsed() = SystemClock.elapsedRealtime() - startMs
        if (command.isEmpty()) {
            return CommandResult(null, "", "Empty command: nothing to execute.", false, elapsed())
        }
        if (timeoutMs <= 0L) {
            return CommandResult(null, "", "Timeout must be positive.", false, elapsed())
        }
        // Refresh first so a stale Ready does not let a command run
        // after the binder died. refresh() never throws.
        manager.refresh()
        if (manager.state.value != ShizukuState.Ready) {
            return CommandResult(null, "", "Shizuku is not ready", false, 0L)
        }
        val appContext = context.applicationContext
        val connected = CompletableDeferred<IBinder>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service != null && !connected.isCompleted) connected.complete(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val args = Shizuku.UserServiceArgs(
            ComponentName(appContext.packageName, PrivilegedCommandService::class.java.name),
        ).daemon(false)
        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Exception) {
            return CommandResult(null, "", e.message ?: "Failed to bind Shizuku UserService.", false, elapsed())
        }
        try {
            val binder = withTimeoutOrNull(BIND_TIMEOUT_MS) { connected.await() }
                ?: return CommandResult(
                    null, "", "Timed out waiting for the Shizuku UserService to connect.",
                    true, elapsed(),
                )
            return withContext(Dispatchers.IO) {
                val result = withTimeoutOrNull(timeoutMs + TRANSACTION_SLACK_MS) {
                    transact(binder, command, timeoutMs)
                }
                result ?: CommandResult(
                    null, "", "Timed out waiting for the command result; outcome unknown.",
                    true, elapsed(),
                )
            }
        } catch (e: CancellationException) {
            return CommandResult(null, "", "Cancelled.", false, elapsed(), cancelled = true)
        } catch (e: Exception) {
            return CommandResult(null, "", e.message ?: "Shizuku UserService call failed.", false, elapsed())
        } finally {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Exception) {
            }
        }
    }

    private fun transact(binder: IBinder, command: List<String>, timeoutMs: Long): CommandResult {
        val startMs = SystemClock.elapsedRealtime()
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(PrivilegedCommandService.DESCRIPTOR)
            data.writeStringArray(command.toTypedArray())
            data.writeLong(timeoutMs)
            binder.transact(PrivilegedCommandService.TRANSACTION_EXECUTE, data, reply, 0)
            reply.readException()
            val hasExitCode = reply.readInt() == 1
            val exitCode = reply.readInt()
            val stdout = reply.readString() ?: ""
            val stderr = reply.readString() ?: ""
            val timedOut = reply.readByte() != 0.toByte()
            val durationMs = reply.readLong()
            return CommandResult(
                exitCode = if (hasExitCode) exitCode else null,
                stdout = stdout,
                stderr = stderr,
                timedOut = timedOut,
                durationMs = if (durationMs > 0) durationMs else SystemClock.elapsedRealtime() - startMs,
            )
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    companion object {
        const val BIND_TIMEOUT_MS = 30_000L
        const val TRANSACTION_SLACK_MS = 5_000L
    }
}
