package com.dexboost

import com.dexboost.domain.model.CompilationMode
import com.dexboost.optimization.ArtCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtCommandBuilderTest {

    @Test
    fun verifyBuildsVerifyCommand() {
        assertEquals(
            listOf("cmd", "package", "compile", "-m", "verify", "-f", "com.example.app"),
            ArtCommandBuilder.build("com.example.app", CompilationMode.VERIFY),
        )
    }

    @Test
    fun speedProfileBuildsSpeedProfileCommand() {
        assertEquals(
            listOf("cmd", "package", "compile", "-m", "speed-profile", "-f", "com.example.app"),
            ArtCommandBuilder.build("com.example.app", CompilationMode.SPEED_PROFILE),
        )
    }

    @Test
    fun speedBuildsSpeedCommand() {
        assertEquals(
            listOf("cmd", "package", "compile", "-m", "speed", "-f", "com.example.app"),
            ArtCommandBuilder.build("com.example.app", CompilationMode.SPEED),
        )
    }

    @Test
    fun resetBuildsResetCommandWithNoFilterFlag() {
        assertEquals(
            listOf("cmd", "package", "compile", "--reset", "com.example.app"),
            ArtCommandBuilder.build("com.example.app", CompilationMode.RESET),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPackageIsRejectedForVerify() {
        ArtCommandBuilder.build("com.example.app; rm -rf /", CompilationMode.VERIFY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPackageIsRejectedForReset() {
        ArtCommandBuilder.build("Not A Package", CompilationMode.RESET)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPackageIsRejected() {
        ArtCommandBuilder.build("", CompilationMode.SPEED)
    }

    @Test
    fun modeArgumentsNeverComeFromRawText() {
        // The -m value must be exactly the enum's filter, even if a
        // caller tried to smuggle text through the package name slot.
        // (Such a package is rejected outright; a valid package yields
        // only the enum filter.)
        for (mode in CompilationMode.entries) {
            val command = ArtCommandBuilder.build("com.example.app", mode)
            if (mode.isReset) {
                assertEquals(false, command.contains("-m"))
            } else {
                val filterIndex = command.indexOf("-m")
                assertEquals(mode.artFilter, command[filterIndex + 1])
            }
        }
    }
}
