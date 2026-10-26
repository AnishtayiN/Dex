package com.dexboost.optimization

import com.dexboost.domain.model.CompilationMode

/**
 * Builds the exact argv for ART compilation commands.
 *
 * The command is `cmd package compile -m <filter> -f <package>` for
 * VERIFY, SPEED_PROFILE and SPEED, and `cmd package compile --reset
 * <package>` for RESET. The `-m` filter comes only from [CompilationMode],
 * never from user text, and the package name is validated first, so no
 * caller-supplied string can alter the command shape.
 */
object ArtCommandBuilder {

    /**
     * Returns the command argv for [mode] applied to [packageName].
     * Throws [IllegalArgumentException] for an invalid package name.
     */
    fun build(packageName: String, mode: CompilationMode): List<String> {
        PackageNameValidator.requireValid(packageName)
        return if (mode.isReset) {
            listOf("cmd", "package", "compile", "--reset", packageName)
        } else {
            // artFilter is non-null for every non-RESET mode by construction.
            listOf("cmd", "package", "compile", "-m", mode.artFilter!!, "-f", packageName)
        }
    }
}
