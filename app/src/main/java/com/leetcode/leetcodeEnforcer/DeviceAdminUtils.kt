package com.leetcode.leetcodeEnforcer

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object DeviceAdminUtils {
    private fun componentName(context: Context): ComponentName {
        return ComponentName(context, LeetCodeDeviceAdminReceiver::class.java)
    }

    fun isActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(componentName(context))
    }

    fun deviceAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_permission_description)
            )
        }
    }

    fun deactivate(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = componentName(context)
        if (dpm.isAdminActive(component)) {
            dpm.removeActiveAdmin(component)
        }
    }
}
