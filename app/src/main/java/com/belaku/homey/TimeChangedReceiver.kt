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
import com.belaku.homey.SetWallWorker.Companion.isSharedPreferencesInitialized
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SpeakService.Companion.speakOut
import com.belaku.homey.StepsService.Companion.Top3
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.google.gson.Gson
import java.util.Calendar

class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_TICK) {

            // Initialize preferences if not already done to avoid lateinit crashes
            if (!isSharedPreferencesInitialized()) {
                val prefs = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                sharedPreferences = prefs
                sharedPreferencesEditor = prefs.edit()
            }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR)
            val currentMin = calendar.get(Calendar.MINUTE)

            // Predict apps the user might need based on usage habits over the last 7 days.
            // Habit calculation is throttled to every 15 minutes and run in the background
            // via goAsync() to keep the system responsive and save battery.
           /* if (currentMin % 15 == 0) {
                val pendingResult = goAsync()
                Thread {
                    try {
                        getAppsOfCurrentTimeWindow(context)
                    } catch (e: Exception) {
                        Log.e("TimeChangedReceiver", "Error predicting apps", e)
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            }*/

            Log.d("TimeChangedReceiver", "Time tick: ${calendar.get(Calendar.HOUR_OF_DAY)}:$currentMin")

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
                    val hourToSpeak = if (currentHour == 0) 12 else currentHour
                    speakOut(hourToSpeak.toString())
                }
            }
        }
    }

    /**
     * Predicts apps the user might need by aggregating usage patterns from the last 7 days
     * within a +/- 30-minute window of the current time. This "habit-based" approach
     * is more robust than just looking at yesterday's data.
     */
    private fun getAppsOfCurrentTimeWindow(context: Context) {
        if (!UsageStatsChecker().hasUsageStatsPermission(context)) return

        if (!isSharedPreferencesInitialized()) {
            val prefs = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            sharedPreferences = prefs
            sharedPreferencesEditor = prefs.edit()
        }

        val aggregatedScores = mutableMapOf<String, Long>()
        val aggregatedData = mutableMapOf<String, Pair<Long, Int>>() // pkg -> (totalDuration, totalOpens)

        // 1. Loop through the last 7 days to find consistent habits at this specific time
        for (day in 1..7) {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -day)
            }

            // Define a 1-hour window centered around the current time on each day
            val startTime = calendar.timeInMillis - (30 * 60 * 1000L)
            val endTime = calendar.timeInMillis + (30 * 60 * 1000L)

            val stats = getAppUsageStatsForRange(context, startTime, endTime)
            stats.forEach { (pkg, duration, opens) ->
                if (isAppInstalled(context, pkg) && isAppLaunchable(context, pkg)) {
                    // Decay weight: More recent days have higher impact (8 down to 2)
                    val weight = 9 - day
                    // Score = (duration in seconds + opens * 60s) * weight
                    val score = ((duration / 1000) + (opens * 60L)) * weight

                    aggregatedScores[pkg] = (aggregatedScores[pkg] ?: 0L) + score
                    val current = aggregatedData[pkg] ?: (0L to 0)
                    aggregatedData[pkg] = (current.first + duration) to (current.second + opens)
                }
            }
        }

        // 2. Take the top 3 apps based on aggregated habit scores
        val top3Result = aggregatedScores.toList()
            .sortedByDescending { it.second }
            .take(3)

        Log.d("TimeChangedReceiver", "Predicted Top Apps: ${top3Result.map { it.first }}")

        // Persist for widget logic
        val pkgList = top3Result.map { it.first }
        sharedPreferencesEditor.putString("TOP3_PKGS", Gson().toJson(pkgList)).apply()

        // Synchronize and recycle old bitmaps to prevent memory leaks before updating the widget
        synchronized(Top3) {
            Top3.forEach { app ->
                if (!app.iconBitmap.isRecycled) {
                    app.iconBitmap.recycle()
                }
            }
            Top3.clear()

            top3Result.forEach { (packageName, _) ->
                val data = aggregatedData[packageName] ?: (0L to 0)
                val appName = getAppNameFromPkg(context, packageName)
                try {
                    val iconDrawable = context.packageManager.getApplicationIcon(packageName)
                    val bitmap = iconDrawable.toBitmap(120, 120)
                    // Display average daily usage for this specific time slot
                    val usageStr = formatMilliseconds(data.first / 7)
                    Top3.add(App(appName, packageName, usageStr, bitmap, data.second / 7))
                } catch (e: Exception) {
                    Log.e("TimeChangedReceiver", "Error loading app icon for $packageName", e)
                }
            }
        }

        // 3. Trigger Widget Update
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, NewAppWidget::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)

        if (ids.isNotEmpty()) {
            val updateIntent = Intent(context, NewAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(updateIntent)
        }
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isAppLaunchable(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Exception) {
            false
        }
    }
}
