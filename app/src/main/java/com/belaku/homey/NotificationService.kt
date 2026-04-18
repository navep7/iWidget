package com.belaku.homey

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SpeakService.Companion.speakOut
import com.belaku.homey.StepsService.Companion.isMyServiceRunning


class NotificationService : NotificationListenerService() {
    private var componentName: ComponentName? = null


    override fun onCreate() {
        super.onCreate()
        Log.d("NoteServiceLOG", "onCreate")
        if (componentName == null) {
            componentName = ComponentName(this, this::class.java)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NoteServiceLOG", "onStartCommand")
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NoteServiceLOG", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        Log.d("NoteServiceLOG", "onListenerDisconnected")

        if (componentName == null) {
            componentName = ComponentName(this, this::class.java)
        }

        componentName?.let { 
            try {
                requestRebind(it)
            } catch (e: Exception) {
                Log.e("NoteServiceLOG", "Failed to request rebind", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        /*if (!isMyServiceRunning( applicationContext, SpeakService::class.java) )
            startService(Intent(applicationContext, SpeakService::class.java))*/


        val packageName = sbn?.packageName ?: ""
        val extras = sbn?.notification?.extras

        // Extract data from notification extras as needed
        val title = extras?.getCharSequence("android.title").toString()
        val text = extras?.getCharSequence("android.text").toString()
        val pkgName = sbn?.packageName
        var appName: String
        try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            appName = (packageManager.getApplicationLabel(ai) as String?).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            appName = "Unknown"
        }


        sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)

        if (sharedPreferences.getBoolean("SPKSERVICE", false))
             speakOut(appName)
        // makeToast(appName)
    }
}