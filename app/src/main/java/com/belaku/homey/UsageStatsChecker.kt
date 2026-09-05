package com.belaku.homey

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

class UsageStatsChecker {
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appContext = context.applicationContext
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // 1. Try checking via AppOpsManager (Standard way)
        try {
            val packageName = appContext.packageName
            val uid = Process.myUid()

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
            // This happens on some devices where the system incorrectly identifies a UID/Package mismatch
            Log.w("UsageStatsChecker", "SecurityException during AppOps check: ${e.message}")
        } catch (e: Exception) {
            Log.e("UsageStatsChecker", "Error checking usage stats permission via AppOps", e)
        }

        // 2. Fallback: Try querying usage stats directly
        // If AppOps check fails due to SecurityException or returns DISALLOWED,
        // we try a direct query which works on many devices if the permission is actually granted.
        try {
            val usageStatsManager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 // Check for any stats in the last minute
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            return stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            Log.e("UsageStatsChecker", "Fallback usage stats query check failed", e)
        }

        return false
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
    }
}
