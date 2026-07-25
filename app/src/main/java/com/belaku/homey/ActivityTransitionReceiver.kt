package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.widgetContext
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
                        presentActivityState = toActivityString(event.activityType).trim()
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

            remoteViews.setTextViewText(R.id.tx_activity_state, "STILL")
            remoteViews.setImageViewResource(R.id.imgv_steps, R.drawable.still)
        } else if (presentActivityState == "WALKING") {
            remoteViews.setViewVisibility(R.id.rl_walking, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews.setViewVisibility(R.id.rl_speed, View.GONE)

            remoteViews.setTextViewText(R.id.tx_activity_state, "WALKING")
            remoteViews.setImageViewResource(R.id.imgv_steps, R.drawable.steps)
        } else if (presentActivityState == "INVEHICLE") {
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
                if (!isMyServiceRunning(widgetContext, SpeedService::class.java))
                widgetContext.startForegroundService(
                    Intent(
                        widgetContext,
                        SpeedService::class.java
                    )
                )


                remoteViews.setTextViewText(R.id.tx_activity_state, "IN A VEHICLE")
                remoteViews.setImageViewResource(R.id.imgv_steps, R.drawable.in_a_vehicle)
            } else if (toTransitionType(event.transitionType) == "EXIT") {
                if(widgetContext.stopService(Intent(widgetContext, SpeedService::class.java))) {
                 //   makeToast(widgetContext, "  ⃠  ")

                    NewAppWidget.Companion.remoteViews?.setViewVisibility(R.id.frame_speed, View.INVISIBLE)
                    NewAppWidget.Companion.remoteViews?.setViewVisibility(R.id.frame_time_speed, View.INVISIBLE)
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
            DetectedActivity.IN_VEHICLE -> "INVEHICLE"
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