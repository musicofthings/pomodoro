package com.example.cozyfocus.data.db

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class AppDatabase_Impl : AppDatabase() {

    private val sessionsState = MutableStateFlow<List<FocusSessionEntity>>(emptyList())
    private val inventoryState = MutableStateFlow<List<InventoryEntryEntity>>(emptyList())
    private val coinLedgerState = MutableStateFlow<List<CoinLedgerEntity>>(emptyList())

    private val daoImpl = object : FocusDao {
        override fun getAllSessions(): Flow<List<FocusSessionEntity>> = sessionsState

        override suspend fun insertSession(session: FocusSessionEntity) {
            sessionsState.value = listOf(session) + sessionsState.value
        }

        override fun getInventory(): Flow<List<InventoryEntryEntity>> = inventoryState

        override suspend fun getInventoryEntry(cosmeticRaw: String): InventoryEntryEntity? {
            return inventoryState.value.firstOrNull { it.cosmeticRaw.equals(cosmeticRaw, ignoreCase = true) }
        }

        override suspend fun insertInventoryEntry(entry: InventoryEntryEntity) {
            inventoryState.value = inventoryState.value + entry
        }

        override fun getCoinLedger(): Flow<List<CoinLedgerEntity>> = coinLedgerState

        override suspend fun insertCoinLedgerEntry(entry: CoinLedgerEntity) {
            coinLedgerState.value = listOf(entry) + coinLedgerState.value
        }
    }

    override fun focusDao(): FocusDao = daoImpl

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        throw UnsupportedOperationException("In-memory implementation")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return InvalidationTracker(this, "focus_sessions", "inventory_entries", "coin_ledger_entries")
    }

    override fun clearAllTables() {
        sessionsState.value = emptyList()
        inventoryState.value = emptyList()
        coinLedgerState.value = emptyList()
    }
}
