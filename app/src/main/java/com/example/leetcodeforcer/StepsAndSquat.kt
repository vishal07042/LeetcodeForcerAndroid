package com.example.leetcodeforcer

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.Calendar

private const val PREFS_NAME = "alarmy_prefs"
private const val KEY_BASELINE_DATE = "baseline_date"
private const val KEY_BASELINE_STEPS = "baseline_steps"
private const val KEY_BASELINE_SQUATS = "baseline_squats"
private const val KEY_LAST_KNOWN_STEPS = "last_known_steps" // Total from file
private const val KEY_LAST_KNOWN_SQUATS = "last_known_squats" // Total from file

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

fun refreshAlarmyData(context: Context): Pair<Int, Int>? {
    return try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "alarmy_data.txt")
        
        if (file.exists()) {
            val content = file.readText() // Content format: "steps:100,squats:20"
            val parts = content.split(",")
            val currentFileSteps = parts[0].substringAfter(":").toInt()
            val currentFileSquats = parts[1].substringAfter(":").toInt()
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // 1. Check Date
            val today = getTodayDate()
            val lastBaselineDate = prefs.getString(KEY_BASELINE_DATE, "")
            
            if (lastBaselineDate != today) {
                // NEW DAY DETECTED!
                
                // Case 1: First time run ever (lastBaselineDate is empty)
                // We should set baseline to current file value so progress starts at 0.
                
                // Case 2: New day (lastBaselineDate is yesterday)
                // We should set baseline to the LAST KNOWN total from yesterday.
                // Why? Because if I did 100 steps yesterday (File=100), and wake up today, 
                // the file still says 100. My baseline for today starts at 100.
                // If I then walk 5 steps, file becomes 105. Progress = 105 - 100 = 5. Correct.
                
                // So, baseline = currentFileSteps (at the moment we detect the date flip).
                // Wait, if I missed checking the app for a week?
                // File says 500. Last known was 100.
                // If I set baseline to 500, I lose 400 steps of progress?
                // Yes, but Alarmy is cumulative. We only care about "Today's" steps.
                // We can't know which of those 400 steps happened today vs yesterday without timestamp.
                // So the safest assumption for "Today's Steps" is counting from the moment we first check today.
                // HOWEVER, better approach: 
                // Baseline should be the value at 00:00:00 today.
                // Since we don't have that, we approximate it using the value present during the first check of the day.
                
                // Exception: If the user closed the app last night at 11PM with 100 steps.
                // And opens it today at 8AM with 100 steps.
                // Baseline becomes 100. Correct.
                
                // If the user closed app at 11PM with 100 steps.
                // Walked 5 steps at 11:30PM. (Total 105).
                // Woke up and walked 5 steps at 7AM. (Total 110).
                // Opens app at 8AM. File says 110.
                // If we set Baseline = 110, we see 0 progress today. We lost the 5 morning steps.
                // If we set Baseline = 100 (last known), then progress = 110 - 100 = 10.
                // But 5 of those were yesterday!
                // So we incorrectly credit yesterday's late steps to today.
                // This is properly the "best effort" behavior for a cumulative counter without timestamps.
                // It's better to over-count (credit late night steps to next morning) than to under-count.
                
                // So strategy: Baseline = Last Known Value from previous day.
                // Unless it's a fresh install (no last known value), then Baseline = Current File Value.
                
                val lastKnownSteps = prefs.getInt(KEY_LAST_KNOWN_STEPS, -1)
                val newBaselineSteps = if (lastKnownSteps == -1) currentFileSteps else lastKnownSteps
                
                val lastKnownSquats = prefs.getInt(KEY_LAST_KNOWN_SQUATS, -1)
                val newBaselineSquats = if (lastKnownSquats == -1) currentFileSquats else lastKnownSquats
                
                editor.putInt(KEY_BASELINE_STEPS, newBaselineSteps)
                editor.putInt(KEY_BASELINE_SQUATS, newBaselineSquats)
                editor.putString(KEY_BASELINE_DATE, today)
                
                Log.i("StepsAndSquat", "New Day ($today)! Previous Total: $lastKnownSteps. New Baseline: $newBaselineSteps")
            }
            
            // 2. Always update the "Last Known" total to the current file value
            editor.putInt(KEY_LAST_KNOWN_STEPS, currentFileSteps)
            editor.putInt(KEY_LAST_KNOWN_SQUATS, currentFileSquats)
            editor.apply()
            
            // 3. Log debug info
            val baselineSteps = prefs.getInt(KEY_BASELINE_STEPS, 0) // Reload in case it changed
            val stepProgress = (currentFileSteps - baselineSteps).coerceAtLeast(0)
            Log.d("StepsAndSquat", "Refreshed: File($currentFileSteps) - Baseline($baselineSteps) = Today($stepProgress)")
            
            // Return calculated progress by calling getter
            getAlarmyData(context)
            
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getTodayDate(): String {
    val calendar = Calendar.getInstance()
    // Using simple local date for baseline tracking is sufficient
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
}