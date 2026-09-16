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
import android.graphics.Rect
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
import android.view.WindowManager
import android.view.WindowMetrics
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
import androidx.core.graphics.scale


class NewAppWidget : AppWidgetProvider() {


    private lateinit var widgetContext: Context
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

        recognizeActivityTransitions()
        sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        if (unlockReceiver == null) {
            unlockReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (Intent.ACTION_USER_PRESENT == intent.action) {

                            widgetContext = context
                            setUI()
                       //     setACAdapter()

                            setOnClickPendingIntents(context)

                        if (!isAppWidMInitialized())
                            appWidM = AppWidgetManager.getInstance(widgetContext)

                        remoteViews?.setTextViewText(R.id.tx_unlocks, sharedPreferences.getInt("unlockCount", 1).toString())
                        sharedPreferencesEditor.putInt("unlockCount", sharedPreferences.getInt("unlockCount", 1) + 1).apply()

                        mAppWidgetIds = appWidM.getAppWidgetIds(ComponentName(widgetContext, NewAppWidget::class.java))
                        appWidM.updateAppWidget(newAppWidget, remoteViews)
                   //     appWidM.notifyAppWidgetViewDataChanged(mAppWidgetIds, R.id.list_apps)
                   //     appWidM.notifyAppWidgetViewDataChanged(mAppWidgetIds, R.id.list_contacts)

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


        getScreenDimens()

        for (appWidgetId in appWidgetIds) {

            widgetContext = context

            setUI()

       //     if (!Constants.boolACadapterSet) {
           //     setACAdapter()
      //          Constants.boolACadapterSet = true
      //      }

            //  Create an intent to launch MainActivity



            if (!isAppWidMInitialized())
                appWidM = AppWidgetManager.getInstance(widgetContext)
            mAppWidgetIds = appWidM.getAppWidgetIds(ComponentName(widgetContext, NewAppWidget::class.java))
            appWidM.updateAppWidget(appWidgetId, remoteViews)
       //     appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_apps)
       //     appWidM.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_contacts)
        }


        super.onUpdate(context, appWidgetManager, appWidgetIds)

    }

    private fun getScreenDimens() {

        val wm = widgetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics: WindowMetrics = wm.currentWindowMetrics
        val bounds: Rect = metrics.bounds
        screenWidth = bounds.width()
        screenHeight = bounds.height()

    }


    private fun setOnClickPendingIntents(context: Context) {





        remoteViews?.setOnClickPendingIntent(R.id.imgbtn_close_activities, getPendingSelfIntent(context, CLOSE_ACTIVITIES))
        remoteViews?.setOnClickPendingIntent(R.id.imgbtn_fab, getPendingSelfIntent(context, ASSISTIVE_TOUCH))

    //    remoteViews?.setOnClickPendingIntent(R.id.switch_arrow, getPendingSelfIntent(context, ARROW_PAGES))
    //    remoteViews?.setOnClickPendingIntent(R.id.btn_ui_down, getPendingSelfIntent(context, ARROW_DOWN))
        remoteViews?.setOnClickPendingIntent(R.id.btn_ui_next, getPendingSelfIntent(context, NEXT_STATE))
        remoteViews?.setOnClickPendingIntent(R.id.btn_ui_prev, getPendingSelfIntent(context, PREV_STATE))

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
            R.id.clock,
            getPendingSelfIntent(context, TIME_CLICK)
        )
        remoteViews?.setOnClickPendingIntent(
            R.id.tx_day_date,
            getPendingSelfIntent(context, DATE_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.rl_battery,
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
            R.id.tw_fact, PendingIntent.getActivity(
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
            R.id.imgv_activity_state,
            getPendingSelfIntent(context, ACTINFO_CLICK)
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

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_menu, PendingIntent.getActivity(
                context, 55,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "Menu"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )


        remoteViews?.setOnClickPendingIntent(R.id.imgbtn_info_steps, PendingIntent.getActivity(
            context, 56,
            Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "WALKING"),
            PendingIntent.FLAG_IMMUTABLE
        )
        )



    }

    private fun locationTxUpdate(context: Context) {
  //      remoteViews?.setTextColor(R.id.tx_place, ColorUtil().matchPrimaryColor())
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
   //         remoteViews?.setTextColor(R.id.tx_place, ColorUtil().matchPrimaryColor())
            remoteViews?.setTextViewText(R.id.tx_weather, tempC.split(".")[0] + "° " + tempKind)
     //       remoteViews?.setTextColor(R.id.tx_weather, ColorUtil().matchPrimaryColor())
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

        if (penNote.isNotEmpty())
            remoteViews?.setTextViewText(R.id.tx_runner, "\uD83D\uDCDD " +penNote)

        remoteViews?.setTextViewText(R.id.tx_act_state, presentActivityState)


        remoteViews?.setTextViewText(R.id.tx_unlocks, sharedPreferences.getInt("unlockCount", 1).toString())


        if (presentActivityState == "STILL") {



            val baseTime = sharedPreferences.getLong("stillChr", SystemClock.elapsedRealtime())
            remoteViews?.setViewVisibility(R.id.still_chronometer, View.VISIBLE)
            remoteViews?.setChronometer(R.id.still_chronometer, baseTime, null, true)
            remoteViews?.setChronometer(R.id.walk_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setChronometer(R.id.speed_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setViewVisibility(R.id.walk_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_steps, View.VISIBLE)

            remoteViews?.setTextViewText(R.id.tx_act_count, Html.fromHtml("\uD800\uDCEF<sup>"+SetWallWorker.Companion.sharedPreferences.getInt("waterCountToday", 0).toString()+"</sup> " ))
            remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.still)
            remoteViews?.setViewVisibility(R.id.rl_still, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.rl_walking, View.GONE)
            remoteViews?.setViewVisibility(R.id.rl_speed, View.GONE)


        } else if (presentActivityState == "WALKING") {


            val baseTime = sharedPreferences.getLong("walkChr", SystemClock.elapsedRealtime())
            remoteViews?.setViewVisibility(R.id.walk_chronometer, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_steps, View.INVISIBLE)
            remoteViews?.setChronometer(R.id.walk_chronometer, baseTime, null, true)
            remoteViews?.setChronometer(R.id.still_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setChronometer(R.id.speed_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setViewVisibility(R.id.still_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)

            remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.steps)
            remoteViews?.setTextViewText(R.id.tx_act_count, stepsToday.toString())
            remoteViews?.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews?.setViewVisibility(R.id.rl_walking, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.rl_speed, View.GONE)

        //    stopSpeedService(context)
        } else if (presentActivityState == "TRAVEL") {

            val baseTime = sharedPreferences.getLong("speedChr", SystemClock.elapsedRealtime())
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.VISIBLE)
            remoteViews?.setChronometer(R.id.speed_chronometer, baseTime, null, true)
            remoteViews?.setChronometer(R.id.walk_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setChronometer(R.id.still_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setViewVisibility(R.id.walk_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.still_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_steps, View.VISIBLE)



            remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.in_a_vehicle)
            remoteViews?.setTextViewText(R.id.tx_act_count, sharedPreferences.getInt("current_speed", 0).toString())
            remoteViews?.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews?.setViewVisibility(R.id.rl_walking, View.GONE)
            remoteViews?.setViewVisibility(R.id.rl_speed, View.VISIBLE)

        }


        locationTxUpdate(widgetContext)

        remoteViews?.setTextViewText(R.id.tx_speed, speedReading)
        val maxSpeed = sharedPreferences.getInt("maxSpeedToday", 0)
        if (maxSpeed > 0)
            remoteViews?.setTextViewText(R.id.tx_max_speed, maxSpeed.toString())



            remoteViews?.setTextViewText(
                R.id.rl_tx_steps,
                "$stepsToday"
            )
        remoteViews?.setTextViewText(
            R.id.rl_tx_steps_in_km,
            " ~ " + String.format("%.1f",  (Integer.parseInt(stepsToday.toString()) * 74f) / 100000f)
        )
            remoteViews?.setTextViewText(R.id.rl_tx_cals, (stepsToday * 0.04 * (80 / 70)).toInt().toString())
            sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()


        if (hour != 0) {


            if (hour < 2) {

                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "LOW"
                )
            } else if (hour in 2..< 5) {
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "MODERATE"
                )
            } else if (hour in 5..< 8) {
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "HIGH"
                )
            } else {
                remoteViews?.setTextViewText(
                    R.id.tx_screenusage_state,
                    "EXCESSIVE"
                )
            }

            remoteViews?.setTextViewText(R.id.tx_screentime, hour.toString() + "+")

            //     remoteViews?.setTextColor(R.id.tx_screenusage_state, ColorUtil().matchPrimaryColor())
         //   remoteViews?.setTextColor(R.id.tx_screentime, ColorUtil().matchSecondaryColor())
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
            val albumArtPath = dataListSongs[songIndex].album.cover
            if (!albumArtPath.isNullOrBlank())
                Picasso.get()
                    .load(albumArtPath)
                    .into(remoteViews!!, R.id.imgbtn_albumcover, NewAppWidget.i_appWidgetIds)
            else remoteViews?.setImageViewResource(R.id.imgbtn_albumcover, R.drawable.launch)

            mMediaPlayer?.let {
                if (it.isPlaying)
                    remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.pause_m)
                else remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.play_m)
            }

        }

