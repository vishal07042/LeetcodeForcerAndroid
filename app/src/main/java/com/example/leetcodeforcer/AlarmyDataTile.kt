package com.example.leetcodeforcer

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class AlarmyDataTile : TileService() {
    
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        // Force refresh from file when tile is clicked
        val refreshedData = refreshAlarmyData(this)
        if (refreshedData != null) {
            val (steps, squats) = refreshedData
            Toast.makeText(this, "Refreshed: Steps=$steps, Squats=$squats", Toast.LENGTH_SHORT).show()
        } else {
             Toast.makeText(this, "No data file found", Toast.LENGTH_SHORT).show()
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        // Just read from prefs (fast) for UI update
        val (steps, squats) = getAlarmyData(this)
        
        tile.label = "Fitness Stats"
        tile.subtitle = "$steps | $squats"
        
        if (steps > 100 && squats > 100) {
            if (tile.state != Tile.STATE_ACTIVE) {
                tile.state = Tile.STATE_ACTIVE
            }
        } else {
            if (tile.state != Tile.STATE_INACTIVE) {
                tile.state = Tile.STATE_INACTIVE
            }
        }
        tile.updateTile()
    }
}