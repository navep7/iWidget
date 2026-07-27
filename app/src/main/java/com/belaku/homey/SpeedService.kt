package com.belaku.homey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
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

        // Using FusedLocationProviderClient with High Accuracy is significantly more battery-efficient than raw GPS_PROVIDER.
        // It manages GPS activation intelligently and leverages sensor fusion.
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1f) // 1 meter for precision as requested
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
        // location.speed is in m/s, multiply by 3.6 for km/h
        val speedKmh = (location.speed * 3.6).toInt()
        updateSpeed(speedKmh)
    }

    private fun updateSpeed(speedKmh: Int) {
        val provider = ComponentName(applicationContext, NewAppWidget::class.java)
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val sharedPreferencesEditor = sharedPreferences.edit()
        
        val today = LocalDate.now().toString()
        val lastSavedDate = sharedPreferences.getString("maxSpeedDate", "")
        
        var maxSpeed = 0
        if (today == lastSavedDate) {
            maxSpeed = sharedPreferences.getInt("maxSpeedToday", 0)
        } else {
            // New day, reset max speed
            sharedPreferencesEditor.putString("maxSpeedDate", today)
            sharedPreferencesEditor.putInt("maxSpeedToday", 0)
            sharedPreferencesEditor.apply()
        }

        if (speedKmh > maxSpeed) {
            maxSpeed = speedKmh
            sharedPreferencesEditor.putInt("maxSpeedToday", maxSpeed).apply()
        }

        val views = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
        views.setTextViewText(R.id.tx_speed, speedKmh.toString())
        views.setTextViewText(R.id.tx_max_speed, maxSpeed.toString())

        appWidgetManager.updateAppWidget(provider, views)
    }

    override fun onDestroy() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
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
            .setSmallIcon(R.drawable.in_a_vehicle) // Added small icon which is mandatory for foreground notifications
            .build()

        try {
            if (ActivityCompat.checkSelfPermission(applicationContext,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                if (ActivityCompat.checkSelfPermission(applicationContext,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                    if (ActivityCompat.checkSelfPermission(applicationContext,
                            android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            startForeground(3, notification)
        } catch (ex: Exception) {
            makeToast(applicationContext, "SpeedServiceEXP - ${ex.message}")
        }
    }
}
