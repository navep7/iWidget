package com.belaku.homey

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.ACTIVITY_SERVICE
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SpeakService.Companion.speakOut
import java.util.Calendar

class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_TICK) {


            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR)
            val currentMin = calendar.get(Calendar.MINUTE)

            Log.d("TimeChangedReceiver", "Time tick received. Current hour: $currentMin")
         //   makeToast("NOW - $currentMin")

            if (currentMin == 0) {
                if (!isMyServiceRunning(SpeakService::class.java)) {
                    appContx.startService(Intent(appContx, SpeakService::class.java))
                }
                speakOut(currentHour)
            }

            // Implement your hour change logic here
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = appContx.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

