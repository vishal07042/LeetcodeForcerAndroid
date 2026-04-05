package com.leetcode.leetcodeEnforcer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

private const val TAG = "AccessibilityAlarm"
private const val WATCHDOG_REQUEST_CODE = 7101
private const val WATCHDOG_INTERVAL_MS = 5 * 60 * 1000L

/** Explicit alarm tick — reschedules enforcement / accessibility recovery (Nopox-style wakeups). */
const val ACTION_ACCESSIBILITY_WATCHDOG = "com.leetcode.leetcodeEnforcer.ACCESSIBILITY_WATCHDOG"

object AccessibilityAlarmScheduler {

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        cancelInternal(app)

        if (!AccessibilityHealthState.hasActiveEnforcement(app)) {
            return
        }
        if (isAccessibilityServiceEnabled(app)) {
            return
        }

        val triggerAt = SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL_MS
        val pi = watchdogPendingIntent(app)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                } else {
                    am.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pi
                )
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not schedule watchdog alarm", e)
        }
    }

    fun cancel(context: Context) {
        cancelInternal(context.applicationContext)
    }

    private fun cancelInternal(app: Context) {
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            am.cancel(watchdogPendingIntent(app))
        } catch (_: Exception) {
        }
    }

    private fun watchdogPendingIntent(app: Context): PendingIntent {
        val intent = Intent(app, AccessibilityRecoveryReceiver::class.java).apply {
            action = ACTION_ACCESSIBILITY_WATCHDOG
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(app, WATCHDOG_REQUEST_CODE, intent, flags)
    }
}
