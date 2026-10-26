package com.dexboost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexboost.data.preferences.UserPreferences
import com.dexboost.data.repository.OperationHistoryRepository
import com.dexboost.domain.model.CompilationMode
import com.dexboost.optimization.ArtCompilationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives ART compilation batches and history.
 *
 * History recording approximation (documented, as specified): [ArtCompilationManager]
 * reports finished items through its onRecord callback as
 * (packageName, mode, result), but the manager does not expose a per-item
 * start time to callers. [startBatch] therefore captures one batch start
 * time and uses it as startedAtMs for every item in that batch, with
 * System.currentTimeMillis() as endedAtMs when the item's record arrives.
 * Durations recorded this way are batch-relative, not per-app precise, and
 * the per-item durationMs still comes from the item's own CommandResult.
 * [OperationHistoryRepository.recordFrom] builds the record (outcome via
 * interpret()) and [OperationHistoryRepository.insert] persists it.
 */
class OptimizationViewModel(
    private val compilationManager: ArtCompilationManager,
    private val historyRepository: OperationHistoryRepository,
    preferences: UserPreferences
) : ViewModel() {

    val queueState = compilationManager.queueState

    // Type is inferred from the repository so this file does not hard-code
    // the OperationRecord entity package.
    val history = historyRepository.history

    val defaultMode: StateFlow<CompilationMode> = preferences.settings
        .map { settings ->
            CompilationMode.fromName(settings.defaultMode) ?: CompilationMode.SPEED_PROFILE
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompilationMode.SPEED_PROFILE)

    val requireConfirmation: StateFlow<Boolean> = preferences.settings
        .map { it.requireConfirmation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun startBatch(
        packages: List<String>,
        mode: CompilationMode,
        timeoutMsPerApp: Long = 300_000L
    ) {
        if (packages.isEmpty()) return
        val batchStartMs = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                compilationManager.runBatch(packages, mode, timeoutMsPerApp) { packageName, itemMode, result ->
                    val endedAtMs = System.currentTimeMillis()
                    viewModelScope.launch {
                        try {
                            val record = historyRepository.recordFrom(
                                packageName,
                                itemMode,
                                result,
                                batchStartMs,
                                endedAtMs
                            )
                            historyRepository.insert(record)
                        } catch (e: Exception) {
                            // History must never break compilation reporting; the
                            // queue state itself still shows the real result.
                        }
                    }
                }
            } catch (e: Exception) {
                // A second concurrent batch throws IllegalStateException, and an
                // externally cancelled batch rethrows CancellationException; the
                // queue state already reflects what happened, so there is
                // nothing further to report from here without inventing data.
            }
        }
    }

    fun cancelBatch() {
        compilationManager.cancel()
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
