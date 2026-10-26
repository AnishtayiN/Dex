package com.dexboost.domain.model

import android.graphics.drawable.Drawable

/**
 * An app installed on the device, as shown in the app list.
 *
 * @property icon Loaded icon, or null when it could not be loaded. This is
 *   UI-only data; callers must never rely on it for identity, filtering,
 *   sorting, or equality-sensitive logic beyond this data class's default
 *   behaviour. The repository loads it best-effort and never crashes on a
 *   missing icon.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isGame: Boolean,
    val isSystem: Boolean,
    val versionName: String?,
    val installedTimeMs: Long,
    val icon: Drawable? = null,
)
