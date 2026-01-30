package com.example.leetcodeforcer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class LeetCodeForcerService : AccessibilityService() {

    companion object {
        private const val TAG = "LeetCodeForcer"

        private val WHITELIST_PACKAGES = setOf(
            "com.mi.globalminusscreen",
            "com.android.chrome",
            "com.android.settings",
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.example.leetcodeforcer",
            "com.miui.home",
            "miui.systemui.plugin",
            "com.android.vending", // Play Store (to update apps?)
            "com.google.android.gm" // Gmail
        )
    }

    private val handler = Handler(Looper.getMainLooper())

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
        // Optimization: if already solved, do NOTHING. Don't even read the package name.
        val isSolved = LeetCodeManager.isSolvedToday(this)
        
        if (isSolved) {
            // Logs once in a while or just stay silent? Let's log once to confirm it's idle.
            return
        }

        if (event == null || event.packageName == null) return
        
        val packageName = event.packageName.toString()

        if (!isPackageAllowed(packageName)) {
            Log.w(TAG, "BLOCKING: $packageName (Reason: LeetCode not solved for today)")
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            // Show toast
            Toast.makeText(this, "LeetCode Forcer: Solve a problem first!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Service interrupted")
    }

    private fun isPackageAllowed(pkg: String): Boolean {
        if (WHITELIST_PACKAGES.contains(pkg)) return true
        // Allow launchers generally (simple heuristic: contains launcher)
        if (pkg.contains("launcher")) return true
        // Allow system UI elements specifically
        if (pkg.contains("systemui")) return true

        if(pkg.contains("com.miui.home")) return true;
        if(pkg.contains("settings")) return true;
        
        return false
    }
}
