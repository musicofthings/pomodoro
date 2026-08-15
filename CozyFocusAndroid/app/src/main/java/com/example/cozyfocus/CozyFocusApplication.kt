package com.cozyfocus.app

import android.app.Application
import com.cozyfocus.app.notifications.CompletionNotificationManager

class CozyFocusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CompletionNotificationManager(this).createChannel()
    }
}
