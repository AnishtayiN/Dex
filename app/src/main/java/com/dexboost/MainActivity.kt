package com.dexboost

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.dexboost.data.preferences.Settings
import com.dexboost.ui.applyLanguage
import com.dexboost.ui.navigation.DexBoostNavGraph
import com.dexboost.ui.readStoredLanguage
import com.dexboost.ui.theme.DexBoostTheme
import com.dexboost.viewmodel.AppsViewModel
import com.dexboost.viewmodel.DexBoostViewModelFactory
import com.dexboost.viewmodel.OptimizationViewModel
import com.dexboost.viewmodel.SettingsViewModel
import com.dexboost.viewmodel.ShizukuViewModel

/**
 * Applies the synchronously mirrored language before content is created, then
 * hosts the Compose navigation graph with ViewModels from the manual factory.
 *
 * Shizuku permission primarily arrives through Shizuku's own listener inside
 * ShizukuManager. onRequestPermissionsResult is additionally forwarded to
 * ShizukuManager.onRequestPermissionResult with the raw grant result; the
 * manager itself ignores any request code other than its own (1001).
 * onActivityResult triggers a status refresh for the case where the user
 * returns from the Shizuku app or system settings.
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLanguage(newBase, readStoredLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DexBoostApplication
        val factory = DexBoostViewModelFactory(
            appsRepository = app.appsRepository,
            shizukuManager = app.shizukuManager,
            compilationManager = app.compilationManager,
            historyRepository = app.historyRepository,
            userPreferences = app.userPreferences
        )
        val appsViewModel = ViewModelProvider(this, factory)[AppsViewModel::class.java]
        val shizukuViewModel = ViewModelProvider(this, factory)[ShizukuViewModel::class.java]
        val optimizationViewModel =
            ViewModelProvider(this, factory)[OptimizationViewModel::class.java]
        val settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            val settings by app.userPreferences.settings.collectAsState(initial = Settings())
            val darkTheme = when (settings.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            DexBoostTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                DexBoostNavGraph(
                    navController = navController,
                    appsViewModel = appsViewModel,
                    shizukuViewModel = shizukuViewModel,
                    optimizationViewModel = optimizationViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val grantResult = grantResults.firstOrNull() ?: PackageManager.PERMISSION_DENIED
        (application as DexBoostApplication).shizukuManager
            .onRequestPermissionResult(requestCode, grantResult)
    }

    @Deprecated("Kept only to refresh Shizuku status after returning from another app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        (application as DexBoostApplication).shizukuManager.refresh()
    }
}
