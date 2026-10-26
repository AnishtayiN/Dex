package com.dexboost.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dexboost.data.model.OperationRecord
import kotlinx.coroutines.flow.Flow

/** Room DAO for [OperationRecord] history. */
@Dao
interface OperationHistoryDao {

    /** All records, newest first. */
    @Query("SELECT * FROM operation_history ORDER BY startedAtMs DESC")
    fun observeAll(): Flow<List<OperationRecord>>

    @Insert
    suspend fun insert(record: OperationRecord)

    @Query("DELETE FROM operation_history")
    suspend fun clearAll()

    @Query("DELETE FROM operation_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
