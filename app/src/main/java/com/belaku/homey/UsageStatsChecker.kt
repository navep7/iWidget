package com.belaku.homey

import android.app.AppOpsManager
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

        val mode = try {
            val packageName = appContext.packageName
            val uid = Process.myUid()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        } catch (e: Exception) {
            Log.e("UsageStatsChecker", "Failed to check usage stats permission", e)
            AppOpsManager.MODE_ERRORED
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
    }
}
