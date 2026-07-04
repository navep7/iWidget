package com.belaku.homey

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


class LockAccessibilityService : AccessibilityService() {


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentAppPackage = event.packageName?.toString()

            if (isUpiApp(applicationContext, currentAppPackage.toString())) {
                // 1. UPI app detected: Temporarily disable monitoring capabilities
                pauseAccessibilityService()
            } else {
                // 2. Safe app detected: Restore screen lock functionality
                resumeAccessibilityService()
            }
        }
    }

    fun isUpiApp(context: Context, targetPackageName: String): Boolean {
        val packageManager = context.packageManager

        // Create the standard mock UPI payment intent
        val upiIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("upi://pay")
        }

        // Query all activities capable of handling UPI links
        val resolveInfoList = packageManager.queryIntentActivities(
            upiIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        // Check if the target package is in the resolved list
        for (resolveInfo in resolveInfoList) {
            if (resolveInfo.activityInfo.packageName.equals(targetPackageName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun pauseAccessibilityService() {
        val info = serviceInfo ?: AccessibilityServiceInfo()

        // Remove text parsing, window tracking, and tap capabilities
        info.eventTypes = 0
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = 0

        serviceInfo = info // Apply changes dynamically
    }

    private fun resumeAccessibilityService() {
        val info = serviceInfo ?: AccessibilityServiceInfo()

        // Restore your original lock-screen event listening parameters
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

        // Match the flags set up in your res/xml/accessibility_service_config.xml
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

        serviceInfo = info // Re-apply configuration
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // The service was likely restarted by the system.
            // You can log this or perform necessary re-initialization.
            // You might want to return START_STICKY or START_REDELIVER_INTENT
            // depending on your service's behavior.
        } else {
            // Proceed with handling the intent data
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }


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