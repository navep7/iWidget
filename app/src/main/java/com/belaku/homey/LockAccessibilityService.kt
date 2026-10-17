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

            if (currentAppPackage != null && isUpiApp(applicationContext, currentAppPackage)) {
                // 1. UPI app detected: Temporarily disable monitoring capabilities to satisfy risk policy
                pauseAccessibilityService()
            } else {
                // 2. Safe app detected: Restore original configuration
                resumeAccessibilityService()
            }
        }
    }

    fun isUpiApp(context: Context, targetPackageName: String): Boolean {
        // Fast path for common UPI apps to avoid overhead during window transitions
        val commonUpiPackages = setOf(
            "in.org.npci.upiapp",                  // BHIM
            "net.one97.paytm",                    // Paytm
            "com.phonepe.app",                    // PhonePe
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.naviapp",                        // Navi
            "com.whatsapp",                       // WhatsApp Pay
            "com.freecharge.android",             // Freecharge
            "com.mobikwik_new",                   // Mobikwik
            "com.amazon.mShop.android.shopping"   // Amazon Pay
        )

        if (commonUpiPackages.contains(targetPackageName.lowercase())) {
            return true
        }

        // Fallback: Query activities that handle upi://pay
        val packageManager = context.packageManager
        val upiIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("upi://pay")
        }

        val resolveInfoList = packageManager.queryIntentActivities(
            upiIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        for (resolveInfo in resolveInfoList) {
            if (resolveInfo.activityInfo.packageName.equals(targetPackageName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun pauseAccessibilityService() {
        val info = serviceInfo ?: AccessibilityServiceInfo()

        // Keep only TYPE_WINDOW_STATE_CHANGED so we can detect when to resume.
        // Stripping all other event types and flags makes the service "dormant" for risk checks.
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = 0 // Remove FLAG_INCLUDE_NOT_IMPORTANT_VIEWS etc.

        serviceInfo = info
    }

    private fun resumeAccessibilityService() {
        val info = serviceInfo ?: AccessibilityServiceInfo()

        // Restore default events needed for normal operation (if any)
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

        // Use flagDefault as defined in accessibility_service_config.xml
        info.flags = 0

        serviceInfo = info
    }

    override fun onInterrupt() {
    }


    override fun onCreate() {
        super.onCreate()

        val notification: Notification = createForegroundNotification()
        val NOTIFICATION_ID = 75
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun createForegroundNotification(): Notification {
        val CHANNEL_ID = "LS"

        val channel = NotificationChannel(
            CHANNEL_ID, "Accessibility Service Channel", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.createNotificationChannel(channel)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lock Service Active")
            .setContentText("Tap widget to lock screen.")
            .setSmallIcon(R.drawable.launch_e)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
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
