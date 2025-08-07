package com.belaku.homey

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.initialSteps
import com.belaku.homey.SetWallWorker.Companion.steps


class StepsService : Service() {

    private lateinit var mSensorEventListener: SensorEventListener
    lateinit var stepCounterSensor: Sensor
    lateinit var sensorManager: SensorManager

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Steps counting..",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()

            startForeground(1, notification)
        }

        appContx = applicationContext
        sensorManager = appContx.getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!!

        mSensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                Log.d("onSensorChanged",  steps.toString())
                steps++

                if (steps > 7)
                    remoteViews?.setViewVisibility(R.id.tx_n_steps, View.VISIBLE)


                if (steps % 10 == 0) {
                    remoteViews?.setTextViewText(R.id.tx_steps, "Today, " + steps.toString())
                    if (boolNewLap) {
                        remoteViews?.setTextViewText(
                            R.id.tx_n_steps,
                            (steps - initialSteps).toString()
                        )
                        initialSteps++
                    }
                    AppWidgetManager.getInstance(appContx).updateAppWidget(newAppWidget, remoteViews)
                }

            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                Log.d("MY_APP", "$sensor - $accuracy")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("Service Status","Starting Service")

        steps = 0
       sensorManager.registerListener(mSensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
      //    makeToast("step UP!")


    //    stopSelf()
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")

        return super.stopService(name)
    }

    override fun onDestroy() {
        Toast.makeText(
            applicationContext, "Service execution completed",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("Stopped","Service Stopped")
        super.onDestroy()
    }
}