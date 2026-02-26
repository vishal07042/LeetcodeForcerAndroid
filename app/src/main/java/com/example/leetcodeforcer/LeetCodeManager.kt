package com.example.leetcodeforcer

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object LeetCodeManager {
    private const val TAG = "LeetCodeManager"
    private const val LEETCODE_API_ENDPOINT = "https://leetcode.com/graphql"

    private const val PREFS_NAME = "leet_prefs"
    private const val KEY_USERNAME = "leetcode_username"
    private const val KEY_LAST_SOLVED_DATE = "todayDateAfterChallenegeComplete"
    private const val KEY_LAST_SOLVED_DATE_5 = "todayDateAfterChallenegeComplete5"
    private const val KEY_NUM_SUBMISSIONS = "numSubmissions"
    private const val KEY_UNIQUE_SOLVED = "uniqueSolved"
    private const val KEY_CACHED_UTC_DATE = "cached_utc_date"
    private const val KEY_CACHED_UTC_DATE_AT = "cached_utc_date_at"
    private const val DATE_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
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

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, "")?.trim().orEmpty()
    }

    fun setUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USERNAME, username.trim()).apply()
    }

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
            val username = getUsername(context)
            if (username.isBlank()) {
                Log.w(TAG, "Username not configured")
                return false
            }
            val responseJson = fetchLeetCodeStats(username) ?: return false
            return processLeetCodeData(context, responseJson, username)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking status", e)
            return false
        }
    }

    private fun fetchLeetCodeStats(username: String): JSONObject? {
        try {
            val conn = URL(LEETCODE_API_ENDPOINT).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject()
            jsonBody.put("query", LEETCODE_ALL_PROBLEMS_QUERY)
            val variables = JSONObject()
            variables.put("username", username)
            jsonBody.put("variables", variables)

            conn.outputStream.use { it.write(jsonBody.toString().toByteArray()) }

            val responseCode = conn.responseCode
            Log.d(TAG, "Request to LeetCode API. Response Code: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                return JSONObject(response)
            }
            Log.e(TAG, "HTTP Error: $responseCode")
        } catch (e: Exception) {
            Log.e(TAG, "Network Error", e)
        }
        return null
    }

    private fun processLeetCodeData(context: Context, data: JSONObject, username: String): Boolean {
        try {
            if (!data.has("data") || data.isNull("data")) return false
            val dataObj = data.getJSONObject("data")
            if (dataObj.isNull("matchedUser")) return false

            val submitStats = dataObj.getJSONObject("matchedUser").getJSONObject("submitStats")
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

            val currentCount = totalAccepted
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedCount = prefs.getInt(KEY_NUM_SUBMISSIONS, -1)

            Log.i(TAG, "================[ LEETCODE STATUS ]================")
            Log.i(TAG, "Time Checked      : ${Calendar.getInstance().time}")
            Log.i(TAG, "User              : $username")
            Log.i(TAG, "Unique Problems   : $uniqueSolved")
            Log.i(TAG, "Total Accepted    : $totalAccepted")
            Log.i(TAG, "Stored Count      : $storedCount")

            if (storedCount == -1) {
                prefs.edit()
                    .putInt(KEY_NUM_SUBMISSIONS, currentCount)
                    .putInt(KEY_UNIQUE_SOLVED, uniqueSolved)
                    .apply()
                return false
            }

            if (currentCount > storedCount) {
                markSolvedToday(context)

                val dailyBaseline = prefs.getInt("daily_baseline", -1)
                var effectiveBaseline = dailyBaseline
                val todayDate = getTodayDateString(context)
                val lastBaselineDate = prefs.getString("daily_baseline_date", "")

                if (dailyBaseline == -1 || lastBaselineDate != todayDate) {
                    effectiveBaseline = storedCount
                    prefs.edit()
                        .putInt("daily_baseline", effectiveBaseline)
                        .putString("daily_baseline_date", todayDate)
                        .apply()
                }

                val actualSolvedToday = currentCount - effectiveBaseline
                if (actualSolvedToday >= 5) {
                    markSolvedToday5(context)
                }

                prefs.edit()
                    .putInt(KEY_NUM_SUBMISSIONS, currentCount)
                    .putInt(KEY_UNIQUE_SOLVED, uniqueSolved)
                    .apply()
                return true
            }

            val dailyBaseline = prefs.getInt("daily_baseline", -1)
            if (dailyBaseline != -1) {
                val actualSolvedToday = currentCount - dailyBaseline
                if (actualSolvedToday >= 5 && !isSolvedToday5(context)) {
                    markSolvedToday5(context)
                }
            }

            prefs.edit().putInt(KEY_UNIQUE_SOLVED, uniqueSolved).apply()
            return isSolvedToday(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing data", e)
        }
        return false
    }

    fun getDetailedStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val username = getUsername(context)
        if (username.isBlank()) {
            return "Status: [ PENDING ]\nSet your LeetCode username to start enforcement."
        }

        val solved = isSolvedToday(context)
        val uniqueSolved = prefs.getInt(KEY_UNIQUE_SOLVED, 0)
        val uniqueMsg = "Username: $username\nTotal Unique Solved: $uniqueSolved\n"

        return if (solved) {
            "${uniqueMsg}Status: [ COMPLETED ]\nYou are free for today!"
        } else {
            "${uniqueMsg}Status: [ PENDING ]\nGoal: Solve LeetCode problems to unlock blocked apps."
        }
    }

    private fun markSolvedToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SOLVED_DATE, getTodayDateString(context)).apply()
    }

    private fun markSolvedToday5(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SOLVED_DATE_5, getTodayDateString(context)).apply()
    }

    fun refreshDateFromApiIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedAt = prefs.getLong(KEY_CACHED_UTC_DATE_AT, 0L)
        if (cachedAt > 0L && (System.currentTimeMillis() - cachedAt) < DATE_CACHE_TTL_MS) {
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

    private fun getTodayDateString(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedDate = prefs.getString(KEY_CACHED_UTC_DATE, null)
        val cachedAt = prefs.getLong(KEY_CACHED_UTC_DATE_AT, 0L)
        if (cachedDate != null && cachedAt > 0L && (System.currentTimeMillis() - cachedAt) < DATE_CACHE_TTL_MS) {
            return cachedDate
        }
        return getFallbackDateString()
    }

    fun resetProgress(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_SOLVED_DATE).apply()
    }

    private fun getFallbackDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}
