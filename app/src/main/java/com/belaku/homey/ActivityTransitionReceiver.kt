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
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        appWidM = AppWidgetManager.getInstance(context)
        remoteViews =
            RemoteViews(context.packageName, R.layout.new_app_widget)
        newAppWidget = ComponentName(context, NewAppWidget::class.java)

        //   // makeToast("!onReceiveAR ${intent.action}")
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Info about activity

                         if (toActivityString(event.activityType).trim() == "STILL") {
                             StepsService.presentActivityState = "STILL"
                             StepsService.presentActivityStateImage = R.drawable.still

                             performPartialUpdate(StepsService.presentActivityState, widgetContext, R.id.tx_activity_state)

                         } else if (toActivityString(event.activityType).trim() == "WALKING") {
                             StepsService.presentActivityState = "WALKING"
                             StepsService.presentActivityStateImage = R.drawable.steps


                             performPartialUpdate(
                                 StepsService.presentActivityState,
                                 widgetContext,
                                 R.id.tx_activity_state
                             )
                         } else if (toActivityString(event.activityType).trim() == "INVEHICLE") {
                             StepsService.presentActivityState = "INVEHICLE"
                             StepsService.presentActivityStateImage = R.drawable.in_a_vehicle


                             performPartialUpdate(
                                 StepsService.presentActivityState,
                                 widgetContext,
                                 R.id.tx_activity_state
                             )
                         }
                }
            }
        }
    }

    private fun performPartialUpdate(state: String, context: Context, appWidgetId: Int) {

        // Target only the views you want to change
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget).apply {
            setTextViewText(R.id.tx_activity_state, StepsService.presentActivityState)
            setImageViewResource(R.id.imgv_steps, StepsService.presentActivityStateImage)
        }

        // Send the partial update
        appWidM.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }



    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun speedTracking() {
        Toast.makeText(widgetContext, "!speedTracking", Toast.LENGTH_SHORT).show()

        locationListenerSpeed =
            LocationListener() { location ->
                run {

                    // makeToast("!locationRrd")

                    if (location.hasSpeed()) {
                        val speedInMps = location.speed // Speed in meters/second

                        // Convert to km/h (optional)
                        val speedInKmph = (speedInMps * 3.6).toInt()


                        /* var rBitmap = Bitmap.createScaledBitmap(
                             BitmapRotated(speedInKmph, widgetContext),
                             95,
                             95,
                             true
                         )*/


                        speedR(speedInKmph.toString())
                    } else speedR("0.0")

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

        // makeToast("!speedR")

        //      remoteViews?.setImageViewBitmap(R.id.needle, rBit)
        /*     remoteViews?.setTextViewText(R.id.tx_speed, strSpeed)
             remoteViews?.setViewVisibility(R.id.needle, View.VISIBLE)
             remoteViews?.setViewVisibility(R.id.tx_speed, View.VISIBLE)*/
        appWidM.updateAppWidget(newAppWidget, remoteViews)

        //    StepsService.locationManager.removeUpdates(locationListenerSpeed)

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            StepsService.locationManager.removeUpdates(locationListenerSpeed)
        }, 600000)


    }

    fun decodeSampledBitmapFromResource(
        resId: Int,
        reqWidth: Int,
        reqHeight: Int,
        context: Context
    ): Bitmap {

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(context.resources, resId, options)
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps
            // both height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
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