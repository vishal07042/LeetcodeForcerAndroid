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
        val active = FocusSettingsManager.isFocusSessionActiveNow(this)
        val sessionCount = FocusSettingsManager.getSessions(this).size
        val message = if (active) {
            "Focus session active. Sessions: $sessionCount"
        } else {
            "No active focus session. Sessions: $sessionCount"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val active = FocusSettingsManager.isFocusSessionActiveNow(this)
        val sessionCount = FocusSettingsManager.getSessions(this).size

        tile.label = "Focus Sessions"
        tile.subtitle = "Sessions: $sessionCount"
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
