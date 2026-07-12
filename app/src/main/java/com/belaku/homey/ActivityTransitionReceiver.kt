package com.belaku.homey

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.widgetContext
import com.belaku.homey.StepsService.Companion.isLocationManagerInitialized
import com.belaku.homey.StepsService.Companion.locationListenerSpeed
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        appWidM = AppWidgetManager.getInstance(context)
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget).apply {
            setTextViewText(R.id.tx_activity_state, StepsService.presentActivityState)
            setImageViewResource(R.id.imgv_steps, StepsService.presentActivityStateImage)
        }
        newAppWidget = ComponentName(context, NewAppWidget::class.java)

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Info about activity

                    presentActivityState = toActivityString(event.activityType).trim()


                         if (presentActivityState == "STILL") {
                             stopSpeedService()
                        //     if (isLocationManagerInitialized())
                        //     StepsService.locationManager.removeUpdates(locationListenerSpeed)
                             remoteViews?.setTextViewText(R.id.tx_speed, "")
                             appWidM.partiallyUpdateAppWidget(R.id.tx_speed, remoteViews)
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "STILL")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.still)

                         } else if (presentActivityState == "WALKING") {
                             stopSpeedService()
                          //   ActivityTransitionReceiver().speedTracking()
                         //    if (isLocationManagerInitialized())
                           //  StepsService.locationManager.removeUpdates(locationListenerSpeed)
                             remoteViews?.setTextViewText(R.id.tx_speed, "")
                             appWidM.partiallyUpdateAppWidget(R.id.tx_speed, remoteViews)
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "WALKING")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.steps)

                         } else if (presentActivityState == "INVEHICLE") {
                          //   ActivityTransitionReceiver().speedTracking()
                             triggerSpeedService()
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "IN A VEHICLE")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.in_a_vehicle)
                         }

                    appWidM.partiallyUpdateAppWidget(R.id.tx_activity_state, remoteViews)
                    appWidM.partiallyUpdateAppWidget(R.id.imgv_steps, remoteViews)
                }
            }
        }
    }


    private fun stopSpeedService() {
        val intent = Intent(widgetContext, SpeedService::class.java)
        widgetContext.stopService(intent)
    }

    private fun triggerSpeedService() {
        val intent = Intent(widgetContext, SpeedService::class.java)
        // Required for Foreground Services on Android 8.0 (API 26) and above
        ContextCompat.startForegroundService(widgetContext, intent)
    }

    // types of activities
    fun toActivityString(activity: Int): String {
        return when (activity) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.WALKING -> "WALKING"
            DetectedActivity.IN_VEHICLE -> "IN VEHICLE"
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