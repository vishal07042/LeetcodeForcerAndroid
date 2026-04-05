package com.leetcode.leetcodeEnforcer

import android.accessibilityservice.AccessibilityServiceInfo
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LeetCodeTileServiceShowStats : TileService() {

    private val TAG = "LeetCodeTileStats"

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick: Tile clicked")
        val tile = qsTile ?: return

        // Update state to loading
        tile.state = Tile.STATE_UNAVAILABLE
        tile.label = "Checking..."
        tile.updateTile()

        Toast.makeText(applicationContext, "Checking LeetCode...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                LeetCodeManager.refreshDateFromApiIfNeeded(applicationContext)
                Log.d(TAG, "Fetching LeetCode status...")
                val lcSolved = LeetCodeManager.checkAndSaveStatus(applicationContext)
                val sessionActive = FocusSettingsManager.isFocusSessionActiveNow(applicationContext)
                val isUnlocked = !sessionActive || lcSolved
                Log.d(TAG, "LC: $lcSolved, SessionActive: $sessionActive -> Unlocked: $isUnlocked")

                withContext(Dispatchers.Main) {
                    updateTileState(isUnlocked)

                    val isServiceEnabled = isAccessibilityServiceEnabled()
                    val serviceStatusMsg = if (isServiceEnabled) "Service: ACTIVE" else "Service: INACTIVE"
                    val blockingMsg = if (!isServiceEnabled) "\n(Blocking disabled)" else ""

                    val lcStatus = LeetCodeManager.getDetailedStatus(applicationContext)
                    val sessionStatus = if (sessionActive) {
                        "Focus Session: [ ACTIVE ]"
                    } else {
                        "Focus Session: [ INACTIVE ]"
                    }
                    val fullMessage = "$serviceStatusMsg$blockingMsg\n\n$sessionStatus\n\n$lcStatus"

                    try {
                        val dialog = android.app.AlertDialog.Builder(this@LeetCodeTileServiceShowStats)
                            .setTitle("LeetCode Status")
                            .setMessage(fullMessage)
                            .setPositiveButton("OK", null)
                            .create()
                        showDialog(dialog)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error showing dialog", e)
                        Toast.makeText(applicationContext, fullMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in TileService", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val lcSolved = LeetCodeManager.isSolvedToday(applicationContext)
        val sessionActive = FocusSettingsManager.isFocusSessionActiveNow(applicationContext)
        updateTileState(!sessionActive || lcSolved)
    }

    private fun updateTileState(isUnlocked: Boolean) {
        val tile = qsTile ?: return
        if (isUnlocked) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "LC Focus"
            tile.subtitle = "Unblocked"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Check LC"
            tile.subtitle = "Blocked"
        }
        tile.updateTile()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = try {
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        } catch (e: Exception) {
            Log.e("TileService", "Error getting enabled services", e)
            return false
        }

        enabledServices?.forEach { service ->
            val info = service.resolveInfo.serviceInfo
            if (info.packageName == packageName && info.name == LeetCodeForcerService::class.java.name) {
                return true
            }
        }
        return false
    }
}
