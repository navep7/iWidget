package com.belaku.homey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.belaku.homey.MainActivity.Companion.makeToast

class SpeedService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L, // 2 seconds interval
                1f,    // 1 meter minimum distance
                this
            )
        } catch (e: SecurityException) {
            makeToast(applicationContext, "speedEx - ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        // location.speed is in m/s, multiply by 3.6 for km/h
        var speedKmh = location.speed * 3.6
      //  makeToast(applicationContext, "speedKmh - $speedKmh")

        if (speedKmh < 5)
            speedKmh = 0.0
        // Broadcast speed to the AppWidgetProvider
        val intent = Intent(this, NewAppWidget::class.java).apply {
            action = "ACTION_UPDATE_SPEED"
            putExtra("EXTRA_SPEED", speedKmh)
        }

        sendBroadcast(intent)
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
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
            .build()

        startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
    }
}
