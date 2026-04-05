package com.leetcode.leetcodeEnforcer

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import android.util.Log

class LeetcodeResetTile : TileService() {

    private val TAG = "LeetcodeResetTile"

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick: Resetting LeetCode progress")
        
        try {
            // Reset the progress in LeetCodeManager
            LeetCodeManager.resetProgress(applicationContext)
            
            // Show feedback
            Toast.makeText(this, "LeetCode Progress Reset!", Toast.LENGTH_SHORT).show()
            
            // Update tile state temporarily to show action was taken
            val tile = qsTile
            if (tile != null) {
                val originalLabel = tile.label
                tile.label = "Reset Complete"
                tile.state = Tile.STATE_ACTIVE
                tile.updateTile()
                
                // Return to normal after a short delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    tile.label = originalLabel
                    tile.state = Tile.STATE_INACTIVE
                    tile.updateTile()
                }, 2000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting progress", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
