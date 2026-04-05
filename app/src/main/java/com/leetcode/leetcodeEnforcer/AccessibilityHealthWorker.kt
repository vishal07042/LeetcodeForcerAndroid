package com.leetcode.leetcodeEnforcer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AccessibilityHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AccessibilityHealthNotifications.showRecoveryNotificationIfNeeded(applicationContext)
        AccessibilityAlarmScheduler.reschedule(applicationContext)
        return Result.success()
    }
}
