package com.belaku.homey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
            if (isMyServiceRunning( context, SpeakService::class.java))
                speakOut(currentHour.toString())
        //    }

            // Implement your hour change logic here
        }
    }

}

