package com.cozyfocus.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppDatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: FocusDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.focusDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun completionIsAtomicAndIdempotent() = runBlocking {
        val session = FocusSessionEntity(
            id = "session-1",
            completedAt = 10_000,
            durationSeconds = 1_500,
            companionRaw = "redPanda"
        )

        assertTrue(dao.completeSessionIfNeeded(session))
        assertFalse(dao.completeSessionIfNeeded(session))
        assertEquals(1, dao.getAllSessions().first().size)
        assertEquals(5, dao.getCoinBalance())
        assertEquals(1, dao.getCoinLedger().first().size)
    }

    @Test
    fun purchaseDebitsOnlyOnceAndOnlyWhenAffordable() = runBlocking {
        dao.insertCoinLedgerEntry(
            CoinLedgerEntity(id = "seed", createdAt = 1, amount = 20, reason = "Test credit")
        )

        assertTrue(dao.purchaseCosmeticIfAffordable("sunHat", 20, acquiredAt = 2))
        assertFalse(dao.purchaseCosmeticIfAffordable("sunHat", 20, acquiredAt = 3))
        assertEquals(0, dao.getCoinBalance())
        assertEquals(listOf("sunHat"), dao.getInventory().first().map { it.cosmeticRaw })
    }

    @Test
    fun unaffordablePurchaseWritesNothing() = runBlocking {
        assertFalse(dao.purchaseCosmeticIfAffordable("roundGlasses", 25, acquiredAt = 2))
        assertTrue(dao.getInventory().first().isEmpty())
        assertTrue(dao.getCoinLedger().first().isEmpty())
    }
}
