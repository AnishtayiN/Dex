package com.dexboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexboost.apps.InstalledAppsRepository
import com.dexboost.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppsSort {
    NAME_ASC, NAME_DESC, INSTALL_NEWEST, INSTALL_OLDEST
}

data class AppsUiState(
    /** All apps loaded for the current includeSystem setting, before filtering. */
    val allApps: List<InstalledApp> = emptyList(),
    /** Apps after search / games-only filtering and sorting. */
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val gamesOnly: Boolean = false,
    val includeSystem: Boolean = false,
    val sort: AppsSort = AppsSort.NAME_ASC,
    val selectedPackages: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    /** Plain English technical message; the UI shows it as-is when no mapped string fits. */
    val error: String? = null
)

class AppsViewModel(
    private val appsRepository: InstalledAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState(isLoading = true))
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val includeSystem = _uiState.value.includeSystem
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val loaded = withContext(Dispatchers.IO) {
                    appsRepository.loadApps(includeSystem)
                }
                _uiState.update { state ->
                    state.copy(allApps = loaded, isLoading = false)
                        .let { applyFilter(it) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    /** Alias for setQuery, kept because callers search by this name. */
    fun search(query: String) = setQuery(query)

    fun setQuery(query: String) {
        _uiState.update { applyFilter(it.copy(query = query)) }
    }

    fun setGamesOnly(gamesOnly: Boolean) {
        _uiState.update { applyFilter(it.copy(gamesOnly = gamesOnly)) }
    }

    fun setIncludeSystem(includeSystem: Boolean) {
        if (includeSystem == _uiState.value.includeSystem) return
        _uiState.update { it.copy(includeSystem = includeSystem) }
        load()
    }

    fun setSort(sort: AppsSort) {
        _uiState.update { applyFilter(it.copy(sort = sort)) }
    }

    fun toggleSelection(packageName: String) {
        _uiState.update { state ->
            val selected = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = selected)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedPackages = it.apps.map { app -> app.packageName }.toSet()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackages = emptySet()) }
    }

    /** Returns null when the app cannot be loaded; never throws to the UI. */
    suspend fun getApp(packageName: String): InstalledApp? {
        return try {
            withContext(Dispatchers.IO) { appsRepository.loadApp(packageName) }
        } catch (e: Exception) {
            null
        }
    }

    private fun applyFilter(state: AppsUiState): AppsUiState {
        val query = state.query.trim()
        var filtered = state.allApps
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
        if (state.gamesOnly) {
            filtered = filtered.filter { it.isGame }
        }
        filtered = when (state.sort) {
            AppsSort.NAME_ASC -> filtered.sortedBy { it.label.lowercase() }
            AppsSort.NAME_DESC -> filtered.sortedByDescending { it.label.lowercase() }
            AppsSort.INSTALL_NEWEST -> filtered.sortedByDescending { it.installedTimeMs }
            AppsSort.INSTALL_OLDEST -> filtered.sortedBy { it.installedTimeMs }
        }
        // Drop selections that are no longer visible/loaded.
        val visible = filtered.map { it.packageName }.toSet()
        return state.copy(apps = filtered, selectedPackages = state.selectedPackages intersect visible)
    }
}
