package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity


class ActivityTransitionReceiver : BroadcastReceiver() {

    private lateinit var applicationContext: Context

    override fun onReceive(context: Context, intent: Intent) {
        applicationContext = context.applicationContext
        appWidM = AppWidgetManager.getInstance(context)
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
        newAppWidget = ComponentName(context, NewAppWidget::class.java)

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Info about activity

                    if (toTransitionType(event.transitionType) == "ENTER") {
                        val detectedState = toActivityString(event.activityType).trim()
                        
                        // Fix: If we get a STILL event but SpeedService is running with non-zero speed, 
                        // ignore the STILL event as it's likely just a traffic stop.
                        if (detectedState == "STILL" && isMyServiceRunning(applicationContext, SpeedService::class.java)) {
                            val sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                            val currentSpeed = sharedPreferences.getInt("currentSpeed", 0)
                            if (currentSpeed > 3) { // If speed > 3 km/h, it's definitely not a real "STILL" state for the user
                                Log.d("ActivityTransition", "Ignoring STILL event because vehicle is moving at $currentSpeed km/h")
                                return@forEach
                            }
                        }

                        presentActivityState = detectedState
                        updateActivityState(event, presentActivityState)
                    }
                }
            }
        }
    }


    private fun updateActivityState(event: ActivityTransitionEvent, presentActivityState: String) {

        Log.d("applicationContext", presentActivityState)

        // Define your specific widget component and the Context
        val provider = ComponentName(applicationContext, NewAppWidget::class.java)
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)


        // Create the RemoteViews object targeting your widget's XML layout
        val remoteViews = RemoteViews(applicationContext.getPackageName(), R.layout.new_app_widget)

    //    makeToast(applicationContext, presentActivityState)

        if (presentActivityState == "STILL") {
            remoteViews.setViewVisibility(R.id.rl_still, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.rl_walking, View.GONE)
            remoteViews.setViewVisibility(R.id.rl_speed, View.GONE)

        //    remoteViews.setTextViewText(R.id.tx_activity_state, "STILL")
        //    remoteViews.setImageViewResource(R.id.imgv_steps, R.drawable.still)
            
            // If we transitioned to STILL, stop the SpeedService if it was running
            if (isMyServiceRunning(applicationContext, SpeedService::class.java)) {
                applicationContext.stopService(Intent(applicationContext, SpeedService::class.java))
                NewAppWidget.Companion.remoteViews?.setChronometer(R.id.speed_chronometer, 0L, null, false)
                NewAppWidget.Companion.remoteViews?.setViewVisibility(R.id.frame_speed, View.INVISIBLE)
                NewAppWidget.Companion.remoteViews?.setViewVisibility(R.id.frame_time_speed, View.INVISIBLE)
            }
        } else if (presentActivityState == "WALKING") {
            remoteViews.setViewVisibility(R.id.rl_walking, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews.setViewVisibility(R.id.rl_speed, View.GONE)

     //       remoteViews.setTextViewText(R.id.tx_activity_state, "WALKING")
      //      remoteViews.setImageViewResource(R.id.imgv_steps, R.drawable.steps)
            
            // If walking, also stop speed service
            if (isMyServiceRunning(applicationContext, SpeedService::class.java)) {
                applicationContext.stopService(Intent(applicationContext, SpeedService::class.java))
            }
        } else if (presentActivityState == "TRAVEL") {
            remoteViews.setViewVisibility(R.id.rl_speed, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews.setViewVisibility(R.id.rl_walking, View.GONE)


            if (toTransitionType(event.transitionType) == "ENTER") {

                NewAppWidget.Companion.remoteViews?.setViewVisibility(
                    R.id.frame_speed,
                    View.VISIBLE
                )
                NewAppWidget.Companion.remoteViews?.setViewVisibility(
                    R.id.frame_time_speed,
                    View.VISIBLE
                )
                if (!isMyServiceRunning(applicationContext, SpeedService::class.java)) {
                    applicationContext.startForegroundService(
                        Intent(
                            applicationContext,
                            SpeedService::class.java
                        )
                    )

                    val baseTime = SystemClock.elapsedRealtime()
                    NewAppWidget.Companion.remoteViews?.setChronometer(
                        R.id.speed_chronometer,
                        baseTime,
                        null,
                        true
                    )

             //       remoteViews.setTextViewText(R.id.tx_activity_state, "IN A VEHICLE")
          //          remoteViews.setImageViewResource(R.id.imgv_steps, R.drawable.in_a_vehicle)
                }
            }
        }


        // Push the update for all instances of the widget
        appWidgetManager.updateAppWidget(provider, remoteViews)

    }


    // types of activities
    fun toActivityString(activity: Int): String {
        return when (activity) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.IN_VEHICLE -> "TRAVEL"
            DetectedActivity.RUNNING -> "RUNNING"
            else -> "UNKNOWN"
        }
    }

    // type of transitions
    fun toTransitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }
}
