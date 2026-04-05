package com.leetcode.leetcodeEnforcer

import android.app.Application

class LeetCodeForcerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AccessibilityHealthScheduler.ensureMonitoring(this)
        AccessibilityAlarmScheduler.reschedule(this)
    }
}
