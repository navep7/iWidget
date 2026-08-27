package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val applicationContext = context.applicationContext

        // Safely initialize widget companion properties if needed
        try {
            appWidM = AppWidgetManager.getInstance(applicationContext)
            newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)
        } catch (e: Exception) {
            Log.e("ActivityTransition", "Failed to initialize widget manager", e)
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    if (toTransitionType(event.transitionType) == "ENTER") {
                        val detectedState = toActivityString(event.activityType).trim()

                        // Fix: Ignore STILL if the vehicle is actually moving (GPS/Activity Recognition mismatch)
                        if (detectedState == "STILL") {
                            val sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                            val currentSpeed = sharedPreferences.getInt("current_speed", 0)
                            if (currentSpeed > 3 && isMyServiceRunning(applicationContext, SpeedService::class.java)) {
                                Log.d("ActivityTransition", "Ignoring STILL event: vehicle moving at $currentSpeed km/h")
                                return@forEach
                            }
                        }

                        presentActivityState = detectedState
                        updateActivityState(applicationContext, detectedState)
                    }
                }
            }
        }
    }

    private fun updateActivityState(context: Context, state: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, NewAppWidget::class.java)
        val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        
        val rv = RemoteViews(context.packageName, R.layout.new_app_widget)

        when (state) {
            "STILL" -> {
                rv.setViewVisibility(R.id.rl_still, View.VISIBLE)
                rv.setViewVisibility(R.id.rl_walking, View.GONE)
                rv.setViewVisibility(R.id.rl_speed, View.GONE)
                
                rv.setChronometer(R.id.speed_chronometer, 0L, null, false)
                rv.setViewVisibility(R.id.frame_speed, View.INVISIBLE)
                rv.setViewVisibility(R.id.frame_time_speed, View.INVISIBLE)
                
                sharedPreferences.edit().putLong("speed_trip_start_time", 0L).apply()
                stopSpeedService(context)
            }
            "WALKING" -> {
                rv.setViewVisibility(R.id.rl_walking, View.VISIBLE)
                rv.setViewVisibility(R.id.rl_still, View.GONE)
                rv.setViewVisibility(R.id.rl_speed, View.GONE)
                
                stopSpeedService(context)
            }
            "TRAVEL" -> {
                rv.setViewVisibility(R.id.rl_speed, View.VISIBLE)
                rv.setViewVisibility(R.id.rl_still, View.GONE)
                rv.setViewVisibility(R.id.rl_walking, View.GONE)

                rv.setViewVisibility(R.id.frame_speed, View.VISIBLE)
                rv.setViewVisibility(R.id.frame_time_speed, View.VISIBLE)
                
                if (!isMyServiceRunning(context, SpeedService::class.java)) {
                    val baseTime = SystemClock.elapsedRealtime()
                    sharedPreferences.edit().putLong("speed_trip_start_time", baseTime).apply()
                    rv.setChronometer(R.id.speed_chronometer, baseTime, null, true)
                    startSpeedService(context)
                } else {
                    val baseTime = sharedPreferences.getLong("speed_trip_start_time", SystemClock.elapsedRealtime())
                    rv.setChronometer(R.id.speed_chronometer, baseTime, null, true)
                }
            }
        }

        try {
            remoteViews = rv
            appWidgetManager.updateAppWidget(provider, rv)
        } catch (e: Exception) {
            Log.e("ActivityTransition", "Widget update failed", e)
        }
    }

    private fun startSpeedService(context: Context) {
        try {
            val intent = Intent(context, SpeedService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("ActivityTransition", "Start service failed", e)
        }
    }

    private fun stopSpeedService(context: Context) {
        try {
            if (isMyServiceRunning(context, SpeedService::class.java)) {
                context.stopService(Intent(context, SpeedService::class.java))
            }
        } catch (e: Exception) {
            Log.e("ActivityTransition", "Stop service failed", e)
        }
    }

    private fun toActivityString(activity: Int): String = when (activity) {
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.WALKING -> "WALKING"
        DetectedActivity.IN_VEHICLE -> "TRAVEL"
        DetectedActivity.RUNNING -> "RUNNING"
        else -> "UNKNOWN"
    }

    private fun toTransitionType(transitionType: Int): String = when (transitionType) {
        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
        else -> "UNKNOWN"
    }
}
