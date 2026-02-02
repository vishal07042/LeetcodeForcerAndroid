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
            "com.miui.securityadd",
            "com.google.android.keep",
            "com.focus.mobile.focus",
            "cc.forestapp",
            "droom.sleepIfUCan",
            "org.brilliant.android",
            "app.getatoms.android",
            "com.anthropic.claude",
            "com.phonepe.app",
            "com.miui.securitycenter",
            "com.miui.powerkeeper",
            "com.miui.cleanmaster",
            "miui.systemui.plugin",
            "com.google.android.packageinstaller",
            "com.miui.home",
            "com.android.settings",
            "com.android.systemui",
            "com.android.settingsaccessibility",
            "com.google.android.inputmethod.latin",
            "com.mi.globalminusscreen",
           
            "com.android.settings",
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.example.leetcodeforcer",
            "com.miui.home",
            "miui.systemui.plugin",
           "com.google.android.apps.docs.editors.docs",
           "com.google.android.apps.docs.editors.sheets",
           "com.google.android.apps.docs.editors.slides",
            "com.google.android.gm" ,
            "notion.id",
            "com.example.peedo",
            "ai.x.grok",
            "com.wlxd.pomochallenge",
            "com.google.android.apps.messaging",
           "com.google.android.apps.bard"

            
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
            performGlobalAction(GLOBAL_ACTION_HOME)
            performGlobalAction(GLOBAL_ACTION_HOME)


            
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
       if (pkg.contains("calendar")) return true;
       if (pkg.contains("home")) return true;
       if (pkg.contains("plan")) return true;
       if (pkg.contains("dialer")) return true;
       if (pkg.contains("contacts")) return true;
        // Allow system UI elements specifically
        if (pkg.contains("systemui")) return true
        if (pkg.contains("plan")) return true

        if(pkg.contains("com.miui.home")) return true;
        if(pkg.contains("settings")) return true;
        
        return false
    }
}
