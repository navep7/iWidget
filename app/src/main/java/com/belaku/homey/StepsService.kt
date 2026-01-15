package com.belaku.homey

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.dayOfTheWeek
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.TAG
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SetWallWorker.Companion.stepsToday
import java.util.Locale


class StepsService : Service() {

    private lateinit var mSensorEventListener: SensorEventListener
    lateinit var stepCounterSensor: Sensor
    lateinit var sensorManager: SensorManager

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()


        val userPresentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    // Handle the screen unlock event here
                    //    makeToast("Screen unlocked!")
                    // You can update UI, start a task, etc.
                }
            }
        }
        registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))



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

        BluetoothState()
        WifiState()

        sensorManager = appContx.getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!!

        mSensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                Log.d("onSensorChanged", stepsToday.toString())
                stepsToday++

                if (stepsToday % 10 == 0) {
                    remoteViews?.setTextViewText(
                        R.id.tx_steps,
                        stepsToday.toString()
                    )
                    sharedPreferencesEditor.putInt(dayOfTheWeek, stepsToday).apply()
                    boolNewLap = sharedPreferences.getBoolean("newLap", false)


                    appWidM.updateAppWidget(newAppWidget, remoteViews)

                }

            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                Log.d("MY_APP", "$sensor - $accuracy")
            }
        }
    }

    private fun updateWidget() {
        val intent = Intent(
            applicationContext,
            NewAppWidget::class.java
        )
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        val ids: IntArray = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(getApplication(), NewAppWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    private fun WifiState() {
        val mWifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onReceive(p0: Context?, intent: Intent?) {


                val action = intent?.action

                if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val wifiState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN
                    )
                    when (wifiState) {
                        WifiManager.WIFI_STATE_ENABLED -> {
                            sharedPreferencesEditor.putBoolean("WifiState", true).apply()
                            Log.d(TAG, "Wi-Fi is enabled")
                            // You can perform actions here when Wi-Fi becomes enabled
                        }

                        WifiManager.WIFI_STATE_DISABLED -> {
                            sharedPreferencesEditor.putBoolean("WifiState", false).apply()
                            Log.d(TAG, "Wi-Fi is disabled")
                            // You can perform actions here when Wi-Fi becomes disabled
                        }
                    }
                    updateWidget()
                } else if (action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    val cm =
                        appContx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                    val isConnected = activeNetwork?.isConnectedOrConnecting == true

                    if (isConnected && activeNetwork?.type == ConnectivityManager.TYPE_WIFI) {
                        // Connected to Wi-Fi
                        sharedPreferencesEditor.putBoolean("WifiConnectionState", true).apply()
                        // You can perform actions here when connected to a Wi-Fi network
                    } else {
                        sharedPreferencesEditor.putBoolean("WifiConnectionState", false).apply()
                        // Not connected to Wi-Fi or connected to a different network type
                        // You can perform actions here when Wi-Fi connection is lost or changed
                    }
                    updateWidget()
                }



            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(mWifiReceiver, intentFilter)

    }


    private fun BluetoothState() {

        sharedPreferences = appContx.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()


        if (!sharedPreferences.getBoolean("BRd", false)) {
            Log.d(TAG, "BlBrRd")
            sharedPreferencesEditor.putBoolean("BRd", true).apply()

            val mBluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                @RequiresApi(Build.VERSION_CODES.S)
                @SuppressLint("UnsafeIntentLaunch")
                override fun onReceive(context: Context, intent: Intent) {

                    val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

                    when (state) {

                        BluetoothAdapter.STATE_CONNECTED -> {
                            Log.d(TAG, "STATE_CONNECTED")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", true)
                                .apply()
                            updateWidget()
                        }

                        BluetoothAdapter.STATE_DISCONNECTED -> {
                            Log.d(TAG, "STATE_DISCONNECTED")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", false)
                                .apply()
                            updateWidget()
                        }

                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "STATE_OFF")
                            sharedPreferencesEditor.putBoolean("BluetoothState", false).apply()
                            updateWidget()
                        }

                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "STATE_ON")
                            sharedPreferencesEditor.putBoolean("BluetoothState", true).apply()
                            updateWidget()
                        }

                    }



                }
            }

            appContx.registerReceiver(
                mBluetoothReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("Service Status", "Starting Service")



        stepsToday = 0
        sensorManager.registerListener(
            mSensorEventListener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        //    makeToast("step UP!")


        //    stopSelf()
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping", "Stopping Service")

        return super.stopService(name)
    }

    override fun onDestroy() {
        Toast.makeText(
            applicationContext, "Service execution completed",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("Stopped", "Service Stopped")
        super.onDestroy()
    }

    companion object {

        var totalUsage: String = ""
        var choosenApps: ArrayList<App> = ArrayList()

        fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
            val manager = appContx.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }


}