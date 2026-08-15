package com.cozyfocus.app.data

import android.content.Context
import com.cozyfocus.app.data.db.AppDatabase
import com.cozyfocus.app.data.db.CoinLedgerEntity
import com.cozyfocus.app.data.db.FocusSessionEntity
import com.cozyfocus.app.data.db.InventoryEntryEntity
import com.cozyfocus.app.data.preferences.TimerPreferences
import com.cozyfocus.app.data.preferences.TimerState
import kotlinx.coroutines.flow.Flow

class DataRepository(
    context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    val preferences: TimerPreferences = TimerPreferences(context)
) {
    private val dao = db.focusDao()

    val timerState: Flow<TimerState> = preferences.timerStateFlow
    val sessions: Flow<List<FocusSessionEntity>> = dao.getAllSessions()
    val inventory: Flow<List<InventoryEntryEntity>> = dao.getInventory()
    val coinLedger: Flow<List<CoinLedgerEntity>> = dao.getCoinLedger()

    suspend fun completeSessionIfNeeded(
        sessionId: String,
        durationSeconds: Long,
        companionRaw: String,
        completedAt: Long
    ): Boolean {
        val session = FocusSessionEntity(
            id = sessionId,
            completedAt = completedAt,
            durationSeconds = durationSeconds,
            companionRaw = companionRaw,
            coinsEarned = 5
        )
        return dao.completeSessionIfNeeded(session)
    }

    suspend fun purchaseCosmetic(cosmeticRaw: String, price: Int): Boolean {
        val purchased = dao.purchaseCosmeticIfAffordable(
            cosmeticRaw = cosmeticRaw,
            price = price,
            acquiredAt = System.currentTimeMillis()
        )
        if (purchased) preferences.setEquippedCosmetic(cosmeticRaw)
        return purchased
    }
}
