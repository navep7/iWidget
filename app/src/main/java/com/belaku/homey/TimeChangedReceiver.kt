package com.belaku.homey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.widgetContext
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
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

            if (currentMin == 0)
            if (isMyServiceRunning( context, SpeakService::class.java)) {
                if (currentHour == 10 && calendar.get(Calendar.AM_PM) == 1) {
                    val speakIntent = Intent(widgetContext, SpeakService::class.java)
                    widgetContext.stopService(speakIntent)
                    remoteViews?.setTextViewText(R.id.tx_time_announcement, "⊘")
                    sharedPreferencesEditor.putBoolean("SPKSERVICE", false).apply()
                }

                speakOut(currentHour.toString())
            }

        }
    }

}

