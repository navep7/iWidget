package com.belaku.homey

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.isAppWidMInitialized
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.time.LocalDate

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val applicationContext = context.applicationContext

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

                        val sharedPrefs = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
                        val oldState = sharedPrefs.getString("presentActivityState", "")

                        if (oldState == detectedState) return@forEach

                        // Save walking duration if stopping WALKING
                        if (oldState == "WALKING") {
                            val walkStartTime = sharedPrefs.getLong("walkChr", 0L)
                            if (walkStartTime != 0L) {
                                val streakDuration = SystemClock.elapsedRealtime() - walkStartTime
                                val todayKey = LocalDate.now().dayOfWeek.name + "_walk_duration"
                                val previousTotal = sharedPrefs.getLong(todayKey, 0L)
                                sharedPrefs.edit { putLong(todayKey, previousTotal + streakDuration) }
                            }
                        }
                        
                        // Save travel duration if stopping TRAVEL
                        if (oldState == "TRAVEL") {
                            val travelStartTime = sharedPrefs.getLong("speedChr", 0L)
                            if (travelStartTime != 0L) {
                                val streakDuration = SystemClock.elapsedRealtime() - travelStartTime
                                val todayKey = LocalDate.now().dayOfWeek.name + "_travel_duration"
                                val previousTotal = sharedPrefs.getLong(todayKey, 0L)
                                sharedPrefs.edit { putLong(todayKey, previousTotal + streakDuration) }
                            }
                        }

                        presentActivityState = detectedState
                        // Save persistent state for widget
                        sharedPrefs.edit {
                            putString("presentActivityState", detectedState)
                        }
                        
                        updateActivityState(applicationContext, detectedState, event.transitionType)
                    }
                }
            }
        }
    }

    private fun updateActivityState(context: Context, state: String, transitionType: Int) {

        makeToast(context, state)
        if (!isAppWidMInitialized()) {
            appWidM = AppWidgetManager.getInstance(context)
            newAppWidget = ComponentName(context, NewAppWidget::class.java)
            remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
        }
        val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)


        when (state) {
            "STILL" -> {
                if (transitionType == 0) {
                    val baseTime = SystemClock.elapsedRealtime()
                    remoteViews?.setViewVisibility(R.id.still_chronometer, View.VISIBLE)
                    remoteViews?.setChronometer(R.id.still_chronometer, baseTime, null, true)
                    sharedPreferences.edit { 
                        putLong("stillChr", baseTime)
                        putLong("walkChr", 0L)
                        putLong("speedChr", 0L)
                    }
                    remoteViews?.setTextViewText(R.id.tx_act_state, "STILL")
                    remoteViews?.setChronometer(R.id.walk_chronometer, SystemClock.elapsedRealtime(), null, false)
                    remoteViews?.setChronometer(R.id.speed_chronometer, SystemClock.elapsedRealtime(), null, false)
                    remoteViews?.setViewVisibility(R.id.walk_chronometer, View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)
                }

                remoteViews?.setTextViewText(R.id.tx_act_count, sharedPreferences.getInt("waterCountToday", 0).toString() + "\uD800\uDCEF" )
                remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.still)
                remoteViews?.setViewVisibility(R.id.rl_still, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.tx_act_plus, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking, View.GONE)
                remoteViews?.setViewVisibility(R.id.rl_speed, View.GONE)

                stopSpeedService(context)
            }
            "WALKING" -> {
                if (transitionType == 0) {
                    val baseTime = SystemClock.elapsedRealtime()

                    remoteViews?.setViewVisibility(R.id.walk_chronometer, View.VISIBLE)
                    remoteViews?.setChronometer(R.id.walk_chronometer, baseTime, null, true)
                    sharedPreferences.edit { 
                        putLong("walkChr", baseTime)
                        putLong("stillChr", 0L)
                        putLong("speedChr", 0L)
                    }
                    remoteViews?.setTextViewText(R.id.tx_act_state, "WALKING")
                    remoteViews?.setChronometer(R.id.still_chronometer, SystemClock.elapsedRealtime(), null, false)
                    remoteViews?.setChronometer(R.id.speed_chronometer, SystemClock.elapsedRealtime(), null, false)
                    remoteViews?.setViewVisibility(R.id.still_chronometer, View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)
                }

                remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.steps)
                remoteViews?.setTextViewText(R.id.tx_act_count, stepsToday.toString())
                remoteViews?.setViewVisibility(R.id.rl_still, View.GONE)
                remoteViews?.setViewVisibility(R.id.tx_act_plus, View.GONE)
                remoteViews?.setViewVisibility(R.id.rl_walking, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_speed, View.GONE)

                stopSpeedService(context)
            }
            "TRAVEL" -> {
                if (transitionType == 0) {
                    val baseTime = SystemClock.elapsedRealtime()
                    remoteViews?.setViewVisibility(R.id.speed_chronometer, View.VISIBLE)
                    remoteViews?.setChronometer(R.id.speed_chronometer, baseTime, null, true)
                    sharedPreferences.edit { 
                        putLong("speedChr", baseTime)
                        putLong("speed_trip_start_time", baseTime)
                        putLong("stillChr", 0L)
                        putLong("walkChr", 0L)
                    }
                    remoteViews?.setTextViewText(R.id.tx_act_state, "TRAVEL")
                    remoteViews?.setChronometer(R.id.walk_chronometer, SystemClock.elapsedRealtime(), null, false)
                    remoteViews?.setChronometer(R.id.still_chronometer, SystemClock.elapsedRealtime(), null, false)
                    remoteViews?.setViewVisibility(R.id.walk_chronometer, View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.tx_act_plus, View.GONE)
                    remoteViews?.setViewVisibility(R.id.still_chronometer, View.INVISIBLE)
                    
                    startSpeedService(context)
                }

                remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.in_a_vehicle)
                remoteViews?.setTextViewText(R.id.tx_act_count, sharedPreferences.getInt("current_speed", 0).toString())
                remoteViews?.setViewVisibility(R.id.rl_still, View.GONE)
                remoteViews?.setViewVisibility(R.id.rl_walking, View.GONE)
                remoteViews?.setViewVisibility(R.id.rl_speed, View.VISIBLE)
            }
        }

        try {
            appWidM.updateAppWidget(newAppWidget, remoteViews)
            Log.d("ActivityTransition", "Widget update Success!")
        } catch (e: Exception) {
            makeToast(context, "Widget update failed ~ " + e)
            Log.d("ActivityTransition", "Widget update failed ~ " + e)
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
