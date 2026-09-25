package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.belaku.homey.MainActivity.Companion.makeToast
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

            if (Top3.isEmpty())
            getAppsOfCurrentTimeWindow(context)
            else {
                if (currentMin % 10 == 0)
                    getAppsOfCurrentTimeWindow(context)
            }

            Log.d("TimeChangedReceiver", "Time tick received. Current Min : $currentHour : $currentMin")

            if (currentMin == 0) {
                if (isMyServiceRunning(context, SpeakService::class.java)) {
                    if (currentHour == 10 && calendar.get(Calendar.AM_PM) == 1) {
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
            var startTime = targetTimeMs - (5 * 60 * 1000)
            val endTime = targetTimeMs + (5 * 60 * 1000)

            var topApps = getAppUsageStatsForRange(
                context = context,
                startTime = startTime,
                endTime = endTime
            )

            if (topApps.isEmpty()) {
                startTime = targetTimeMs - (20 * 60 * 1000)
                topApps = getAppUsageStatsForRange(
                    context = context,
                    startTime = startTime,
                    endTime = endTime
                )
            }
            
            // Persist package names for the widget launch logic
            val pkgList = topApps.take(3).map { it.first }
            sharedPreferencesEditor.putString("TOP3_PKGS", Gson().toJson(pkgList)).apply()
            
            Top3.clear()
            topApps.take(3).forEach { (packageName, durationMs) ->
                val appName = getAppNameFromPkg(context, packageName)
                try {
                    val iconDrawable = context.packageManager.getApplicationIcon(packageName)
                    // Scale to 150x150 to stay under the RemoteViews size limit and render clearly
                    val bitmap = iconDrawable.toBitmap(150, 150)
                    Top3.add(App(appName, packageName, (durationMs / 1000).toString() + " s", bitmap))
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
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(updateIntent)
        }
    }
}
