package com.belaku.homey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.makeToast
import com.google.android.gms.location.*
import java.time.LocalDate


class SpeedService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        makeToast(applicationContext, "⚡")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    onLocationChanged(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            makeToast(applicationContext, "speedEx - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun onLocationChanged(location: Location) {
        val speedKmh = (location.speed * 3.6).toInt()
        updateSpeed(speedKmh)
    }

    private fun updateSpeed(speedKmh: Int) {
        val provider = ComponentName(applicationContext, NewAppWidget::class.java)
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val sharedPreferencesEditor = sharedPreferences.edit()
        
        // Save current speed so ActivityTransitionReceiver can check it
        sharedPreferencesEditor.putInt("current_speed", speedKmh)
        
        val today = LocalDate.now().toString()
        val lastSavedDate = sharedPreferences.getString("maxSpeedDate", "")
        
        var maxSpeed = 0
        if (today == lastSavedDate) {
            maxSpeed = sharedPreferences.getInt("maxSpeedToday", 0)
        } else {
            sharedPreferencesEditor.putString("maxSpeedDate", today)
            sharedPreferencesEditor.putInt("maxSpeedToday", 0)
        }

        if (speedKmh > maxSpeed) {
            maxSpeed = speedKmh
            sharedPreferencesEditor.putInt("maxSpeedToday", maxSpeed)
        }
        sharedPreferencesEditor.apply()

        val views = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
        views.setTextViewText(R.id.tx_speed, speedKmh.toString())
        views.setTextViewText(R.id.tx_max_speed, maxSpeed.toString())
        
        // Restore chronometer state
        val baseTime = sharedPreferences.getLong("speed_trip_start_time", 0L)
        if (baseTime != 0L) {
            views.setChronometer(R.id.speed_chronometer, baseTime, null, true)
        }

        appWidgetManager.updateAppWidget(provider, views)
    }

    override fun onDestroy() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        // Reset speed on stop
        applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            .edit().putInt("current_speed", 0).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "SpeedServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Speed Tracker", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Vehicle Speed")
            .setContentText("Reading real-time GPS data for widget")
            .setSmallIcon(R.drawable.in_a_vehicle)
            .setOngoing(true) // Prevent notification from being cleared easily
            .build()

        try {
            startForeground(3, notification)
        } catch (ex: Exception) {
            makeToast(applicationContext, "SpeedServiceEXP - ${ex.message}")
        }
    }
}
