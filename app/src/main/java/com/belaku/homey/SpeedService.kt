package com.belaku.homey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.makeToast
import com.google.android.gms.location.*


class SpeedService : Service() {

    companion object {
        var maxSpeed = 0
    }

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

        // Initialize maxSpeed from persistence on start
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        maxSpeed = sharedPreferences.getInt("maxSpeedToday", 0)
    }

    private fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).toInt() else 0
        updateSpeed(speedKmh)
    }

    private fun updateSpeed(speedKmh: Int) {
        val sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val sharedPreferencesEditor = sharedPreferences.edit()
        
        // Save current speed so Widget and ActivityTransitionReceiver can check it
        sharedPreferencesEditor.putInt("current_speed", speedKmh)
        
        // Note: Date-based reset of maxSpeedToday and weekly archival are handled 
        // centrally in NewAppWidget.todaysDate() to avoid race conditions. 
        maxSpeed = sharedPreferences.getInt("maxSpeedToday", 0)

        if (speedKmh > maxSpeed) {
            maxSpeed = speedKmh
            sharedPreferencesEditor.putInt("maxSpeedToday", maxSpeed)
        }
        sharedPreferencesEditor.apply()

        // Notify widget to update. NewAppWidget will read values from SharedPreferences.
        val updateIntent = Intent(applicationContext, NewAppWidget::class.java)
        updateIntent.action = "ACTION_UPDATE_SPEED"
        updateIntent.putExtra("EXTRA_SPEED", speedKmh.toDouble())
        sendBroadcast(updateIntent)
    }

    override fun onDestroy() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        
        val sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        
        // Note: Accumulated trip duration is handled centrally in ActivityTransitionReceiver.
        // History archival is managed by NewAppWidget.

        // Reset current speed on stop
        sharedPreferences.edit().putInt("current_speed", 0).apply()
            
        // Final update to widget to clear speed display
        val updateIntent = Intent(applicationContext, NewAppWidget::class.java)
        updateIntent.action = "ACTION_UPDATE_SPEED"
        updateIntent.putExtra("EXTRA_SPEED", 0.0)
        sendBroadcast(updateIntent)
        
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
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(3, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(3, notification)
            }
        } catch (ex: Exception) {
            makeToast(applicationContext, "SpeedServiceEXP - ${ex.message}")
        }
    }
}
