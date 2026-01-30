package com.example.leetcodeforcer

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeetCodeTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        // Update state to loading
        tile.state = Tile.STATE_UNAVAILABLE
        tile.label = "Checking..."
        tile.updateTile()

        Toast.makeText(this, "Checking LeetCode stats...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            val isSolved = LeetCodeManager.checkAndSaveStatus(applicationContext)
            
            withContext(Dispatchers.Main) {
                updateTileState(isSolved)
                val status = LeetCodeManager.getDetailedStatus(applicationContext)
                Toast.makeText(applicationContext, status, Toast.LENGTH_LONG).show()
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
}
