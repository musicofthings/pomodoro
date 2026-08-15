package com.cozyfocus.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cozyfocus.app.data.preferences.TimerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supportedAction = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        if (!supportedAction) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val state = TimerPreferences(appContext).currentState()
                val sessionId = state.activeSessionId
                val deadline = state.targetDeadlineTimestamp
                if (sessionId != null && deadline != null) {
                    if (deadline <= System.currentTimeMillis()) {
                        SessionCompletionCoordinator(appContext).completeIfExpired(
                            expectedSessionId = sessionId,
                            notify = true
                        )
                    } else {
                        CompletionAlarmScheduler(appContext).schedule(sessionId, deadline)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
