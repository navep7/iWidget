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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.AppWidgetTarget
import java.security.MessageDigest
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
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
                            sharedPreferences =
                                getSharedPreferences("UserPreferences", MODE_PRIVATE)
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
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                }
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
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
                        Log.w(
                            "StepsService",
                            "Starting foreground without specific types due to missing permissions"
                        )
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



                    if (stepsToday < 10 && presentActivityState == "WALKING") {
                        remoteViews?.setTextViewText(
                            R.id.tx_act_count,
                            "$stepsToday"
                        )
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday)
                            .apply()
                    } else if (stepsToday % 10 == 0) {

                        if (stepsToday < 131) {
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
                                " ~ " + String.format(
                                    "%.1f",
                                    (Integer.parseInt(stepsToday.toString()) * 74f) / 100000f
                                )
                            )
                            remoteViews?.setTextViewText(
                                R.id.rl_tx_cals,
                                (stepsToday * 0.04 * (80 / 70)).toInt().toString()
                            )
                        }
                        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday)
                            .apply()
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

    private fun updateWidget(p0: Context?) {
        val intent = Intent(
            p0,
            NewAppWidget::class.java
        )
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        val ids: IntArray = AppWidgetManager.getInstance(p0)
            .getAppWidgetIds(ComponentName(application, NewAppWidget::class.java))
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
                    updateWidget(p0)
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
                    updateWidget(p0)
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
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            sharedPreferencesEditor.putBoolean("BluetoothState", false).apply()
                        }

                        BluetoothAdapter.STATE_TURNING_OFF -> { /* Bluetooth is turning off */
                        }

                        BluetoothAdapter.STATE_ON -> {
                            sharedPreferencesEditor.putBoolean("BluetoothState", true).apply()
                        }

                        BluetoothAdapter.STATE_TURNING_ON -> { /* Bluetooth is turning on */
                        }
                    }
                }

                if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == intent.action) {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            makeToast(applicationContext, "Headset connected: ${device?.name}")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", true)
                                .apply()
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            makeToast(applicationContext, "Headset disconnected: ${device?.name}")
                            sharedPreferencesEditor.putBoolean("BluetoothConnectionState", false)
                                .apply()
                        }

                    }
                }

                updateWidget(context)

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
            mLatLng = LatLng(lat, lng)

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


    fun loadStaticStreetView(applicationContext: Context, mLatLng: LatLng) {

        val apiKey =
            "AIzaSyDypKvRVtn8t_F0yKtSfK5PuI54FBNDvCk" // Ensure this key has Street View Static API enabled
        val size = "600x400"
        val fov = 90 // Default field of view
        val zoom = 18

        // Build the Street View request URL
        val streetViewUrl = "https://maps.googleapis.com/maps/api/streetview" +
                "?size=$size" +
                "&location=${mLatLng.latitude},${mLatLng.longitude}" +
                "&fov=$fov" +
                "&key=$apiKey"

        val widgetTargetStreet = AppWidgetTarget(
            applicationContext,
            R.id.imgv_street_icon,
            remoteViews,
            newAppWidget
        )

        Glide.with(applicationContext)
            .asBitmap()
            .load(streetViewUrl)
            .placeholder(R.drawable.walp_icon)
            .error(android.R.drawable.stat_notify_error)
            .into(widgetTargetStreet)

        // Build the Static Map request URL
        val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=${mLatLng.latitude},${mLatLng.longitude}" +
                "&zoom=$zoom" +
                "&size=400x400" +
                "&maptype=satellite" +
                "&key=$apiKey"

        val headers = LazyHeaders.Builder()
            .addHeader("User-Agent", "HomeyApp/1.0 (${applicationContext.packageName})")
            .build()

        val latRad = Math.toRadians(mLatLng.latitude)
        val xtile = kotlin.math.floor((mLatLng.longitude + 180.0) / 360.0 * (1 shl zoom)).toInt()
        val ytile = kotlin.math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()

        val osmTileUrl = "https://tile.openstreetmap.org/$zoom/$xtile/$ytile.png"
        val esriTileUrl = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$zoom/$ytile/$xtile"

        val osmGlideUrl = GlideUrl(osmTileUrl, headers)
        val esriGlideUrl = GlideUrl(esriTileUrl, headers)

        val widgetTargetMap = AppWidgetTarget(
            applicationContext,
            R.id.imgv_map_icon, // The ID of the ImageView in your widget XML
            remoteViews,
            newAppWidget
        )

        val staticMapTransform = MapMarkerTransformation(mLatLng, zoom, isTile = false)
        val tileTransform = MapMarkerTransformation(mLatLng, zoom, isTile = true)

        // Load the image into your view using Glide with marker transformation and tile fallbacks
        Glide.with(applicationContext)
            .asBitmap() // RemoteViews require a Bitmap
            .load(staticMapUrl)
            .transform(staticMapTransform)
            .error(
                Glide.with(applicationContext)
                    .asBitmap()
                    .load(osmGlideUrl)
                    .transform(tileTransform)
                    .error(
                        Glide.with(applicationContext)
                            .asBitmap()
                            .load(esriGlideUrl)
                            .transform(tileTransform)
                            .placeholder(R.drawable.walp_icon)
                            .error(android.R.drawable.stat_notify_error)
                    )
            )
            .placeholder(R.drawable.walp_icon) // Handles the loading state automatically
            .into(widgetTargetMap)
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

        lateinit var mLatLng: LatLng
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

        fun ismLatLngInitialized(): Boolean {
            return ::mLatLng.isInitialized
        }


        var twitterProfileName: String = "Fact"
        var mLocationResult: LocationResult? = null
        var totalUsage: String = ""
        var choosenApps: ArrayList<App> = ArrayList()

        fun isStepsAdapterInitialized(): Boolean {
            return ::stepsAdapter.isInitialized
        }


        @OptIn(DelicateCoroutinesApi::class)
        fun getWeatherData(latLng: LatLng) {

            try {
                val weatherService = Retrofit.Builder()
                    .baseUrl("https://api.open-meteo.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherService::class.java)


                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        weatherData = weatherService.getWeather(
                            latLng.latitude.toString(),
                            latLng.longitude.toString()
                        )
                        withContext(Dispatchers.Main) {
                            val tempVal = weatherData.currentWeather.temperature
                            tempC = "${tempVal}°C"
                            val code = weatherData.currentWeather.weathercode
                            
                            if (code == 0) {
                                tempKind = "Clear"
                                weatherIconID = "800"
                            } else if (code in 1..3) {
                                tempKind = "Clouds"
                                weatherIconID = "801"
                            } else if (code >= 51) {
                                tempKind = "Rain"
                                weatherIconID = "500"
                            } else {
                                tempKind = "Clear"
                                weatherIconID = "800"
                            }
                            weatherIconState = tempKind
                            weatherIconUrl = ""

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

class MapMarkerTransformation(
    private val latLng: LatLng,
    private val zoom: Int,
    private val isTile: Boolean = false
) : BitmapTransformation() {
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val config = toTransform.config ?: Bitmap.Config.ARGB_8888
        val result = toTransform.copy(config, true)
        val canvas = Canvas(result)

        // Draw the map image at full size without truncation
        canvas.drawBitmap(toTransform, 0f, 0f, null)

        val markerX: Float
        val markerY: Float

        if (isTile) {
            val mapSize = 256.0 * (1 shl zoom)
            val sinLat = sin(Math.toRadians(latLng.latitude)).coerceIn(-0.9999, 0.9999)
            val globalPixelX = (latLng.longitude + 180.0) / 360.0 * mapSize
            val globalPixelY = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI)) * mapSize

            val xtile = floor(globalPixelX / 256.0).toInt()
            val ytile = floor(globalPixelY / 256.0).toInt()

            val tileLeft = xtile * 256.0
            val tileTop = ytile * 256.0

            val tilePixelX = globalPixelX - tileLeft
            val tilePixelY = globalPixelY - tileTop

            val scaleX = toTransform.width.toFloat() / 256f
            val scaleY = toTransform.height.toFloat() / 256f

            markerX = (tilePixelX * scaleX).toFloat()
            markerY = (tilePixelY * scaleY).toFloat()
        } else {
            // Static map is centered on latLng
            markerX = toTransform.width / 2f
            markerY = toTransform.height / 2f
        }

        val paintPin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.FILL
        }

        val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = toTransform.width * 0.02f
        }

        val paintInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val radius = toTransform.width * 0.06f

        // Draw pin pointer / teardrop shape centered at (markerX, markerY)
        val path = Path().apply {
            moveTo(markerX - radius, markerY - radius * 0.5f)
            lineTo(markerX + radius, markerY - radius * 0.5f)
            lineTo(markerX, markerY + radius * 1.5f)
            close()
        }
        canvas.drawPath(path, paintPin)
        canvas.drawPath(path, paintBorder)

        // Draw head circle
        canvas.drawCircle(markerX, markerY - radius * 0.5f, radius, paintPin)
        canvas.drawCircle(markerX, markerY - radius * 0.5f, radius, paintBorder)
        canvas.drawCircle(markerX, markerY - radius * 0.5f, radius * 0.4f, paintInner)

        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("MapMarkerTransformation_${latLng.latitude}_${latLng.longitude}_${zoom}_${isTile}".toByteArray())
    }
}