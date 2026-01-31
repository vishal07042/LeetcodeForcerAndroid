package com.example.leetcodeforcer

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

        Toast.makeText(applicationContext, "Checking stats...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Coroutine started. Fetching status...")
                val isSolved = LeetCodeManager.checkAndSaveStatus(applicationContext)
                Log.d(TAG, "Status fetched. Solved: $isSolved")
                
                withContext(Dispatchers.Main) {
                    updateTileState(isSolved)
                    
                    // Construct the full status message
                    val isServiceEnabled = isAccessibilityServiceEnabled()
                    val serviceStatusMsg = if (isServiceEnabled) "Service: ACTIVE" else "Service: INACTIVE"
                    val blockingMsg = if (!isServiceEnabled) "\n(Blocking disabled)" else ""
                    
                    val lcStatus = LeetCodeManager.getDetailedStatus(applicationContext)
                    
                    val fullMessage = "$serviceStatusMsg$blockingMsg\n\n$lcStatus"
                    Log.d(TAG, "Displaying Dialog: $fullMessage")
                    
                    try {
                        val dialog = android.app.AlertDialog.Builder(this@LeetCodeTileServiceShowStats)
                            .setTitle("LeetCode Stats")
                            .setMessage(fullMessage)
                            .setPositiveButton("OK", null)
                            .create()
                        
                        showDialog(dialog)
                    } catch (e: Exception) {
                         Log.e(TAG, "Error showing dialog", e)
                         // Fallback to Toast if dialog fails (though unlikely if suppression is the only issue)
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
        // Update UI when tile becomes visible
        val solvedToday = LeetCodeManager.isSolvedToday(applicationContext)
        updateTileState(solvedToday)
    }

    private fun updateTileState(isSolved: Boolean) {
        val tile = qsTile ?: return
        if (isSolved) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "LC Solved"
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

