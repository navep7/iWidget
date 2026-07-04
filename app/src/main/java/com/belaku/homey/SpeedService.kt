package com.belaku.homey

import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.Manifest

import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.RemoteViews
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews

class SpeedService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 2000L, 0f, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        // location.getSpeed() returns speed in meters per second (m/s)
        val speedMps = location.speed
        val speedKmh = speedMps * 3.6 // Convert to km/h

        // Update the App Widget

        remoteViews?.setTextViewText(R.id.tx_speed, "${speedKmh.toInt()} KmpH")
        appWidM.updateAppWidget(newAppWidget, remoteViews)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
