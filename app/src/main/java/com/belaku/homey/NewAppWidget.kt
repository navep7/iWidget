package com.belaku.homey


// Weather Key - 9fa8e101240ab18615e3133b051e767e


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Html
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.AdapterView
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.RECEIVER_NOT_EXPORTED
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.cityLat
import com.belaku.homey.MainActivity.Companion.cityLng
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.mBluetoothAdapter
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.tempC
import com.belaku.homey.MainActivity.Companion.tempKind
import com.belaku.homey.MainActivity.Companion.weatherIconID
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.MusicActivity.Companion.ispDataListInitialized
import com.belaku.homey.MusicActivity.Companion.pDatalistSongs
import com.belaku.homey.MusicService.Companion.boolMusicServiceRunning
import com.belaku.homey.MusicService.Companion.mMediaPlayer
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.RemindersActivity.Companion.adapterHabits
import com.belaku.homey.RemindersActivity.Companion.arrayListHabits
import com.belaku.homey.RemindersActivity.Companion.isadapterHabitsInitialized
import com.belaku.homey.SetWallWorker.Companion.appUsageStats
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.getFavoriteContacts
import com.belaku.homey.SetWallWorker.Companion.hour
import com.belaku.homey.SetWallWorker.Companion.isPinNoteInitialized
import com.belaku.homey.SetWallWorker.Companion.isWallBitmapInitialized
import com.belaku.homey.SetWallWorker.Companion.ismActInitialized
import com.belaku.homey.SetWallWorker.Companion.mAct
import com.belaku.homey.SetWallWorker.Companion.pinNote
import com.belaku.homey.SetWallWorker.Companion.scaledBitmap
import com.belaku.homey.SetWallWorker.Companion.screenHeight
import com.belaku.homey.SetWallWorker.Companion.screenWidth
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SetWallWorker.Companion.wallBitmap
import com.belaku.homey.StepsService.Companion.choosenApps
import com.belaku.homey.StepsService.Companion.isMyServiceRunning
import com.belaku.homey.StepsService.Companion.isStepsAdapterInitialized
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.belaku.homey.StepsService.Companion.stepsAdapter
import com.belaku.homey.StepsService.Companion.stepsData
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale


class NewAppWidget : AppWidgetProvider() {


    private lateinit var activityTransitionRequest: ActivityTransitionRequest
    private lateinit var pendingIntentActivityTransitions: PendingIntent
    private lateinit var activityTransitions: ArrayList<ActivityTransition>
    private var requestCodeAT: Int = 57
    private lateinit var intentActivityTransitionReceiver: Intent
    private lateinit var activityTransitionReceiver: ActivityTransitionReceiver
    private var speedReading: String = ""
    private var boolKm: Boolean = false
    private lateinit var cName: String
    private val TAG: String = "NewAppWidget"
    private var wallpColors: ArrayList<Int> = ArrayList()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var clickPendingIntentTemplateContact: PendingIntent
    private lateinit var clickIntentContact: Intent
    private lateinit var clickPendingIntentTemplateApp: PendingIntent
    private lateinit var clickIntentApp: Intent
    private lateinit var serviceIntentContact: Intent
    private lateinit var serviceIntentApp: Intent


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        widgetContext = context!!
        onEn = true

        appUsageStats(widgetContext)

        sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        if (unlockReceiver == null) {
            unlockReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (Intent.ACTION_USER_PRESENT == intent.action) {

                            widgetContext = context
                            setUI()
                            setACAdapter()

                            setOnClickPendingIntents(context)

                        if (!isAppWidMInitialized())
                            appWidM = AppWidgetManager.getInstance(widgetContext)

                        makeToast(widgetContext, "ꗃ " + sharedPreferences.getInt("unlockCount", 1))
                        sharedPreferencesEditor.putInt("unlockCount", sharedPreferences.getInt("unlockCount", 1) + 1).apply()

                        mAppWidgetIds = appWidM.getAppWidgetIds(ComponentName(widgetContext, NewAppWidget::class.java))
                        appWidM.updateAppWidget(newAppWidget, remoteViews)
                        appWidM.notifyAppWidgetViewDataChanged(mAppWidgetIds, R.id.list_apps)
                        appWidM.notifyAppWidgetViewDataChanged(mAppWidgetIds, R.id.list_contacts)

                    }
                }
            }

            // Register the receiver programmatically to bypass manifest restrictions
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            context.applicationContext.registerReceiver(unlockReceiver, filter)

        }
            if(ismActInitialized())
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mAct)

    }

    @SuppressLint("MissingPermission")
    private fun recognizeActivityTransitions() {

        activityTransitionReceiver = ActivityTransitionReceiver()
        val intentFilterActivityTransitionReceiver = IntentFilter("com.belaku.homey.CUSTOM_ACTION") // Use a unique action string
        widgetContext.registerReceiver(activityTransitionReceiver, intentFilterActivityTransitionReceiver, RECEIVER_NOT_EXPORTED)

        intentActivityTransitionReceiver = Intent(widgetContext, ActivityTransitionReceiver::class.java)
        requestCodeAT = 57
        pendingIntentActivityTransitions = PendingIntent.getBroadcast(
            widgetContext,
            requestCodeAT,
            intentActivityTransitionReceiver,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        activityTransitions = ArrayList<ActivityTransition>()
        activityTransitions.apply {
            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )

            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )

            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        activityTransitionRequest = ActivityTransitionRequest(activityTransitions)

        // myPendingIntent is the instance of PendingIntent where the app receives callbacks.
        ActivityRecognition.getClient(widgetContext)
            .requestActivityTransitionUpdates(activityTransitionRequest, pendingIntentActivityTransitions)


    }

    fun calculateCaloriesFromSteps(steps: Int, weightKg: Double, heightCm: Double): Double {
        if (steps <= 0 || weightKg <= 0.0 || heightCm <= 0.0) return 0.0

        // 1. Estimate stride length (average multiplier is 0.414 for men, 0.413 for women)
        val strideLengthCm = heightCm * 0.414

        // 2. Convert total steps to total distance in kilometers
        val distanceKm = (steps * strideLengthCm) / 100_000.0

        // 3. Convert kilometers to miles (Standard MET formulas use miles)
        val distanceMiles = distanceKm * 0.621371

        // 4. Convert weight to pounds
        val weightLbs = weightKg * 2.20462

        // 5. Apply the standard walking metabolic constant (approx. 0.57 calories per pound per mile)
        val caloriesPerMilePerLb = 0.57

        return distanceMiles * weightLbs * caloriesPerMilePerLb
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        widgetContext = context!!
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        widgetContext = context
        Log.d(TAG, "!onUpdate")
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
        newAppWidget = ComponentName(context, NewAppWidget::class.java)

        sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        i_appWidgetIds = appWidgetIds


        for (appWidgetId in appWidgetIds) {

            widgetContext = context

            setUI()

       //     if (!Constants.boolACadapterSet) {
                setACAdapter()
      //          Constants.boolACadapterSet = true
      //      }

            //  Create an intent to launch MainActivity

            setOnClickPendingIntents(context)

            if (!isAppWidMInitialized())
                appWidM = AppWidgetManager.getInstance(widgetContext)
            mAppWidgetIds = appWidM.getAppWidgetIds(ComponentName(widgetContext, NewAppWidget::class.java))
            appWidM.updateAppWidget(appWidgetId, remoteViews)
            appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_apps)
            appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_contacts)
        }


        super.onUpdate(context, appWidgetManager, appWidgetIds)

    }


    private fun setOnClickPendingIntents(context: Context) {


        val dummyIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(), // Empty intent does nothing
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(R.id.rl_widget_layout, dummyIntent)


        val intentMain = Intent(context, MainActivity::class.java)
        val pendingIntentMain = PendingIntent.getActivity(
            context,
            0,
            intentMain,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE with modern Android
        )

        // Set the click listener on the widget button
        remoteViews?.setOnClickPendingIntent(R.id.imgv_conf, pendingIntentMain)

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_steps,
            getPendingSelfIntent(context, STEPS_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_more_activities,
            getPendingSelfIntent(context, NEXT_ACT_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.clock,
            getPendingSelfIntent(context, TIME_CLICK)
        )
        remoteViews?.setOnClickPendingIntent(
            R.id.tx_day_date,
            getPendingSelfIntent(context, DATE_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_battery,
            getPendingSelfIntent(context, BATTERY_INFO)
        )
        remoteViews?.setOnClickPendingIntent(
            R.id.tx_refresh_weather,
            getPendingSelfIntent(context, GET_WEATHER)
        )



        remoteViews?.setOnClickPendingIntent(
            R.id.tx_time_announcement,
            getPendingSelfIntent(context, Time_A_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_dialler,
            getPendingSelfIntent(context, DIAL_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_albumcover,
            getPendingSelfIntent(context, P_THUMBNAIL_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_playpause,
            getPendingSelfIntent(context, PLAYPAUSE_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_ps,
            getPendingSelfIntent(context, PS_CLICK)
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contacts,
            getPendingSelfIntent(context, C_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.rl_player, PendingIntent.getActivity(
                context, 2,
                Intent(context, MusicActivity::class.java).putExtra("songIndex", songIndex),
                PendingIntent.FLAG_IMMUTABLE
            )
        )



        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_apps,
            getPendingSelfIntent(context, A_CLICKED)
        )

        val aiIntent = Intent(context, AiActivity::class.java)
        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val aiPendingIntent = PendingIntent.getActivity(
            context,
            3,
            aiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.fab_ai,
            aiPendingIntent
        )


        val remindersIntent = Intent(context, RemindersActivity::class.java)
        remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val remindersPendingIntent = PendingIntent.getActivity(
            context,
            4,
            remindersIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_reminders,
            remindersPendingIntent
        )

      /*  remoteViews?.setOnClickPendingIntent(
            R.id.tx_nextplan,
            remindersPendingIntent
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_breathe,
            getPendingSelfIntent(context, BREATHE_INC)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_drink,
            getPendingSelfIntent(context, DRINK_INC)
        )*/


        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_speech, PendingIntent.getActivity(
                context, 5,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "StT"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_runner, PendingIntent.getActivity(
                context, 6,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "AddNote"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_twitter, PendingIntent.getActivity(
                context, 7,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "ST"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )


        /*remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_twitter, PendingIntent.getActivity(
                context, 8,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "STH"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )*/

        val intentBluetooth = Intent(context, DialogActivity::class.java)
        if (mBluetoothAdapter.isEnabled)
            intentBluetooth.putExtra("DialogIntent", "BLUEDisable")
        else intentBluetooth.putExtra("DialogIntent", "BLUEEnable")
        val pendingIntentBluetooth = PendingIntent.getActivity(
            context,
            9,
            intentBluetooth,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews?.setOnClickPendingIntent(R.id.fab_blue, pendingIntentBluetooth)


        val intentWifi = Intent(context, DialogActivity::class.java)
        if (isWifiEnabled(context))
            intentWifi.putExtra("DialogIntent", "WifiDisable")
        else intentWifi.putExtra("DialogIntent", "WifiEnable")
        val pendingIntentWifi = PendingIntent.getActivity(
            context,
            10,
            intentWifi,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews?.setOnClickPendingIntent(R.id.fab_wifi, pendingIntentWifi)

        remoteViews?.setOnClickPendingIntent(
            R.id.fab_torch,
            getPendingSelfIntent(context, TORCH_STATE)
        )


        val launcherIntentGaps = Intent(context, GapsActivity::class.java)
        launcherIntentGaps.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        launcherIntentGaps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val launcherPendingIntentGaps = PendingIntent.getActivity(
            context,
            11,
            launcherIntentGaps,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_g_apps,
            launcherPendingIntentGaps
        )

        val launcherIntentNPs = Intent(context, MySpaceActivity::class.java)
        launcherIntentNPs.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        launcherIntentNPs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val launcherPendingIntentNPs = PendingIntent.getActivity(
            context,
            12,
            launcherIntentNPs,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_myspace,
            launcherPendingIntentNPs
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_qr,
            PendingIntent.getActivity(
                context, 13,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "qrClick"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_scr_time,
            PendingIntent.getActivity(
                context, 14,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "screenTimeInfo"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_steps,
            getPendingSelfIntent(context, STEPSINFO_CLICK)
        )



        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_lock,
            getPendingSelfIntent(context, LOCK_PHONE)
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_set, PendingIntent.getActivity(
                context, 16,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "WCh"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_rewards_count, PendingIntent.getActivity(
                widgetContext,
                18,
                Intent(widgetContext, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "AD"
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )


        val mapsIntent = Intent(context, MapsActivity::class.java)
        val mapsPendingIntent = PendingIntent.getActivity(
            context,
            17,
            mapsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_open_maps,
            mapsPendingIntent
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_map_icon,
            mapsPendingIntent
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.rl_water_reminder,
            getPendingSelfIntent(context, WATER_REMINDER_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_water_count,
            getPendingSelfIntent(context, WATER_REMINDER_CLICK)
        )

    }

    private fun locationTxUpdate(context: Context) {
        if (!isLocationEnabled(context)) {
            remoteViews?.setTextViewText(R.id.tx_place, "Please Enable Location services!")

            val locIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            val locPendingIntent = PendingIntent.getActivity(
                context,
                18,
                locIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews?.setOnClickPendingIntent(
                R.id.tx_place,
                locPendingIntent
            )
        } else {

            if (::cName.isInitialized)
                if (cName != cityname)
                    cName = cityname
            cName = cityname

            remoteViews?.setTextViewText(R.id.tx_place, cName)
            remoteViews?.setTextViewText(R.id.tx_weather, tempC.split(".")[0] + "° " + tempKind)
            if (weatherIconID.startsWith("5"))
                remoteViews?.setImageViewResource(R.id.imgv_weather_icon, R.drawable.rain)
            if (weatherIconID.equals("800"))
                remoteViews?.setImageViewResource(
                    R.id.imgv_weather_icon,
                    R.drawable.clear_sky
                )
            if (weatherIconID.equals("801") || weatherIconID.equals("802") || weatherIconID.equals(
                    "803"
                ) || weatherIconID.equals("804")
            )
                remoteViews?.setImageViewResource(R.id.imgv_weather_icon, R.drawable.clouds)
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setUI() {

        locationTxUpdate(widgetContext)

        remoteViews?.setTextViewText(R.id.tx_speed, speedReading)
        val maxSpeed = sharedPreferences.getInt("maxSpeedToday", 0)
        if (maxSpeed > 0)
            remoteViews?.setTextViewText(R.id.tx_max_speed, maxSpeed.toString())


            stepsToday = sharedPreferences.getInt(LocalDate.now().dayOfWeek.name, 0)

            remoteViews?.setTextViewText(
                R.id.tx_steps,
                "$stepsToday Steps"
            )
            remoteViews?.setTextViewText(
                R.id.rl_tx_steps,
                "$stepsToday"
            )
            remoteViews?.setTextViewText(R.id.rl_tx_cals, (stepsToday * 0.04 * (80 / 70)).toInt().toString())
            sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()


        if (hour != 0) {
            remoteViews?.setTextViewText(
                R.id.tx_screentime,
                "$hour+ Hours"
            )

            if (hour < 2)
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "LOW"
                )
            else if (hour in 2..< 5)
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "MODERATE"
                )
            else if (hour in 5..< 8)
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "HIGH"
                )
            else if (hour >= 8)
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "EXCESSIVE"
                )
        }

        val spkServiceRunning = sharedPreferences.getBoolean("SPKSERVICE", false)
        if (spkServiceRunning)
            remoteViews?.setTextViewText(R.id.tx_time_announcement, "\uD83D\uDDE3")
        else remoteViews?.setTextViewText(R.id.tx_time_announcement, "⊘")

        if (ispDataListInitialized() && pDatalistSongs.size > songIndex) {
            remoteViews?.setTextViewText(
                R.id.tx_music_details,
                pDatalistSongs[songIndex].title + " | " + pDatalistSongs[songIndex].album.title + " | " + pDatalistSongs[songIndex].artist.name
            )
            Picasso.get()
                .load(dataListSongs[songIndex].album.cover)
                .into(remoteViews!!, R.id.imgbtn_albumcover, NewAppWidget.i_appWidgetIds)

            mMediaPlayer?.let {
                if (it.isPlaying)
                    remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.pause_m)
                else remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.play_m)
            }

        }

        if (isPinNoteInitialized()) {
            remoteViews?.setTextViewText(R.id.tx_runner, pinNote)
        }

        getPreciseEnergyCounter(widgetContext)
        seekWifiState()
        seekBluetoothState()
        todaysDate()
        loadStepsData() // Always refresh stepsData from disk to ensure persistence
        wallColors()
        setSomeTwAndWallDescUI()

        recognizeActivityTransitions()

        if (isMyServiceRunning(widgetContext, SpeedService::class.java)) {
            remoteViews?.setViewVisibility(R.id.frame_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.frame_max_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.frame_time_speed, View.VISIBLE)

        }

        val waterCount = sharedPreferences.getInt("waterCountToday", 0)
        remoteViews?.setTextViewText(R.id.tx_water_count, waterCount.toString())


    }


    private fun applyThinFilmOverlay(
        originalBitmap: Bitmap,
        filmColor: Int,
        filmAlpha: Int
    ): Bitmap {
        // Create a mutable bitmap for drawing
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)

        // Draw the original bitmap
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // Create a paint object for the "film" effect
        val paint = Paint()
        paint.color = filmColor
        // Set the transparency (0 = fully transparent, 255 = fully opaque)
        paint.alpha = filmAlpha

        // Draw the semi-transparent color over the entire canvas
        canvas.drawRect(
            0f,
            0f,
            originalBitmap.width.toFloat(),
            originalBitmap.height.toFloat(),
            paint
        )

        return resultBitmap
    }


    private fun seekWifiState() {

        val wifiState = sharedPreferences.getBoolean("WifiState", false)
        val wifiConnectionState = sharedPreferences.getBoolean("WifiConnectionState", false)
        if (wifiState && wifiConnectionState)
            remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_on)
        else if (wifiState)
            remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_on_but_not_connected)
        else remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_off)
    }

    private fun seekBluetoothState() {

        val blState = sharedPreferences.getBoolean("BluetoothState", false)
        val blConnectionState = sharedPreferences.getBoolean("BluetoothConnectionState", false)
        if (blState && blConnectionState)
            remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_on)
        else if (blState)
            remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_red)
        else remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_off)
    }

    private fun setACAdapter() {

        setContactsAdapter()
        setContactsClick()
        setAppsAdapter()
        setAppsClick()
    }

    fun getInvertedColor(color: Int): Int {
        // 0x00FFFFFF represents a mask for the RGB components (ignoring alpha).
        // XORing with this value inverts the bits of the R, G, and B components.
        return color xor 0x00FFFFFF
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun wallColors() {

        val wallpaperManager = WallpaperManager.getInstance(widgetContext)
        val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)


        if (wallpaperColors != null) {
            Log.d("wallColors", "notNULL")

            primaryColor = wallpaperColors.primaryColor.toArgb()

            if (wallpaperColors.secondaryColor != null)
                secondaryColor = wallpaperColors.secondaryColor!!.toArgb()
            else secondaryColor = Color.GREEN

            if (wallpaperColors.tertiaryColor != null)
                tertianaryColor = wallpaperColors.tertiaryColor!!.toArgb()
            else tertianaryColor = Color.BLUE

            wallpColors.add(primaryColor)
            wallpColors.add(secondaryColor)
            wallpColors.add(tertianaryColor)

            remoteViews?.setColorInt(
                R.id.imgbtn_lock,
                "setColorFilter",
                Color.RED,
                Color.RED
            )


            val metrics = DisplayMetrics()

            if (ismActInitialized()) {
                mAct.getWindowManager().getDefaultDisplay().getMetrics(metrics)
                screenHeight = metrics.heightPixels
                screenWidth = metrics.widthPixels
                remoteViews?.setImageViewBitmap(
                    R.id.imgv_player,
                    createGradientBitmap(screenWidth, 100, primaryColor, tertianaryColor)
                )


            }



            if (ColorUtil().isColorDark(primaryColor)) {


                remoteViews?.setInt(R.id.imgv_conf, "setColorFilter", Color.BLACK)
                remoteViews?.setInt(R.id.imgbtn_speech, "setColorFilter", Color.BLACK)
                remoteViews?.setInt(R.id.imgbtn_qr, "setColorFilter", Color.BLACK)
                remoteViews?.setInt(R.id.imgbtn_set, "setColorFilter", Color.BLACK)
                remoteViews?.setInt(R.id.imgbtn_lock, "setColorFilter", Color.BLACK)
                remoteViews?.setInt(R.id.imgv_ps, "setColorFilter", Color.BLACK)
                remoteViews?.setTextColor(R.id.tx_myspace, Color.BLACK)
                remoteViews?.setTextColor(R.id.tx_rewards_count, Color.WHITE)
                remoteViews?.setInt(R.id.imgv_dialler, "setColorFilter", Color.BLACK)



                if (isWallBitmapInitialized()) {
                    scaledBitmap =
                        Bitmap.createScaledBitmap(wallBitmap, screenWidth, screenHeight, true)

                    remoteViews?.setImageViewBitmap(
                        R.id.imgv_widget_layout,
                        applyThinFilmOverlay(
                            drawableToBitmap(
                                widgetContext, RoundedBitmapDrawableFactory.create(
                                    widgetContext.resources, BitmapBlurHelper.blurBitmap(
                                        widgetContext,
                                        Bitmap.createBitmap(
                                            scaledBitmap,
                                            10,
                                            25,
                                            screenWidth - 20,
                                            screenHeight - 150
                                        )
                                    )
                                )
                            ), android.R.color.white, 75
                        )
                    )

                    blurWallBitmap = blur(widgetContext, wallBitmap)
                }


                remoteViews?.setTextColor(
                    R.id.clock,
                    widgetContext.resources.getColor(android.R.color.holo_red_light)
                )

            } else {



                remoteViews?.setInt(R.id.imgv_conf, "setColorFilter", Color.WHITE)
                remoteViews?.setInt(R.id.imgbtn_speech, "setColorFilter", Color.WHITE)
                remoteViews?.setInt(R.id.imgbtn_qr, "setColorFilter", Color.WHITE)
                remoteViews?.setInt(R.id.imgbtn_set, "setColorFilter", Color.WHITE)
                remoteViews?.setInt(R.id.imgbtn_lock, "setColorFilter", Color.WHITE)
                remoteViews?.setInt(R.id.imgv_ps, "setColorFilter", Color.WHITE)
                remoteViews?.setTextColor(R.id.tx_myspace, Color.WHITE)
                remoteViews?.setTextColor(R.id.tx_rewards_count, Color.BLACK)
                remoteViews?.setInt(R.id.imgv_dialler, "setColorFilter", Color.WHITE)


                if (isWallBitmapInitialized()) {
                    scaledBitmap =
                        Bitmap.createScaledBitmap(wallBitmap, screenWidth, screenHeight, true)

                    remoteViews?.setImageViewBitmap(
                        R.id.imgv_widget_layout,
                        applyThinFilmOverlay(
                            drawableToBitmap(
                                widgetContext, RoundedBitmapDrawableFactory.create(
                                    widgetContext.resources, BitmapBlurHelper.blurBitmap(
                                        widgetContext,
                                        Bitmap.createBitmap(
                                            scaledBitmap,
                                            10,
                                            25,
                                            screenWidth - 20,
                                            screenHeight - 150
                                        )
                                    )
                                )
                            ), android.R.color.black, 75
                        )
                    )

                    blurWallBitmap = blur(widgetContext, wallBitmap)
                }


                remoteViews?.setTextColor(
                    R.id.clock,
                    widgetContext.resources.getColor(android.R.color.holo_red_dark)
                )

            }

        } else Log.d("wallColors", "NULL")


    }

    fun blur(context: Context?, image: Bitmap): Bitmap {

        var BITMAP_SCALE = 0.1f; // Increased scale slightly for better quality/stability
        var BLUR_RADIUS = 25f; // Adjust blur intensity

        val width = Math.max(1, Math.round(image.width * BITMAP_SCALE).toInt())
        val height = Math.max(1, Math.round(image.height * BITMAP_SCALE).toInt())

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)

        rs.destroy()

        return outputBitmap
    }


    fun createGradientBitmap(width: Int, height: Int, startColor: Int, endColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Define the gradient
        val gradient = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )

        // Use ShapeDrawable to apply the gradient
        val shapeDrawable = ShapeDrawable(RectShape())
        shapeDrawable.paint.shader = gradient
        shapeDrawable.setBounds(0, 0, width, height)

        // Draw the shape onto the canvas
        shapeDrawable.draw(canvas)

        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setSomeTwAndWallDescUI() {

        if (checkCompanionVariable()) {
            remoteViews?.setTextViewText(R.id.tx_walldesc, wD)
            remoteViews?.setTextViewText(
                R.id.tx_walltype_updateinfo,
                Html.fromHtml(
                    qT.split(" ")[0].substring(0, 1)
                        .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
            noRewards = sharedPreferences.getInt("noRewards", 7)

            if (noRewards > 1)
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "$noRewards")
            else {
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.INVISIBLE)
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "\uD83D\uDC41\uFE0FAD!")
             /*   remoteViews?.setOnClickPendingIntent(
                    R.id.imgbtn_set, PendingIntent.getActivity(
                        widgetContext,
                        18,
                        Intent(widgetContext, DialogActivity::class.java).putExtra(
                            "DialogIntent",
                            "AD"
                        ),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )*/
            }

        }


    }


    private fun setAppsAdapter() {
        serviceIntentApp = Intent(widgetContext, RemoteViewsAppsService::class.java)
        serviceIntentApp.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newAppWidget)
        serviceIntentApp.setData(Uri.parse(serviceIntentApp.toUri(Intent.URI_INTENT_SCHEME))) // Required for unique intents
        remoteViews?.setRemoteAdapter(R.id.list_apps, serviceIntentApp)
        remoteViews?.setEmptyView(R.id.list_apps, R.id.widget_empty_view_apps)
    }

    private fun setContactsAdapter() {
        serviceIntentContact = Intent(widgetContext, RemoteViewsContactsService::class.java)
        serviceIntentContact.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newAppWidget)
        serviceIntentContact.setData(Uri.parse(serviceIntentContact.toUri(Intent.URI_INTENT_SCHEME))) // Required for unique intents
        remoteViews?.setRemoteAdapter(R.id.list_contacts, serviceIntentContact)
        remoteViews?.setEmptyView(R.id.list_contacts, R.id.widget_empty_view_contacts)
    }

    private fun setAppsClick() {
        // Set the PendingIntent template for the list items
        clickIntentApp = Intent(widgetContext, NewAppWidget::class.java)
        clickIntentApp.setAction(ACTION_LIST_APPITEM_CLICK)
        clickPendingIntentTemplateApp = PendingIntent.getBroadcast(
            widgetContext,
            1,
            clickIntentApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // Use FLAG_MUTABLE for security
        )
        remoteViews?.setPendingIntentTemplate(R.id.list_apps, clickPendingIntentTemplateApp)

    }

    private fun setContactsClick() {
        // Set the PendingIntent template for the list items
        clickIntentContact = Intent(widgetContext, NewAppWidget::class.java)
        clickIntentContact.setAction(ACTION_LIST_CONTACTITEM_CLICK)
        clickPendingIntentTemplateContact = PendingIntent.getBroadcast(
            widgetContext,
            0,
            clickIntentContact,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // Use FLAG_MUTABLE for security
        )
        remoteViews?.setPendingIntentTemplate(R.id.list_contacts, clickPendingIntentTemplateContact)
    }

    @SuppressLint("ResourceAsColor", "ResourceType")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub

        super.onReceive(context, intent)


        if (intent.action == "ACTION_UPDATE_SPEED") {
            speedReading = intent.getDoubleExtra("EXTRA_SPEED", 0.0).toString()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, NewAppWidget::class.java)
            )

            for (id in ids) {
                setUI()
                setACAdapter()
            }
        }

        widgetContext = context


        Log.d(TAG, "!onReceive")
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
        newAppWidget = ComponentName(context, NewAppWidget::class.java)
        sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        setUI()
        handleIntentActions(intent)

        if (!isAppWidMInitialized())
            appWidM = AppWidgetManager.getInstance(widgetContext)

        val appWidgetIds = appWidM.getAppWidgetIds(newAppWidget)
      //  appWidM = AppWidgetManager.getInstance(context)
        appWidM.updateAppWidget(newAppWidget, remoteViews)
        appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_apps)
        appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_contacts)

    }


    @SuppressLint("InflateParams", "ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleIntentActions(intent: Intent) {

        val appWidgetManager = AppWidgetManager.getInstance(widgetContext)
        // 3. Get IDs for all active widgets of this provider
        val thisAppWidget = ComponentName(widgetContext.getPackageName(), javaClass.getName())
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        // 4. Manually trigger onUpdate
       // onUpdate(context, appWidgetManager, appWidgetIds!!)

        if (TODO_CLICK == intent.action) {
            makeToast(widgetContext,"inc")
         //   sharedPreferencesEditor.putInt()
        }

        if (TIME_CLICK == intent.action) {

            val mClockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            widgetContext.startActivity(mClockIntent)

        }
        if (DATE_CLICK == intent.action) {

            val startMillis = System.currentTimeMillis()
            val builder = CalendarContract.CONTENT_URI.buildUpon()
                .appendPath("time")
            ContentUris.appendId(builder, startMillis)

            val intentCalendar = Intent(Intent.ACTION_VIEW)
                .setData(builder.build())
            intentCalendar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            widgetContext.startActivity(intentCalendar)
        }

        if (STEPSINFO_CLICK == intent.action) {

            widgetContext.startActivity(
                Intent(widgetContext, DialogActivity::class.java)
                    .putExtra("DialogIntent", "stepsInfo")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (STEPS_CLICK == intent.action) {
            makeToast(widgetContext, "$stepsToday ~ " + String.format("%.1f", stepsToday * 74f / 100000f) + " Km")
            remoteViews?.setTextViewText(R.id.tx_steps, "$stepsToday Steps")

        }

        if (NEXT_ACT_CLICK == intent.action) {
            makeToast(widgetContext, "  $presentActivityState")
            widgetContext.startActivity(
                Intent(
                    widgetContext,
                    DialogActivity::class.java
                ).putExtra("DialogIntent", "activitiesInfo")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        if (BATTERY_INFO == intent.action) {
            val powerUsageIntent = Intent("android.intent.action.POWER_USAGE_SUMMARY")
            if (powerUsageIntent.resolveActivity(widgetContext.getPackageManager()) != null) {
                powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                widgetContext.startActivity(powerUsageIntent)
            }
        } else if(SPEED_CHECK == intent.action) {

            if (isMyServiceRunning(widgetContext, SpeedService::class.java)) {
                if(widgetContext.stopService(Intent(widgetContext, SpeedService::class.java))) {
                    makeToast(widgetContext, "  ⃠  ")
                    remoteViews?.setChronometer(R.id.speed_chronometer, 0L, null, false)
                    remoteViews?.setViewVisibility(R.id.frame_speed, View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.frame_max_speed, android.view.View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.frame_time_speed, View.INVISIBLE)
                    }
            } else {
                val baseTime = SystemClock.elapsedRealtime()
                remoteViews?.setChronometer(R.id.speed_chronometer, baseTime, null, true)
                remoteViews?.setViewVisibility(R.id.frame_speed, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.frame_max_speed, android.view.View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.frame_time_speed, View.VISIBLE)
                remoteViews?.setTextViewText(R.id.tx_max_speed, "MAX")
                sharedPreferencesEditor.putInt("maxSpeedToday", 0).apply()
                    widgetContext.startForegroundService(
                    Intent(
                        widgetContext,
                        SpeedService::class.java
                    ))
            }

        } else if (GET_WEATHER == intent.action) {
            remoteViews?.setViewVisibility(R.id.progressBar_cyclic_weather, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.tx_refresh_weather, View.INVISIBLE)
            appWidM.updateAppWidget(newAppWidget, remoteViews)
            StepsService.getWeatherData(LatLng(cityLat, cityLng))

        } else if (PLAYPAUSE_CLICK == intent.action) {
            if (boolMusicServiceRunning) {
                try {
                    if (mMediaPlayer != null)
                        if (mMediaPlayer!!.isPlaying) {
                            mMediaPlayer!!.pause()
                            remoteViews?.setImageViewResource(
                                R.id.imgbtn_playpause,
                                R.drawable.play_m
                            )
                        } else {
                            startMusicActivity(songIndex)
                            remoteViews?.setImageViewResource(
                                R.id.imgbtn_playpause,
                                R.drawable.pause_m
                            )
                            mMediaPlayer!!.play()
                        }
                } catch (ex: Exception) {
                    showException(ex.message.toString())
                    startMusicActivity(0)
                }
            } else {
                startMusicActivity(songIndex)
            }


        } else if (P_THUMBNAIL_CLICK == intent.action) {
            widgetContext.startActivity(
                Intent(
                    widgetContext,
                    DialogActivity::class.java
                ).putExtra("DialogIntent", "SongCover")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else if (ACTION_LIST_CONTACTITEM_CLICK == intent.action) {
            // Extract the item position or ID from the intent extras

            getFavoriteContacts()
            val position = intent.getIntExtra(
                EXTRA_CONTACTITEM_POSITION,
                AdapterView.INVALID_POSITION
            )
            val viewID = intent.getIntExtra(
                EXTRA_CONTACTVIEW_ID,
                7
            )


            if (position != AdapterView.INVALID_POSITION) {

                if (viewID == 0)
                    dialPhoneNumber(widgetContext, favContacts[position].number)
                else if (viewID == 1) {
                    if (favContacts.size != position)
                        unMarkAsFav(favContacts[position].id)
                }
            } else {
                val pickContactIntent =
                    Intent(widgetContext, DialogActivity::class.java)
                pickContactIntent.putExtra("DialogIntent", "PC")
                pickContactIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                widgetContext.startActivity(pickContactIntent)
            }

        } else if (ACTION_LIST_APPITEM_CLICK == intent.action) {
            // Extract the item position or ID from the intent extras
            val position = intent.getIntExtra(
                EXTRA_APPITEM_POSITION,
                AdapterView.INVALID_POSITION
            )
            val viewID = intent.getIntExtra(
                EXTRA_APPVIEW_ID,
                7
            )

            sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences.edit()


            if (position != AdapterView.INVALID_POSITION) {

                if (viewID == 0) {
                    val launchIntent: Intent =
                        widgetContext.packageManager.getLaunchIntentForPackage(
                            choosenApps[position].pName
                        )!!

                    // Optional: Add flags for desired behavior (e.g., to ensure a new task is created)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    widgetContext.startActivity(launchIntent)
                }
            }
        } else if (FAB_SHARE == intent.action) {

            val inflater =
                widgetContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val appWidgetView: View = inflater.inflate(R.layout.new_app_widget, null)

            appWidgetView.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(screenHeight - 725, View.MeasureSpec.EXACTLY)
            );
            appWidgetView.layout(
                0,
                0,
                appWidgetView.getMeasuredWidth(),
                appWidgetView.getMeasuredHeight()
            );

            var bitmapWidget = Bitmap.createBitmap(
                appWidgetView.width,
                appWidgetView.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmapWidget)

            appWidgetView.draw(canvas)

            bitmapWidget = Bitmap.createScaledBitmap(
                bitmapWidget,
                Math.round(bitmapWidget.width * 50 / 100.0f),
                Math.round(bitmapWidget.height * 50 / 100.0f),
                true
            )

            shareBitmap(bitmapWidget)


        } else if (WIFI_AUTO == intent.action) {
            var wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            wifiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            widgetContext.startActivity(wifiIntent)
        } else if (TORCH_STATE == intent.action) {

            val isFlashAvailable =
                widgetContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            if (!isFlashAvailable) {
                  return
            }
            val cameraManager =
                widgetContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var cameraId: String? = null
            try {
                cameraId = cameraManager.cameraIdList[0] // Typically the back camera
            } catch (ex: CameraAccessException) {
                showException(ex.message.toString())
            }

            try {
                if (cameraId != null) {
                    if (!sharedPreferences.getBoolean("Torch", false)) {
                        cameraManager.setTorchMode(cameraId, true)
                        remoteViews?.setImageViewResource(R.id.fab_torch, R.drawable.torch_on)
                        sharedPreferencesEditor.putBoolean("Torch", true).apply()
                    } else {
                        cameraManager.setTorchMode(cameraId, false)
                        remoteViews?.setImageViewResource(R.id.fab_torch, R.drawable.torch_off)
                        sharedPreferencesEditor.putBoolean("Torch", false).apply()
                    }
                }
            } catch (ex: CameraAccessException) {
                showException(ex.message.toString())
            }


        } else if (STEPS_NOW == intent.action) {
            boolNewLap = !boolNewLap

            sharedPreferencesEditor.putBoolean("newLap", boolNewLap).apply()

        } else if (LOCK_PHONE == intent.action) {
            if (widgetContext != null) {
                if (isAccessibilityServiceEnabled(
                        widgetContext,
                        LockAccessibilityService::class.java
                    )
                )
                    LockAccessibilityService.lockScreenAccessibility(widgetContext)
                else widgetContext.startActivity(
                    Intent(
                        widgetContext,
                        DialogActivity::class.java
                    ).putExtra("DialogIntent", "AccessibilityPermDialog")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

        } else if (SET_CLICKED == intent.action) {
            val launchIntent: Intent =
                widgetContext.packageManager.getLaunchIntentForPackage("com.belaku.homey")!!
            widgetContext.startActivity(launchIntent)
        } else if (A_CLICKED == intent.action) {
            val intentApps = Intent(widgetContext, AppsActivity::class.java)
            intentApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            widgetContext.startActivity(intentApps)
        } else if (C_CLICKED == intent.action) {
            val intentContacts = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            intentContacts.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            widgetContext.startActivity(intentContacts)
        } else if (DIAL_CLICK == intent.action) {
            val intentDial = Intent(Intent.ACTION_DIAL)
            intentDial.data = Uri.parse("tel:") // Replace with the desired number
            widgetContext.startActivity(intentDial.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

        } else if (PS_CLICK == intent.action) {
            val pm: PackageManager = widgetContext.getPackageManager()
            val intent = pm.getLaunchIntentForPackage("com.android.vending")
            intent?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            widgetContext.startActivity(intent);

        } else if (Time_A_CLICKED == intent.action) {

            var boolSpkService = sharedPreferences.getBoolean("SPKSERVICE", false)
            val speakIntent = Intent(widgetContext, SpeakService::class.java)
            if (!boolSpkService) {
                widgetContext.startService(speakIntent)
                remoteViews?.setTextViewText(R.id.tx_time_announcement, "\uD83D\uDDE3")
                sharedPreferencesEditor.putBoolean("SPKSERVICE", true).apply()
            } else {
                widgetContext.stopService(speakIntent)
                remoteViews?.setTextViewText(R.id.tx_time_announcement, "⊘")
                sharedPreferencesEditor.putBoolean("SPKSERVICE", false).apply()
            }

        } else if (ADD_TODO_CLICK == intent.action) {
            makeToast(widgetContext, "Add Todo Clicked!")
        } else if (WATER_REMINDER_CLICK == intent.action) {
            val waterCount = sharedPreferences.getInt("waterCountToday", 0) + 1
            sharedPreferencesEditor.putInt("waterCountToday", waterCount).apply()
            remoteViews?.setTextViewText(R.id.tx_water_count, waterCount.toString())
        }
    }




    private fun startMusicActivity(songIndex: Int) {
        var intentMusic = Intent(widgetContext, MusicActivity::class.java)
        intentMusic.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentMusic.putExtra("songIndex", songIndex)
        widgetContext.startActivity(intentMusic)
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun unMarkAsFav(contactId: String) {
        // Replace with the actual contact ID
        val values = ContentValues()
        values.put(ContactsContract.Contacts.STARRED, 0) // 1 for favorite, 0 for not favorite

        widgetContext.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            ContactsContract.Contacts._ID + " = ?",
            arrayOf<String>(contactId.toString())
        )

        getFavoriteContacts()
        //   val appWidgetIds = appWidM.getAppWidgetIds(newAppWidget)
        //   appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_contacts)

  //      widgetContext.startActivity(Intent(widgetContext, DialogActivity::class.java).putExtra("DialogIntent", "WCh").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    }




    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.S)
    fun getPreciseEnergyCounter(context: Context) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        remoteViews?.setTextViewText(R.id.tx_battery, energy.toString())
        remoteViews?.setProgressBar(R.id.progressBar_battery, 100, energy.toInt(), false)

        val greenColor =
            ColorStateList.valueOf(context.resources.getColor(android.R.color.holo_green_light))
        val redColor =
            ColorStateList.valueOf(context.resources.getColor(android.R.color.holo_red_light))
        val amberColor =
            ColorStateList.valueOf(context.resources.getColor(android.R.color.holo_orange_light))

        if (energy.toInt() > 70) {
            remoteViews?.setColorStateList(
                R.id.progressBar_battery,
                "setProgressTintList",
                greenColor
            )
            remoteViews?.setTextColor(
                R.id.tx_battery,
                widgetContext.resources.getColor(android.R.color.holo_green_dark)
            )
        } else if (energy.toInt() < 30) {
            remoteViews?.setColorStateList(
                R.id.progressBar_battery,
                "setProgressTintList",
                redColor
            )
            remoteViews?.setTextColor(
                R.id.tx_battery,
                widgetContext.resources.getColor(android.R.color.holo_red_dark)
            )
        } else {
            remoteViews?.setColorStateList(
                R.id.progressBar_battery,
                "setProgressTintList",
                amberColor
            )
            remoteViews?.setTextColor(
                R.id.tx_battery,
                widgetContext.resources.getColor(android.R.color.holo_orange_dark)
            )
        }


      /*  return if (energy != Long.MIN_VALUE) {
            energy // Energy remaining in microampere-hours (µAh)
        } else {
            0L
        }*/
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager != null) {
            return wifiManager.isWifiEnabled
        }
        return false // Handle the case where WifiManager is null
    }



    private fun shareBitmap(bitmapWidget: Bitmap) {

        val cachePath: File = File(widgetContext.getCacheDir(), "images")
        cachePath.mkdirs() // Create the directory if it doesn't exist
        val imageFile: File = File(cachePath, "image_to_share.png")

        try {
            val outputStream: FileOutputStream = FileOutputStream(imageFile)
            bitmapWidget.compress(
                Bitmap.CompressFormat.PNG,
                100,
                outputStream
            ) // Adjust format and quality as needed
            outputStream.flush()
            outputStream.close()
        } catch (ex: IOException) {
            showException(ex.message.toString())
            return  // Handle the error appropriately
        }

        val contentUri = FileProvider.getUriForFile(
            widgetContext,
            widgetContext.getApplicationContext().getPackageName() + ".fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("image/*")
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temporary read permission

        widgetContext.startActivity(
            Intent.createChooser(shareIntent, "Share Image Using")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }




    fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService?>
    ): Boolean {
        val am: AccessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices: List<AccessibilityServiceInfo> =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName.equals(context.packageName) && enabledServiceInfo.name.equals(
                    service.name
                )
            ) return true
        }

        return false
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:" + phoneNumber)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent)

    }


    private fun launchApp(context: Context, pkgName: String) {
        val launchIntent: Intent = context.packageManager.getLaunchIntentForPackage(pkgName)!!
        context.startActivity(launchIntent)
    }


    private fun readApps() {

        val gson = Gson()
        val response: String = sharedPreferences.getString("MUA", "").toString()
        if (response.length > 0)
            choosenApps = gson.fromJson(
                response,
                object : TypeToken<List<App?>?>() {}.type
            )

        sortApps(choosenApps)

        appIndex = 0

    }


    private fun sortApps(apps: List<App>) {

        Collections.sort<App>(
            apps
        ) { p0, p1 ->
            p1.usage.compareTo(p0.usage)
        }

    }


    private fun showAppsDialog(context: Context) {

        context.startActivity(
            Intent(
                context,
                AppChooserDialog::class.java
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    }


    protected fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.setAction(action)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        lateinit var blurWallBitmap: Bitmap
        private var unlockReceiver: BroadcastReceiver? = null
        lateinit var widgetContext: Context
        lateinit var i_appWidgetIds: IntArray
        lateinit var gpBitmap: Bitmap
        var totalScreenTimeInHours: Long = 0
        lateinit var wD: String
        lateinit var qT: String
        lateinit var uT: String
        lateinit var dU: String

        lateinit var formattedDate: String
        lateinit var timeOfDay: String
        var timelyWish: String = ""
        var gpName: String = ""
        var hashSetAppUsage: HashSet<AppUsage> = HashSet()
        lateinit var dayOfTheWeek: String
        var vpStepsPos: Int = 0
        var noRewards: Int = 0
        var tW: String = "..."
        lateinit var appWidM: AppWidgetManager
        lateinit var mAppWidgetIds: IntArray



        fun isAppWidMInitialized(): Boolean {
            if (::appWidM.isInitialized)
                return true
             else
                return false

        }

        var selectedApps: ArrayList<SelectedApp> = ArrayList()
        lateinit var selectedApp: Bitmap

        var primaryColor = R.color.light_blue_900
        var secondaryColor = R.color.bg_light
        var tertianaryColor = R.color.bg_dark


        var favContacts: ArrayList<Contact> = ArrayList()
        var onEn: Boolean = false
        var remoteViews: RemoteViews? = null
        var lapCount: Int = 0


        fun drawableToBitmap(context: Context, drawable: Drawable): Bitmap {

            if (drawable is BitmapDrawable) {
                if (drawable.bitmap != null) {
                    return drawable.bitmap
                } else return drawableToBitmap(
                    context,
                    AppCompatResources.getDrawable(context, R.drawable.face_holder)!!
                )
            }

            val bitmap: Bitmap =
                if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                    Bitmap.createBitmap(
                        1,
                        1,
                        Bitmap.Config.ARGB_8888
                    ) // Single color bitmap will be created of 1x1 pixel
                } else {
                    Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        @SuppressLint("Range")
        fun greeting() {

            val currentHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]


            timeOfDay = if (currentHour < 6) {
                "Night!"
            } else if (currentHour < 12) {
                "Morni!"
            } else if (currentHour < 17) {
                "Noon!"
            } else if (currentHour < 21) {
                "Eve!"
            } else {
                "Night!"
            }

            timelyWish = timeOfDay


            val c: Cursor? = widgetContext.contentResolver
                .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null)
            c?.moveToFirst()

            Log.d("gpColNAmes", c?.columnNames.contentToString())

            try {
                gpName = c?.getString(c.getColumnIndex("display_name")).toString()
            } catch (ex: Exception) {
                showException(ex.message.toString())
            }

            //    remoteViews?.setImageViewBitmap(R.id.imgbtn_n_apps, gpBitmap)

            Log.d("gpName - ", gpName)
            c!!.close()

            if (timeOfDay == "Morni!")
                timelyWish = "$timeOfDay \uD83C\uDF3B "//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Noon!")
                timelyWish = "$timeOfDay ☀\uFE0F "//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Eve!")
                timelyWish = "$timeOfDay \uD83C\uDF41 "//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Night!")
                timelyWish = "$timeOfDay \uD83D\uDCA4 "//, ${gpName.split(" ").get(0)}!"

        }

        private fun showException(exp: String) {
            remoteViews?.setTextViewText(R.id.tx_runner, exp)
        }


        fun loadStepsData() {
            stepsData.clear()
            val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            val currentDay = LocalDate.now().dayOfWeek.name
            for (dayKey in days) {
                val count = if (dayKey == currentDay) stepsToday else sharedPreferences.getInt(dayKey, 0)
                stepsData.add(count.toString())
            }
        }


        fun todaysDate() {

            val c: Date = Calendar.getInstance().time
            val dfDate = SimpleDateFormat("d", Locale.getDefault())
            val dfMonth = SimpleDateFormat("MMM", Locale.getDefault())

            var postFixDate = ""



            if (dfDate.format(c).length == 1) {
                when (dfDate.format(c).trim().toInt()) {
                    1 -> postFixDate = "ˢᵗ"
                    2 -> postFixDate = "ⁿᵈ"
                    3 -> postFixDate = "ʳᵈ"
                    in 4..9 -> postFixDate = "ᵗʰ"

                }
            } else {
                when (dfDate.format(c).trim().toInt()) {
                    in 11..20 -> postFixDate = "ᵗʰ"
                    21, 31 -> postFixDate = "ˢᵗ"
                    22 -> postFixDate = "ⁿᵈ"
                    23 -> postFixDate = "ʳᵈ"
                    in 24..30 -> postFixDate = "ᵗʰ"

                }
            }

            val now = LocalDate.now()
            val dayName = now.dayOfWeek.name

            if (!::formattedDate.isInitialized) {
                formattedDate = dfDate.format(c) + postFixDate + " " + dfMonth.format(c)
                loadStepsData()
            } else if (formattedDate != dfDate.format(c) + postFixDate + " " + dfMonth.format(c)) {


                for (i in arrayListHabits)
                    i.isChecked = false

                if (isadapterHabitsInitialized()) adapterHabits.notifyDataSetChanged()
                // Midnight transition detected
                appUsageStats(widgetContext)

                // 1. Ensure current count is saved to disk before resetting
                sharedPreferencesEditor.putInt(dayName, stepsToday).apply()

                // 2. Reset counter for the NEW day
                stepsToday = 0
                sharedPreferencesEditor.putInt(dayName, 0).apply()
                sharedPreferencesEditor.putInt("unlockCount", 0).apply()
                sharedPreferencesEditor.putInt("waterCountToday", 0).apply()

                sharedPreferencesEditor.putInt("maxSpeedToday", 0).apply()
                sharedPreferencesEditor.putString("maxSpeedDate", LocalDate.now().toString()).apply()

                // 3. Weekly reset logic (Monday)
                if (now.dayOfWeek == java.time.DayOfWeek.MONDAY) {
                    val days = listOf("TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
                    days.forEach { sharedPreferencesEditor.putInt(it, 0) }

                    for (i in arrayListHabits) {
                        i.isChecked = false
                        sharedPreferencesEditor.putBoolean("${i.name}StateSu", false).apply()
                        sharedPreferencesEditor.putBoolean("${i.name}StateTu", false).apply()
                        sharedPreferencesEditor.putBoolean("${i.name}StateW", false).apply()
                        sharedPreferencesEditor.putBoolean("${i.name}StateTh", false).apply()
                        sharedPreferencesEditor.putBoolean("${i.name}StateF", false).apply()
                        sharedPreferencesEditor.putBoolean("${i.name}StateS", false).apply()
                    }
                    if (isadapterHabitsInitialized()) adapterHabits.notifyDataSetChanged()
                }

                formattedDate = dfDate.format(c) + postFixDate + " " + dfMonth.format(c)

                // 4. Synchronize the history list and notify adapter
                loadStepsData()
                if (isStepsAdapterInitialized()) stepsAdapter.notifyDataSetChanged()
            }


            sharedPreferencesEditor.putBoolean("DateSet", true).apply()
            sharedPreferencesEditor.putString("fD", formattedDate).apply()


            remoteViews?.setTextViewText(
                R.id.tx_day_date,
                SimpleDateFormat("EEE", Locale.getDefault()).format(c) +
                        ", " + formattedDate
            )


            greeting()
            remoteViews?.setTextViewText(R.id.tx_wish, timelyWish)

            try {

                if (gpName.length > 0)
                    remoteViews?.setTextViewText(
                        R.id.tx_myspace,
                        gpName.split(" ").get(0).substring(0, 1) + gpName.split(" ").get(1).substring(0, 1)
                    )
            } catch (ex: Exception) {
                gpName = ""
                showException(ex.message.toString())
            }
        }

        fun checkCompanionVariable(): Boolean {
            if (::wD.isInitialized && ::qT.isInitialized && ::uT.isInitialized && ::dU.isInitialized) {
                return true
            } else {
                return false
            }
        }

        private var appIndex: Int = 0

        lateinit var newAppWidget: ComponentName

        private const val FAB_SHARE = "fabShare"
        private const val WIFI_AUTO = "wifiAuto"
        private const val TORCH_STATE = "torch"

        //    private const val RL_INVERT = "rlInvert"
        private const val NEXT_STATE = "nextState"
        private const val PREV_STATE = "nextState"
        private const val SPEED_INFO = "sppedInfo"
        private const val TODO_CLICK = "todo1Click"
        private const val TIME_CLICK = "timeClick"
        private const val DATE_CLICK = "dateClick"
        private const val STEPSINFO_CLICK = "stepsinfoClick"
        private const val STEPS_CLICK = "stepsClick"
        private const val NEXT_ACT_CLICK = "nextActlick"
        private const val BATTERY_INFO = "batteryInfo"
        private const val GET_WEATHER = "getWeather"
        private const val SPEED_CHECK = "speedCheck"
        private const val STEPS_NOW = "newSteps"
        private const val LOCK_PHONE = "lockPhone"
        private const val SET_CLICKED = "setButtonClick"
        private const val BREATHE_INC = "breatheInc"
        private const val DRINK_INC = "drinkInc"

        private const val P_THUMBNAIL_CLICK = "p_album_click"
        private const val PLAYPAUSE_CLICK = "pp_click"
        private const val Time_A_CLICKED = "ta_click"
        private const val PS_CLICK = "psClick"
        private const val DIAL_CLICK = "dialClick"
        private const val C_CLICKED = "CClicked"
        private const val A_CLICKED = "AClicked"
        private const val ADD_TODO_CLICK = "addTodoClick"
        private const val WATER_REMINDER_CLICK = "waterReminderClick"
        private const val ACTION_LIST_CONTACTITEM_CLICK = "Contact_Item_Click"
        private const val ACTION_LIST_APPITEM_CLICK = "App_Item_Click"
        const val EXTRA_CONTACTITEM_POSITION = "Contact_Item_Pos"
        const val EXTRA_APPITEM_POSITION = "App_Item_Pos"
        const val EXTRA_CONTACTVIEW_ID = "CID"
        const val EXTRA_APPVIEW_ID = "AID"

    }


}