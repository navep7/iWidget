package com.belaku.homey

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SpeakService.Companion.speakOut


class NotificationService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("NoteServiceLOG", "onCreate")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NoteServiceLOG", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NoteServiceLOG", "onListenerDisconnected")

        // Request rebind to ensure the service stays active
        requestRebind(ComponentName(this, NotificationService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // Extract app name
        var appName: String
        try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            appName = packageManager.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            appName = "Unknown"
        }

        // Initialize sharedPreferences if not already done (context safe)
        val prefs = applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)

        if (prefs.getBoolean("SPKSERVICE", false)) {
             speakOut(appName)
        }
    }
}