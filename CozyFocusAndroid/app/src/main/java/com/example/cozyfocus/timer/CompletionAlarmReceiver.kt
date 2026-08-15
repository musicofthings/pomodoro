package com.cozyfocus.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CompletionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CompletionAlarmScheduler.ACTION_COMPLETE_SESSION) return
        val sessionId = intent.getStringExtra(CompletionAlarmScheduler.EXTRA_SESSION_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SessionCompletionCoordinator(context.applicationContext).completeIfExpired(
                    expectedSessionId = sessionId,
                    notify = true
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
