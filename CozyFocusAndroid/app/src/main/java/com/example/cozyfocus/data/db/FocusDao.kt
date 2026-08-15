package com.cozyfocus.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT * FROM inventory_entries")
    fun getInventory(): Flow<List<InventoryEntryEntity>>

    @Query("SELECT * FROM inventory_entries WHERE cosmeticRaw = :cosmeticRaw LIMIT 1")
    suspend fun getInventoryEntry(cosmeticRaw: String): InventoryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInventoryEntry(entry: InventoryEntryEntity): Long

    @Query("SELECT * FROM coin_ledger_entries ORDER BY createdAt DESC")
    fun getCoinLedger(): Flow<List<CoinLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCoinLedgerEntry(entry: CoinLedgerEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_ledger_entries")
    suspend fun getCoinBalance(): Int

    @Transaction
    suspend fun completeSessionIfNeeded(session: FocusSessionEntity): Boolean {
        if (insertSession(session) == -1L) return false
        insertCoinLedgerEntry(
            CoinLedgerEntity(
                id = "reward-${session.id}",
                createdAt = session.completedAt,
                amount = session.coinsEarned,
                reason = "Completed focus session"
            )
        )
        return true
    }

    @Transaction
    suspend fun purchaseCosmeticIfAffordable(
        cosmeticRaw: String,
        price: Int,
        acquiredAt: Long
    ): Boolean {
        if (price < 0 || getCoinBalance() < price || getInventoryEntry(cosmeticRaw) != null) {
            return false
        }
        if (insertInventoryEntry(InventoryEntryEntity(cosmeticRaw, acquiredAt)) == -1L) {
            return false
        }
        insertCoinLedgerEntry(
            CoinLedgerEntity(
                id = "purchase-$cosmeticRaw",
                createdAt = acquiredAt,
                amount = -price,
                reason = "Purchased $cosmeticRaw"
            )
        )
        return true
    }
}
