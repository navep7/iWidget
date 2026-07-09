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
                        //     if (isLocationManagerInitialized())
                        //     StepsService.locationManager.removeUpdates(locationListenerSpeed)
                             remoteViews?.setTextViewText(R.id.tx_speed, "")
                             appWidM.partiallyUpdateAppWidget(R.id.tx_speed, remoteViews)
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "STILL")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.still)

                         } else if (presentActivityState == "WALKING") {
                          //   ActivityTransitionReceiver().speedTracking()
                         //    if (isLocationManagerInitialized())
                           //  StepsService.locationManager.removeUpdates(locationListenerSpeed)
                             remoteViews?.setTextViewText(R.id.tx_speed, "")
                             appWidM.partiallyUpdateAppWidget(R.id.tx_speed, remoteViews)
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "WALKING")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.steps)

                         } else if (presentActivityState == "INVEHICLE") {
                          //   ActivityTransitionReceiver().speedTracking()
                             remoteViews?.setTextViewText(R.id.tx_activity_state, "IN A VEHICLE")
                             remoteViews?.setImageViewResource(R.id.imgv_steps, R.drawable.in_a_vehicle)
                         }

                    appWidM.partiallyUpdateAppWidget(R.id.tx_activity_state, remoteViews)
                    appWidM.partiallyUpdateAppWidget(R.id.imgv_steps, remoteViews)
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
                    }

                }
            }

        StepsService.locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
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

        if (speed > 5) {
            remoteViews?.setTextViewText(R.id.tx_speed, speed.toString() + " KmpH")
            appWidM.partiallyUpdateAppWidget(R.id.tx_speed, remoteViews)
        } else {
            remoteViews?.setTextViewText(R.id.tx_speed, "")
            appWidM.partiallyUpdateAppWidget(R.id.tx_speed, remoteViews)
        }
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