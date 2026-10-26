package com.dexboost.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Keys for the DataStore settings. Values:
 * language: "en" | "fa" | "system"
 * theme: "dark" | "light" | "system"
 * defaultMode: a [com.dexboost.domain.model.CompilationMode] name, e.g. "SPEED_PROFILE"
 */
object PreferencesKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
    val DEFAULT_MODE = stringPreferencesKey("default_mode")
    val REQUIRE_CONFIRMATION = booleanPreferencesKey("require_confirmation")
}

/**
 * Names of the synchronous SharedPreferences mirror. Only language and theme are
 * mirrored, because MainActivity must apply the locale before setContent and
 * DataStore reads are asynchronous. DataStore remains the source of truth for
 * the settings Flow; the mirror is a read cache for locale application only
 * and is rewritten on every language/theme setter call.
 */
object SyncSettingsPrefs {
    const val NAME = "dexboost_settings_sync"
    const val KEY_LANGUAGE = "language"
    const val KEY_THEME = "theme"
}

private val Context.dexBoostDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dexboost_settings"
)

/** User settings stored in DataStore (plus the sync mirror described above). */
data class Settings(
    val language: String = "system",
    val theme: String = "system",
    val defaultMode: String = "SPEED_PROFILE",
    val requireConfirmation: Boolean = true
)

class UserPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.dexBoostDataStore

    val settings: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            Settings(
                language = prefs[PreferencesKeys.LANGUAGE] ?: "system",
                theme = prefs[PreferencesKeys.THEME] ?: "system",
                defaultMode = prefs[PreferencesKeys.DEFAULT_MODE] ?: "SPEED_PROFILE",
                requireConfirmation = prefs[PreferencesKeys.REQUIRE_CONFIRMATION] ?: true
            )
        }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = language }
        appContext.getSharedPreferences(SyncSettingsPrefs.NAME, Context.MODE_PRIVATE)
            .edit().putString(SyncSettingsPrefs.KEY_LANGUAGE, language).apply()
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme }
        appContext.getSharedPreferences(SyncSettingsPrefs.NAME, Context.MODE_PRIVATE)
            .edit().putString(SyncSettingsPrefs.KEY_THEME, theme).apply()
    }

    suspend fun setDefaultMode(defaultMode: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_MODE] = defaultMode }
    }

    suspend fun setRequireConfirmation(requireConfirmation: Boolean) {
        dataStore.edit { it[PreferencesKeys.REQUIRE_CONFIRMATION] = requireConfirmation }
    }
}
