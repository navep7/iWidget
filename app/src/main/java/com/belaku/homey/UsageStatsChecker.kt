package com.belaku.homey

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.provider.Settings
import android.util.Log

class UsageStatsChecker {
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appContext = context.applicationContext
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val packageName = appContext.packageName
        val uid = appContext.applicationInfo.uid

        // 1. Try checking via AppOpsManager
        try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    uid,
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    uid,
                    packageName
                )
            }
            if (mode == AppOpsManager.MODE_ALLOWED) {
                return true
            }
        } catch (e: SecurityException) {
            // This happens on some devices where the system identifies a UID/Package mismatch.
            // We log it and proceed to the fallback check.
            Log.w("UsageStatsChecker", "SecurityException during AppOps check for $packageName (uid: $uid): ${e.message}")
        } catch (e: Exception) {
            Log.e("UsageStatsChecker", "Error checking usage stats permission via AppOps", e)
        }

        // 2. Fallback: Try querying usage stats directly
        // This is often more reliable on devices where AppOps returns inconsistent results.
        return try {
            val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 // Check for any stats in the last minute
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            Log.e("UsageStatsChecker", "Fallback usage stats query check failed", e)
            false
        }
    }

    fun requestUsageStatsPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.e("UsageStatsChecker", "Failed to launch usage access settings", e)
        }
    }
}
