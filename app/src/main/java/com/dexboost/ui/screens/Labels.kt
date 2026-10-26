package com.dexboost.ui.screens

import com.dexboost.R
import com.dexboost.domain.model.CommandOutcome
import com.dexboost.domain.model.CompilationMode
import com.dexboost.optimization.ArtCompilationManager
import com.dexboost.shizuku.ShizukuState
import com.dexboost.ui.components.ChipTone

/** Maps a command outcome to its string resource and chip tone. */
fun outcomeLabelRes(outcome: CommandOutcome): Int = when (outcome) {
    CommandOutcome.CONFIRMED -> R.string.outcome_confirmed
    CommandOutcome.FAILED -> R.string.outcome_failed
    CommandOutcome.UNSUPPORTED -> R.string.outcome_unsupported
    CommandOutcome.PERMISSION_DENIED -> R.string.outcome_permission_denied
    CommandOutcome.TIMEOUT_UNKNOWN -> R.string.outcome_timeout_unknown
    CommandOutcome.CANCELLED -> R.string.outcome_cancelled
    CommandOutcome.UNKNOWN -> R.string.outcome_unknown
}

fun outcomeTone(outcome: CommandOutcome): ChipTone = when (outcome) {
    CommandOutcome.CONFIRMED -> ChipTone.POSITIVE
    CommandOutcome.FAILED, CommandOutcome.PERMISSION_DENIED -> ChipTone.NEGATIVE
    CommandOutcome.UNSUPPORTED, CommandOutcome.TIMEOUT_UNKNOWN -> ChipTone.WARNING
    CommandOutcome.CANCELLED, CommandOutcome.UNKNOWN -> ChipTone.NEUTRAL
}

/**
 * OperationRecord stores the outcome as the CommandOutcome enum name
 * (a String). Parses it back, defaulting to UNKNOWN for anything unrecognised.
 */
fun outcomeFromName(name: String): CommandOutcome {
    return try {
        CommandOutcome.valueOf(name)
    } catch (e: Exception) {
        CommandOutcome.UNKNOWN
    }
}

/**
 * OperationRecord stores the mode as the CompilationMode enum name.
 * Falls back to SPEED_PROFILE for anything unrecognised.
 */
fun modeFromName(name: String): CompilationMode {
    return CompilationMode.fromName(name) ?: CompilationMode.SPEED_PROFILE
}

fun shizukuLabelRes(state: ShizukuState): Int = when (state) {
    ShizukuState.Ready -> R.string.shizuku_status_ready
    ShizukuState.NotInstalled -> R.string.shizuku_status_not_installed
    ShizukuState.NotRunning -> R.string.shizuku_status_not_running
    ShizukuState.BinderDead -> R.string.shizuku_status_binder_dead
    ShizukuState.PermissionDenied -> R.string.shizuku_status_permission_denied
    ShizukuState.Unknown -> R.string.shizuku_status_unknown
}

fun shizukuTone(state: ShizukuState): ChipTone = when (state) {
    ShizukuState.Ready -> ChipTone.POSITIVE
    ShizukuState.PermissionDenied, ShizukuState.BinderDead -> ChipTone.NEGATIVE
    ShizukuState.NotInstalled, ShizukuState.NotRunning -> ChipTone.WARNING
    ShizukuState.Unknown -> ChipTone.NEUTRAL
}

fun modeNameRes(mode: CompilationMode): Int = when (mode) {
    CompilationMode.VERIFY -> R.string.mode_verify
    CompilationMode.SPEED_PROFILE -> R.string.mode_speed_profile
    CompilationMode.SPEED -> R.string.mode_speed
    CompilationMode.RESET -> R.string.mode_reset
}

fun modeDescRes(mode: CompilationMode): Int = when (mode) {
    CompilationMode.VERIFY -> R.string.mode_verify_desc
    CompilationMode.SPEED_PROFILE -> R.string.mode_speed_profile_desc
    CompilationMode.SPEED -> R.string.mode_speed_desc
    CompilationMode.RESET -> R.string.mode_reset_desc
}

fun queueStatusLabelRes(status: ArtCompilationManager.QueueStatus): Int = when (status) {
    ArtCompilationManager.QueueStatus.QUEUED -> R.string.queue_status_queued
    ArtCompilationManager.QueueStatus.RUNNING -> R.string.queue_status_running
    ArtCompilationManager.QueueStatus.DONE -> R.string.queue_status_done
    ArtCompilationManager.QueueStatus.CANCELLED -> R.string.queue_status_cancelled
}

fun queueStatusTone(status: ArtCompilationManager.QueueStatus): ChipTone = when (status) {
    ArtCompilationManager.QueueStatus.RUNNING -> ChipTone.INFO
    ArtCompilationManager.QueueStatus.DONE -> ChipTone.POSITIVE
    ArtCompilationManager.QueueStatus.CANCELLED -> ChipTone.NEUTRAL
    ArtCompilationManager.QueueStatus.QUEUED -> ChipTone.NEUTRAL
}
