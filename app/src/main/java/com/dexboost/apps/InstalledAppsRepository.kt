package com.dexboost.apps

import com.dexboost.domain.model.InstalledApp

/**
 * Loads installed apps from the device. Implementations use
 * PackageManager only — never shell commands.
 */
interface InstalledAppsRepository {

    /**
     * Loads installed apps sorted by label (case-insensitive).
     * DexBoost itself is always excluded. When [includeSystem] is false,
     * system apps are excluded as well.
     */
    suspend fun loadApps(includeSystem: Boolean): List<InstalledApp>

    /**
     * Loads a single app by package name, or null when it is not
     * installed, the name is invalid, or it is DexBoost itself.
     */
    suspend fun loadApp(packageName: String): InstalledApp?
}
