package com.dexboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dexboost.apps.InstalledAppsRepository
import com.dexboost.data.preferences.UserPreferences
import com.dexboost.data.repository.OperationHistoryRepository
import com.dexboost.optimization.ArtCompilationManager
import com.dexboost.shizuku.ShizukuManager

/**
 * Simple manual factory (no DI framework). MainActivity builds one instance
 * from the DexBoostApplication singletons and hands it to ViewModelProvider.
 */
class DexBoostViewModelFactory(
    private val appsRepository: InstalledAppsRepository,
    private val shizukuManager: ShizukuManager,
    private val compilationManager: ArtCompilationManager,
    private val historyRepository: OperationHistoryRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppsViewModel::class.java) ->
                AppsViewModel(appsRepository) as T
            modelClass.isAssignableFrom(ShizukuViewModel::class.java) ->
                ShizukuViewModel(shizukuManager) as T
            modelClass.isAssignableFrom(OptimizationViewModel::class.java) ->
                OptimizationViewModel(compilationManager, historyRepository, userPreferences) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(userPreferences) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
