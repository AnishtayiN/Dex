package com.dexboost

import com.dexboost.shizuku.PrivilegedCommandService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the UserService command allow-list (defence in depth). */
class PrivilegedCommandServiceTest {

    @Test
    fun speedProfileCommandIsAllowed() {
        assertTrue(
            PrivilegedCommandService.isAllowedCommand(
                listOf("cmd", "package", "compile", "-m", "speed-profile", "-f", "com.example.game"),
            ),
        )
    }

    @Test
    fun speedAndVerifyAreAllowed() {
        assertTrue(
            PrivilegedCommandService.isAllowedCommand(
                listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.game"),
            ),
        )
        assertTrue(
            PrivilegedCommandService.isAllowedCommand(
                listOf("cmd", "package", "compile", "-m", "verify", "-f", "com.example.game"),
            ),
        )
    }

    @Test
    fun resetCommandIsAllowed() {
        assertTrue(
            PrivilegedCommandService.isAllowedCommand(
                listOf("cmd", "package", "compile", "--reset", "com.example.game"),
            ),
        )
    }

    @Test
    fun shellCommandsAreRefused() {
        assertFalse(PrivilegedCommandService.isAllowedCommand(listOf("sh", "-c", "id")))
        assertFalse(PrivilegedCommandService.isAllowedCommand(listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.game; id")))
        assertFalse(PrivilegedCommandService.isAllowedCommand(listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.game", "extra")))
        assertFalse(PrivilegedCommandService.isAllowedCommand(listOf("cmd", "package", "compile", "-m", "everything", "-f", "com.example.game")))
        assertFalse(PrivilegedCommandService.isAllowedCommand(emptyList()))
    }

    @Test
    fun invalidPackageNamesAreRefused() {
        assertFalse(
            PrivilegedCommandService.isAllowedCommand(
                listOf("cmd", "package", "compile", "--reset", "not a package"),
            ),
        )
    }
}
