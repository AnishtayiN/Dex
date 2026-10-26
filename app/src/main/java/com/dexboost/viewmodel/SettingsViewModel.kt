package com.dexboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexboost.data.preferences.Settings
import com.dexboost.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: UserPreferences
) : ViewModel() {

    val settings: Flow<Settings> = preferences.settings

    fun setLanguage(language: String) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { preferences.setTheme(theme) }
    }

    fun setDefaultMode(defaultMode: String) {
        viewModelScope.launch { preferences.setDefaultMode(defaultMode) }
    }

    fun setRequireConfirmation(requireConfirmation: Boolean) {
        viewModelScope.launch { preferences.setRequireConfirmation(requireConfirmation) }
    }
}
