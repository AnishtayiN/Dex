package com.dexboost.optimization

/**
 * Validates Android package names before they are ever used in a shell
 * command, and escapes arbitrary strings for single-quoted shell use.
 *
 * A valid package name matches `^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$`,
 * is at most 255 characters in total, and has no segment longer than
 * 63 characters. The character class alone excludes every shell
 * metacharacter (`;`, `$`, backtick, `|`, `&`, whitespace, quotes, ...),
 * so a validated name is safe to pass as a single argv element.
 */
object PackageNameValidator {

    private val PACKAGE_NAME_REGEX =
        Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

    private const val MAX_TOTAL_LENGTH = 255
    private const val MAX_SEGMENT_LENGTH = 63

    /** Returns true only for a structurally valid package name. */
    fun isValid(packageName: String): Boolean {
        if (packageName.isEmpty() || packageName.length > MAX_TOTAL_LENGTH) return false
        if (!PACKAGE_NAME_REGEX.matches(packageName)) return false
        return packageName.split('.').all { it.length <= MAX_SEGMENT_LENGTH }
    }

    /**
     * Returns normally for a valid package name, otherwise throws
     * [IllegalArgumentException]. Callers must validate before building
     * any shell command.
     */
    fun requireValid(packageName: String) {
        require(isValid(packageName)) { "Invalid package name" }
    }

    /**
     * Escapes an arbitrary string for use as one single-quoted shell
     * argument: wraps it in single quotes and replaces each embedded
     * single quote with `'\''` (close quote, escaped quote, reopen quote).
     *
     * This is a general-purpose escaping helper. It is used only for
     * already-validated package names in practice; validated names never
     * contain characters that need escaping, but the function itself is
     * correct for arbitrary input.
     */
    fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }
}
