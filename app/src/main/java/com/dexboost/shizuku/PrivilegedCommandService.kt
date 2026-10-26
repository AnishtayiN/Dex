package com.dexboost.shizuku

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import com.dexboost.domain.model.CommandResult
import com.dexboost.optimization.PackageNameValidator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Privileged command service, run by the Shizuku server.
 *
 * Why a UserService: in Shizuku API 13.1.5 `Shizuku.newProcess` is
 * private — it is not a usable public API, and upstream guidance is to
 * use a UserService for privileged operations. This service is the
 * supported path: Shizuku starts this service in a process running
 * with the Shizuku server's ADB/shell-level privilege (not root), and
 * [ShizukuCommandExecutor] talks to it over Binder.
 *
 * The service executes argv arrays directly with
 * `Runtime.exec(Array<String>)` — there is no shell, no `sh -c`, no
 * string concatenation. As defence in depth it also refuses any
 * command that is not exactly an ART compilation command for a
 * validated package (see [isAllowedCommand]): even if some other app
 * managed to bind this service, it could only trigger ART compilation,
 * and only with the caller's own (unprivileged) rights, because the
 * privilege comes from Shizuku starting the service, not from this
 * class.
 *
 * Binder protocol (transaction code [TRANSACTION_EXECUTE]):
 * request: interface token, String[] command, Long timeoutMs.
 * reply: noException marker, Int hasExitCode (1/0), Int exitCode
 * (-1 when absent), String stdout, String stderr, Byte timedOut,
 * Long durationMs. Output is truncated to [MAX_OUTPUT_CHARS] per
 * stream so a reply can never exceed the Binder transaction limit.
 */
class PrivilegedCommandService : Service() {

    override fun onBind(intent: Intent?): IBinder = CommandBinder()

    inner class CommandBinder : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == TRANSACTION_EXECUTE) {
                data.enforceInterface(DESCRIPTOR)
                val command = data.createStringArray()?.toList() ?: emptyList()
                val timeoutMs = data.readLong()
                val result = runAllowedCommand(command, timeoutMs)
                reply?.apply {
                    writeNoException()
                    writeInt(if (result.exitCode != null) 1 else 0)
                    writeInt(result.exitCode ?: -1)
                    writeString(result.stdout)
                    writeString(result.stderr)
                    writeByte(if (result.timedOut) 1 else 0)
                    writeLong(result.durationMs)
                }
                return true
            }
            return super.onTransact(code, data, reply, flags)
        }
    }

    private fun runAllowedCommand(command: List<String>, timeoutMs: Long): CommandResult {
        val start = System.currentTimeMillis()
        fun done(exitCode: Int?, stdout: String, stderr: String, timedOut: Boolean): CommandResult =
            CommandResult(
                exitCode = exitCode,
                stdout = stdout.take(MAX_OUTPUT_CHARS),
                stderr = stderr.take(MAX_OUTPUT_CHARS),
                timedOut = timedOut,
                durationMs = System.currentTimeMillis() - start,
            )
        if (!isAllowedCommand(command)) {
            return done(null, "", "Refused: not an allowed ART compilation command.", false)
        }
        if (timeoutMs <= 0L) {
            return done(null, "", "Timeout must be positive.", false)
        }
        val process = try {
            Runtime.getRuntime().exec(command.toTypedArray())
        } catch (e: Exception) {
            return done(null, "", e.message ?: "Failed to start command.", false)
        }
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val outThread = thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).forEachLine { stdout.append(it).append('\n') }
            } catch (_: Exception) {
            }
        }
        val errThread = thread {
            try {
                BufferedReader(InputStreamReader(process.errorStream)).forEachLine { stderr.append(it).append('\n') }
            } catch (_: Exception) {
            }
        }
        val finished = try {
            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            process.destroy()
            return done(null, stdout.toString(), stderr.toString(), true)
        }
        if (!finished) {
            process.destroyForcibly()
            outThread.join(1000)
            errThread.join(1000)
            // A timeout means the outcome is unknown, never success/failure.
            return done(null, stdout.toString(), stderr.toString(), true)
        }
        outThread.join(5000)
        errThread.join(5000)
        val exitCode = try {
            process.exitValue()
        } catch (e: Exception) {
            return done(null, stdout.toString(), stderr.toString(), true)
        }
        return done(exitCode, stdout.toString(), stderr.toString(), false)
    }

    companion object {
        const val DESCRIPTOR = "com.dexboost.shizuku.PrivilegedCommandService"
        const val TRANSACTION_EXECUTE = IBinder.FIRST_CALL_TRANSACTION
        const val MAX_OUTPUT_CHARS = 8000

        /**
         * Only `cmd package compile` commands are allowed, with the
         * exact shapes produced by ArtCommandBuilder: either
         * `-m <filter> -f <package>` with filter in
         * {verify, speed-profile, speed}, or `--reset <package>`.
         * Package names must pass [PackageNameValidator].
         */
        fun isAllowedCommand(command: List<String>): Boolean {
            if (command.size < 4 || command[0] != "cmd" || command[1] != "package" || command[2] != "compile") {
                return false
            }
            val rest = command.drop(3)
            return when {
                rest.size == 4 && rest[0] == "-m" && rest[1] in setOf("verify", "speed-profile", "speed") &&
                    rest[2] == "-f" && PackageNameValidator.isValid(rest[3]) -> true
                rest.size == 2 && rest[0] == "--reset" && PackageNameValidator.isValid(rest[1]) -> true
                else -> false
            }
        }
    }
}
