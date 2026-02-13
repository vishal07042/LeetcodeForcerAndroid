package com.example.leetcodeforcer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to receive fitness data from Alarmy app via Intent.
 * This is more secure than reading from a file that users can edit.
 * 
 * Expected Intent format from Alarmy:
 * Action: "com.example.leetcodeforcer.ALARMY_DATA_UPDATE"
 * Extras:
 *   - "steps" (Int): Current cumulative step count
 *   - "squats" (Int): Current cumulative squat count
 *   - "timestamp" (Long): Unix timestamp when data was sent
 */
class AlarmyDataReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_ALARMY_UPDATE = "com.example.leetcodeforcer.ALARMY_DATA_UPDATE"
        const val EXTRA_STEPS = "steps"
        const val EXTRA_SQUATS = "squats"
        const val EXTRA_TIMESTAMP = "timestamp"
        
        // Package name of the Alarmy app (update this to match actual Alarmy package)
        const val ALARMY_PACKAGE = "droom.sleepIfUCan"
        
        private const val TAG = "AlarmyDataReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARMY_UPDATE) {
            Log.w(TAG, "Received intent with wrong action: ${intent.action}")
            return
        }
        
        // Security: Verify the sender is actually Alarmy
        val senderPackage = intent.getStringExtra("sender_package")
        if (senderPackage != ALARMY_PACKAGE) {
            Log.e(TAG, "Rejected data from unauthorized sender: $senderPackage")
            return
        }
        
        // Extract data
        val steps = intent.getIntExtra(EXTRA_STEPS, -1)
        val squats = intent.getIntExtra(EXTRA_SQUATS, -1)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1L)
        
        // Validate data
        if (steps < 0 || squats < 0 || timestamp < 0) {
            Log.e(TAG, "Invalid data received: steps=$steps, squats=$squats, timestamp=$timestamp")
            return
        }
        
        // Additional validation: Check timestamp is recent (within last 5 minutes)
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - timestamp
        if (timeDiff > 5 * 60 * 1000 || timeDiff < -60 * 1000) {
            Log.e(TAG, "Timestamp is too old or in the future. Diff: ${timeDiff}ms")
            return
        }
        
        Log.i(TAG, "Received valid data from Alarmy: steps=$steps, squats=$squats, timestamp=$timestamp")
        
        // Save to SharedPreferences using the same logic as file-based approach
        saveAlarmyDataFromIntent(context, steps, squats)
    }
    
    private fun saveAlarmyDataFromIntent(context: Context, currentSteps: Int, currentSquats: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Check if it's a new day
        val today = getTodayDate()
        val lastBaselineDate = prefs.getString(KEY_BASELINE_DATE, "")
        
        if (lastBaselineDate != today) {
            // New day detected - set baseline to last known values
            val lastKnownSteps = prefs.getInt(KEY_LAST_KNOWN_STEPS, -1)
            val newBaselineSteps = if (lastKnownSteps == -1) currentSteps else lastKnownSteps
            
            val lastKnownSquats = prefs.getInt(KEY_LAST_KNOWN_SQUATS, -1)
            val newBaselineSquats = if (lastKnownSquats == -1) currentSquats else lastKnownSquats
            
            editor.putInt(KEY_BASELINE_STEPS, newBaselineSteps)
            editor.putInt(KEY_BASELINE_SQUATS, newBaselineSquats)
            editor.putString(KEY_BASELINE_DATE, today)
            
            Log.i(TAG, "New Day ($today)! Setting baseline: steps=$newBaselineSteps, squats=$newBaselineSquats")
        }
        
        // Anti-cheat: Detect suspicious decreases
        val lastKnownSteps = prefs.getInt(KEY_LAST_KNOWN_STEPS, 0)
        val lastKnownSquats = prefs.getInt(KEY_LAST_KNOWN_SQUATS, 0)
        
        if (currentSteps < lastKnownSteps - 10) {
            Log.w(TAG, "Suspicious decrease in steps: $lastKnownSteps -> $currentSteps. Possible tampering!")
            // You could choose to reject this update or flag the user
        }
        
        if (currentSquats < lastKnownSquats - 10) {
            Log.w(TAG, "Suspicious decrease in squats: $lastKnownSquats -> $currentSquats. Possible tampering!")
        }
        
        // Update last known values
        editor.putInt(KEY_LAST_KNOWN_STEPS, currentSteps)
        editor.putInt(KEY_LAST_KNOWN_SQUATS, currentSquats)
        editor.putString(KEY_DATA_SOURCE, "intent")
        editor.putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
        editor.apply()
        
        val baselineSteps = prefs.getInt(KEY_BASELINE_STEPS, 0)
        val stepProgress = (currentSteps - baselineSteps).coerceAtLeast(0)
        
        Log.d(TAG, "Updated: File($currentSteps) - Baseline($baselineSteps) = Today($stepProgress)")
    }
}
