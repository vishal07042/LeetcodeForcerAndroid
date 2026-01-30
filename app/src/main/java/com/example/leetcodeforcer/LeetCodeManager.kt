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
    private const val KEY_NUM_SUBMISSIONS = "numSubmissions"
    private const val KEY_UNIQUE_SOLVED = "uniqueSolved"

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
        val today = getTodayDateString()
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
            } else if (currentCount > storedCount) {
                Log.i(TAG, ">> SUCCESS: New submission found! ($currentCount > $storedCount)")
                markSolvedToday(context)
                prefs.edit()
                    .putInt(KEY_NUM_SUBMISSIONS, currentCount)
                    .putInt(KEY_UNIQUE_SOLVED, uniqueSolved)
                    .apply()
                return true
            } else {
                // Update unique count even if total submissions didn't increase (just in case)
                prefs.edit().putInt(KEY_UNIQUE_SOLVED, uniqueSolved).apply()
                
                val solvedToday = isSolvedToday(context)
                if (solvedToday) {
                     Log.i(TAG, ">> STATUS: ALREADY SOLVED TODAY.")
                     return true
                }
                Log.i(TAG, ">> STATUS: PENDING. (Need to solve at least 1 problem)")
                Log.i(TAG, ">> HINT: Go to LeetCode and get one 'Accepted' submission.")
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
        
        return if (solved) {
            "${uniqueMsg}Status: [ COMPLETED ]\nYou are free for today!"
        } else {
            "${uniqueMsg}Status: [ PENDING ]\nGoal: Get ONE more accepted."
        }
    }

    private fun markSolvedToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_SOLVED_DATE, getTodayDateString())
            .apply()
        Log.i(TAG, "Marked as solved for today!")
    }

    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}
