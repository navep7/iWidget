package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.belaku.homey.SetWallWorker.Companion.formatMilliseconds
import com.belaku.homey.SetWallWorker.Companion.getAppNameFromPkg
import com.belaku.homey.SetWallWorker.Companion.getAppUsageStatsForRange
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SpeakService.Companion.speakOut
import com.belaku.homey.StepsService.Companion.Top3
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.google.gson.Gson
import java.util.Calendar

class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_TICK) {

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR)
            val currentMin = calendar.get(Calendar.MINUTE)

            // Refresh Top3 apps every 10 minutes or if empty
            if (Top3.isEmpty() || currentMin % 10 == 0) {
                getAppsOfCurrentTimeWindow(context)
            }

            Log.d("TimeChangedReceiver", "Time tick received. Current time: $currentHour:$currentMin")

            // Hourly announcement
            if (currentMin == 0) {
                if (isMyServiceRunning(context, SpeakService::class.java)) {
                    // Stop service at 10 PM
                    if (currentHour == 10 && calendar.get(Calendar.AM_PM) == Calendar.PM) {
                        val speakIntent = Intent(context, SpeakService::class.java)
                        context.stopService(speakIntent)
                        NewAppWidget.applyTimeAnnouncementState(
                            false,
                            ColorUtil().isColorDark(NewAppWidget.primaryColor)
                        )
                        sharedPreferencesEditor.putBoolean("SPKSERVICE", false).apply()
                    }
                    speakOut(currentHour.toString())
                }
            }
        }
    }

    private fun getAppsOfCurrentTimeWindow(context: Context) {
        if (UsageStatsChecker().hasUsageStatsPermission(context)) {
            val yesterdayTarget = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            
            val targetTimeMs = yesterdayTarget.timeInMillis
            var rangeMinutes = 5
            var topApps: List<Triple<String, Long, Int>> = emptyList()

            // Incrementally expand search window if not enough apps found
            while (rangeMinutes <= 60) {
                val startTime = targetTimeMs - (rangeMinutes * 60 * 1000)
                val endTime = targetTimeMs + (rangeMinutes * 60 * 1000)
                
                topApps = getAppUsageStatsForRange(context, startTime, endTime)
                    .filter { isAppInstalled(context, it.first) && isAppLaunchable(context, it.first) }
                
                if (topApps.size >= 3) break
                rangeMinutes += 15
            }

            val top3Result = topApps.take(3)
            
            // Persist package names for the widget launch logic
            val pkgList = top3Result.map { it.first }
            sharedPreferencesEditor.putString("TOP3_PKGS", Gson().toJson(pkgList)).apply()

            Top3.clear()
            top3Result.forEach { (packageName, durationMs, openCount) ->
                val appName = getAppNameFromPkg(context, packageName)
                try {
                    val iconDrawable = context.packageManager.getApplicationIcon(packageName)
                    // Downscale bitmap to stay safely under RemoteViews Binder limit (TransactionTooLargeException)
                    val bitmap = iconDrawable.toBitmap(120, 120)
                    val usageStr = formatMilliseconds(durationMs)
                    Top3.add(App(appName, packageName, usageStr, bitmap, openCount))
                } catch (e: Exception) {
                    Log.e("TimeChangedReceiver", "Error adding app icon for $packageName", e)
                }
            }

            // Trigger Widget Update
            val updateIntent = Intent(context, NewAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, NewAppWidget::class.java)
                )
                if (ids.isNotEmpty()) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    context.sendBroadcast(this)
                }
            }
        }
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isAppLaunchable(context: Context, packageName: String): Boolean {
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }
}
