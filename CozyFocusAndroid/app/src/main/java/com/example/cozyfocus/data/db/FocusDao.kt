package com.example.cozyfocus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Query("SELECT * FROM inventory_entries")
    fun getInventory(): Flow<List<InventoryEntryEntity>>

    @Query("SELECT * FROM inventory_entries WHERE cosmeticRaw = :cosmeticRaw LIMIT 1")
    suspend fun getInventoryEntry(cosmeticRaw: String): InventoryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryEntry(entry: InventoryEntryEntity)

    @Query("SELECT * FROM coin_ledger_entries ORDER BY createdAt DESC")
    fun getCoinLedger(): Flow<List<CoinLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoinLedgerEntry(entry: CoinLedgerEntity)
}
