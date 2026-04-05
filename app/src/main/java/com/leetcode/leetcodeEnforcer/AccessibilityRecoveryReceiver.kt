package com.leetcode.leetcodeEnforcer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AccessibilityRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext

        if (intent?.action == "com.leetcode.leetcodeEnforcer.OPEN_BATTERY_SETTINGS") {
            BatteryOptimizationHelper.openBatteryOptimizationSettings(app)
            return
        }

        if (intent?.action == ACTION_ACCESSIBILITY_WATCHDOG) {
            AccessibilityHealthScheduler.ensureMonitoring(app)
            AccessibilityHealthScheduler.runNow(app)
            AccessibilityHealthNotifications.showRecoveryNotificationIfNeeded(app)
            AccessibilityAlarmScheduler.reschedule(app)
            return
        }

        if (!AccessibilityHealthState.shouldRunRecoveryTrigger(app, intent?.action)) {
            AccessibilityAlarmScheduler.reschedule(app)
            return
        }

        AccessibilityHealthScheduler.ensureMonitoring(app)
        AccessibilityHealthScheduler.runNow(app)
        AccessibilityAlarmScheduler.reschedule(app)
    }
}
