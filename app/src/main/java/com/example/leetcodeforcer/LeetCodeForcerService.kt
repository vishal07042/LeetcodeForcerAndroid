package com.example.leetcodeforcer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class LeetCodeForcerService : AccessibilityService() {


    companion object {
        private const val TAG = "LeetCodeForcer"

        // Apps that require 5 problems to be solved
        private val APPS_REQUIRE_5_PROBLEMS = setOf(
            "com.tencent.ig"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected")
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return
        val packageName = event.packageName.toString()

        if (!FocusSettingsManager.isFocusSessionActiveNow(this)) {
            return
        }

        val userBlacklist = FocusSettingsManager.getBlacklist(this)
        if (userBlacklist.contains(packageName)) {
            blockPackage(packageName, "Blocked by your blacklist")
            return
        }

        if (LeetCodeManager.isSolvedToday(this)) {
            return
        }

        if (!isPackageAllowedWhenLocked(packageName)) {
            blockPackage(packageName, "Solve today on LeetCode to unlock")
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Service interrupted")
    }

    private fun isPackageAllowedWhenLocked(pkg: String): Boolean {
        if (FocusSettingsManager.isAlwaysAllowedPackage(pkg)) return true

        val userWhitelist = FocusSettingsManager.getWhitelist(this)
        if (userWhitelist.contains(pkg)) return true

        // Specific 5-problem rule for certain apps
        if (APPS_REQUIRE_5_PROBLEMS.contains(pkg)) {
            val isFiveProblemsSolved = LeetCodeManager.isSolvedToday5(this) // Use the 5-problem check
            if (!isFiveProblemsSolved) {
                Log.w(TAG, "BLOCKING: $pkg (Requires 5 LeetCode problems today)")
                Toast.makeText(this, "Solve 5 LeetCode problems to use this app!", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        return false
    }

    private fun blockPackage(packageName: String, message: String) {
        Log.w(TAG, "BLOCKING: $packageName ($message)")
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
