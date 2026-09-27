package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.time.LocalDate

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val applicationContext = context.applicationContext

        if (!ActivityTransitionResult.hasResult(intent)) return

        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val sharedPrefs = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        result.transitionEvents.forEach { event ->
            // Only process ENTER transitions as entering a new state implies exiting the previous one.
            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                val detectedState = toActivityString(event.activityType)
                if (detectedState == "UNKNOWN") return@forEach

                // Filter out false "STILL" events if the vehicle is actually moving at significant speed.
                if (detectedState == "STILL") {
                    val currentSpeed = sharedPrefs.getInt("current_speed", 0)
                    if (currentSpeed > 3 && isMyServiceRunning(applicationContext, SpeedService::class.java)) {
                        Log.d("ActivityTransition", "Ignoring STILL: GPS speed is $currentSpeed km/h")
                        return@forEach
                    }
                }

                val oldState = sharedPrefs.getString("presentActivityState", "")
                if (oldState == detectedState) return@forEach

                Log.d("ActivityTransition", "Moving from $oldState to $detectedState")

                // 1. Save the total duration of the activity we are leaving.
                savePreviousStateDuration(sharedPrefs, oldState)

                // 2. Update persistent state and the global companion variable.
                // commit = true is used here to ensure the data is written immediately before the receiver process finishes.
                presentActivityState = detectedState
                val baseTime = SystemClock.elapsedRealtime()

                sharedPrefs.edit(commit = true) {
                    putString("presentActivityState", detectedState)
                    // Reset chronometer base times for the new active state.
                    when (detectedState) {
                        "STILL" -> {
                            putLong("stillChr", baseTime)
                            putLong("walkChr", 0L)
                            putLong("speedChr", 0L)
                        }
                        "WALKING" -> {
                            putLong("walkChr", baseTime)
                            putLong("stillChr", 0L)
                            putLong("speedChr", 0L)
                        }
                        "TRAVEL" -> {
                            putLong("speedChr", baseTime)
                            putLong("speed_trip_start_time", baseTime)
                            putLong("stillChr", 0L)
                            putLong("walkChr", 0L)
                        }
                    }
                }

                // 3. Start or Stop the SpeedService based on the new state.
                handleServiceTransitions(applicationContext, detectedState)

                // 4. Trigger a widget refresh broadcast. This forces NewAppWidget to recreate RemoteViews
                // and re-apply all adaptive backgrounds, colors, and listeners based on the updated state.
                triggerWidgetUpdate(applicationContext)

                makeToast(applicationContext, detectedState)
            }
        }
    }

    private fun savePreviousStateDuration(sharedPrefs: SharedPreferences, oldState: String?) {
        val now = SystemClock.elapsedRealtime()
        val dayKey = LocalDate.now().dayOfWeek.name

        when (oldState) {
            "WALKING" -> {
                val startTime = sharedPrefs.getLong("walkChr", 0L)
                if (startTime != 0L) {
                    val duration = now - startTime
                    val totalKey = "${dayKey}_walk_duration"
                    val currentTotal = sharedPrefs.getLong(totalKey, 0L)
                    sharedPrefs.edit(commit = true) { putLong(totalKey, currentTotal + duration) }
                }
            }
            "TRAVEL" -> {
                val startTime = sharedPrefs.getLong("speedChr", 0L)
                if (startTime != 0L) {
                    val duration = now - startTime
                    val totalKey = "${dayKey}_travel_duration"
                    val currentTotal = sharedPrefs.getLong(totalKey, 0L)
                    sharedPrefs.edit(commit = true) { putLong(totalKey, currentTotal + duration) }
                }
            }
        }
    }

    private fun handleServiceTransitions(context: Context, newState: String) {
        if (newState == "TRAVEL") {
            try {
                val intent = Intent(context, SpeedService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("ActivityTransition", "Start SpeedService failed", e)
            }
        } else {
            try {
                if (isMyServiceRunning(context, SpeedService::class.java)) {
                    context.stopService(Intent(context, SpeedService::class.java))
                }
            } catch (e: Exception) {
                Log.e("ActivityTransition", "Stop SpeedService failed", e)
            }
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NewAppWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = componentName
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            Log.e("ActivityTransition", "Widget update trigger failed", e)
        }
    }

    private fun toActivityString(activity: Int): String = when (activity) {
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.WALKING, DetectedActivity.RUNNING -> "WALKING"
        DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE -> "TRAVEL"
        else -> "UNKNOWN"
    }
}
