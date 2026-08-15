package com.cozyfocus.app.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class CompletionAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(sessionId: String, deadlineTimestamp: Long) {
        val alarm = requireNotNull(
            completionPendingIntent(sessionId, PendingIntent.FLAG_UPDATE_CURRENT)
        )
        if (canScheduleExactAlarm()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    deadlineTimestamp,
                    alarm
                )
                return
            } catch (_: SecurityException) {
                // Exact-alarm access can be revoked between the capability check and scheduling.
            }
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            deadlineTimestamp,
            alarm
        )
    }

    fun cancel() {
        completionPendingIntent("", PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    fun canScheduleExactAlarm(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun completionPendingIntent(sessionId: String, creationFlag: Int): PendingIntent? {
        return PendingIntent.getBroadcast(
            context,
            COMPLETION_REQUEST_CODE,
            Intent(context, CompletionAlarmReceiver::class.java).apply {
                action = ACTION_COMPLETE_SESSION
                putExtra(EXTRA_SESSION_ID, sessionId)
            },
            creationFlag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_SESSION_ID = "com.cozyfocus.app.extra.SESSION_ID"
        const val ACTION_COMPLETE_SESSION = "com.cozyfocus.app.action.COMPLETE_SESSION"
        private const val COMPLETION_REQUEST_CODE = 2001
    }
}
