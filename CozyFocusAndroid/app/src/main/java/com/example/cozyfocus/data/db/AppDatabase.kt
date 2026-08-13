package com.example.cozyfocus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FocusSessionEntity::class,
        InventoryEntryEntity::class,
        CoinLedgerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun focusDao(): FocusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase_Impl()
                INSTANCE = instance
                instance
            }
        }
    }
}
