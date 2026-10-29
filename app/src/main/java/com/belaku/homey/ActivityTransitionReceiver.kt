package com.belaku.homey

import android.annotation.SuppressLint
import android.app.PendingIntent
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
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.time.LocalDate

/**
 * Receiver for Activity Transition updates. 
 * Optimized to handle only ENTER transitions and batch state updates.
 */
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

                // Start one batch edit for efficiency. commit=true ensures data is persistent before the receiver finishes.
                sharedPrefs.edit(commit = true) {
                    // 1. Save the total duration of the activity we are leaving.
                    savePreviousStateDuration(sharedPrefs, this, oldState)

                    // 2. Update persistent state and the global companion variable.
                    presentActivityState = detectedState
                    val baseTime = SystemClock.elapsedRealtime()

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

                // 4. Trigger a widget refresh broadcast.
                triggerWidgetUpdate(applicationContext)
            }
        }
    }

    private fun savePreviousStateDuration(sharedPrefs: SharedPreferences, editor: SharedPreferences.Editor, oldState: String?) {
        val now = SystemClock.elapsedRealtime()
        val dayKey = LocalDate.now().dayOfWeek.name

        when (oldState) {
            "WALKING" -> {
                val startTime = sharedPrefs.getLong("walkChr", 0L)
                if (startTime != 0L) {
                    val duration = now - startTime
                    val totalKey = "${dayKey}_walk_duration"
                    val currentTotal = sharedPrefs.getLong(totalKey, 0L)
                    editor.putLong(totalKey, currentTotal + duration)
                }
            }
            "TRAVEL" -> {
                val startTime = sharedPrefs.getLong("speedChr", 0L)
                if (startTime != 0L) {
                    val duration = now - startTime
                    val totalKey = "${dayKey}_travel_duration"
                    val currentTotal = sharedPrefs.getLong(totalKey, 0L)
                    editor.putLong(totalKey, currentTotal + duration)
                }
            }
        }
    }

    private fun handleServiceTransitions(context: Context, newState: String) {
        val intent = Intent(context, SpeedService::class.java)
        try {
            if (newState == "TRAVEL") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else if (isMyServiceRunning(context, SpeedService::class.java)) {
                context.stopService(intent)
            }
        } catch (e: Exception) {
            Log.e("ActivityTransition", "Service transition failed for $newState", e)
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val componentName = ComponentName(context, NewAppWidget::class.java)
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(componentName)
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

    companion object {
        private const val TRANSITIONS_REQUEST_CODE = 57
        private const val TRANSITIONS_ACTION = "action.TRANSITIONS_DATA"

        /**
         * Registers for activity transition updates using centralized configuration.
         * Optimization: Only registers for ENTER transitions to reduce system wake-ups.
         */
        @SuppressLint("MissingPermission")
        fun requestUpdates(context: Context) {
            val activities = listOf(
                DetectedActivity.STILL,
                DetectedActivity.WALKING,
                DetectedActivity.IN_VEHICLE
            )

            val transitions = activities.map { type ->
                ActivityTransition.Builder()
                    .setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            }

            val request = ActivityTransitionRequest(transitions)
            ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, getTransitionsPendingIntent(context))
                .addOnSuccessListener { Log.d("ActivityTransition", "Registered successfully") }
                .addOnFailureListener { e -> Log.e("ActivityTransition", "Registration failed", e) }
        }

        /**
         * Unregisters activity transition updates.
         */
        @SuppressLint("MissingPermission")
        fun removeUpdates(context: Context) {
            ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(getTransitionsPendingIntent(context))
                .addOnSuccessListener { Log.d("ActivityTransition", "Removed successfully") }
                .addOnFailureListener { e -> Log.e("ActivityTransition", "Removal failed", e) }
        }

        /**
         * Builds the PendingIntent for activity transition updates.
         * Consistent flags ensure compatibility with Android 12+ (API 31).
         */
        fun getTransitionsPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
                action = TRANSITIONS_ACTION
            }
            return PendingIntent.getBroadcast(
                context,
                TRANSITIONS_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
    }
}
