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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.LocalDate
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

            val locationRequest = LocationRequest.create()
            locationRequest.setInterval(10000)
            locationRequest.setSmallestDisplacement(3f)
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
                            if (ismGoogleMapInitialized()) {
                                getAddress(location.latitude, location.longitude)
                                var icon: BitmapDescriptor? = null
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val icnGenerator = IconGenerator(applicationContext)
                                    // Bitmap bmp = icnGenerator.makeIcon(Html.fromHtml("<b><font color=\"#000000\">" + mAddresses[0] + mAddresses[1] + mAddresses[2] + "\n" + mAddresses[3] + mAddresses[4] + "</font></b>"));
                                    val bmp: Bitmap = icnGenerator.makeIcon(
                                        cityname
                                    )
                                    icon = BitmapDescriptorFactory.fromBitmap(bmp)
                                }
                                mGoogleMap.clear()
                                var mLatLng: LatLng = LatLng(location.latitude, location.longitude)

                                if (cityname.isNotEmpty()) {
                                    var markerOptions =
                                        MarkerOptions().position(mLatLng).icon(icon).title(cityname)
                                    var markerAddress = mGoogleMap.addMarker(markerOptions)
                                    mStreetViewPanorama.setPosition(
                                        LatLng(
                                            location.latitude,
                                            location.longitude
                                        )
                                    )
                                }
                            }
                        }
                    }

                    fun getAddress(lat: Double, lng: Double) {
                        val gcd = Geocoder(applicationContext)
                        Locale.getDefault()
                        try {
                            var cAddrs = gcd.getFromLocation(lat, lng, 1)!!
                            //   // makeToast(cAddrs?.get(0)!!.subLocality)

                            cityLat = lat
                            cityLng = lng
                            if (cAddrs.isNotEmpty())
                                if (cAddrs.get(0) != null)
                                    if (cAddrs.get(0).subLocality != null)
                                        cityname = cAddrs.get(0)!!.subLocality
                                    else if (cAddrs.get(0).locality != null)
                                        cityname = cAddrs.get(0)!!.locality
                                    else cityname = "remoteAreaMaybe!"


                        } catch (e: IOException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                             makeToast("GCD - IOException \n $e")
                        }

                    }


                    override fun onMarkerClick(p0: Marker): Boolean {
                        // makeToast("nothin")
                        return true
                    }
                },
                Looper.getMainLooper()
            )


        val userPresentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    // Handle the screen unlock event here
                    //    // makeToast("Screen unlocked!")
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
                if (presentActivityState != "IN VEHICLE") {

                    stepsToday++

                    if (!ismActInitialized()) {
                    sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                    sharedPreferencesEditor = sharedPreferences.edit()
                        }



                    if (stepsToday < 10) {
                        remoteViews?.setTextViewText(
                            R.id.tx_steps,
                            "$stepsToday Steps"
                        )
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()
                    } else if (stepsToday % 10 == 0)  {
                        if(stepsToday < 131) {
                            remoteViews?.setTextViewText(
                                R.id.tx_steps,
                                "$stepsToday steps"
                            )
                        }
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()
                    } else if (stepsToday % 131 == 0) {

                        remoteViews?.setTextViewText(
                        R.id.tx_steps,
                        "${String.format("%.1f",  (Integer.parseInt(stepsToday.toString()) * 74f) / 100000f)} km"
                    )
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()
                }


                    sharedPreferencesEditor.putString("day", LocalDate.now().dayOfWeek.name).apply()


                if (isAppWidMInitialized())
                    appWidM.updateAppWidget(newAppWidget, remoteViews)

            }
        }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                Log.d("MY_APP", "$sensor - $accuracy")
            }
        }
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

   //     makeToast("!BluetoothState")
        val mBluetoothStateReceiver = object : BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent?.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            makeToast("Bluetooth OFF")
                            sharedPreferencesEditor.putBoolean("BluetoothState", false).apply()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> { /* Bluetooth is turning off */ }
                        BluetoothAdapter.STATE_ON -> {
                            makeToast("Bluetooth ON")
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
                            makeToast("Headset connected: ${device?.name}")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", true).apply()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            makeToast("Headset disconnected: ${device?.name}")
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


        sensorManager.registerListener(
            mSensorEventListener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        //    // makeToast("step UP!")


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

        lateinit var usageStatsManager: UsageStatsManager
        lateinit var stepsAdapter: StepsAdapter
        val stepsData: ArrayList<String> = ArrayList()
        var presentActivityState = ""
        var presentActivityStateImage = R.drawable.walp_icon
        lateinit var locationListenerSpeed: LocationListener
        lateinit var locationManager: LocationManager
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
                            tempC.split(".")[0] + "°"
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