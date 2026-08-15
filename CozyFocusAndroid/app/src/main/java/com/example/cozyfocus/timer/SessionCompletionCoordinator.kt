package com.cozyfocus.app.timer

import android.content.Context
import com.cozyfocus.app.data.DataRepository
import com.cozyfocus.app.notifications.CompletionNotificationManager

class SessionCompletionCoordinator(
    context: Context,
    private val repository: DataRepository = DataRepository(context),
    private val scheduler: CompletionAlarmScheduler = CompletionAlarmScheduler(context),
    private val notifications: CompletionNotificationManager = CompletionNotificationManager(context)
) {
    suspend fun completeIfExpired(
        expectedSessionId: String? = null,
        nowTimestamp: Long = System.currentTimeMillis(),
        notify: Boolean = false
    ): CompletionResult {
        val state = repository.preferences.currentState()
        val sessionId = state.activeSessionId ?: return CompletionResult.NO_ACTIVE_SESSION
        if (expectedSessionId != null && expectedSessionId != sessionId) {
            return CompletionResult.STALE_REQUEST
        }
        val deadline = state.targetDeadlineTimestamp ?: return CompletionResult.NOT_DUE
        if (deadline > nowTimestamp) return CompletionResult.NOT_DUE

        val duration = state.activeSessionDurationSeconds ?: return CompletionResult.INVALID_STATE
        val companion = state.activeSessionCompanion ?: state.selectedCompanion
        val inserted = repository.completeSessionIfNeeded(
            sessionId = sessionId,
            durationSeconds = duration,
            companionRaw = companion,
            completedAt = deadline
        )
        repository.preferences.completeTimerIfMatches(sessionId, deadline)
        scheduler.cancel()
        if (inserted && notify) notifications.showCompletion()
        return if (inserted) CompletionResult.COMPLETED else CompletionResult.ALREADY_COMPLETED
    }
}

enum class CompletionResult {
    NO_ACTIVE_SESSION,
    STALE_REQUEST,
    NOT_DUE,
    INVALID_STATE,
    COMPLETED,
    ALREADY_COMPLETED
}