        if (isPinNoteInitialized()) {
            remoteViews?.setTextViewText(R.id.tx_runner, "\uD83D\uDCDD " +pinNote)
      //      remoteViews?.setTextColor(R.id.tx_runner, ColorUtil().matchTertianaryColor())
        }

        getPreciseEnergyCounter(widgetContext)
        seekWifiState()
        seekBluetoothState()
        todaysDate(widgetContext)
        loadStepsData() // Always refresh stepsData from disk to ensure persistence
        wallColors()
        setSomeTwAndWallDescUI()


        if (isMyServiceRunning(widgetContext, SpeedService::class.java)) {
            remoteViews?.setViewVisibility(R.id.tx_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.tx_max_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.VISIBLE)

        }

        val waterCount = sharedPreferences.getInt("waterCountToday", 0)
        remoteViews?.setTextViewText(R.id.tx_water_count, waterCount.toString())

        setOnClickPendingIntents(widgetContext)

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
            remoteViews?.setImageViewResource(R.id.menu_wifi, R.drawable.wifi_on)
        else if (wifiState) {
            remoteViews?.setImageViewResource(R.id.menu_wifi, R.drawable.wifi_on_but_not_connected)
            widgetContext.startActivity(
                Intent(widgetContext, DialogActivity::class.java)
                    .putExtra("DialogIntent", "Menu")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }else remoteViews?.setImageViewResource(R.id.menu_wifi, R.drawable.wifi_off)
    }

