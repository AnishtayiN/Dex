package com.dexboost.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recorded ART compilation operation, stored for history display.
 *
 * [mode] stores the [com.dexboost.domain.model.CompilationMode] enum name
 * (for example "SPEED"); [outcome] stores the
 * [com.dexboost.domain.model.CommandOutcome] enum name produced by
 * `CommandResult.interpret()`. [stdout] and [stderr] are truncated to
 * 4000 characters each by the repository before insert, so history rows
 * never hold unbounded shell output.
 */
@Entity(tableName = "operation_history")
data class OperationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val mode: String,
    val outcome: String,
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
)
