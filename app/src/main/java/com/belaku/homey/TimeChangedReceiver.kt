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



}
