package com.dexboost

import android.app.Application
import com.dexboost.apps.InstalledAppsRepository
import com.dexboost.apps.PackageManagerInstalledAppsRepository
import com.dexboost.data.preferences.UserPreferences
import com.dexboost.data.repository.DexBoostDatabase
import com.dexboost.data.repository.OperationHistoryRepository
import com.dexboost.data.repository.buildDatabase
import com.dexboost.optimization.ArtCompilationManager
import com.dexboost.shizuku.ShizukuCommandExecutor
import com.dexboost.shizuku.ShizukuManager

/**
 * Holds the app singletons. ViewModels receive these through
 * DexBoostViewModelFactory; no DI framework is used. The database uses
 * the sibling module's buildDatabase() helper.
 */
class DexBoostApplication : Application() {

    val database: DexBoostDatabase by lazy {
        buildDatabase(applicationContext)
    }

    val historyRepository: OperationHistoryRepository by lazy {
        OperationHistoryRepository(database.historyDao())
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(applicationContext)
    }

    val shizukuManager: ShizukuManager by lazy {
        ShizukuManager()
    }

    val commandExecutor: ShizukuCommandExecutor by lazy {
        ShizukuCommandExecutor(shizukuManager, applicationContext)
    }

    val compilationManager: ArtCompilationManager by lazy {
        ArtCompilationManager(commandExecutor)
    }

    val appsRepository: InstalledAppsRepository by lazy {
        PackageManagerInstalledAppsRepository(applicationContext)
    }
}
