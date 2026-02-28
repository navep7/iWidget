package com.belaku.homey

import android.Manifest
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
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.cityLat
import com.belaku.homey.MainActivity.Companion.cityLng
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.tempC
import com.belaku.homey.MainActivity.Companion.tempKind
import com.belaku.homey.MainActivity.Companion.weatherData
import com.belaku.homey.MainActivity.Companion.weatherIconID
import com.belaku.homey.MainActivity.Companion.weatherIconState
import com.belaku.homey.MainActivity.Companion.weatherIconUrl
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.dayOfTheWeek
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.TAG
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SetWallWorker.Companion.stepsToday
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Locale


class StepsService : Service() {

    private lateinit var mSensorEventListener: SensorEventListener
    lateinit var stepCounterSensor: Sensor
    lateinit var sensorManager: SensorManager

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()



            if (!isLocationEnabled(applicationContext)) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                applicationContext.startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
            }

            var locationRequest = LocationRequest.create()
            locationRequest.setInterval(30000)
            locationRequest.setSmallestDisplacement(1f)
            locationRequest.setFastestInterval(10000)
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)

            //instantiating the LocationCallBack


            var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback(), GoogleMap.OnMarkerClickListener {
                    override fun onLocationResult(locationResult: LocationResult) {
                        mLocationResult = locationResult
                        val location = locationResult.lastLocation
                        if (location != null) {

                            if (!sharedPreferences.getBoolean("boolWeather", false)) {
                                sharedPreferencesEditor.putBoolean("boolWeather", true).apply()
                                getWeatherData(LatLng(location.latitude, location.longitude))
                            }

                            getAddress(location.latitude, location.longitude)
                        }
                    }

                    fun getAddress(lat: Double, lng: Double) {
                        val gcd = Geocoder(applicationContext)
                        Locale.getDefault()
                        try {
                            var cAddrs = gcd.getFromLocation(lat, lng, 1)!!
                            //   makeToast(cAddrs?.get(0)!!.subLocality)

                            cityLat = lat
                            cityLng = lng
                            if (cAddrs.isNotEmpty())
                                if (cAddrs.get(0) != null)
                                    if (cAddrs.get(0).locality != null)
                                        cityname = cAddrs.get(0)!!.locality
                                    else if (cAddrs.get(0).subLocality != null)
                                        cityname = cAddrs.get(0)!!.subLocality


                        } catch (e: IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                            makeToast("GCD - IOException \n $e")
                        }

                    }


                    override fun onMarkerClick(p0: Marker): Boolean {
                        makeToast("nothin")
                        return true
                    }
                },
                Looper.getMainLooper()
            )


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

        BluetoothState(this)
        WifiState(this)

        sensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager
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

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
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

    private fun WifiState(contx: StepsService) {
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
                        contx.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
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


    private fun BluetoothState(contx: StepsService) {

        sharedPreferences = contx.getSharedPreferences("UserPreferences", MODE_PRIVATE)
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

            contx.registerReceiver(
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

        var twitterProfileName: String = "Fact"
        lateinit var mLocationResult: LocationResult
        var totalUsage: String = ""
        var choosenApps: ArrayList<App> = ArrayList()


        @OptIn(DelicateCoroutinesApi::class)
        fun getWeatherData(latLng: LatLng) {

            try {
                val weatherService = Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/data/2.5/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherService::class.java)


                GlobalScope.launch(Dispatchers.IO) {
                    val openWeatherApiKey = "9fa8e101240ab18615e3133b051e767e"
                    weatherData = weatherService.getWeather(
                        latLng.latitude.toString(),
                        latLng.longitude.toString(), openWeatherApiKey
                    )
                    withContext(Dispatchers.Main) {
                        //  updateUI(weatherData)
                        tempC = "${weatherData.main.temp - 273}°C"
                        weatherIconState = weatherData.weather.get(0).main
                        Log.d("weatherIconSubState", weatherData.weather.toString())
                        tempKind = weatherData.weather.get(0).main
                        weatherIconID = weatherData.weather.get(0).id
                        weatherIconUrl =
                            "http://openweathermap.org/img/wn/" + weatherIconID + "@2x.png"


                        Log.d("weatherInfo", tempC + " - " + tempKind)

                        remoteViews?.setTextViewText(
                            R.id.tx_weather,
                            tempC.split(".")[0] + "°C, " + tempKind
                        )
                        if (weatherIconID.startsWith("5"))
                            remoteViews?.setImageViewResource(
                                R.id.imgv_weather_icon,
                                R.drawable.rain
                            )
                        if (weatherIconID.equals("800"))
                            remoteViews?.setImageViewResource(
                                R.id.imgv_weather_icon,
                                R.drawable.clear_sky
                            )
                        if (weatherIconID.equals("801") || weatherIconID.equals("802") || weatherIconID.equals(
                                "803"
                            ) || weatherIconID.equals("804")
                        )
                            remoteViews?.setImageViewResource(
                                R.id.imgv_weather_icon,
                                R.drawable.clouds
                            )


                        remoteViews?.setViewVisibility(
                            R.id.progressBar_cyclic_weather,
                            View.INVISIBLE
                        )
                        remoteViews?.setViewVisibility(R.id.tx_refresh_weather, View.VISIBLE)
                        appWidM.updateAppWidget(newAppWidget, remoteViews)
                    }
                }
            } catch (ex: Exception) {
                Log.d("WD Excep7 - ", ex.toString())
                makeToast("Weather EXP - ${ex.message}")
            }

            //   makeToast(tempC)

        }



        fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }


}