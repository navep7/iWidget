package com.belaku.homey

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


class LockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()

        val notification: Notification = createForegroundNotification() // Implement this method
        val NOTIFICATION_ID = 75
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun createForegroundNotification(): Notification {
        // Create a NotificationChannel for Android O and above
        val CHANNEL_ID = "LS"

        val channel = NotificationChannel(
            CHANNEL_ID, "Accessibility Service Channel", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.createNotificationChannel(channel)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Accessibility Service Running")
            .setContentText("Your accessibility service is active.")
            .setSmallIcon(R.drawable.launch_e) // Replace with your icon
            .setPriority(NotificationCompat.PRIORITY_LOW) // Adjust priority as needed

        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        return Service.START_STICKY
    }


    companion object {
        fun lockScreenAccessibility(context: Context) {
            val intentService = Intent(
                context,
                LockAccessibilityService::class.java
            )
            context.startForegroundService(intentService)
        }
    }
}