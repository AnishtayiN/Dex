package com.dexboost.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.dexboost.domain.model.InstalledApp
import com.dexboost.optimization.PackageNameValidator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [InstalledAppsRepository] backed by [PackageManager].
 *
 * Icons are loaded here via [ApplicationInfo.loadIcon], best-effort:
 * a missing or unloadable icon yields a null icon, never a crash.
 * (Callers that prefer to defer icon loading may ignore [InstalledApp.icon]
 * and load icons on demand via PackageManager themselves.)
 *
 * DexBoost itself ("com.dexboost") is always excluded from listings.
 * All PackageManager work runs on Dispatchers.IO.
 */
class PackageManagerInstalledAppsRepository(context: Context) : InstalledAppsRepository {

    private val appContext: Context = context.applicationContext
    private val packageManager: PackageManager
        get() = appContext.packageManager

    override suspend fun loadApps(includeSystem: Boolean): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val pm = packageManager
            val applications = getInstalledApplications(pm)
            applications
                .asSequence()
                .filter { it.packageName != DEXBOOST_PACKAGE }
                .filter { includeSystem || !it.isSystemApp() }
                .mapNotNull { appInfo -> toInstalledApp(pm, appInfo) }
                .sortedBy { it.label.lowercase(Locale.ROOT) }
                .toList()
        }

    override suspend fun loadApp(packageName: String): InstalledApp? =
        withContext(Dispatchers.IO) {
            if (!PackageNameValidator.isValid(packageName)) return@withContext null
            if (packageName == DEXBOOST_PACKAGE) return@withContext null
            val pm = packageManager
            val appInfo = try {
                getApplicationInfo(pm, packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } catch (e: RuntimeException) {
                null
            } ?: return@withContext null
            toInstalledApp(pm, appInfo)
        }

    private fun toInstalledApp(pm: PackageManager, appInfo: ApplicationInfo): InstalledApp? {
        val packageName = appInfo.packageName
        val label = try {
            appInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotEmpty() }
        } catch (e: RuntimeException) {
            null
        } ?: packageName
        val packageInfo = try {
            getPackageInfo(pm, packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: RuntimeException) {
            null
        }
        val icon = try {
            appInfo.loadIcon(pm)
        } catch (e: RuntimeException) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
        return InstalledApp(
            packageName = packageName,
            label = label,
            isGame = appInfo.category == ApplicationInfo.CATEGORY_GAME,
            isSystem = appInfo.isSystemApp(),
            versionName = packageInfo?.versionName,
            // firstInstallTime has existed since API 9, so no SDK
            // branch is needed for it; it is read from PackageInfo
            // on every API level.
            installedTimeMs = packageInfo?.firstInstallTime ?: 0L,
            icon = icon,
        )
    }

    private fun ApplicationInfo.isSystemApp(): Boolean =
        (flags and ApplicationInfo.FLAG_SYSTEM) != 0

    /**
     * getInstalledApplications with a fallback: if the GET_META_DATA
     * query fails on a device, retry without flags rather than failing
     * the whole listing.
     */
    private fun getInstalledApplications(pm: PackageManager): List<ApplicationInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(
                        PackageManager.GET_META_DATA.toLong(),
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        } catch (e: RuntimeException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(
                        PackageManager.ApplicationInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(0)
                }
            } catch (e2: RuntimeException) {
                emptyList()
            }
        }
    }

    private fun getApplicationInfo(pm: PackageManager, packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong(),
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }
    }

    private fun getPackageInfo(pm: PackageManager, packageName: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
    }

    private companion object {
        const val DEXBOOST_PACKAGE = "com.dexboost"
    }
}
