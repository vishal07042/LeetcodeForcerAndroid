package com.example.leetcodeforcer

import android.content.Context
import android.util.Log
import java.util.Calendar

internal const val PREFS_NAME = "alarmy_prefs"
internal const val KEY_BASELINE_DATE = "baseline_date"
internal const val KEY_BASELINE_STEPS = "baseline_steps"
internal const val KEY_BASELINE_SQUATS = "baseline_squats"
internal const val KEY_LAST_KNOWN_STEPS = "last_known_steps" // Total from file
internal const val KEY_LAST_KNOWN_SQUATS = "last_known_squats" // Total from file
internal const val KEY_DATA_SOURCE = "data_source" // "intent" or "file"
internal const val KEY_LAST_UPDATE_TIME = "last_update_time" // Timestamp of last update

fun getAlarmyData(context: Context): Pair<Int, Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // We calculate progress dynamically: Last Known Total - Baseline
    val lastKnownSteps = prefs.getInt(KEY_LAST_KNOWN_STEPS, 0)
    val baselineSteps = prefs.getInt(KEY_BASELINE_STEPS, 0)
    
    val lastKnownSquats = prefs.getInt(KEY_LAST_KNOWN_SQUATS, 0)
    val baselineSquats = prefs.getInt(KEY_BASELINE_SQUATS, 0)
    
    // Safety check: ensure no negative progress (e.g. if file was manually reset or new device)
    val stepProgress = (lastKnownSteps - baselineSteps).coerceAtLeast(0)
    val squatProgress = (lastKnownSquats - baselineSquats).coerceAtLeast(0)
    
    return Pair(stepProgress, squatProgress)
}


internal fun getTodayDate(): String {
    val calendar = Calendar.getInstance()
    // Using simple local date for baseline tracking is sufficient
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
}


fun getDataSourceInfo(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val source = prefs.getString(KEY_DATA_SOURCE, "none") ?: "none"
    val lastUpdate = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L)
    
    if (lastUpdate == 0L) {
        return "No data received yet"
    }
    
    val timeDiff = System.currentTimeMillis() - lastUpdate
    val minutesAgo = timeDiff / (60 * 1000)
    
    val timeStr = when {
        minutesAgo < 1 -> "just now"
        minutesAgo < 60 -> "$minutesAgo min ago"
        else -> "${minutesAgo / 60}h ago"
    }
    
    val sourceIcon = when (source) {
        "intent" -> "🔒" // Secure
        "file" -> "📄" // File-based (less secure)
        else -> "❓"
    }
    
    return "$sourceIcon ${source.uppercase()} ($timeStr)"
}
