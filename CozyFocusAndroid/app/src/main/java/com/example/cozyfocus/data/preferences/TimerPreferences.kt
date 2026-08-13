package com.example.cozyfocus.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cozy_focus_prefs")

data class TimerState(
    val durationIndex: Int = 5,
    val targetDeadlineTimestamp: Long? = null,
    val pausedRemainingSeconds: Long? = null,
    val hapticsEnabled: Boolean = true,
    val selectedCompanion: String = "redPanda",
    val equippedCosmetic: String? = null
)

class TimerPreferences(private val context: Context) {

    private object Keys {
        val DURATION_INDEX = intPreferencesKey("timer.durationIndex")
        val TARGET_DEADLINE = longPreferencesKey("timer.targetDeadline")
        val PAUSED_REMAINING = longPreferencesKey("timer.pausedRemaining")
        val HAPTICS_ENABLED = booleanPreferencesKey("timer.hapticsEnabled")
        val SELECTED_COMPANION = stringPreferencesKey("profile.companion")
        val EQUIPPED_COSMETIC = stringPreferencesKey("profile.equippedCosmetic")
    }

    val timerStateFlow: Flow<TimerState> = context.dataStore.data.map { prefs ->
        TimerState(
            durationIndex = prefs[Keys.DURATION_INDEX] ?: 5,
            targetDeadlineTimestamp = prefs[Keys.TARGET_DEADLINE],
            pausedRemainingSeconds = prefs[Keys.PAUSED_REMAINING],
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            selectedCompanion = prefs[Keys.SELECTED_COMPANION] ?: "redPanda",
            equippedCosmetic = prefs[Keys.EQUIPPED_COSMETIC]
        )
    }

    suspend fun saveDurationIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DURATION_INDEX] = index
            prefs.remove(Keys.PAUSED_REMAINING)
        }
    }

    suspend fun startTimer(deadlineTimestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TARGET_DEADLINE] = deadlineTimestamp
            prefs.remove(Keys.PAUSED_REMAINING)
        }
    }

    suspend fun pauseTimer(remainingSeconds: Long) {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TARGET_DEADLINE)
            prefs[Keys.PAUSED_REMAINING] = remainingSeconds
        }
    }

    suspend fun resetTimer() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TARGET_DEADLINE)
            prefs.remove(Keys.PAUSED_REMAINING)
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setSelectedCompanion(companionRaw: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_COMPANION] = companionRaw
        }
    }

    suspend fun setEquippedCosmetic(cosmeticRaw: String?) {
        context.dataStore.edit { prefs ->
            if (cosmeticRaw != null) {
                prefs[Keys.EQUIPPED_COSMETIC] = cosmeticRaw
            } else {
                prefs.remove(Keys.EQUIPPED_COSMETIC)
            }
        }
    }
}