    private fun seekBluetoothState() {

        val blState = sharedPreferences.getBoolean("BluetoothState", false)
        val blConnectionState = sharedPreferences.getBoolean("BluetoothConnectionState", false)
        if (blState && blConnectionState)
            remoteViews?.setImageViewResource(R.id.menu_blue, R.drawable.blue_on)
        else if (blState) {
            remoteViews?.setImageViewResource(R.id.menu_blue, R.drawable.blue_red)
            widgetContext.startActivity(
                Intent(widgetContext, DialogActivity::class.java)
                    .putExtra("DialogIntent", "Menu")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else remoteViews?.setImageViewResource(R.id.menu_blue, R.drawable.blue_off)
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
        try {
            val wallpaperManager = WallpaperManager.getInstance(widgetContext)
            val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)

            if (wallpaperColors != null) {
                Log.d("wallColors", "notNULL")

                primaryColor = wallpaperColors.primaryColor.toArgb()
                secondaryColor = wallpaperColors.secondaryColor?.toArgb() ?: Color.GREEN
                tertianaryColor = wallpaperColors.tertiaryColor?.toArgb() ?: Color.BLUE

                wallpColors.clear()
                wallpColors.add(primaryColor)
                wallpColors.add(secondaryColor)
                wallpColors.add(tertianaryColor)

                val metrics = widgetContext.resources.displayMetrics
                if (screenWidth == 0 || screenHeight == 0) {
                    screenWidth = metrics.widthPixels
                    screenHeight = metrics.heightPixels
                }

                if (ismActInitialized()) {
                    mAct.windowManager.defaultDisplay.getMetrics(metrics)
                    screenHeight = metrics.heightPixels
                    screenWidth = metrics.widthPixels
                }

                remoteViews?.setImageViewBitmap(
                    R.id.imgv_player,
                    createGradientBitmap(screenWidth, 100, primaryColor, tertianaryColor)
                )

                if (isWallBitmapInitialized(widgetContext)) {
                    val currentWallBitmap = wallBitmap
                    if (!currentWallBitmap.isRecycled) {
                        scaledBitmap = Bitmap.createScaledBitmap(currentWallBitmap, screenWidth, screenHeight, true)

                        if (!scaledBitmap.isRecycled) {
                            val overlayColor = if (ColorUtil().isColorDark(primaryColor)) android.R.color.black else android.R.color.white
                            
                            val cropX = 10
                            val cropY = 25
                            val cropW = (screenWidth - 20).coerceAtLeast(1)
                            val cropH = (screenHeight - 150).coerceAtLeast(1)

                            if (cropX + cropW <= scaledBitmap.width && cropY + cropH <= scaledBitmap.height) {
                                val croppedBitmap = Bitmap.createBitmap(scaledBitmap, cropX, cropY, cropW, cropH)
                                val blurredBitmap = BitmapBlurHelper.blurBitmap(widgetContext, croppedBitmap)
                                val roundedDrawable = RoundedBitmapDrawableFactory.create(widgetContext.resources, blurredBitmap)
                                val finalBitmap = drawableToBitmap(widgetContext, roundedDrawable)

                                remoteViews?.setImageViewBitmap(
                                    R.id.imgv_widget_layout,
                                    applyThinFilmOverlay(finalBitmap, overlayColor, 75)
                                )
                            }
                        }

                        if (!currentWallBitmap.isRecycled) {
                            blurWallBitmap = blur(widgetContext, currentWallBitmap)
                        }
                    }
                }
            } else {
                Log.d("wallColors", "NULL")
            }
        } catch (e: Exception) {
            Log.e("wallColors", "Error in wallColors", e)
        }
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

        inputBitmap.recycle()
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
       // remoteViews?.setRemoteAdapter(R.id.list_apps, serviceIntentApp)
     //   remoteViews?.setEmptyView(R.id.list_apps, R.id.widget_empty_view_apps)
    }

    private fun setContactsAdapter() {
        serviceIntentContact = Intent(widgetContext, RemoteViewsContactsService::class.java)
        serviceIntentContact.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newAppWidget)
        serviceIntentContact.setData(Uri.parse(serviceIntentContact.toUri(Intent.URI_INTENT_SCHEME))) // Required for unique intents
    //    remoteViews?.setRemoteAdapter(R.id.list_contacts, serviceIntentContact)
      //  remoteViews?.setEmptyView(R.id.list_contacts, R.id.widget_empty_view_contacts)
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
      //  remoteViews?.setPendingIntentTemplate(R.id.list_apps, clickPendingIntentTemplateApp)

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
    //    remoteViews?.setPendingIntentTemplate(R.id.list_contacts, clickPendingIntentTemplateContact)
    }

