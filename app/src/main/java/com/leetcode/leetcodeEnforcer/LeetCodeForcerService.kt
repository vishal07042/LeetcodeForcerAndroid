package com.leetcode.leetcodeEnforcer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

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
        showPersistentNotification()
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return
        val packageName = event.packageName.toString()

        // Anti-uninstall protection
        if (packageName == "com.android.settings") {
            val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val componentName = android.content.ComponentName(this, LeetCodeDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(componentName) && FocusSettingsManager.isFocusSessionActiveNow(this) && !LeetCodeManager.isSolvedToday(this)) {
                var foundTarget = event.text.any { it?.contains("LeetCode Forcer", ignoreCase = true) == true }
                val rootNode = rootInActiveWindow
                if (rootNode != null && !foundTarget) {
                    val nodes = rootNode.findAccessibilityNodeInfosByText("LeetCode Forcer")
                    if (!nodes.isNullOrEmpty()) {
                        foundTarget = true
                    }
                }
                if (foundTarget) {
                    Toast.makeText(this, "Uninstallation is locked during active focus sessions!", Toast.LENGTH_SHORT).show()
                    performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                    performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                    }
                    return
                }
            }
        }

        val isFrozen = LeetCodeManager.isFrozen(this)
        val isFrozen2 = LeetCodeManager.isFrozen2(this)
        val solved = LeetCodeManager.isSolvedToday(this)

        // If frozen and not solved, we enforce 24/7 (ignore sessions)
        if ((isFrozen || isFrozen2) && !solved) {
            if (!isPackageAllowedWhenLocked(packageName)) {
                blockPackage(packageName, "Extreme Mode: Solve LeetCode to unlock")
            }
            return
        }

        if (!FocusSettingsManager.isFocusSessionActiveNow(this)) {
            return
        }

        if (solved) {
            return
        }

        if (!isPackageAllowedWhenLocked(packageName)) {
            blockPackage(packageName, "Solve a question  on LeetCode to unlock")
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

        // Allow background/system apps that don't have a launcher icon
        if (!isLauncherApp(pkg)) {
            return true
        }

        return false
    }

    private fun isLauncherApp(pkg: String): Boolean {
        return try {
            packageManager.getLaunchIntentForPackage(pkg) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun blockPackage(packageName: String, message: String) {
        Log.w(TAG, "BLOCKING: $packageName ($message)")
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPersistentNotification() {
        val channelId = "LeetCodeForcerChannel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LeetCode Forcer Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the app active and ensures you are focused"
            }
            notificationManager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LeetCode Forcer is Active")
            .setContentText("Monitoring your phone usage for focus sessions.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
            
        notificationManager?.notify(1001, notification)
    }
}
