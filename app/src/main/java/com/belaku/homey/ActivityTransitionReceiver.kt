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
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.widgetContext
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
                             if (StepsService.isLocationManagerInitialized())
                             StepsService.locationManager.removeUpdates(locationListenerSpeed)
                             remoteViews?.setTextViewText(R.id.tx_speed, "")
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "STILL")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.still)
                             appWidM.updateAppWidget(intArrayOf(R.id.tx_activity_state, R.id.imgv_steps, R.id.tx_speed), remoteViews)
                         } else if (presentActivityState == "WALKING") {
                             ActivityTransitionReceiver().speedTracking()
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "WALKING")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.steps)
                             appWidM.updateAppWidget(intArrayOf(R.id.tx_activity_state, R.id.imgv_steps), remoteViews)
                         } else if (presentActivityState == "INVEHICLE") {
                             ActivityTransitionReceiver().speedTracking()
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "IN A VEHICLE")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.in_a_vehicle)
                             appWidM.updateAppWidget(intArrayOf(R.id.tx_activity_state, R.id.imgv_steps), remoteViews)
                         }
                }
            }
        }
    }




    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun speedTracking() {

        locationListenerSpeed =
            LocationListener() { location ->
                run {

                    if (location.hasSpeed()) {
                        val speedInMps = location.speed // Speed in meters/second

                        // Convert to km/h (optional)
                        val speedInKmph = (speedInMps * 3.6).toInt()

                        speedR(speedInKmph.toString())
                    } else speedR("0.0")

                }
            }

        StepsService.locationManager.requestLocationUpdates(
            LocationManager.FUSED_PROVIDER,
            0,
            0f,
            locationListenerSpeed
        )
    }

    private fun speedR(strSpeed: String) {

        var speed = 0

        if (strSpeed.contains("."))
            speed = strSpeed.split(".")[0].trim().toInt()
        else speed = strSpeed.trim().toInt()

        if (speed > 5)
        remoteViews?.setTextViewText(R.id.tx_speed, strSpeed + " Kmph")
        else remoteViews?.setTextViewText(R.id.tx_speed, "")

        appWidM.updateAppWidget(R.id.tx_speed, remoteViews)
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