    @SuppressLint("ResourceAsColor", "ResourceType")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: $action")

        // Initialize core components
        widgetContext = context
        newAppWidget = ComponentName(context, NewAppWidget::class.java)
        appWidM = AppWidgetManager.getInstance(context)
        sharedPreferences = context.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        // Handle specific system broadcasts
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                context.startForegroundService(Intent(context, StepsService::class.java))
            }
            "ACTION_UPDATE_SPEED" -> {
                speedReading = intent.getDoubleExtra("EXTRA_SPEED", 0.0).toString()
            }
        }

        // Standard AppWidgetProvider handling
        super.onReceive(context, intent)

        // For custom actions and speed updates, perform a unified UI refresh
        // Standard actions like ACTION_APPWIDGET_UPDATE are already handled by onUpdate via super.onReceive
        val standardActions = listOf(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_DELETED,
            AppWidgetManager.ACTION_APPWIDGET_DISABLED,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED
        )

        if (action !in standardActions) {
            if (remoteViews == null) {
                remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
            }
            getScreenDimens()
            setUI()
            handleIntentActions(intent)
            appWidM.updateAppWidget(newAppWidget, remoteViews)
        }
    }

    @SuppressLint("InflateParams", "ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleIntentActions(intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTINFO_CLICK -> {
                val current = sharedPreferences.getBoolean("activitiesORcontrols", false)
                sharedPreferencesEditor.putBoolean("activitiesORcontrols", !current).apply()
                val show = !current

                remoteViews?.apply {
                    setViewVisibility(R.id.imgbtn_close_activities, if (show) View.VISIBLE else View.INVISIBLE)
                    setViewVisibility(R.id.btn_ui_prev, if (show) View.VISIBLE else View.INVISIBLE)
                    setViewVisibility(R.id.btn_ui_next, if (show) View.VISIBLE else View.INVISIBLE)
                    setViewVisibility(R.id.ll_activity_states, if (show) View.VISIBLE else View.INVISIBLE)

                    setViewVisibility(R.id.rl_setwall, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_qr, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_g_apps, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_lock, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_speech, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.tx_myspace, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgv_conf, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgv_ps, if (show) View.INVISIBLE else View.VISIBLE)
                    setViewVisibility(R.id.imgv_dialler, if (show) View.INVISIBLE else View.VISIBLE)

                    if (show) {
                        setViewVisibility(R.id.rl_still, if (presentActivityState == "STILL") View.VISIBLE else View.GONE)
                        setViewVisibility(R.id.rl_walking, if (presentActivityState == "WALKING") View.VISIBLE else View.GONE)
                        setViewVisibility(R.id.rl_speed, if (presentActivityState == "TRAVEL") View.VISIBLE else View.GONE)
                    }
                }
            }
            NEXT_STATE -> {
                val displayedAct = sharedPreferences.getString("displayedAct", presentActivityState)
                val next = when (displayedAct) {
                    "STILL" -> "WALKING"
                    "WALKING" -> "TRAVEL"
                    else -> "STILL"
                }
                sharedPreferencesEditor.putString("displayedAct", next).apply()
                remoteViews?.apply {
                    setViewVisibility(R.id.rl_still, if (next == "STILL") View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.rl_walking, if (next == "WALKING") View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.rl_speed, if (next == "TRAVEL") View.VISIBLE else View.GONE)
                }
            }
            PREV_STATE -> {
                val displayedAct = sharedPreferences.getString("displayedAct", presentActivityState)
                val prev = when (displayedAct) {
                    "STILL" -> "TRAVEL"
                    "WALKING" -> "STILL"
                    else -> "WALKING"
                }
                sharedPreferencesEditor.putString("displayedAct", prev).apply()
                remoteViews?.apply {
                    setViewVisibility(R.id.rl_still, if (prev == "STILL") View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.rl_walking, if (prev == "WALKING") View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.rl_speed, if (prev == "TRAVEL") View.VISIBLE else View.GONE)
                }
            }
            CLOSE_ACTIVITIES -> {
                sharedPreferencesEditor.putBoolean("activitiesORcontrols", false).apply()
                remoteViews?.apply {
                    setViewVisibility(R.id.imgbtn_close_activities, View.INVISIBLE)
                    setViewVisibility(R.id.btn_ui_prev, View.INVISIBLE)
                    setViewVisibility(R.id.btn_ui_next, View.INVISIBLE)
                    setViewVisibility(R.id.ll_activity_states, View.INVISIBLE)
                    setViewVisibility(R.id.rl_setwall, View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_qr, View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_g_apps, View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_lock, View.VISIBLE)
                    setViewVisibility(R.id.imgbtn_speech, View.VISIBLE)
                    setViewVisibility(R.id.tx_myspace, View.VISIBLE)
                    setViewVisibility(R.id.imgv_conf, View.VISIBLE)
                    setViewVisibility(R.id.imgv_ps, View.VISIBLE)
                    setViewVisibility(R.id.imgv_dialler, View.VISIBLE)
                }
            }
            ASSISTIVE_TOUCH -> {
                val current = sharedPreferences.getBoolean("rlControls", false)
                sharedPreferencesEditor.putBoolean("rlControls", !current).apply()
                val show = !current

                remoteViews?.apply {
                    if (show) {
                        setViewVisibility(R.id.ll_activity_states, View.INVISIBLE)
                        setViewVisibility(R.id.rl_setwall, View.VISIBLE)
                        setViewVisibility(R.id.imgbtn_qr, View.VISIBLE)
                        setViewVisibility(R.id.imgbtn_g_apps, View.VISIBLE)
                        setViewVisibility(R.id.imgbtn_lock, View.VISIBLE)
                        setViewVisibility(R.id.imgbtn_speech, View.VISIBLE)
                        setViewVisibility(R.id.tx_myspace, View.VISIBLE)
                        setViewVisibility(R.id.imgv_conf, View.VISIBLE)
                        setViewVisibility(R.id.imgv_ps, View.VISIBLE)
                        setViewVisibility(R.id.imgv_dialler, View.VISIBLE)
                    } else {
                        setViewVisibility(R.id.rl_still, if (presentActivityState == "STILL") View.VISIBLE else View.GONE)
                        setViewVisibility(R.id.rl_walking, if (presentActivityState == "WALKING") View.VISIBLE else View.GONE)
                        setViewVisibility(R.id.rl_speed, if (presentActivityState == "TRAVEL") View.VISIBLE else View.GONE)

                        setViewVisibility(R.id.ll_activity_states, View.VISIBLE)
                        setViewVisibility(R.id.rl_setwall, View.INVISIBLE)
                        setViewVisibility(R.id.imgbtn_qr, View.INVISIBLE)
                        setViewVisibility(R.id.imgbtn_g_apps, View.INVISIBLE)
                        setViewVisibility(R.id.imgbtn_lock, View.INVISIBLE)
                        setViewVisibility(R.id.imgbtn_speech, View.INVISIBLE)
                        setViewVisibility(R.id.tx_myspace, View.INVISIBLE)
                        setViewVisibility(R.id.imgv_conf, View.INVISIBLE)
                        setViewVisibility(R.id.imgv_ps, View.INVISIBLE)
                        setViewVisibility(R.id.imgv_dialler, View.INVISIBLE)
                    }
                }
            }
            TODO_CLICK -> makeToast(widgetContext, "inc")
            TIME_CLICK -> {
                val mClockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                widgetContext.startActivity(mClockIntent)
            }
            DATE_CLICK -> {
                val startMillis = System.currentTimeMillis()
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                ContentUris.appendId(builder, startMillis)
                val intentCalendar = Intent(Intent.ACTION_VIEW).setData(builder.build())
                intentCalendar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                widgetContext.startActivity(intentCalendar)
            }
            STEPS_CLICK -> {
                makeToast(widgetContext, "$stepsToday ~ " + String.format("%.1f", stepsToday * 74f / 100000f) + " Km")
                remoteViews?.setTextViewText(R.id.tx_act_count, "$stepsToday")
                widgetContext.startActivity(Intent(widgetContext, DialogActivity::class.java)
                    .putExtra("DialogIntent", "stepsInfo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            NEXT_ACT_CLICK -> {
                makeToast(widgetContext, "  $presentActivityState")
                widgetContext.startActivity(Intent(widgetContext, DialogActivity::class.java)
                    .putExtra("DialogIntent", "activitiesInfo").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            BATTERY_INFO -> {
                val powerUsageIntent = Intent("android.intent.action.POWER_USAGE_SUMMARY")
                if (powerUsageIntent.resolveActivity(widgetContext.packageManager) != null) {
                    powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    widgetContext.startActivity(powerUsageIntent)
                }
            }
            SPEED_CHECK -> {
                if (isMyServiceRunning(widgetContext, SpeedService::class.java)) {
                    if (widgetContext.stopService(Intent(widgetContext, SpeedService::class.java))) {
                        makeToast(widgetContext, "  ⃠  ")
                        remoteViews?.setChronometer(R.id.speed_chronometer, 0L, null, false)
                        remoteViews?.setViewVisibility(R.id.tx_speed, View.INVISIBLE)
                        remoteViews?.setViewVisibility(R.id.tx_max_speed, View.INVISIBLE)
                        remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)
                    }
                } else {
                    val baseTime = SystemClock.elapsedRealtime()
                    remoteViews?.setChronometer(R.id.speed_chronometer, baseTime, null, true)
                    remoteViews?.setViewVisibility(R.id.tx_speed, View.VISIBLE)
                    remoteViews?.setViewVisibility(R.id.tx_max_speed, View.VISIBLE)
                    remoteViews?.setViewVisibility(R.id.speed_chronometer, View.VISIBLE)
                    remoteViews?.setTextViewText(R.id.tx_max_speed, "MAX")
                    sharedPreferencesEditor.putInt("maxSpeedToday", 0).apply()
                    widgetContext.startForegroundService(Intent(widgetContext, SpeedService::class.java))
                }
            }
            GET_WEATHER -> {
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic_weather, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.tx_refresh_weather, View.INVISIBLE)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                StepsService.getWeatherData(LatLng(cityLat, cityLng))
            }
            PLAYPAUSE_CLICK -> {
                if (boolMusicServiceRunning) {
                    try {
                        mMediaPlayer?.let {
                            if (it.isPlaying) {
                                it.pause()
                                remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.play_m)
                            } else {
                                startMusicActivity(songIndex)
                                remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.pause_m)
                                it.play()
                            }
                        } ?: startMusicActivity(songIndex)
                    } catch (ex: Exception) {
                        remoteViews?.setTextViewText(R.id.tx_runner, ex.message)
                        startMusicActivity(0)
                    }
                } else {
                    startMusicActivity(songIndex)
                }
            }
            P_THUMBNAIL_CLICK -> {
                widgetContext.startActivity(Intent(widgetContext, DialogActivity::class.java)
                    .putExtra("DialogIntent", "SongCover").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_LIST_CONTACTITEM_CLICK -> {
                getFavoriteContacts(widgetContext)
                val position = intent.getIntExtra(EXTRA_CONTACTITEM_POSITION, AdapterView.INVALID_POSITION)
                val viewID = intent.getIntExtra(EXTRA_CONTACTVIEW_ID, 7)
                if (position != AdapterView.INVALID_POSITION) {
                    if (viewID == 0) dialPhoneNumber(widgetContext, favContacts[position].number)
                    else if (viewID == 1 && favContacts.size > position) unMarkAsFav(favContacts[position].id)
                } else {
                    widgetContext.startActivity(Intent(widgetContext, DialogActivity::class.java)
                        .putExtra("DialogIntent", "PC").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            ACTION_LIST_APPITEM_CLICK -> {
                val position = intent.getIntExtra(EXTRA_APPITEM_POSITION, AdapterView.INVALID_POSITION)
                val viewID = intent.getIntExtra(EXTRA_APPVIEW_ID, 7)
                if (position != AdapterView.INVALID_POSITION && viewID == 0) {
                    widgetContext.packageManager.getLaunchIntentForPackage(choosenApps[position].pName)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        widgetContext.startActivity(it)
                    }
                }
            }
            FAB_SHARE -> {
                val inflater = widgetContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val appWidgetView = inflater.inflate(R.layout.new_app_widget, null)
                appWidgetView.measure(
                    View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(screenHeight - 725, View.MeasureSpec.EXACTLY)
                )
                appWidgetView.layout(0, 0, appWidgetView.measuredWidth, appWidgetView.measuredHeight)
                var bitmapWidget = Bitmap.createBitmap(appWidgetView.width, appWidgetView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmapWidget)
                appWidgetView.draw(canvas)
                bitmapWidget = Bitmap.createScaledBitmap(bitmapWidget, Math.round(bitmapWidget.width * 0.5f), Math.round(bitmapWidget.height * 0.5f), true)
                shareBitmap(bitmapWidget)
            }
            WIFI_AUTO -> {
                widgetContext.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            TORCH_STATE -> {
                if (widgetContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    val cameraManager = widgetContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    try {
                        val cameraId = cameraManager.cameraIdList[0]
                        val isTorchOn = sharedPreferences.getBoolean("Torch", false)
                        cameraManager.setTorchMode(cameraId, !isTorchOn)
                        remoteViews?.setImageViewResource(R.id.menu_torch, if (isTorchOn) R.drawable.torch_off else R.drawable.torch_on)
                        sharedPreferencesEditor.putBoolean("Torch", !isTorchOn).apply()
                    } catch (ex: Exception) {
                        remoteViews?.setTextViewText(R.id.tx_runner, ex.message)
                    }
                }
            }
            STEPS_NOW -> {
                boolNewLap = !boolNewLap
                sharedPreferencesEditor.putBoolean("newLap", boolNewLap).apply()
            }
            LOCK_PHONE -> {
                if (isAccessibilityServiceEnabled(widgetContext, LockAccessibilityService::class.java)) {
                    LockAccessibilityService.lockScreenAccessibility(widgetContext)
                } else {
                    widgetContext.startActivity(Intent(widgetContext, DialogActivity::class.java)
                        .putExtra("DialogIntent", "AccessibilityPermDialog").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            SET_CLICKED -> {
                widgetContext.packageManager.getLaunchIntentForPackage("com.belaku.homey")?.let {
                    widgetContext.startActivity(it)
                }
            }
            A_CLICKED -> {
                widgetContext.startActivity(Intent(widgetContext, AppsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            C_CLICKED -> {
                widgetContext.startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            DIAL_CLICK -> {
                widgetContext.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PS_CLICK -> {
                widgetContext.packageManager.getLaunchIntentForPackage("com.android.vending")?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    widgetContext.startActivity(it)
                }
            }
            Time_A_CLICKED -> {
                val current = sharedPreferences.getBoolean("SPKSERVICE", false)
                val speakIntent = Intent(widgetContext, SpeakService::class.java)
                if (!current) {
                    widgetContext.startService(speakIntent)
                    remoteViews?.setTextViewText(R.id.tx_time_announcement, "\uD83D\uDDE3")
                    sharedPreferencesEditor.putBoolean("SPKSERVICE", true).apply()
                    makeToast(widgetContext, "Incoming notifications and hour changes will be read out loud.")
                } else {
                    widgetContext.stopService(speakIntent)
                    remoteViews?.setTextViewText(R.id.tx_time_announcement, "⊘")
                    sharedPreferencesEditor.putBoolean("SPKSERVICE", false).apply()
                }
            }
            ADD_TODO_CLICK -> makeToast(widgetContext, "Add Todo Clicked!")
            WATER_REMINDER_CLICK -> {
                val waterCount = sharedPreferences.getInt("waterCountToday", 0) + 1
                sharedPreferencesEditor.putInt("waterCountToday", waterCount).apply()
                remoteViews?.setTextViewText(R.id.tx_water_count, waterCount.toString())
            }
            MENU_CLICK -> makeToast(widgetContext, "hi")
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

        getFavoriteContacts(widgetContext)
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
            setColorStateList(greenColor)
            remoteViews?.setTextColor(
                R.id.tx_battery,
                widgetContext.resources.getColor(android.R.color.holo_green_dark)
            )
        } else if (energy.toInt() < 30) {
            setColorStateList(redColor)
            remoteViews?.setTextColor(
                R.id.tx_battery,
                widgetContext.resources.getColor(android.R.color.holo_red_dark)
            )
        } else {
            setColorStateList(amberColor)
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

    private fun setColorStateList(color: ColorStateList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            remoteViews?.setColorStateList(
                R.id.progressBar_battery,
                "setProgressTintList",
                color
            )
        } else {
            // Fallback for Android 11 and below (e.g., using a solid int color if applicable)
          //yet2impl  remoteViews?.setInt(R.id.progressBar_battery, "setTint", color)
        }
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

        var penNote: String = ""
        lateinit var blurWallBitmap: Bitmap
        private var unlockReceiver: BroadcastReceiver? = null
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

        fun isblurWallBitmapInitialized(): Boolean {
            if (::blurWallBitmap.isInitialized)
                return true
            else
                return false

        }


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
        fun greeting(context: Context) {

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


            val c: Cursor? = context.contentResolver
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
                timelyWish = "\uD83C\uDF3B"//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Noon!")
                timelyWish = "☀\uFE0F"//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Eve!")
                timelyWish = "\uD83C\uDF41"//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Night!")
                timelyWish = "\uD83D\uDCA4"//, ${gpName.split(" ").get(0)}!"

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


        @SuppressLint("ResourceAsColor")
        fun todaysDate(context: Context) {

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
                appUsageStats(context)

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


            greeting(context)
            remoteViews?.setTextViewText(R.id.tw_fact, timelyWish)

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


        fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
            val r = (Color.red(color1) + ratio * (Color.red(color2) - Color.red(color1))).toInt()
            val g = (Color.green(color1) + ratio * (Color.green(color2) - Color.green(color1))).toInt()
            val b = (Color.blue(color1) + ratio * (Color.blue(color2) - Color.blue(color1))).toInt()
            return Color.rgb(r, g, b)
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

        private const val CLOSE_ACTIVITIES = "closeActivities"
        private const val ASSISTIVE_TOUCH = "assistiveTouch"
        //    private const val RL_INVERT = "rlInvert"
        private const val NEXT_STATE = "nextState"
        private const val PREV_STATE = "prevState"
        private const val ARROW_PAGES = "arrowUp"
        private const val SPEED_INFO = "sppedInfo"
        private const val TODO_CLICK = "todo1Click"
        private const val TIME_CLICK = "timeClick"
        private const val DATE_CLICK = "dateClick"
        private const val ACTINFO_CLICK = "actinfoClick"
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
        private const val MENU_CLICK = "menuClick"
        private const val ACTION_LIST_CONTACTITEM_CLICK = "Contact_Item_Click"
        private const val ACTION_LIST_APPITEM_CLICK = "App_Item_Click"
        const val EXTRA_CONTACTITEM_POSITION = "Contact_Item_Pos"
        const val EXTRA_APPITEM_POSITION = "App_Item_Pos"
        const val EXTRA_CONTACTVIEW_ID = "CID"
        const val EXTRA_APPVIEW_ID = "AID"

    }


}
