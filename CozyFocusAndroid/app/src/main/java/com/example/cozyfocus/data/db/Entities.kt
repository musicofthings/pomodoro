package com.example.cozyfocus.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val completedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val companionRaw: String,
    val coinsEarned: Int = 5
)

@Entity(tableName = "inventory_entries")
data class InventoryEntryEntity(
    @PrimaryKey val cosmeticRaw: String,
    val acquiredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "coin_ledger_entries")
data class CoinLedgerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val amount: Int,
    val reason: String
)
