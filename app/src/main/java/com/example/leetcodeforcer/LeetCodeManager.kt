package com.example.leetcodeforcer

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object LeetCodeManager {
    private const val TAG = "LeetCodeManager"
    private const val TARGET_USERNAME = "professionalprovishal" // Replace if needed
    private const val LEETCODE_API_ENDPOINT = "https://leetcode.com/graphql"

    private const val PREFS_NAME = "leet_prefs"
    private const val KEY_LAST_SOLVED_DATE = "todayDateAfterChallenegeComplete"
    private const val KEY_LAST_SOLVED_DATE_5 = "todayDateAfterChallenegeComplete5"
    private const val KEY_NUM_SUBMISSIONS = "numSubmissions"
    private const val KEY_UNIQUE_SOLVED = "uniqueSolved"
    private const val KEY_CACHED_UTC_DATE = "cached_utc_date"
    private const val KEY_CACHED_UTC_DATE_AT = "cached_utc_date_at"
    private const val DATE_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours — date changes once per day
    private const val TIME_API_URL = "https://gettimeapi.dev/v1/time"

    private const val LEETCODE_ALL_PROBLEMS_QUERY = """
        query userSessionProgress(${"$"}username: String!) {
          matchedUser(username: ${"$"}username) {
            submitStats {
              acSubmissionNum {
                difficulty
                count
                submissions
              }
            }
          }
        }
    """

    fun isSolvedToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSolvedDate = prefs.getString(KEY_LAST_SOLVED_DATE, null)
        val today = getTodayDateString(context)
        return lastSolvedDate == today
    }

    fun isSolvedToday5(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSolvedDate = prefs.getString(KEY_LAST_SOLVED_DATE_5, null)
        val today = getTodayDateString(context)
        return lastSolvedDate == today
    }

    suspend fun checkAndSaveStatus(context: Context): Boolean {
        try {
            val responseJson = fetchLeetCodeStats() ?: return false
            return processLeetCodeData(context, responseJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking status", e)
            return false
        }
    }

    private fun fetchLeetCodeStats(): JSONObject? {
        try {
            val url = URL(LEETCODE_API_ENDPOINT)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject()
            jsonBody.put("query", LEETCODE_ALL_PROBLEMS_QUERY)
            val variables = JSONObject()
            variables.put("username", TARGET_USERNAME)
            jsonBody.put("variables", variables)

            conn.outputStream.use { it.write(jsonBody.toString().toByteArray()) }

            val responseCode = conn.responseCode
            Log.d(TAG, "Request to LeetCode API. Response Code: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                Log.v(TAG, "API Response: $response")
                return JSONObject(response)
            } else {
                Log.e(TAG, "HTTP Error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network Error", e)
        }
        return null
    }

    private fun processLeetCodeData(context: Context, data: JSONObject): Boolean {
        try {
            if (!data.has("data") || data.isNull("data")) return false
            val dataObj = data.getJSONObject("data")
            if (dataObj.isNull("matchedUser")) return false
            
            val submitStats = dataObj.getJSONObject("matchedUser")
                .getJSONObject("submitStats")
            val acSubmissionNum = submitStats.getJSONArray("acSubmissionNum")
            
            var totalAccepted = 0
            var uniqueSolved = 0
            
            for (i in 0 until acSubmissionNum.length()) {
                val obj = acSubmissionNum.getJSONObject(i)
                if (obj.optString("difficulty") == "All") {
                    totalAccepted = obj.getInt("submissions")
                    uniqueSolved = obj.getInt("count")
                    break
                }
            }

            // We use totalAccepted to detect *any* progress (including re-solving)
            val currentCount = totalAccepted 
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedCount = prefs.getInt(KEY_NUM_SUBMISSIONS, -1)

            Log.i(TAG, "================[ LEETCODE STATUS ]================")
            Log.i(TAG, "Time Checked      : ${Calendar.getInstance().time}")
            Log.i(TAG, "User              : $TARGET_USERNAME")
            Log.i(TAG, "Unique Problems   : $uniqueSolved")
            Log.i(TAG, "Total Accepted    : $totalAccepted")
            Log.i(TAG, "Stored Count      : $storedCount")
            
            if (storedCount == -1) {
                Log.i(TAG, ">> First run: Initializing counts.")
                prefs.edit()
                    .putInt(KEY_NUM_SUBMISSIONS, currentCount)
                    .putInt(KEY_UNIQUE_SOLVED, uniqueSolved)
                    .apply()
                Log.i(TAG, ">> Current Goal   : Increase total accepted from $currentCount")
                return false
            } else {
                // Determine new progress
                val solvedTodayCount = if (currentCount > storedCount) currentCount - storedCount else 0
                
                // Check for 1 problem unlock (Normal unlock)
                if (currentCount > storedCount) {
                    Log.i(TAG, ">> SUCCESS: New submission found! ($currentCount > $storedCount)")
                    markSolvedToday(context)
                    
                    // Don't update stored count yet if we want to track daily progress relative to start of day?
                    // Actually, if we update stored count, we lose the "baseline" for the day.
                    // But the original app logic was: stored count tracks "last known count".
                    // If we update it, next check shows 0 progress.
                    
                    // FIX: We need a SEPARATE baseline for "Start of Day Count" to verify 5 problems.
                    // For now, let's keep it simple: We allow the user to accumulate 5 problems.
                    // But if we update KEY_NUM_SUBMISSIONS, we lose the ability to count to 5 across multiple checks.
                    
                    // STRATEGY: 
                    // 1. KEY_NUM_SUBMISSIONS = Total count at very first install (or reset).
                    // Actually, we should probably treat KEY_NUM_SUBMISSIONS as the dynamic tracker.
                    // We need a KEY_START_OF_DAY_COUNT to track daily progress.
                    
                    // Let's implement a simpler "Daily Baseline" logic if not present.
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val dailyBaseline = prefs.getInt("daily_baseline", -1)
                    var effectiveBaseline = dailyBaseline
                    
                    val todayDate = getTodayDateString(context)
                    val lastBaselineDate = prefs.getString("daily_baseline_date", "")
                    
                    if (dailyBaseline == -1 || lastBaselineDate != todayDate) {
                        // New day or first run: Set baseline to storedCount (last known)
                        effectiveBaseline = storedCount
                        prefs.edit()
                            .putInt("daily_baseline", effectiveBaseline)
                            .putString("daily_baseline_date", todayDate)
                            .apply()
                         Log.i(TAG, ">> New Day Baseline set to: $effectiveBaseline")
                    }
                    
                    val actualSolvedToday = currentCount - effectiveBaseline
                    Log.i(TAG, ">> Today Solved So Far: $actualSolvedToday")
                    
                    if (actualSolvedToday >= 5) {
                        Log.i(TAG, ">> SUCCESS: 5 Problems Solved Goal Reached!")
                        markSolvedToday5(context)
                    }
                    
                    // Update latest count
                    prefs.edit()
                        .putInt(KEY_NUM_SUBMISSIONS, currentCount)
                        .putInt(KEY_UNIQUE_SOLVED, uniqueSolved)
                        .apply()
                        
                    return true
                } else {
                     // Check if ALREADY solved 5 today based on baseline
                     val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                     val dailyBaseline = prefs.getInt("daily_baseline", -1)
                     if (dailyBaseline != -1) {
                         val actualSolvedToday = currentCount - dailyBaseline
                          if (actualSolvedToday >= 5) {
                            if (!isSolvedToday5(context)) {
                                markSolvedToday5(context)
                            }
                        }
                     }

                     // Update unique count
                     prefs.edit().putInt(KEY_UNIQUE_SOLVED, uniqueSolved).apply()
                     
                     val solvedToday = isSolvedToday(context)
                     if (solvedToday) {
                          Log.i(TAG, ">> STATUS: ALREADY SOLVED TODAY (At least 1).")
                          return true
                     }
                     Log.i(TAG, ">> STATUS: PENDING.")
                }
            }
            Log.i(TAG, "==================================================")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing data", e)
        }
        return false

    }

    fun getDetailedStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val solved = isSolvedToday(context)
        val storedCount = prefs.getInt(KEY_NUM_SUBMISSIONS, 0)
        val uniqueSolved = prefs.getInt(KEY_UNIQUE_SOLVED, 0)
        
        val uniqueMsg = "Total Unique Solved: $uniqueSolved\n"
        
        val currentCount = prefs.getInt(KEY_NUM_SUBMISSIONS, 0)
        // Note: KEY_NUM_SUBMISSIONS actually stores the *starting* count for the day now, effectively.
        // Wait, the logic above in checkAndSaveStatus used storedCount as baseline.
        // But we actually need to persist the baseline somewhere else if we want KEY_NUM_SUBMISSIONS to be current.
        // Actually, KEY_NUM_SUBMISSIONS in the previous logic was holding the *last known* count.
        // To track "today's progress", we need a "start of day" count.
        
        // Let's assume for now we just show status based on isSolvedToday.
        // Since we don't have the "current total" readily available here without fetching, 
        // we can't show exact "2/5" progress in this text easily without another fetch or pref.
        
        return if (solved) {
            "${uniqueMsg}Status: [ COMPLETED ]\nYou are free for today!"
        } else {
            "${uniqueMsg}Status: [ PENDING ]\nGoal: Solve 5 problems to unlock specific apps."
        }
    }

    private fun markSolvedToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_SOLVED_DATE, getTodayDateString(context))
            .apply()
        Log.i(TAG, "Marked as solved for today!")
    }

    private fun markSolvedToday5(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_SOLVED_DATE_5, getTodayDateString(context))
            .apply()
        Log.i(TAG, "Marked as 5 PROBLEMS solved for today!")
    }

    /**
     * Fetches current UTC date from API and saves to SharedPreferences.
     * Call this when user clicks the tile. Only fetches if cache is empty or older than 24 hours.
     */
    fun refreshDateFromApiIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedAt = prefs.getLong(KEY_CACHED_UTC_DATE_AT, 0L)
        if (cachedAt > 0L && (System.currentTimeMillis() - cachedAt) < DATE_CACHE_TTL_MS) {
            Log.v(TAG, "Date cache still valid, skipping API fetch")
            return
        }
        try {
            val conn = URL(TIME_API_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val date = json.optString("date", "")
                if (date.isNotBlank()) {
                    Log.d(TAG, "Fetched date from gettimeapi.dev: $date (saved to SharedPreferences)")
                    prefs.edit()
                        .putString(KEY_CACHED_UTC_DATE, date)
                        .putLong(KEY_CACHED_UTC_DATE_AT, System.currentTimeMillis())
                        .apply()
                    return
                }
            }
            Log.e(TAG, "Time API returned $responseCode or empty date.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch date from Time API: ${e.message}", e)
        }
    }

    /** Returns today's date string (UTC). Uses cached value from SharedPreferences; never hits the API. */
    private fun getTodayDateString(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedDate = prefs.getString(KEY_CACHED_UTC_DATE, null)
        val cachedAt = prefs.getLong(KEY_CACHED_UTC_DATE_AT, 0L)
        if (cachedDate != null && cachedAt > 0L && (System.currentTimeMillis() - cachedAt) < DATE_CACHE_TTL_MS) {
            Log.v(TAG, "Using cached date: $cachedDate")
            return cachedDate
        }
        return getFallbackDateString()
    }

    fun resetProgress(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Only remove the "Solved Today" flag. 
        // We DO NOT clear the 'daily_baseline' or 'storedCount', so the 5-problem progress is preserved.
        prefs.edit().remove(KEY_LAST_SOLVED_DATE).apply()
        
        Log.i(TAG, "LeetCode 1-problem status reset! (5-problem progress preserved)")
    }

    private fun getFallbackDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}
