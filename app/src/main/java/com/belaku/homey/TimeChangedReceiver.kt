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
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import java.util.Calendar

class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_TICK) {


            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR)
            val currentMin = calendar.get(Calendar.MINUTE)

            Log.d("TimeChangedReceiver", "Time tick received. Current Min : $currentMin")
         //   makeToast("NOW - $currentMin")

            if (currentMin == 0)
            if (isMyServiceRunning(SpeakService::class.java))
                speakOut(currentHour)
        //    }

            // Implement your hour change logic here
        }
    }

}

