package com.dexboost.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dexboost.ui.screens.AppDetailsScreen
import com.dexboost.ui.screens.AppsScreen
import com.dexboost.ui.screens.HistoryScreen
import com.dexboost.ui.screens.HomeScreen
import com.dexboost.ui.screens.QueueScreen
import com.dexboost.ui.screens.SettingsScreen
import com.dexboost.ui.screens.ShizukuSetupScreen
import com.dexboost.viewmodel.AppsViewModel
import com.dexboost.viewmodel.OptimizationViewModel
import com.dexboost.viewmodel.SettingsViewModel
import com.dexboost.viewmodel.ShizukuViewModel

object DexBoostRoutes {
    const val HOME = "home"
    const val APPS = "apps"
    const val DETAILS = "details/{packageName}"
    const val QUEUE = "queue"
    const val HISTORY = "history"
    const val SETUP = "setup"
    const val SETTINGS = "settings"

    /** Package names contain only [a-z0-9_.] so they are safe as a path argument. */
    fun details(packageName: String): String = "details/$packageName"
}

@Composable
fun DexBoostNavGraph(
    navController: NavHostController,
    appsViewModel: AppsViewModel,
    shizukuViewModel: ShizukuViewModel,
    optimizationViewModel: OptimizationViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = DexBoostRoutes.HOME,
        modifier = modifier
    ) {
        composable(DexBoostRoutes.HOME) {
            HomeScreen(
                appsViewModel = appsViewModel,
                shizukuViewModel = shizukuViewModel,
                optimizationViewModel = optimizationViewModel,
                onNavigate = { navController.navigate(it) }
            )
        }
        composable(DexBoostRoutes.APPS) {
            AppsScreen(
                appsViewModel = appsViewModel,
                optimizationViewModel = optimizationViewModel,
                onAppClick = { navController.navigate(DexBoostRoutes.details(it)) },
                onQueueNavigate = { navController.navigate(DexBoostRoutes.QUEUE) }
            )
        }
        composable(
            route = DexBoostRoutes.DETAILS,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            AppDetailsScreen(
                packageName = packageName,
                appsViewModel = appsViewModel,
                optimizationViewModel = optimizationViewModel,
                onQueueNavigate = { navController.navigate(DexBoostRoutes.QUEUE) }
            )
        }
        composable(DexBoostRoutes.QUEUE) {
            QueueScreen(optimizationViewModel = optimizationViewModel)
        }
        composable(DexBoostRoutes.HISTORY) {
            HistoryScreen(optimizationViewModel = optimizationViewModel)
        }
        composable(DexBoostRoutes.SETUP) {
            ShizukuSetupScreen(shizukuViewModel = shizukuViewModel)
        }
        composable(DexBoostRoutes.SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                optimizationViewModel = optimizationViewModel
            )
        }
    }
}
