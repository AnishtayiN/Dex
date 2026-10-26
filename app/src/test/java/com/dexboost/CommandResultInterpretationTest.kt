package com.dexboost

import com.dexboost.domain.model.CommandOutcome
import com.dexboost.domain.model.CommandResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandResultInterpretationTest {

    private fun result(
        exitCode: Int?,
        stdout: String = "",
        stderr: String = "",
        timedOut: Boolean = false,
        cancelled: Boolean = false,
    ): CommandResult = CommandResult(
        exitCode = exitCode,
        stdout = stdout,
        stderr = stderr,
        timedOut = timedOut,
        durationMs = 10L,
        cancelled = cancelled,
    )

    @Test
    fun exitZeroIsConfirmed() {
        assertEquals(CommandOutcome.CONFIRMED, result(0, stdout = "Success").interpret())
    }

    @Test
    fun exitZeroWithEmptyOutputIsConfirmedNotFailure() {
        assertEquals(CommandOutcome.CONFIRMED, result(0).interpret())
    }

    @Test
    fun cancelledBeatsEverything() {
        assertEquals(
            CommandOutcome.CANCELLED,
            result(0, timedOut = true, cancelled = true).interpret(),
        )
        assertEquals(
            CommandOutcome.CANCELLED,
            result(null, cancelled = true).interpret(),
        )
    }

    @Test
    fun timeoutIsTimeoutUnknown() {
        assertEquals(
            CommandOutcome.TIMEOUT_UNKNOWN,
            result(null, timedOut = true).interpret(),
        )
        assertEquals(
            CommandOutcome.TIMEOUT_UNKNOWN,
            result(0, timedOut = true).interpret(),
        )
    }

    @Test
    fun permissionDeniedIsDetectedCaseInsensitively() {
        assertEquals(
            CommandOutcome.PERMISSION_DENIED,
            result(1, stderr = "Permission denied").interpret(),
        )
        assertEquals(
            CommandOutcome.PERMISSION_DENIED,
            result(1, stdout = "PERMISSION DENIED").interpret(),
        )
        assertEquals(
            CommandOutcome.PERMISSION_DENIED,
            result(1, stderr = "Operation not allowed").interpret(),
        )
        assertEquals(
            CommandOutcome.PERMISSION_DENIED,
            result(1, stderr = "java.lang.SecurityException: denied").interpret(),
        )
    }

    @Test
    fun unsupportedIsDetected() {
        assertEquals(
            CommandOutcome.UNSUPPORTED,
            result(1, stderr = "Unknown command: compile").interpret(),
        )
        assertEquals(
            CommandOutcome.UNSUPPORTED,
            result(1, stderr = "No such file or directory").interpret(),
        )
        assertEquals(
            CommandOutcome.UNSUPPORTED,
            result(1, stderr = "cmd: not found").interpret(),
        )
        assertEquals(
            CommandOutcome.UNSUPPORTED,
            result(1, stderr = "Unsupported operation").interpret(),
        )
        assertEquals(
            CommandOutcome.UNSUPPORTED,
            result(1, stderr = "Unrecognized option").interpret(),
        )
    }

    @Test
    fun genericNonZeroIsFailed() {
        assertEquals(
            CommandOutcome.FAILED,
            result(1, stderr = "Something went wrong").interpret(),
        )
        assertEquals(
            CommandOutcome.FAILED,
            result(255).interpret(),
        )
    }

    @Test
    fun nullExitCodeWithoutCancelOrTimeoutIsUnknown() {
        assertEquals(CommandOutcome.UNKNOWN, result(null).interpret())
    }
}
