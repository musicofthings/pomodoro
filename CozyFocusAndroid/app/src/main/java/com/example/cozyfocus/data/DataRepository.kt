package com.example.cozyfocus.data

import android.content.Context
import com.example.cozyfocus.data.db.AppDatabase
import com.example.cozyfocus.data.db.CoinLedgerEntity
import com.example.cozyfocus.data.db.FocusSessionEntity
import com.example.cozyfocus.data.db.InventoryEntryEntity
import com.example.cozyfocus.data.preferences.TimerPreferences
import com.example.cozyfocus.data.preferences.TimerState
import kotlinx.coroutines.flow.Flow

class DataRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.focusDao()
    val preferences = TimerPreferences(context)

    val timerState: Flow<TimerState> = preferences.timerStateFlow
    val sessions: Flow<List<FocusSessionEntity>> = dao.getAllSessions()
    val inventory: Flow<List<InventoryEntryEntity>> = dao.getInventory()
    val coinLedger: Flow<List<CoinLedgerEntity>> = dao.getCoinLedger()

    suspend fun completeSession(durationSeconds: Long, companionRaw: String) {
        val session = FocusSessionEntity(
            durationSeconds = durationSeconds,
            companionRaw = companionRaw,
            coinsEarned = 5
        )
        dao.insertSession(session)
        dao.insertCoinLedgerEntry(
            CoinLedgerEntity(
                amount = session.coinsEarned,
                reason = "Completed focus session"
            )
        )
        preferences.resetTimer()
    }

    suspend fun purchaseCosmetic(cosmeticRaw: String, price: Int, currentBalance: Int): Boolean {
        if (currentBalance < price) return false
        if (dao.getInventoryEntry(cosmeticRaw) != null) return false

        val inventoryEntry = InventoryEntryEntity(cosmeticRaw = cosmeticRaw)
        val debitEntry = CoinLedgerEntity(amount = -price, reason = "Purchased $cosmeticRaw")

        dao.insertInventoryEntry(inventoryEntry)
        dao.insertCoinLedgerEntry(debitEntry)
        preferences.setEquippedCosmetic(cosmeticRaw)
        return true
    }
}
