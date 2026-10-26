package com.dexboost.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dexboost.data.model.OperationRecord

/** Room database for DexBoost. Version 1 is the first schema. */
@Database(
    entities = [OperationRecord::class],
    version = 1,
    exportSchema = true,
)
abstract class DexBoostDatabase : RoomDatabase() {

    abstract fun historyDao(): OperationHistoryDao
}

/**
 * Builds the DexBoost database.
 *
 * `fallbackToDestructiveMigration()` is kept deliberately for v1 only:
 * no prior schema exists, so there is no user data a migration could
 * destroy. It must be revisited (replaced with a real migration) before
 * any future schema version ships.
 */
fun buildDatabase(context: Context): DexBoostDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        DexBoostDatabase::class.java,
        DATABASE_NAME,
    )
        .fallbackToDestructiveMigration()
        .build()
}

private const val DATABASE_NAME = "dexboost.db"
