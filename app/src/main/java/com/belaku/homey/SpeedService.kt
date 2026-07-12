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
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.makeToast


class SpeedService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        makeToast(applicationContext, "sp33D !onCreate")
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

        if (speedKmh < 1)
            speedKmh = 0.0

        updateSpeed(speedKmh)

    }

    private fun updateSpeed(speedKmh: Double) {


        // Define your specific widget component and the Context
        val provider: ComponentName = ComponentName(applicationContext, NewAppWidget::class.java)
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(provider)


// Create the RemoteViews object targeting your widget's XML layout
        val views: RemoteViews = RemoteViews(applicationContext.getPackageName(), R.layout.new_app_widget)

        if (speedKmh < 5.0)
            views.setTextViewText(R.id.tx_speed, "")
        else views.setTextViewText(R.id.tx_speed, String.format("%.1f", speedKmh) + " KmpH")

// Update only the speed TextView with the new text



// Push the update for all instances of the widget
        appWidgetManager.updateAppWidget(provider, views)

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
