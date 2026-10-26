package com.dexboost.shizuku

import com.dexboost.domain.model.CommandResult

/**
 * Runs one already-validated command with the privileges of the Shizuku
 * server process (ADB/shell level). This is not root, and no outcome is
 * ever invented: a result reports exactly what happened.
 *
 * Contract for every implementation:
 * - The command is passed as an argument array ([List] of argv elements).
 *   Implementations must never join it into a shell string, never run it
 *   through a shell interpreter, and never execute arbitrary user text.
 *   Validation of which commands are allowed belongs to the caller
 *   (the engine layer); this interface only executes what it is given.
 * - No unprivileged fallback, ever. If the privileged channel is not
 *   available, the implementation returns an error [CommandResult]
 *   instead of running the command locally (no `Runtime.exec`, no `su`,
 *   no local shell). Running the same command without Shizuku would
 *   silently do something different from what the caller asked for, so
 *   failing honestly is the only correct behaviour.
 * - Timeout means "outcome unknown", never success and never failure.
 *   On timeout the implementation returns [CommandResult.timedOut] =
 *   `true` with [CommandResult.exitCode] = `null`, together with
 *   whatever output had been captured so far. The process is destroyed,
 *   but a destroyed process may still have finished before the kill
 *   took effect, so its exit code must not be reported.
 * - Coroutine cancellation is respected: when the calling coroutine is
 *   cancelled, the implementation destroys the process and returns a
 *   [CommandResult] with [CommandResult.cancelled] = `true` and
 *   [CommandResult.exitCode] = `null`, rather than hanging or
 *   reporting a result the caller no longer wants.
 * - A timed-out or cancelled result is not an error result: callers
 *   must check [CommandResult.timedOut] and [CommandResult.cancelled]
 *   before interpreting [CommandResult.exitCode].
 */
interface PrivilegedCommandExecutor {

    /**
     * Executes [command] as an argv array and waits up to [timeoutMs]
     * milliseconds for it to finish.
     *
     * @param command argv array, already validated by the caller.
     *   Must not be empty.
     * @param timeoutMs maximum time to wait, in milliseconds. Must be
     *   positive; a non-positive value is rejected as an error result
     *   rather than being silently reinterpreted.
     */
    suspend fun execute(command: List<String>, timeoutMs: Long): CommandResult
}
