package com.leetcode.leetcodeEnforcer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class LeetCodeForcerService : AccessibilityService() {

    companion object {
        private const val TAG = "LeetCodeForcer"

        private val APPS_REQUIRE_5_PROBLEMS = setOf(
            "com.tencent.ig"
        )

        private val APP_NAME_TEXTS = listOf("LeetCode Forcer", "leetcode forcer")
        private val DANGEROUS_TEXTS = listOf(
            "uninstall",
            "force stop",
            "disable",
            "deactivate",
            "clear data",
            "remove",
            "delete",
            "turn off"
        )
        private val SETTINGS_CONTEXT_TEXTS = listOf(
            "accessibility",
            "device admin",
            "device administrator",
            "permissions"
        )
        private val APP_DETAIL_TITLE_IDS = listOf(
            "com.android.settings:id/collapsing_appbar_extended_title",
            "com.android.settings:id/collapsing_toolbar",
            "android:id/alertTitle",
            "com.miui.securitycenter:id/tv_title"
        )
        private val DANGEROUS_CLASS_NAMES = setOf(
            "com.android.packageinstaller.UninstallerActivity",
            "com.android.settings.SubSettings",
            "com.miui.applicationlock.PrivacyAndAppLockManageActivity"
        )
        private const val SAMSUNG_MULTI_UNINSTALL_ID = "com.sec.android.app.launcher:id/multi_select_uninstall"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected")
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return
        val packageName = event.packageName.toString()
        val className = event.className?.toString().orEmpty()

        if (shouldBlockProtectedSettings(event, packageName, className)) {
            Log.w(TAG, "Blocking protected settings pkg=$packageName cls=$className")
            blockAndLock("Security: Protected settings locked during focus!")
            return
        }

        val isFrozen = LeetCodeManager.isFrozen(this)
        val isFrozen2 = LeetCodeManager.isFrozen2(this)
        val isFrozen3 = LeetCodeManager.isFrozen3(this)
        val solved = LeetCodeManager.isSolvedToday(this)
        val activeSession = FocusSettingsManager.getActiveSessionNow(this)

        if (!solved) {
            if (isFrozen2) {
                val allowed = isPackageAllowedInFreeze2(packageName)
                Log.i(TAG, "pkg=$packageName mode=freeze2 allowed=$allowed")
                if (!allowed) {
                    blockPackage(packageName, "Extreme Mode 2: only selected apps are allowed until you solve LeetCode.")
                }
                return
            }

            if (isFrozen) {
                val allowed = isPackageAllowedInFreeze1(packageName)
                Log.i(TAG, "pkg=$packageName mode=freeze1 allowed=$allowed")
                if (!allowed) {
                    blockPackage(packageName, "Extreme Mode: Solve LeetCode to unlock")
                }
                return
            }

            if (isFrozen3 && activeSession != null) {
                val allowed = isPackageAllowedInSession(packageName, activeSession)
                Log.i(TAG, "pkg=$packageName mode=freeze3 allowed=$allowed")
                if (!allowed) {
                    blockPackage(packageName, "Extreme Mode 3: Solve LeetCode to unlock")
                }
                return
            }
        }

        if (activeSession == null || solved) return

        val allowed = isPackageAllowedInSession(packageName, activeSession)
        Log.i(TAG, "pkg=$packageName mode=session allowed=$allowed")
        if (!allowed) {
            blockPackage(packageName, "Solve a question on LeetCode to unlock")
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Service interrupted")
    }

    private fun shouldBlockProtectedSettings(
        event: AccessibilityEvent,
        packageName: String,
        className: String
    ): Boolean {
        if (!LeetCodeManager.isPreventUninstallEnabled(this)) return false
        if (!isProtectedSettingsPackage(packageName) && className !in DANGEROUS_CLASS_NAMES) return false

        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, LeetCodeDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminComponent)) return false

        val root = rootInActiveWindow
        val source = event.source
        try {
            val hasAppName = root?.let { containsAnyText(it, APP_NAME_TEXTS) } == true ||
                source?.let { containsAnyText(it, APP_NAME_TEXTS) } == true
            if (!hasAppName) return false

            val hasDangerousText = root?.let { containsAnyText(it, DANGEROUS_TEXTS) } == true ||
                source?.let { containsAnyText(it, DANGEROUS_TEXTS) } == true
            val inSettingsContext = root?.let { containsAnyText(it, SETTINGS_CONTEXT_TEXTS) } == true
            val onAppDetailPage = root?.let { containsAnyViewId(it, APP_DETAIL_TITLE_IDS) } == true
            val hasSamsungUninstall = source?.viewIdResourceName.equals(SAMSUNG_MULTI_UNINSTALL_ID, ignoreCase = true) ||
                root?.let { containsViewId(it, SAMSUNG_MULTI_UNINSTALL_ID) } == true

            return hasSamsungUninstall ||
                className in DANGEROUS_CLASS_NAMES ||
                (packageName == "com.miui.securitycenter" && (hasDangerousText || onAppDetailPage)) ||
                (packageName == "com.android.settings" && (hasDangerousText || inSettingsContext || onAppDetailPage)) ||
                (packageName.contains("packageinstaller", ignoreCase = true))
        } finally {
            try {
                source?.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun isProtectedSettingsPackage(pkg: String): Boolean {
        return pkg == "com.android.settings" ||
            pkg == "com.android.settingsaccessibility" ||
            pkg == "com.miui.securitycenter" ||
            pkg.contains("packageinstaller", ignoreCase = true) ||
            pkg.contains("installer", ignoreCase = true)
    }

    private fun containsAnyText(node: AccessibilityNodeInfo, values: List<String>): Boolean {
        val text = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        if (values.any { text.contains(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) }) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsAnyText(child, values)) return true
        }
        return false
    }

    private fun containsAnyViewId(node: AccessibilityNodeInfo, ids: List<String>): Boolean {
        val id = node.viewIdResourceName.orEmpty()
        if (ids.any { id.equals(it, ignoreCase = true) }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsAnyViewId(child, ids)) return true
        }
        return false
    }

    private fun containsViewId(node: AccessibilityNodeInfo, exactId: String): Boolean {
        if (node.viewIdResourceName.equals(exactId, ignoreCase = true)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsViewId(child, exactId)) return true
        }
        return false
    }

    private fun isPackageAllowedInFreeze1(pkg: String): Boolean {
        if (FocusSettingsManager.isAlwaysAllowedPackage(pkg)) return true
        return !isLauncherApp(pkg)
    }

    private fun isPackageAllowedInFreeze2(pkg: String): Boolean {
        if (FocusSettingsManager.isAlwaysAllowedPackage(pkg)) return true
        if (FocusSettingsManager.getFreeze2Whitelist(this).contains(pkg)) return true

        if (APPS_REQUIRE_5_PROBLEMS.contains(pkg)) {
            val isFiveProblemsSolved = LeetCodeManager.isSolvedToday5(this)
            if (!isFiveProblemsSolved) {
                Toast.makeText(this, "Solve 5 LeetCode problems to use this app!", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        return !isLauncherApp(pkg)
    }

    private fun isPackageAllowedInSession(pkg: String, session: FocusSession): Boolean {
        if (FocusSettingsManager.isAlwaysAllowedPackage(pkg)) return true
        if (session.whitelist.contains(pkg)) return true
        return !isLauncherApp(pkg)
    }

    private fun isLauncherApp(pkg: String): Boolean {
        return try {
            packageManager.getLaunchIntentForPackage(pkg) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun blockPackage(packageName: String, message: String) {
        Log.w(TAG, "BLOCKING: $packageName ($message)")
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun blockAndLock(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
