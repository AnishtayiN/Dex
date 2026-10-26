package com.dexboost.data.repository

import com.dexboost.data.model.OperationRecord
import com.dexboost.domain.model.CompilationMode
import com.dexboost.domain.model.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Persists ART compilation results as history records.
 *
 * Shell output is truncated to [MAX_OUTPUT_CHARS] characters per stream
 * before insert, so a verbose command can never grow the database
 * without bound. The mode is stored as the [CompilationMode] enum name
 * and the outcome as the name produced by `CommandResult.interpret()`.
 */
class OperationHistoryRepository(
    private val dao: OperationHistoryDao,
) {

    /** All history records, newest first. */
    val history: Flow<List<OperationRecord>>
        get() = dao.observeAll()

    /** Same as [history], as a function for callers that prefer it. */
    fun observeAll(): Flow<List<OperationRecord>> = dao.observeAll()

    suspend fun insert(record: OperationRecord) {
        withContext(Dispatchers.IO) {
            dao.insert(record)
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
        }
    }

    suspend fun deleteById(id: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteById(id)
        }
    }

    /**
     * Builds a record from a finished command. [durationMs] comes from
     * the executor-measured [CommandResult.durationMs]; [startedAtMs]
     * and [endedAtMs] are the caller-observed wall-clock bounds of the
     * operation.
     */
    fun recordFrom(
        packageName: String,
        mode: CompilationMode,
        result: CommandResult,
        startedAtMs: Long,
        endedAtMs: Long,
    ): OperationRecord {
        return OperationRecord(
            packageName = packageName,
            mode = mode.name,
            outcome = result.interpret().name,
            exitCode = result.exitCode,
            stdout = result.stdout.take(MAX_OUTPUT_CHARS),
            stderr = result.stderr.take(MAX_OUTPUT_CHARS),
            durationMs = result.durationMs,
            startedAtMs = startedAtMs,
            endedAtMs = endedAtMs,
        )
    }

    companion object {
        const val MAX_OUTPUT_CHARS = 4000
    }
}
