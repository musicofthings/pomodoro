package com.example.cozyfocus.services

import android.content.Context

class DistractionShieldManager(private val context: Context) {
    var isShielding: Boolean = false
        private set

    var statusText: String = "Not enabled"
        private set

    fun enableShielding() {
        isShielding = true
        statusText = "Distractions are paused for this focus sprint"
    }

    fun disableShielding() {
        isShielding = false
        statusText = "Not enabled"
    }

    fun toggleShielding() {
        if (isShielding) {
            disableShielding()
        } else {
            enableShielding()
        }
    }
}
