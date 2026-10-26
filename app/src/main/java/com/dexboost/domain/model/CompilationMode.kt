package com.dexboost.domain.model

/**
 * ART compilation filter for `cmd package compile`.
 *
 * The [artFilter] value is the only string ever passed to the shell command
 * (see ArtCommandBuilder). It always comes from this enum, never from user
 * text, so a caller cannot inject an arbitrary `-m` argument.
 *
 * Shizuku provides ADB/shell-level access only. Nothing here claims root
 * access or any guaranteed performance, temperature, ping, or GPU gain.
 */
enum class CompilationMode(val artFilter: String?) {
    VERIFY("verify"),
    SPEED_PROFILE("speed-profile"),
    SPEED("speed"),

    /** Resets compilation state; has no `-m` filter. */
    RESET(null);

    /** True only for [RESET]. */
    val isReset: Boolean
        get() = this == RESET

    companion object {
        /**
         * Parses a mode name case-insensitively, or returns null for an
         * unknown name. Accepts the enum name (e.g. "speed-profile") and
         * the ART filter value; "_" and "-" are treated equivalently so
         * "speed_profile" also resolves to [SPEED_PROFILE]. A null input
         * returns null instead of throwing.
         */
        fun fromName(name: String?): CompilationMode? {
            if (name == null) return null
            val normalized = name.trim().lowercase().replace('_', '-')
            if (normalized.isEmpty()) return null
            return entries.firstOrNull { entry ->
                entry.name.lowercase() == normalized || entry.artFilter == normalized
            }
        }
    }
}
