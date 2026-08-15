package com.cozyfocus.app.timer

import kotlin.math.ceil
import kotlin.math.max

object FocusTimerMath {
    fun remainingSeconds(deadlineTimestamp: Long, nowTimestamp: Long): Long {
        return max(0L, ceil((deadlineTimestamp - nowTimestamp) / 1000.0).toLong())
    }

    fun elapsedWholeMinutes(durationSeconds: Long, remainingSeconds: Long): Int {
        return ((durationSeconds - remainingSeconds).coerceAtLeast(0) / 60).toInt()
    }
}
