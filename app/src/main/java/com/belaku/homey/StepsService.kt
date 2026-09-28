package com.belaku.homey

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationListener
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
import androidx.core.content.ContextCompat
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.cityLat
import com.belaku.homey.MainActivity.Companion.cityLng
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.currentLocation
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.tempC
import com.belaku.homey.MainActivity.Companion.tempKind
import com.belaku.homey.MainActivity.Companion.weatherData
import com.belaku.homey.MainActivity.Companion.weatherIconID
import com.belaku.homey.MainActivity.Companion.weatherIconState
import com.belaku.homey.MainActivity.Companion.weatherIconUrl
import com.belaku.homey.MapsActivity.Companion.ismGoogleMapInitialized
import com.belaku.homey.MapsActivity.Companion.mGoogleMap
import com.belaku.homey.MapsActivity.Companion.mStreetViewPanorama
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.isAppWidMInitialized
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.TAG
import com.belaku.homey.SetWallWorker.Companion.isSharedPreferencesInitialized
import com.belaku.homey.SetWallWorker.Companion.ismActInitialized
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.LocalDate
import java.util.Locale


class StepsService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mSensorEventListener: SensorEventListener
    lateinit var stepCounterSensor: Sensor
    lateinit var sensorManager: SensorManager

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()

        if (!isSharedPreferencesInitialized()) {
            sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences.edit()
        }

            if (!isLocationEnabled(applicationContext)) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                applicationContext.startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
            }

            val locationRequest = LocationRequest.create()
            locationRequest.setInterval(10000)
            locationRequest.setSmallestDisplacement(3f)
            locationRequest.setFastestInterval(10000)
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)

            var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback(), GoogleMap.OnMarkerClickListener {
                    override fun onLocationResult(locationResult: LocationResult) {
                        mLocationResult = locationResult
                        val location = locationResult.lastLocation
                        if (location != null) {

                            if (location.hasSpeed()) {
                                val speedInMps = location.speed // Speed in meters/second

                                // Convert to km/h (optional)
                                speedInKmph = (speedInMps * 3.6).toInt()
                            //    speedR(speedInKmph.toString())
                            }


                            if (!isSharedPreferencesInitialized()) {
                                sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                                sharedPreferencesEditor = sharedPreferences.edit()
                            }

                            if (!sharedPreferences.getBoolean("boolWeather", false)) {
                                sharedPreferencesEditor.putBoolean("boolWeather", true).apply()
                                getWeatherData(LatLng(location.latitude, location.longitude))
                            }

                            currentLocation = location

                            getAddress(location.latitude, location.longitude)
                        }
                    }

                    override fun onMarkerClick(p0: Marker): Boolean {
                        return true
                    }
                },
                Looper.getMainLooper()
            )


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

            if (Build.VERSION.SDK_INT >= 34) {
                var type = 0
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }

                try {
                    if (type != 0) {
                        startForeground(1, notification, type)
                    } else {
                        // Fallback: if no runtime permissions are granted, we can't start with those types.
                        // However, health and location types MUST be started with their respective types.
                        // If we have neither, we might have to stop the service or start without types
                        // if the manifest allows (but it doesn't here).
                        Log.w("StepsService", "Starting foreground without specific types due to missing permissions")
                        startForeground(1, notification)
                    }
                } catch (e: Exception) {
                    Log.e("StepsService", "startForeground failed", e)
                    stopSelf()
                    return
                }
            } else {
                startForeground(1, notification)
            }
        }

        BluetoothState(this)
        WifiState(this)

        sensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor != null) {
            stepCounterSensor = sensor
        } else {
            Log.w("StepsService", "Sensor.TYPE_STEP_COUNTER not available")
        }

        mSensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!presentActivityState.equals("TRAVEL")) {

                    stepsToday++

                    if (!ismActInitialized()) {
                    sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                    sharedPreferencesEditor = sharedPreferences.edit()
                        }



                    if (stepsToday < 10) {
                        remoteViews?.setTextViewText(
                            R.id.tx_act_count,
                            "$stepsToday"
                        )
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()
                    } else if (stepsToday % 10 == 0)  {

                        if(stepsToday < 131) {
                            if (presentActivityState == "WALKING")
                            remoteViews?.setTextViewText(
                                R.id.tx_act_count,
                                "$stepsToday"
                            )
                            remoteViews?.setTextViewText(
                                R.id.rl_tx_steps,
                                "$stepsToday"
                            )
                            remoteViews?.setTextViewText(
                                R.id.rl_tx_steps_in_km,
                                " ~ " + String.format("%.1f",  (Integer.parseInt(stepsToday.toString()) * 74f) / 100000f)
                            )
                            remoteViews?.setTextViewText(R.id.rl_tx_cals, (stepsToday * 0.04 * (80 / 70)).toInt().toString())
                        }
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()
                    }


                    sharedPreferencesEditor.putString("day", LocalDate.now().dayOfWeek.name).apply()


                if (isAppWidMInitialized() && remoteViews != null) {
                    try {
                        appWidM.updateAppWidget(newAppWidget, remoteViews)
                    } catch (e: Exception) {
                        Log.e("StepsService", "Failed to update widget", e)
                    }
                }

            }
        }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                Log.d("MY_APP", "$sensor - $accuracy")
            }
        }

        if (isSharedPreferencesInitialized())
        stepsToday = sharedPreferences.getInt(LocalDate.now().dayOfWeek.name, 0)

    }

    fun isLocationEnabled(context: Context): Boolean {
        locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
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

        val mBluetoothStateReceiver = object : BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent?.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            sharedPreferencesEditor.putBoolean("BluetoothState", false).apply()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> { /* Bluetooth is turning off */ }
                        BluetoothAdapter.STATE_ON -> {
                            sharedPreferencesEditor.putBoolean("BluetoothState", true).apply()
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> { /* Bluetooth is turning on */ }
                    }
                }

                if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == intent.action) {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            makeToast(applicationContext, "Headset connected: ${device?.name}")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", true).apply()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            makeToast(applicationContext, "Headset disconnected: ${device?.name}")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", false).apply()
                        }

                    }
                }

                updateWidget()

            }
        }

        val bluetoothFilter = IntentFilter()
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        bluetoothFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(mBluetoothStateReceiver, bluetoothFilter)



    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        if (::stepCounterSensor.isInitialized) {
            sensorManager.registerListener(
                mSensorEventListener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }


        //    stopSelf()
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping", "Stopping Service")

        return super.stopService(name)
    }

    fun getAddress(lat: Double, lng: Double) {
        serviceScope.launch(Dispatchers.IO) {
            val gcd = Geocoder(applicationContext, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    gcd.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val address = addresses.firstOrNull()
                            val name = address?.subLocality
                                ?: address?.locality
                                ?: "remoteAreaMaybe!"
                            updateAddressAndMap(lat, lng, name)
                        }

                        override fun onError(errorMessage: String?) {
                            Log.e("StepsService", "Geocode error: $errorMessage")
                            updateAddressAndMap(lat, lng, "remoteAreaMaybe!")
                        }
                    })
                } catch (e: Exception) {
                    Log.e("StepsService", "Geocode exception", e)
                    updateAddressAndMap(lat, lng, "remoteAreaMaybe!")
                }
            } else {
                var name = "remoteAreaMaybe!"
                try {
                    @Suppress("DEPRECATION")
                    val cAddrs = gcd.getFromLocation(lat, lng, 1)
                    val address = cAddrs?.firstOrNull()
                    name = address?.subLocality
                        ?: address?.locality
                        ?: "remoteAreaMaybe!"
                } catch (e: IOException) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        makeToast(applicationContext, "GCD - IOException \n $e")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                updateAddressAndMap(lat, lng, name)
            }
        }
    }

    private fun updateAddressAndMap(lat: Double, lng: Double, name: String) {
        serviceScope.launch(Dispatchers.Main) {
            cityLat = lat
            cityLng = lng
            cityname = name

            if (ismGoogleMapInitialized()) {
                var icon: BitmapDescriptor? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val icnGenerator = IconGenerator(applicationContext)
                        val bmp: Bitmap = icnGenerator.makeIcon(cityname)
                        icon = BitmapDescriptorFactory.fromBitmap(bmp)
                    } catch (e: Exception) {
                        Log.e("StepsService", "Failed to generate icon", e)
                    }
                }
                mGoogleMap.clear()
                val mLatLng = LatLng(lat, lng)

                if (cityname.isNotEmpty()) {
                    val markerOptions = MarkerOptions().position(mLatLng).icon(icon).title(cityname)
                    mGoogleMap.addMarker(markerOptions)
                    mStreetViewPanorama.setPosition(mLatLng)
                }

                val cameraPosition = CameraPosition.Builder()
                    .target(mLatLng)
                    .tilt(55f)
                    .zoom(20f)
                    .bearing(0f)
                    .build()

                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        Toast.makeText(
            applicationContext, "Service execution completed",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("Stopped", "Service Stopped")
        super.onDestroy()
    }

    companion object {

        var Top3: ArrayList<App> = ArrayList()
        var strDurationWalk: String = ""
        var strDurationTravel: String = ""
        var speedInKmph: Int = 0
        lateinit var usageStatsManager: UsageStatsManager
        lateinit var stepsAdapter: StepsAdapter
        val stepsData: ArrayList<String> = ArrayList()
        val speedData: ArrayList<String> = ArrayList()
        var presentActivityState = ""
        var presentActivityStateImage = R.drawable.walp_icon
        lateinit var locationListenerSpeed: LocationListener
        lateinit var locationManager: LocationManager

        fun isLocationManagerInitialized(): Boolean {
            if (::locationManager.isInitialized && ::locationListenerSpeed.isInitialized)
                return true
            else
                return false

        }
        var twitterProfileName: String = "Fact"
        var mLocationResult: LocationResult? = null
        var totalUsage: String = ""
        var choosenApps: ArrayList<App> = ArrayList()

        fun isStepsAdapterInitialized(): Boolean {
            if (::stepsAdapter.isInitialized)
                return true
            else
                return false
        }


        @OptIn(DelicateCoroutinesApi::class)
        fun getWeatherData(latLng: LatLng) {

            try {
                val weatherService = Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/data/2.5/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherService::class.java)


                GlobalScope.launch(Dispatchers.IO) {
                    try {
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
                                tempC.split(".")[0] + "° " + tempKind
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
                            if (NewAppWidget.isAppWidMInitialized() && remoteViews != null) {
                                try {
                                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                                } catch (e: Exception) {
                                    Log.e("StepsService", "Error updating weather widget", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("StepsService", "Error fetching weather data", e)
                        withContext(Dispatchers.Main) {
                            remoteViews?.setViewVisibility(
                                R.id.progressBar_cyclic_weather,
                                View.INVISIBLE
                            )
                            remoteViews?.setViewVisibility(R.id.tx_refresh_weather, View.VISIBLE)
                            if (NewAppWidget.isAppWidMInitialized() && remoteViews != null) {
                                try {
                                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                                } catch (e: Exception) {
                                    Log.e("StepsService", "Error updating weather widget", e)
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.d("WD Excep7 - ", ex.toString())
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic_weather, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.tx_refresh_weather, View.VISIBLE)
                if (NewAppWidget.isAppWidMInitialized() && remoteViews != null) {
                    try {
                        appWidM.updateAppWidget(newAppWidget, remoteViews)
                    } catch (e: Exception) {
                        Log.e("StepsService", "Error updating weather widget", e)
                    }
                }
            }

            //   // makeToast(tempC)

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