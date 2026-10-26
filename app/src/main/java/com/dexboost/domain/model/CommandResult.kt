package com.dexboost.domain.model

/**
 * Raw outcome of one privileged shell command.
 *
 * This is pure data: [interpret] performs no I/O and never claims more
 * than the command's own exit code and output support.
 *
 * @property exitCode Process exit code, or null if the command never
 *   produced one (for example, it was cancelled before finishing).
 * @property timedOut True when the executor gave up waiting for output.
 * @property cancelled True when the work was cancelled by the caller.
 */
data class CommandResult(
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean,
    val durationMs: Long,
    val cancelled: Boolean = false,
) {
    /**
     * Classifies this result.
     *
     * Rules, in order:
     * - [cancelled] -> [CommandOutcome.CANCELLED]
     * - [timedOut] -> [CommandOutcome.TIMEOUT_UNKNOWN] (the command may
     *   still have run; its real outcome is unknown)
     * - exit code 0 -> [CommandOutcome.CONFIRMED]. Empty output is not a
     *   failure, but it is also not extra proof of anything beyond the
     *   exit code itself.
     * - non-zero exit code -> [CommandOutcome.PERMISSION_DENIED] if the
     *   output mentions a permission problem, [CommandOutcome.UNSUPPORTED]
     *   if it mentions an unknown/unsupported command, otherwise
     *   [CommandOutcome.FAILED].
     * - null exit code (and not cancelled/timed out) -> [CommandOutcome.UNKNOWN].
     */
    fun interpret(): CommandOutcome {
        if (cancelled) return CommandOutcome.CANCELLED
        if (timedOut) return CommandOutcome.TIMEOUT_UNKNOWN
        val code = exitCode
        if (code == 0) return CommandOutcome.CONFIRMED
        if (code != null) {
            val combined = (stdout + "\n" + stderr).lowercase()
            if (
                combined.contains("permission denied") ||
                    combined.contains("not allowed") ||
                    combined.contains("securityexception")
            ) {
                return CommandOutcome.PERMISSION_DENIED
            }
            if (
                combined.contains("unknown command") ||
                    combined.contains("no such") ||
                    combined.contains("not found") ||
                    combined.contains("unsupported") ||
                    combined.contains("unrecognized")
            ) {
                return CommandOutcome.UNSUPPORTED
            }
            return CommandOutcome.FAILED
        }
        return CommandOutcome.UNKNOWN
    }
}

/** Classification of a [CommandResult] produced by [CommandResult.interpret]. */
enum class CommandOutcome {
    CONFIRMED,
    FAILED,
    UNSUPPORTED,
    PERMISSION_DENIED,
    TIMEOUT_UNKNOWN,
    CANCELLED,
    UNKNOWN,
}
