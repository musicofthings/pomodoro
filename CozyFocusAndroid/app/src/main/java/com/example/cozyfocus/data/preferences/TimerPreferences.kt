package com.cozyfocus.app.data.preferences

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cozy_focus_prefs")

data class TimerState(
    val durationIndex: Int = 5,
    val targetDeadlineTimestamp: Long? = null,
    val pausedRemainingSeconds: Long? = null,
    val activeSessionId: String? = null,
    val activeSessionDurationSeconds: Long? = null,
    val activeSessionCompanion: String? = null,
    val lastCompletedSessionId: String? = null,
    val lastCompletedAt: Long? = null,
    val hapticsEnabled: Boolean = true,
    val selectedCompanion: String = "redPanda",
    val equippedCosmetic: String? = null,
    val selectedSound: String = "rainfall",
    val notificationPermissionRequested: Boolean = false
)

class TimerPreferences(private val context: Context) {

    private object Keys {
        val DURATION_INDEX = intPreferencesKey("timer.durationIndex")
        val TARGET_DEADLINE = longPreferencesKey("timer.targetDeadline")
        val PAUSED_REMAINING = longPreferencesKey("timer.pausedRemaining")
        val ACTIVE_SESSION_ID = stringPreferencesKey("timer.activeSessionId")
        val ACTIVE_SESSION_DURATION = longPreferencesKey("timer.activeSessionDuration")
        val ACTIVE_SESSION_COMPANION = stringPreferencesKey("timer.activeSessionCompanion")
        val LAST_COMPLETED_SESSION_ID = stringPreferencesKey("timer.lastCompletedSessionId")
        val LAST_COMPLETED_AT = longPreferencesKey("timer.lastCompletedAt")
        val HAPTICS_ENABLED = booleanPreferencesKey("timer.hapticsEnabled")
        val SELECTED_COMPANION = stringPreferencesKey("profile.companion")
        val EQUIPPED_COSMETIC = stringPreferencesKey("profile.equippedCosmetic")
        val SELECTED_SOUND = stringPreferencesKey("audio.selectedSound")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notifications.permissionRequested")
    }

    val timerStateFlow: Flow<TimerState> = context.dataStore.data.map { prefs ->
        TimerState(
            durationIndex = prefs[Keys.DURATION_INDEX] ?: 5,
            targetDeadlineTimestamp = prefs[Keys.TARGET_DEADLINE],
            pausedRemainingSeconds = prefs[Keys.PAUSED_REMAINING],
            activeSessionId = prefs[Keys.ACTIVE_SESSION_ID],
            activeSessionDurationSeconds = prefs[Keys.ACTIVE_SESSION_DURATION],
            activeSessionCompanion = prefs[Keys.ACTIVE_SESSION_COMPANION],
            lastCompletedSessionId = prefs[Keys.LAST_COMPLETED_SESSION_ID],
            lastCompletedAt = prefs[Keys.LAST_COMPLETED_AT],
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            selectedCompanion = prefs[Keys.SELECTED_COMPANION] ?: "redPanda",
            equippedCosmetic = prefs[Keys.EQUIPPED_COSMETIC],
            selectedSound = prefs[Keys.SELECTED_SOUND] ?: "rainfall",
            notificationPermissionRequested = prefs[Keys.NOTIFICATION_PERMISSION_REQUESTED] ?: false
        )
    }

    suspend fun currentState(): TimerState = timerStateFlow.first()

    suspend fun saveDurationIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DURATION_INDEX] = index
            prefs.remove(Keys.PAUSED_REMAINING)
            clearActiveSession(prefs)
            clearLastCompletion(prefs)
        }
    }

    suspend fun startTimer(
        sessionId: String,
        deadlineTimestamp: Long,
        durationSeconds: Long,
        companionRaw: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TARGET_DEADLINE] = deadlineTimestamp
            prefs[Keys.ACTIVE_SESSION_ID] = sessionId
            prefs[Keys.ACTIVE_SESSION_DURATION] = durationSeconds
            prefs[Keys.ACTIVE_SESSION_COMPANION] = companionRaw
            prefs.remove(Keys.PAUSED_REMAINING)
            clearLastCompletion(prefs)
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
            clearActiveSession(prefs)
            clearLastCompletion(prefs)
        }
    }

    suspend fun completeTimerIfMatches(sessionId: String, completedAt: Long): Boolean {
        var matched = false
        context.dataStore.edit { prefs ->
            if (prefs[Keys.ACTIVE_SESSION_ID] != sessionId) return@edit
            matched = true
            clearActiveSession(prefs)
            prefs[Keys.LAST_COMPLETED_SESSION_ID] = sessionId
            prefs[Keys.LAST_COMPLETED_AT] = completedAt
        }
        return matched
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

    suspend fun setSelectedSound(soundId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_SOUND] = soundId
        }
    }

    suspend fun markNotificationPermissionRequested() {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_REQUESTED] = true
        }
    }

    private fun clearActiveSession(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs.remove(Keys.TARGET_DEADLINE)
        prefs.remove(Keys.PAUSED_REMAINING)
        prefs.remove(Keys.ACTIVE_SESSION_ID)
        prefs.remove(Keys.ACTIVE_SESSION_DURATION)
        prefs.remove(Keys.ACTIVE_SESSION_COMPANION)
    }

    private fun clearLastCompletion(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs.remove(Keys.LAST_COMPLETED_SESSION_ID)
        prefs.remove(Keys.LAST_COMPLETED_AT)
    }
}
