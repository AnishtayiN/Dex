package com.dexboost.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.dexboost.data.preferences.SyncSettingsPrefs
import java.util.Locale

/**
 * Applies a stored language override ("en" / "fa") to a Context.
 * "system" (or a blank value) means no override: the Context is returned unchanged.
 * Uses the API 26+ path: Configuration.setLocale + LocaleList + createConfigurationContext.
 */
fun applyLanguage(context: Context, languageCode: String): Context {
    if (languageCode.isBlank() || languageCode == "system") return context
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    config.setLocales(LocaleList(locale))
    return context.createConfigurationContext(config)
}

/** Reads the synchronously mirrored language, defaulting to "system". */
fun readStoredLanguage(context: Context): String {
    return context.getSharedPreferences(SyncSettingsPrefs.NAME, Context.MODE_PRIVATE)
        .getString(SyncSettingsPrefs.KEY_LANGUAGE, "system") ?: "system"
}

/** Reads the synchronously mirrored theme, defaulting to "system". */
fun readStoredTheme(context: Context): String {
    return context.getSharedPreferences(SyncSettingsPrefs.NAME, Context.MODE_PRIVATE)
        .getString(SyncSettingsPrefs.KEY_THEME, "system") ?: "system"
}
