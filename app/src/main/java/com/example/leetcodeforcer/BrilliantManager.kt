package com.example.leetcodeforcer

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.app.usage.UsageStatsManager.INTERVAL_DAILY
import java.util.Calendar

const val MIN_BRILLIANT_MINUTES = 30L

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageStatsPermission(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

fun getBrilliantUsageMinutes(context: Context): Long {
    if (!hasUsageStatsPermission(context)) return 0L
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()
    val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(INTERVAL_DAILY, startTime, endTime)
    val brilliantPackage = "org.brilliant.android"
    val brilliantStats = usageStatsList.find { it.packageName == brilliantPackage }
    return brilliantStats?.totalTimeInForeground?.div(60000) ?: 0L
}

/** True when user has used Brilliant for at least MIN_BRILLIANT_MINUTES today. */
fun isBrilliantTaskDone(context: Context): Boolean {
    return getBrilliantUsageMinutes(context) >= MIN_BRILLIANT_MINUTES
}

fun getBrilliantStatusMessage(context: Context): String {
    if (!hasUsageStatsPermission(context)) {
        return "Brilliant: Grant Usage Access to track (need $MIN_BRILLIANT_MINUTES min/day)"
    }
    val minutesDone = getBrilliantUsageMinutes(context)
    return if (minutesDone >= MIN_BRILLIANT_MINUTES) {
        "Brilliant: [ COMPLETED ]\nMinutes done: $minutesDone\nMinutes to go: 0 (goal reached)"
    } else {
        val minutesToGo = MIN_BRILLIANT_MINUTES - minutesDone
        "Brilliant: [ PENDING ]\nMinutes done: $minutesDone\nMinutes to go: $minutesToGo (goal: $MIN_BRILLIANT_MINUTES min)"
    }
}
