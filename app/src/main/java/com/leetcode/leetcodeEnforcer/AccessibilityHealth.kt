package com.leetcode.leetcodeEnforcer

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val ACCESSIBILITY_CHANNEL_ID = "leetcodeforcer_accessibility_health"
private const val ACCESSIBILITY_NOTIFICATION_ID = 2002
private const val ACCESSIBILITY_WORK_NAME = "accessibility-health-monitor"

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val componentName = ComponentName(context, LeetCodeForcerService::class.java)
    val flattenedComponent = componentName.flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()

    if (enabledServices.split(':').any { it.equals(flattenedComponent, ignoreCase = true) }) {
        return true
    }

    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
        val serviceInfo = it.resolveInfo.serviceInfo
        serviceInfo.packageName == componentName.packageName &&
            serviceInfo.name == componentName.className
    }
}

object AccessibilityHealthScheduler {
    fun ensureMonitoring(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<AccessibilityHealthWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(ACCESSIBILITY_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ACCESSIBILITY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        AccessibilityAlarmScheduler.reschedule(context.applicationContext)
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AccessibilityHealthWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(ACCESSIBILITY_WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

object BatteryOptimizationHelper {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val packageManager = context.packageManager
        val intentToLaunch = when {
            requestIntent.resolveActivity(packageManager) != null -> requestIntent
            fallbackIntent.resolveActivity(packageManager) != null -> fallbackIntent
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        context.startActivity(intentToLaunch)
    }
}

object AccessibilityHealthState {
    fun hasActiveEnforcement(context: Context): Boolean {
        return LeetCodeManager.isFrozen(context) ||
            LeetCodeManager.isFrozen2(context) ||
            LeetCodeManager.isFrozen3(context) ||
            FocusSettingsManager.getSessions(context).isNotEmpty() ||
            LeetCodeManager.isPreventUninstallEnabled(context)
    }

    fun shouldRunRecoveryTrigger(context: Context, action: String?): Boolean {
        return when (action) {
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT -> hasActiveEnforcement(context) && !isAccessibilityServiceEnabled(context)
            else -> hasActiveEnforcement(context)
        }
    }
}

object AccessibilityHealthNotifications {
    fun showRecoveryNotificationIfNeeded(context: Context) {
        if (!AccessibilityHealthState.hasActiveEnforcement(context)) {
            cancelRecoveryNotification(context)
            return
        }

        if (isAccessibilityServiceEnabled(context)) {
            cancelRecoveryNotification(context)
            return
        }

        ensureChannel(context)

        val launchIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val batteryIntent = Intent(context, AccessibilityRecoveryReceiver::class.java).apply {
            action = "com.leetcode.leetcodeEnforcer.OPEN_BATTERY_SETTINGS"
        }
        val batteryPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            batteryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
            "LeetCode Forcer detected that Accessibility is off while focus enforcement is configured. Tap to reopen Accessibility settings."
        } else {
            "LeetCode Forcer detected that Accessibility is off while focus enforcement is configured. Reopen Accessibility settings and disable battery restrictions for better reliability."
        }

        val notification = NotificationCompat.Builder(context, ACCESSIBILITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Accessibility check needed")
            .setContentText("LeetCode Forcer needs Accessibility turned on to keep enforcement active.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Open settings", pendingIntent)
            .apply {
                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                    addAction(0, "Battery settings", batteryPendingIntent)
                }
            }
            .build()

        NotificationManagerCompat.from(context).notify(ACCESSIBILITY_NOTIFICATION_ID, notification)
        AccessibilityAlarmScheduler.reschedule(context)
    }

    fun cancelRecoveryNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(ACCESSIBILITY_NOTIFICATION_ID)
        AccessibilityAlarmScheduler.reschedule(context)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            ACCESSIBILITY_CHANNEL_ID,
            "Accessibility health",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when the accessibility service stops while enforcement is active."
        }
        manager.createNotificationChannel(channel)
    }
}
