package com.belaku.homey


// Weather Key - 9fa8e101240ab18615e3133b051e767e


import android.Manifest
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
import androidx.annotation.RequiresPermission
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.cityLat
import com.belaku.homey.MainActivity.Companion.cityLng
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.tempC
import com.belaku.homey.MainActivity.Companion.tempKind
import com.belaku.homey.MainActivity.Companion.weatherIconID
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
import com.belaku.homey.SetWallWorker.Companion.hour
import com.belaku.homey.SetWallWorker.Companion.isPinNoteInitialized
import com.belaku.homey.SetWallWorker.Companion.isSharedPreferencesInitialized
import com.belaku.homey.SetWallWorker.Companion.isWallBitmapInitialized
import com.belaku.homey.SetWallWorker.Companion.ismActInitialized
import com.belaku.homey.SetWallWorker.Companion.lastAppUsageStatsQueryTimeMs
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
import java.time.LocalDate
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import androidx.core.graphics.scale
import com.belaku.homey.MainActivity.Companion.makeSnack
import com.belaku.homey.StepsService.Companion.Top3
import com.belaku.homey.StepsService.Companion.strDurationTravel
import com.belaku.homey.StepsService.Companion.strDurationWalk


class NewAppWidget : AppWidgetProvider() {


    private lateinit var widgetContext: Context
    private lateinit var activityTransitionRequest: ActivityTransitionRequest
    private lateinit var pendingIntentActivityTransitions: PendingIntent
    private lateinit var activityTransitions: ArrayList<ActivityTransition>
    private var requestCodeAT: Int = 57
    private lateinit var intentActivityTransitionReceiver: Intent
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



    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        if (context == null) return

        val appContext = context.applicationContext
        widgetContext = appContext
        onEn = true
        ensurePrefs(appContext)

        ensureUnlockReceiver(appContext)

        if (ismActInitialized())
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mAct)
    }

    private fun ensureUnlockReceiver(context: Context) {
        if (unlockReceiver != null) return

        val appContext = context.applicationContext
        unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (Intent.ACTION_USER_PRESENT == intent.action) {
                    val appContextInside = ctx.applicationContext

                    // Increment count in SharedPreferences first
                    val prefs = appContextInside.getSharedPreferences("UserPreferences", MODE_PRIVATE)
                    val currentCount = prefs.getInt("unlockCount", 0)
                    prefs.edit().putInt("unlockCount", currentCount + 1).apply()

                    // Trigger a refresh. We use a fresh instance to avoid stale property captures.
                    val manager = AppWidgetManager.getInstance(appContextInside)
                    val componentName = ComponentName(appContextInside, NewAppWidget::class.java)
                    val ids = manager.getAppWidgetIds(componentName)

                    val provider = NewAppWidget()
                    provider.onUpdate(appContextInside, manager, ids)
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(unlockReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(unlockReceiver, filter)
        }
    }

    /** Ensures the shared companion [sharedPreferences] / editor are usable from any entry point. */
    private fun ensurePrefs(context: Context) {
        if (!isSharedPreferencesInitialized()) {
            sharedPreferences = context.applicationContext
                .getSharedPreferences("UserPreferences", MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences.edit()
        }
    }

    /** Ensures [remoteViews] is non-null before any setter is invoked on it. */
    private fun ensureRemoteViews(context: Context) {
        if (remoteViews == null) {
            remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
        }
    }

    @SuppressLint("MissingPermission")
    private fun recognizeActivityTransitions() {

        // ActivityTransitionReceiver is already declared in the manifest with the
        // "action.TRANSITIONS_DATA" filter, so no runtime registration is needed here.
        // Registering it again on every onEnabled() leaked a receiver and never unregistered it.

        // setUI() calls this on every widget click (via onReceive()). The registration below is
        // idempotent by PendingIntent (same request code + action), but re-issuing it is still a
        // synchronous IPC call to Google Play Services on every single tap. Since the request
        // never actually changes at runtime, only register once and skip on subsequent calls -
        // this was contributing to the perceived click delay.
        if (activityTransitionsRegistered) return

   //     remoteViews?.setTextViewText(R.id.tx_act_state, "fetching..,")

        intentActivityTransitionReceiver =
            Intent(widgetContext, ActivityTransitionReceiver::class.java).setAction("action.TRANSITIONS_DATA")
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
        try {
            ActivityRecognition.getClient(widgetContext)
                .requestActivityTransitionUpdates(
                    activityTransitionRequest,
                    pendingIntentActivityTransitions
                )
                .addOnSuccessListener {
                    activityTransitionsRegistered = true
                    Log.d(TAG, "Activity transition updates registered")
                }
                .addOnFailureListener { e -> Log.e(TAG, "Activity transition updates failed", e) }
        } catch (e: Exception) {
            Log.e(TAG, "requestActivityTransitionUpdates threw", e)
        }
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

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        if (context == null) return
        val appContext = context.applicationContext
        widgetContext = appContext

        unlockReceiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister unlockReceiver", e)
            }
            unlockReceiver = null
        }

        // Clean up everything registered in onEnabled, otherwise the receiver leaks.
        try {
            if (::pendingIntentActivityTransitions.isInitialized) {
                ActivityRecognition.getClient(widgetContext)
                    .removeActivityTransitionUpdates(pendingIntentActivityTransitions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "removeActivityTransitionUpdates failed", e)
        }
        activityTransitionsRegistered = false

        remoteViews = null
        onEn = false
    }

     
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        widgetContext = context.applicationContext
        Log.d(TAG, "!onUpdate")

        ensureUnlockReceiver(widgetContext)

        try {
            remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
            newAppWidget = ComponentName(context, NewAppWidget::class.java)
            appWidM = appWidgetManager
            i_appWidgetIds = appWidgetIds
            mAppWidgetIds = i_appWidgetIds

            ensurePrefs(widgetContext)

            getScreenDimens()

            setUI()

            mAppWidgetIds = appWidgetIds
            for (appWidgetId in appWidgetIds) {
                appWidM.updateAppWidget(appWidgetId, remoteViews)
            }
        } catch (e: Exception) {
            // Never let an exception escape a BroadcastReceiver callback: it kills the host process.
            Log.e(TAG, "onUpdate failed", e)
        }
    }

    private fun getScreenDimens() {
        try {
            val wm = widgetContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (wm != null) {
                val metrics: WindowMetrics = wm.currentWindowMetrics
                val bounds: Rect = metrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) {
                    screenWidth = bounds.width()
                    screenHeight = bounds.height()
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getScreenDimens via WindowManager failed", e)
        }

        // Fallback so downstream Bitmap.createScaledBitmap() never receives 0/negative sizes.
        val dm: DisplayMetrics = widgetContext.resources.displayMetrics
        screenWidth = dm.widthPixels.coerceAtLeast(1)
        screenHeight = dm.heightPixels.coerceAtLeast(1)
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
            R.id.tx_place,
            getPendingSelfIntent(context, PLACE_CLICK)
        )
        remoteViews?.setOnClickPendingIntent(
            R.id.tx_place_permission_hint,
            getPendingSelfIntent(context, LOC_P_REQ)
        )
        remoteViews?.setOnClickPendingIntent(
            R.id.tx_weather,
            getPendingSelfIntent(context, WEATHER_CLICK)
        )
        remoteViews?.setOnClickPendingIntent(
            R.id.tx_weather_permission_hint,
            getPendingSelfIntent(context, LOC_P_REQ)
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
            R.id.tx_time_announcement_permission_hint,
            getPendingSelfIntent(context, NOT_P_REQ)
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
            getPendingSelfIntent(context, SCRTIME_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_scrtime_permission_hint,
            getPendingSelfIntent(context, APP_USAGE_P_REQ)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_activity_state,
            getPendingSelfIntent(context, ACTINFO_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_act_permission_hint,
            getPendingSelfIntent(context, ACT_RECOGNITION_P_REQ)
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
            R.id.tx_act_plus,
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

        remoteViews?.setOnClickPendingIntent(R.id.imgbtn_info_speed, PendingIntent.getActivity(
            context, 58,
            Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "SPEED"),
            PendingIntent.FLAG_IMMUTABLE
        )
        )

        remoteViews?.setOnClickPendingIntent(R.id.imgv_app1, getPendingSelfIntent(context, APP1_CLICK))
        remoteViews?.setOnClickPendingIntent(R.id.imgv_app2, getPendingSelfIntent(context, APP2_CLICK))
        remoteViews?.setOnClickPendingIntent(R.id.imgv_app3, getPendingSelfIntent(context, APP3_CLICK))



    }

    private fun locationTxUpdate(context: Context) {
  //      remoteViews?.setTextColor(R.id.tx_place, ColorUtil().matchPrimaryColor())
        if (!isLocationEnabled(context)) {
            remoteViews?.setTextViewText(R.id.tx_place, "Please Enable Location services!")
        } else {

            cName = cityname.ifBlank { "⚠ Place Information" }

            remoteViews?.setTextViewText(R.id.tx_place, cName)
   //         remoteViews?.setTextColor(R.id.tx_place, ColorUtil().matchPrimaryColor())

            // tempC may still be empty before the first weather response; split()[0] on an
            // empty string yields "" and rendered a stray "° " label.
            if (tempC.isNotBlank()) {
                remoteViews?.setTextViewText(
                    R.id.tx_weather,
                    tempC.substringBefore(".") + "° " + tempKind
                )
            } else remoteViews?.setTextViewText(R.id.tx_weather, "⚠ Weather Information")
     //       remoteViews?.setTextColor(R.id.tx_weather, ColorUtil().matchPrimaryColor())
            when {
                weatherIconID.startsWith("5") ->
                    remoteViews?.setImageViewResource(R.id.imgv_weather_icon, R.drawable.rain)

                weatherIconID == "800" ->
                    remoteViews?.setImageViewResource(R.id.imgv_weather_icon, R.drawable.clear_sky)

                weatherIconID in setOf("801", "802", "803", "804") ->
                    remoteViews?.setImageViewResource(R.id.imgv_weather_icon, R.drawable.clouds)
            }
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        return try {
            val locationManager = context.getSystemService(LOCATION_SERVICE) as? LocationManager
                ?: return false
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "isLocationEnabled failed", e)
            false
        }
    }


    @SuppressLint("SuspiciousIndentation")
      
    private fun setUI() {

        // setUI() is reachable from several receiver paths; make sure the shared state it
        // relies on exists, otherwise lateinit access throws UninitializedPropertyAccessException.
        ensureRemoteViews(widgetContext)
        ensurePrefs(widgetContext)

        // SharedPreferences is the source of truth: ActivityTransitionReceiver persists the state
        // before requesting this redraw. Only trusting the static when it was blank meant a stale
        // in-memory value (e.g. left over from an earlier process) shadowed the real state.
        val storedActivityState = sharedPreferences.getString("presentActivityState", "") ?: ""
        if (storedActivityState.isNotBlank()) {
            presentActivityState = storedActivityState
        }

        if (penNote.isNotEmpty())
            remoteViews?.setTextViewText(R.id.tx_runner, "\uD83D\uDCDD " + penNote)

        remoteViews?.setTextViewText(R.id.tx_act_state, presentActivityState)

        if (Top3.isNotEmpty()) {
            remoteViews?.setImageViewBitmap(R.id.imgv_app1, Top3.get(0).iconBitmap)
            if (Top3.size > 1)
                remoteViews?.setImageViewBitmap(R.id.imgv_app2, Top3.get(1).iconBitmap)
            if (Top3.size > 2)
                remoteViews?.setImageViewBitmap(R.id.imgv_app3, Top3.get(2).iconBitmap)
        }

        remoteViews?.setTextViewText(R.id.tx_unlocks, sharedPreferences.getInt("unlockCount", 0).toString())


        if (presentActivityState == "STILL") {

            var baseTime = sharedPreferences.getLong("stillChr", 0L)
            if (baseTime == 0L) {
                baseTime = SystemClock.elapsedRealtime()
                sharedPreferencesEditor.putLong("stillChr", baseTime).apply()
            }
            remoteViews?.setViewVisibility(R.id.still_chronometer, View.VISIBLE)
            remoteViews?.setChronometer(R.id.still_chronometer, baseTime, null, true)
            remoteViews?.setChronometer(R.id.walk_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setChronometer(R.id.speed_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setViewVisibility(R.id.walk_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_steps, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_speed, View.VISIBLE)

            remoteViews?.setTextViewText(R.id.tx_act_count, sharedPreferences.getInt("waterCountToday", 0).toString() + "\uD800\uDCEF" )
            remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.still)
            remoteViews?.setViewVisibility(R.id.rl_still, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.tx_act_plus, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.rl_walking, View.GONE)
            remoteViews?.setViewVisibility(R.id.rl_speed, View.GONE)


        } else if (presentActivityState == "WALKING") {


            var baseTime = sharedPreferences.getLong("walkChr", 0L)
            if (baseTime == 0L) {
                baseTime = SystemClock.elapsedRealtime()
                sharedPreferencesEditor.putLong("walkChr", baseTime).apply()
            }
            remoteViews?.setViewVisibility(R.id.walk_chronometer, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_steps, View.INVISIBLE)
            remoteViews?.setChronometer(R.id.walk_chronometer, baseTime, null, true)
            remoteViews?.setChronometer(R.id.still_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setChronometer(R.id.speed_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setViewVisibility(R.id.still_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)

            remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.steps)
            remoteViews?.setTextViewText(R.id.tx_act_count, stepsToday.toString())
            remoteViews?.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews?.setViewVisibility(R.id.tx_act_plus, View.GONE)
            remoteViews?.setViewVisibility(R.id.rl_walking, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.rl_speed, View.GONE)

        //    stopSpeedService(context)
        } else if (presentActivityState == "TRAVEL") {

            var baseTime = sharedPreferences.getLong("speedChr", 0L)
            if (baseTime == 0L) {
                baseTime = SystemClock.elapsedRealtime()
                sharedPreferencesEditor.putLong("speedChr", baseTime).apply()
            }
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.VISIBLE)
            remoteViews?.setChronometer(R.id.speed_chronometer, baseTime, null, true)
            remoteViews?.setChronometer(R.id.walk_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setChronometer(R.id.still_chronometer, SystemClock.elapsedRealtime(), null, false)
            remoteViews?.setViewVisibility(R.id.walk_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.still_chronometer, View.INVISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_steps, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_info_speed, View.INVISIBLE)




            remoteViews?.setImageViewResource(R.id.imgv_activity_state, R.drawable.in_a_vehicle)
            remoteViews?.setTextViewText(R.id.tx_act_count, sharedPreferences.getInt("current_speed", 0).toString())
            remoteViews?.setViewVisibility(R.id.rl_still, View.GONE)
            remoteViews?.setViewVisibility(R.id.tx_act_plus, View.GONE)
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
            " ~ " + String.format(Locale.getDefault(), "%.1f", (stepsToday * 74f) / 100000f)
        )
        // The previous formula used integer division `(80 / 70)` which always evaluated to 1,
        // silently discarding the weight factor.
        val weightKg = sharedPreferences.getInt("userWeightKg", 70).toDouble()
        val heightCm = sharedPreferences.getInt("userHeightCm", 170).toDouble()
        remoteViews?.setTextViewText(
            R.id.rl_tx_cals,
            calculateCaloriesFromSteps(stepsToday, weightKg, heightCm).toInt().toString()
        )
        sharedPreferencesEditor.putInt(LocalDate.now().dayOfWeek.name, stepsToday).apply()


        val hasUsagePermission = hasFeaturePermission(widgetContext, FeaturePermission.USAGE_STATS)
        if (hasUsagePermission) {
            // appUsageStats() queries a week of UsageStatsManager data and decodes every app's
            // icon bitmap - setUI() runs synchronously on the main thread for every widget tap
            // (via onReceive()), so re-running this on each click was a major source of the
            // perceived click delay. The underlying usage data changes on the order of minutes,
            // not clicks, so throttle it instead of dropping it entirely.
            val now = SystemClock.elapsedRealtime()
            if (now - lastAppUsageStatsQueryTimeMs >= APP_USAGE_STATS_MIN_REFRESH_INTERVAL_MS) {
                lastAppUsageStatsQueryTimeMs = now
                appUsageStats(widgetContext)
            }
        }

        if (hasUsagePermission || hour != 0) {
            val usageState = when {
                hour < 2 -> "LOW"
                hour in 2..< 5 -> "MODERATE"
                hour in 5..< 8 -> "HIGH"
                else -> "EXCESSIVE"
            }
            remoteViews?.setTextViewText(R.id.tx_screenusage_state, usageState)
            remoteViews?.setTextViewText(R.id.tx_screentime, "$hour+")
        }

        val hasStepsPermission = hasFeaturePermission(widgetContext, FeaturePermission.STEPS)
        if (hasStepsPermission) {
            recognizeActivityTransitions()
        }
        val showActivityControls = hasStepsPermission && sharedPreferences.getBoolean("activitiesORcontrols", false)
        updateActivityUi(remoteViews, showActivityControls)

        val spkServiceRunning = sharedPreferences.getBoolean("SPKSERVICE", false)
        applyTimeAnnouncementState(spkServiceRunning, ColorUtil().isColorDark(primaryColor))

        if (ispDataListInitialized() && songIndex >= 0 && pDatalistSongs.size > songIndex) {
            val song = pDatalistSongs[songIndex]
            remoteViews?.setTextViewText(
                R.id.tx_music_details,
                song.title + " | " + song.album.title + " | " + song.artist.name
            )
            // Previously indexed dataListSongs after bounds-checking pDatalistSongs, which
            // could throw IndexOutOfBoundsException / UninitializedPropertyAccessException.
            val albumArtPath = song.album.cover
            if (!albumArtPath.isNullOrBlank() && isAppWidgetIdsInitialized()) {
                try {
                    Picasso.get()
                        .load(albumArtPath)
                        .into(remoteViews!!, R.id.imgbtn_albumcover, i_appWidgetIds)
                } catch (e: Exception) {
                    Log.e(TAG, "Album art load failed", e)
                    remoteViews?.setImageViewResource(R.id.imgbtn_albumcover, R.drawable.launch)
                }
            } else {
                remoteViews?.setImageViewResource(R.id.imgbtn_albumcover, R.drawable.launch)
            }

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
        seekWifiBluetoothState()
        todaysDate(widgetContext)
        loadStepsData() // Always refresh stepsData from disk to ensure persistence
        setSomeTwAndWallDescUI()

        val dayKey = LocalDate.now().dayOfWeek.name.toUpperCase()
        var durationMillisWalk = sharedPreferences.getLong(dayKey + "_walk_duration", 0L)

        val baseTimeW = sharedPreferences.getLong("walkChr", 0L)
        if (baseTimeW != 0L) {
            // Show sum of all walking streaks in a day by adding current streak to the stored total
            durationMillisWalk += (SystemClock.elapsedRealtime() - baseTimeW)
        }
        remoteViews?.setTextViewText(R.id.tx_active_walk_duration, "Active : " + formatDuration(durationMillisWalk))

        var durationMillisTravel = sharedPreferences.getLong(dayKey + "_travel_duration", 0L)
        val baseTimeT = sharedPreferences.getLong("speedChr", 0L)
        if (baseTimeT != 0L) {
            // Show sum of all walking streaks in a day by adding current streak to the stored total
            durationMillisTravel += (SystemClock.elapsedRealtime() - baseTimeT)
        }
        remoteViews?.setTextViewText(R.id.tx_active_speed_duration, "Active : " + formatDuration(durationMillisTravel))


        if (isMyServiceRunning(widgetContext, SpeedService::class.java)) {
            remoteViews?.setViewVisibility(R.id.tx_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.tx_max_speed, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.speed_chronometer, View.VISIBLE)

        }

        val waterCount = sharedPreferences.getInt("waterCountToday", 0)
        remoteViews?.setTextViewText(R.id.tx_water_count, waterCount.toString())

        updatePermissionHints(widgetContext, remoteViews!!)

        setOnClickPendingIntents(widgetContext)



    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }


    private fun updatePermissionHints(context: Context, rv: RemoteViews) {
        val showPlaceHint = !hasFeaturePermission(context, FeaturePermission.PLACE_INFO)
        rv.setViewVisibility(R.id.tx_place_permission_hint, if (showPlaceHint) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.tx_weather_permission_hint, if (showPlaceHint) View.VISIBLE else View.GONE)

        val showTimeHint = !hasFeaturePermission(context, FeaturePermission.NOTIFICATIONS)
        rv.setViewVisibility(R.id.tx_time_announcement_permission_hint, if (showTimeHint) View.VISIBLE else View.GONE)

        val showStepsHint = !hasFeaturePermission(context, FeaturePermission.STEPS)
        rv.setViewVisibility(R.id.tx_act_permission_hint, if (showStepsHint) View.VISIBLE else View.GONE)

        val showUsageHint = !hasFeaturePermission(context, FeaturePermission.USAGE_STATS)
        rv.setViewVisibility(R.id.tx_scrtime_permission_hint, if (showUsageHint) View.VISIBLE else View.GONE)
    }

    private fun updateActivityUi(rv: RemoteViews?, show: Boolean) {
        rv?.apply {
            setViewVisibility(
                R.id.imgbtn_close_activities,
                if (show) View.VISIBLE else View.INVISIBLE
            )
            setViewVisibility(R.id.btn_ui_prev, if (show) View.VISIBLE else View.INVISIBLE)
            setViewVisibility(R.id.btn_ui_next, if (show) View.VISIBLE else View.INVISIBLE)
            setViewVisibility(
                R.id.ll_activity_states,
                if (show) View.VISIBLE else View.INVISIBLE
            )

            setViewVisibility(R.id.rl_setwall, if (show) View.INVISIBLE else View.VISIBLE)
            setViewVisibility(R.id.imgbtn_qr, if (show) View.INVISIBLE else View.VISIBLE)
            setViewVisibility(
                R.id.imgbtn_g_apps,
                if (show) View.INVISIBLE else View.VISIBLE
            )
            setViewVisibility(R.id.imgbtn_lock, if (show) View.INVISIBLE else View.VISIBLE)
            setViewVisibility(
                R.id.imgbtn_speech,
                if (show) View.INVISIBLE else View.VISIBLE
            )
            setViewVisibility(R.id.tx_myspace, if (show) View.INVISIBLE else View.VISIBLE)
            setViewVisibility(R.id.imgv_conf, if (show) View.INVISIBLE else View.VISIBLE)
            setViewVisibility(R.id.imgv_ps, if (show) View.INVISIBLE else View.VISIBLE)
            setViewVisibility(R.id.imgv_dialler, if (show) View.INVISIBLE else View.VISIBLE)

            if (show) {
                val displayedAct = sharedPreferences.getString("displayedAct", presentActivityState) ?: presentActivityState
                setViewVisibility(
                    R.id.rl_still,
                    if (displayedAct == "STILL") View.VISIBLE else View.GONE
                )
                setViewVisibility(
                    R.id.rl_walking,
                    if (displayedAct == "WALKING") View.VISIBLE else View.GONE
                )
                setViewVisibility(
                    R.id.rl_speed,
                    if (displayedAct == "TRAVEL") View.VISIBLE else View.GONE
                )
            }
        }
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

    
    private fun seekWifiBluetoothState() {
        val wifiState = sharedPreferences.getBoolean("WifiState", false)
        val wifiConnectionState = sharedPreferences.getBoolean("WifiConnectionState", false)
        val blState = sharedPreferences.getBoolean("BluetoothState", false)
        val blConnectionState = sharedPreferences.getBoolean("BluetoothConnectionState", false)

        val wifiIcon = when {
            wifiState && wifiConnectionState -> R.drawable.wifi_on
            wifiState -> R.drawable.wifi_on_but_not_connected
            else -> R.drawable.wifi_off
        }
        remoteViews?.setImageViewResource(R.id.menu_wifi, wifiIcon)

        val blIcon = when {
            blState && blConnectionState -> R.drawable.blue_on
            blState -> R.drawable.blue_red
            else -> R.drawable.blue_off
        }
        remoteViews?.setImageViewResource(R.id.menu_blue, blIcon)

        val menuIcon = when {
            wifiState && !wifiConnectionState -> R.drawable.wifi_on_but_not_connected
            blState && !blConnectionState -> R.drawable.blue_red
            else -> R.drawable.more_s
        }
        remoteViews?.setImageViewResource(R.id.imgv_menu, menuIcon)
    }


    fun getInvertedColor(color: Int): Int {
        // 0x00FFFFFF represents a mask for the RGB components (ignoring alpha).
        // XORing with this value inverts the bits of the R, G, and B components.
        return color xor 0x00FFFFFF
    }

    /**
     * Keeps the shaded panels readable over any wallpaper.
     *
     * The place/weather cards and the steps, unlocks and screen-time stats panels contrast against
     * the wallpaper - light shaded plates on dark wallpapers, dark shaded plates on light ones - and
     * keep their existing 15dp glass look via
     * [R.drawable.gradient_glass_light]/[R.drawable.gradient_glass_dark].
     *
     * The controls panel is deliberately inverted: it matches the wallpaper's own tone rather than
     * contrasting with it, using the 24dp
     * [R.drawable.rounded_panel_light]/[R.drawable.rounded_panel_dark] pair. Its icon pills therefore
     * contrast against that plate ([R.drawable.rounded_corner_light] on the dark plate,
     * [R.drawable.rounded_corner_gray] on the light one), and the icons sitting on those pills are
     * re-tinted to contrast with the pill in turn.
     *
     * All labels sitting on a glass plate are inverted against that plate, otherwise the default
     * white text would wash out on the light plate that a dark wallpaper produces. The accent glyphs
     * ([R.id.tx_open_maps], [R.id.tx_refresh_weather]) follow the same rule but stay tinted, using a
     * darkened or lightened shade of [tertianaryColor] instead of plain black/white.
     *
     * [R.id.tx_time_announcement] is tinted from [tertianaryColor] too, but shifted the opposite way:
     * it sits directly on the wallpaper rather than on a plate, so it lightens over dark wallpapers.
     *
     * RemoteViews has no setBackgroundResource helper, so the setter is invoked reflectively.
     */
    private fun applyAdaptivePanelBackgrounds(isWallpaperDark: Boolean) {
        try {
            // Intentionally inverted relative to the glass panels: the controls plate follows the
            // wallpaper's own tone instead of contrasting against it.
            val controlsBackground =
                if (isWallpaperDark) R.drawable.rounded_panel_dark
                else R.drawable.rounded_panel_light

            val glassBackground =
                if (isWallpaperDark) R.drawable.gradient_glass_light
                else R.drawable.gradient_glass_dark

            // Every panel that sits directly on the wallpaper and needs a readable backdrop.
            val glassPanelIds = listOf(
                R.id.ll_place,
                R.id.id_weather,
                R.id.rl_activity_states,
                R.id.rl_unlocks,
                R.id.rl_scrtime
            )

            remoteViews?.setInt(R.id.rl_controls, "setBackgroundResource", controlsBackground)
            glassPanelIds.forEach { panelId ->
                remoteViews?.setInt(panelId, "setBackgroundResource", glassBackground)
            }

            // The controls plate matches the wallpaper tone, so its pills must contrast with the
            // plate: light pills on the dark plate, the original dark pills on the light plate.
            val pillBackground =
                if (isWallpaperDark) R.drawable.rounded_corner_light
                else R.drawable.rounded_corner_gray

            // imgbtn_g_apps takes the adaptive pill like its siblings, but is deliberately left out
            // of the tint loop below: a colour filter would flatten the Google logo's brand colours.
            listOf(
                R.id.imgbtn_g_apps,
                R.id.imgv_conf,
                R.id.imgbtn_speech,
                R.id.imgbtn_qr,
                R.id.rl_setwall,
                R.id.imgbtn_lock,
                R.id.imgv_ps,
                R.id.imgv_dialler,
                R.id.tx_myspace
            ).forEach { pillId ->
                remoteViews?.setInt(pillId, "setBackgroundResource", pillBackground)
            }

            // Icons sit on the pill, so they invert against it rather than against the wallpaper.
            val pillContentColor = if (isWallpaperDark) Color.BLACK else Color.WHITE

            listOf(
                R.id.imgv_conf,
                R.id.imgbtn_speech,
                R.id.imgbtn_qr,
                R.id.imgbtn_lock,
                R.id.imgv_ps,
                R.id.imgv_dialler,
                R.id.imgbtn_set
            ).forEach { iconId ->
                remoteViews?.setInt(iconId, "setColorFilter", pillContentColor)
            }

            remoteViews?.setTextColor(R.id.tx_myspace, pillContentColor)

            // Contrast against the glass plate, not against the wallpaper: a dark wallpaper
            // yields a light plate, which needs dark text.
            val glassTextColor = if (isWallpaperDark) Color.BLACK else Color.WHITE

            val glassLabelIds = listOf(
                R.id.tx_place,
                R.id.tx_weather,
                R.id.tx_act_state,
                R.id.tx_act_count,
                R.id.tx_act_plus,
                R.id.tx_unlocks,
                R.id.tx_screenusage_state,
                R.id.tx_screentime
            )

            glassLabelIds.forEach { textViewId ->
                remoteViews?.setTextColor(textViewId, glassTextColor)
            }

            // Accent glyphs keep the tertiary hue but shift shade so they stay visible on the
            // plate: darker shade on the light plate, lighter shade on the dark plate.
            // darkenColor preserves the source alpha, so force full opacity - a translucent
            // tertianaryColor would otherwise render the glyphs faded.
            val accentShade =
                if (isWallpaperDark) ColorUtil().darkenColor(tertianaryColor, 0.45f)
                else ColorUtil().lightenColor(tertianaryColor, 0.35f)
            val accentColor = ColorUtils.setAlphaComponent(accentShade, 255)

            listOf(R.id.tx_open_maps, R.id.tx_refresh_weather).forEach { glyphId ->
                remoteViews?.setTextColor(glyphId, accentColor)
            }

            // The time-announcement glyph sits straight on the wallpaper instead of a plate and is
            // also a service indicator, so its glyph and colour are set together by the shared
            // helper: wallpaper accent while speaking, muted grey when off.
            applyTimeAnnouncementState(
                sharedPreferences.getBoolean("SPKSERVICE", false),
                isWallpaperDark
            )
        } catch (e: Exception) {
            Log.e("wallColors", "Unable to apply adaptive panel backgrounds", e)
        }
    }

    @SuppressLint("ResourceAsColor")
      
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

                applyAdaptivePanelBackgrounds(ColorUtil().isColorDark(primaryColor))

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

                // RemoteViews enforces a strict bitmap memory budget for the entire widget tree
                // (see "RemoteViews for widget update exceeds maximum bitmap memory usage").
                // screenWidth/screenHeight are the full device screen resolution - fine for
                // actually setting the wallpaper (SetWallWorker), but far too large for a bitmap
                // attached to this 300dp-wide widget. Downscale to a widget-appropriate size
                // before creating/attaching any bitmap here, scaling the crop margins by the
                // same factor so the crop still looks correct.
                val maxWidgetImageWidth = 480
                val widgetImageScale =
                    if (screenWidth > maxWidgetImageWidth) {
                        maxWidgetImageWidth.toFloat() / screenWidth.coerceAtLeast(1)
                    } else 1f
                val widgetImgWidth = Math.round(screenWidth * widgetImageScale).coerceAtLeast(1)
                val widgetImgHeight = Math.round(screenHeight * widgetImageScale).coerceAtLeast(1)

                remoteViews?.setImageViewBitmap(
                    R.id.imgv_player,
                    createGradientBitmap(2 * widgetImgWidth, 80, primaryColor, tertianaryColor)
                )

                if (isWallBitmapInitialized(widgetContext)) {
                    val currentWallBitmap = wallBitmap

                    // The RenderScript blur pipeline below (scale + crop + blur + round, plus a
                    // second full-bitmap blur() pass for blurWallBitmap) is by far the most
                    // expensive work in this provider. setUI() -> wallColors() runs from
                    // onReceive() on *every single widget tap*, so without this cache every click
                    // paid for two RenderScript blur passes before the tap's own effect could even
                    // be applied - that synchronous cost is what made clicks feel delayed.
                    // The source bitmap only changes when SetWallWorker installs a new wallpaper,
                    // so it is a safe and cheap cache key (reference identity).
                    val cacheHit = cachedWidgetBackgroundBitmap != null &&
                            lastWallColorsSourceBitmap === currentWallBitmap &&
                            !currentWallBitmap.isRecycled

                    if (cacheHit) {
                        remoteViews?.setImageViewBitmap(R.id.imgv_widget_layout, cachedWidgetBackgroundBitmap)
                    } else if (!currentWallBitmap.isRecycled) {
                        scaledBitmap = Bitmap.createScaledBitmap(
                            currentWallBitmap,
                            widgetImgWidth,
                            widgetImgHeight,
                            true
                        )

                        if (!scaledBitmap.isRecycled) {
                            // android.R.color.black is a *resource id*, not an ARGB int; using it
                            // as a Paint color produced a garbage tint.
                            val overlayColor =
                                if (ColorUtil().isColorDark(primaryColor)) Color.BLACK else Color.WHITE

                            val cropX = Math.round(10 * widgetImageScale).coerceAtLeast(1)
                            val cropY = Math.round(25 * widgetImageScale).coerceAtLeast(1)
                            val cropW = (widgetImgWidth - cropX * 2).coerceAtLeast(1)
                            val cropH = (widgetImgHeight - Math.round(150 * widgetImageScale).coerceAtLeast(1)).coerceAtLeast(1)

                            if (cropX + cropW <= scaledBitmap.width && cropY + cropH <= scaledBitmap.height) {
                                val croppedBitmap = Bitmap.createBitmap(scaledBitmap, cropX, cropY, cropW, cropH)
                                val blurredBitmap = BitmapBlurHelper.blurBitmap(widgetContext, croppedBitmap)
                                val roundedDrawable = RoundedBitmapDrawableFactory.create(widgetContext.resources, blurredBitmap)
                                val finalBitmap = drawableToBitmap(widgetContext, roundedDrawable)

                                remoteViews?.setImageViewBitmap(
                                    R.id.imgv_widget_layout,
                                    finalBitmap
                                )

                                // Cache the result so subsequent clicks (which all funnel through
                                // setUI()) reuse it instead of re-running the blur pipeline.
                                val previousCached = cachedWidgetBackgroundBitmap
                                cachedWidgetBackgroundBitmap = finalBitmap
                                lastWallColorsSourceBitmap = currentWallBitmap
                                if (previousCached != null && previousCached != finalBitmap && !previousCached.isRecycled) {
                                    previousCached.recycle()
                                }

                                // Release intermediates: these wallpaper-sized bitmaps are the
                                // main source of OutOfMemoryError in this provider.
                                if (croppedBitmap != blurredBitmap) croppedBitmap.recycle()
                            }
                        }

                        if (!currentWallBitmap.isRecycled) {
                            blurWallBitmap = blur(widgetContext, currentWallBitmap)
                        }
                    }
                }
            } else {
                Log.d("wallColors", "NULL")
                // No wallpaper colors available - default to the light plates, which stay
                // readable on the majority of (darker) wallpapers.
                applyAdaptivePanelBackgrounds(true)
            }
        } catch (e: Exception) {
            Log.e("wallColors", "Error in wallColors", e)
        }
    }

    fun blur(context: Context?, image: Bitmap): Bitmap {

        val bitmapScale = 0.1f // Increased scale slightly for better quality/stability
        val blurRadius = 25f // Adjust blur intensity

        val width = Math.round(image.width * bitmapScale).coerceAtLeast(1)
        val height = Math.round(image.height * bitmapScale).coerceAtLeast(1)

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        var rs: RenderScript? = null
        var theIntrinsic: ScriptIntrinsicBlur? = null
        var tmpIn: Allocation? = null
        var tmpOut: Allocation? = null
        try {
            rs = RenderScript.create(context)
            theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
            tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

            theIntrinsic.setRadius(blurRadius)
            theIntrinsic.setInput(tmpIn)
            theIntrinsic.forEach(tmpOut)
            tmpOut.copyTo(outputBitmap)
        } finally {
            // These native allocations were never released, leaking on every refresh.
            tmpIn?.destroy()
            tmpOut?.destroy()
            theIntrinsic?.destroy()
            rs?.destroy()
            if (!inputBitmap.isRecycled) inputBitmap.recycle()
        }

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

      
    private fun setSomeTwAndWallDescUI() {

        if (checkCompanionVariable()) {
            remoteViews?.setTextViewText(R.id.tx_walldesc, wD)
         //   wallColors()

            // qT may be blank or start with a space; substring(0, 1) threw
            // StringIndexOutOfBoundsException in that case.
            val firstToken = qT.trim().substringBefore(" ")
            if (firstToken.isNotEmpty()) {
                val label = firstToken.replaceFirstChar { it.uppercase() }
                remoteViews?.setTextViewText(
                    R.id.tx_walltype_updateinfo,
                    Html.fromHtml(
                        "$label..,\t ||| \t$dU mins, once.\t ||| \t↺ @ $uT",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )
            }
            noRewards = sharedPreferences.getInt("noRewards", 7)

            if (noRewards > 1) {
                wallColors()
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "$noRewards")
            } else {
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



    @SuppressLint("ResourceAsColor", "ResourceType")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: $action")



        try {
            widgetContext = context.applicationContext
            ensurePrefs(widgetContext)
            newAppWidget = ComponentName(context, NewAppWidget::class.java)
            appWidM = AppWidgetManager.getInstance(context)
            i_appWidgetIds = appWidM.getAppWidgetIds(newAppWidget)
            mAppWidgetIds = i_appWidgetIds

            // Handle specific system broadcasts
            when (action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    // Starting a foreground service from a background broadcast can throw
                    // ForegroundServiceStartNotAllowedException; never let it escape.
                    try {
                        context.startForegroundService(Intent(context, StepsService::class.java))
                    } catch (e: Exception) {
                        Log.e(TAG, "startForegroundService(StepsService) failed", e)
                    }
                }

                "ACTION_UPDATE_SPEED" -> {
                    val speed = intent.getDoubleExtra("EXTRA_SPEED", 0.0)
                    speedReading = Math.round(speed).toString()
                }
            }

            // Standard AppWidgetProvider handling
            super.onReceive(context, intent)

            // For custom actions and speed updates, perform a unified UI refresh.
            // Standard actions like ACTION_APPWIDGET_UPDATE are already handled by onUpdate
            // via super.onReceive.
            if (action !in STANDARD_WIDGET_ACTIONS) {
                ensureRemoteViews(context)
                getScreenDimens()
                setUI()
                handleIntentActions(intent)
                remoteViews?.let { appWidM.updateAppWidget(newAppWidget, it) }
            }
        } catch (e: Exception) {
            // An exception escaping a BroadcastReceiver crashes the hosting process.
            Log.e(TAG, "onReceive($action) failed", e)
        }
    }

    @SuppressLint("InflateParams", "ResourceAsColor")
      
    private fun handleIntentActions(intent: Intent) {
        val action = intent.action ?: return

        when (action) {


            LOC_P_REQ -> {
                makeSnack("Location permissions granted")
                    requestFeaturePermission(widgetContext, FeaturePermission.PLACE_INFO)
            }
            NOT_P_REQ -> {
                requestFeaturePermission(widgetContext, FeaturePermission.NOTIFICATIONS)
            }
            APP_USAGE_P_REQ -> {
                requestFeaturePermission(widgetContext, FeaturePermission.USAGE_STATS)

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post(object : Runnable {
                    override fun run() {
                        if (hasFeaturePermission(widgetContext, FeaturePermission.USAGE_STATS)) {
                            val intent = Intent(widgetContext, DialogActivity::class.java).apply {
                                putExtra("DialogIntent", "screenTimeInfoWithoutDialog")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            widgetContext.startActivity(intent)
                            remoteViews?.setViewVisibility(R.id.tx_scrtime_permission_hint, View.GONE)
                        } else {
                            handler.postDelayed(this, 1000)
                        }
                    }
                })
            }
            ACT_RECOGNITION_P_REQ -> {
                sharedPreferencesEditor.putBoolean("activitiesORcontrols", true).apply()
                requestFeaturePermission(widgetContext, FeaturePermission.STEPS)
            }
            C_CLICKED -> {
                val intentContacts = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                intentContacts.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                widgetContext.startActivity(intentContacts)
            }
            DIAL_CLICK -> {
                val intentDial = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    widgetContext.startActivity(intentDial)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open dialer", e)
                }
            }
            ACTINFO_CLICK -> {
                if (hasFeaturePermission(widgetContext, FeaturePermission.STEPS)) {
                    recognizeActivityTransitions()
                    val current = sharedPreferences.getBoolean("activitiesORcontrols", false)
                    val show = !current
                    sharedPreferencesEditor.putBoolean("activitiesORcontrols", show).apply()
                    updateActivityUi(remoteViews, show)
                } else {
                    sharedPreferencesEditor.putBoolean("activitiesORcontrols", true).apply()
                    requestFeaturePermission(widgetContext, FeaturePermission.STEPS)
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
                updateActivityUi(remoteViews, false)
            }
            ASSISTIVE_TOUCH -> {
                val current = sharedPreferences.getBoolean("rlControls", false)
                sharedPreferencesEditor.putBoolean("rlControls", !current).apply()
                val show = !current

                if (show) {
                    sharedPreferencesEditor.putBoolean("activitiesORcontrols", false).apply()
                    updateActivityUi(remoteViews, false)
                } else {
                    val hasSteps = hasFeaturePermission(widgetContext, FeaturePermission.STEPS)
                    if (hasSteps) {
                        sharedPreferencesEditor.putBoolean("activitiesORcontrols", true).apply()
                        updateActivityUi(remoteViews, true)
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
                // Without ACTIVITY_RECOGNITION the count is always 0, so ask for it
                // here – at the moment the user shows interest in the steps tile.
                if (!hasFeaturePermission(widgetContext, FeaturePermission.STEPS)) {
                    requestFeaturePermission(widgetContext, FeaturePermission.STEPS)
                    return
                }
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
                    if (isSharedPreferencesInitialized()) {
                        sharedPreferencesEditor.putInt("maxSpeedToday", 0).apply()
                    }
                    try {
                        // Throws ForegroundServiceStartNotAllowedException on Android 12+ when
                        // the app is not allowed to start a foreground service from background.
                        widgetContext.startForegroundService(Intent(widgetContext, SpeedService::class.java))
                    } catch (e: Exception) {
                        Log.e(TAG, "startForegroundService(SpeedService) failed", e)
                        // Roll the UI back so it does not claim a trip is being tracked.
                        remoteViews?.setChronometer(R.id.speed_chronometer, 0L, null, false)
                        remoteViews?.setViewVisibility(R.id.tx_speed, View.INVISIBLE)
                        remoteViews?.setViewVisibility(R.id.tx_max_speed, View.INVISIBLE)
                        remoteViews?.setViewVisibility(R.id.speed_chronometer, View.INVISIBLE)
                    }
                }
            }
            GET_WEATHER -> {
                // cityLat/cityLng are only meaningful with location access, otherwise the
                // spinner would spin forever – ask for the permission instead.
                if (!hasFeaturePermission(widgetContext, FeaturePermission.PLACE_INFO)) {
                    requestFeaturePermission(widgetContext, FeaturePermission.PLACE_INFO)
                    return
                }
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic_weather, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.tx_refresh_weather, View.INVISIBLE)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                StepsService.getWeatherData(LatLng(cityLat, cityLng))
            }
            PLACE_CLICK -> {
                 if (!isLocationEnabled(widgetContext)) {
                    val locIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    widgetContext.startActivity(locIntent)
                } else {
                    if (cityname.isNotBlank())
                        remoteViews?.setTextViewText(R.id.tx_place, cityname)
               //     else remoteViews?.setTextViewText(R.id.tx_place, "fetching..,")
                }
            }
            SCRTIME_CLICK -> {
                if (!hasFeaturePermission(widgetContext, FeaturePermission.USAGE_STATS)) {
                    requestFeaturePermission(widgetContext, FeaturePermission.USAGE_STATS)
                } else {
                    val intent = Intent(widgetContext, DialogActivity::class.java).apply {
                        putExtra("DialogIntent", "screenTimeInfo")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    widgetContext.startActivity(intent)
                }
            }
            WEATHER_CLICK -> {
                if (!hasFeaturePermission(widgetContext, FeaturePermission.PLACE_INFO)) {
                    requestFeaturePermission(widgetContext, FeaturePermission.PLACE_INFO)
                } else if (!isLocationEnabled(widgetContext)) {
                    val locIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    widgetContext.startActivity(locIntent)
                } else {
                    makeToast(widgetContext, "Weather: $tempC $tempKind")
                }
            }
            PLAYPAUSE_CLICK -> {
                if (boolMusicServiceRunning) {
                    try {
                        mMediaPlayer?.let {
                            if (it.isPlaying) {
                                it.pause()
                                remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.play_m)
                            } else {
                                // Previously also launched MusicActivity here, which resumed the
                                // existing player *and* started a second playback session.
                                it.play()
                                remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.pause_m)
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
            FAB_SHARE -> {
                val inflater = widgetContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
                if (inflater != null) {
                    val appWidgetView = inflater.inflate(R.layout.new_app_widget, null)
                    val measureW = screenWidth.coerceAtLeast(1)
                    val measureH = (screenHeight - 725).coerceAtLeast(1)
                    appWidgetView.measure(
                        View.MeasureSpec.makeMeasureSpec(measureW, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(measureH, View.MeasureSpec.EXACTLY)
                    )
                    appWidgetView.layout(0, 0, appWidgetView.measuredWidth, appWidgetView.measuredHeight)

                    // view.width/height are 0 for a detached, manually measured view, which made
                    // Bitmap.createBitmap() throw "width and height must be > 0".
                    val bmpW = appWidgetView.measuredWidth.coerceAtLeast(1)
                    val bmpH = appWidgetView.measuredHeight.coerceAtLeast(1)
                    var bitmapWidget = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    appWidgetView.draw(Canvas(bitmapWidget))
                    bitmapWidget = Bitmap.createScaledBitmap(
                        bitmapWidget,
                        Math.round(bmpW * 0.5f).coerceAtLeast(1),
                        Math.round(bmpH * 0.5f).coerceAtLeast(1),
                        true
                    )
                    shareBitmap(bitmapWidget)
                }
            }
            WIFI_AUTO -> {
                widgetContext.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            TORCH_STATE -> {
                if (widgetContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    val cameraManager = widgetContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    try {
                        // cameraIdList[0] threw ArrayIndexOutOfBoundsException on devices that
                        // report the flash feature but expose no camera ids.
                        val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                        if (cameraId != null) {
                            val isTorchOn = sharedPreferences.getBoolean("Torch", false)
                            cameraManager.setTorchMode(cameraId, !isTorchOn)
                            remoteViews?.setImageViewResource(R.id.menu_torch, if (isTorchOn) R.drawable.torch_off else R.drawable.torch_on)
                            sharedPreferencesEditor.putBoolean("Torch", !isTorchOn).apply()
                        }
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
                    // Without NEW_TASK this throws AndroidRuntimeException from a receiver context.
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    widgetContext.startActivity(it)
                }
            }
            A_CLICKED -> {
                widgetContext.startActivity(Intent(widgetContext, AppsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PS_CLICK -> {
                widgetContext.packageManager.getLaunchIntentForPackage("com.android.vending")?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    widgetContext.startActivity(it)
                }
            }
            Time_A_CLICKED -> {
                if (!hasFeaturePermission(widgetContext, FeaturePermission.NOTIFICATIONS)) {
                    requestFeaturePermission(widgetContext, FeaturePermission.NOTIFICATIONS)
                    return
                } else {
                    val current = sharedPreferences.getBoolean("SPKSERVICE", false)
                    val speakIntent = Intent(widgetContext, SpeakService::class.java)
                    if (!current) {
                        widgetContext.startService(speakIntent)
                        applyTimeAnnouncementState(true, ColorUtil().isColorDark(primaryColor))
                        sharedPreferencesEditor.putBoolean("SPKSERVICE", true).apply()
                        makeToast(
                            widgetContext,
                            "Incoming notifications and hour changes will be read out loud."
                        )
                    } else {
                        widgetContext.stopService(speakIntent)
                        applyTimeAnnouncementState(false, ColorUtil().isColorDark(primaryColor))
                        sharedPreferencesEditor.putBoolean("SPKSERVICE", false).apply()
                    }
                }
            }
            ADD_TODO_CLICK -> makeToast(widgetContext, "Add Todo Clicked!")
            WATER_REMINDER_CLICK -> {
                val waterCount = sharedPreferences.getInt("waterCountToday", 0) + 1
                sharedPreferencesEditor.putInt("waterCountToday", waterCount).apply()
                remoteViews?.setTextViewText(R.id.tx_water_count, waterCount.toString())
                if (presentActivityState == "STILL")
                remoteViews?.setTextViewText(R.id.tx_act_count, waterCount.toString() + "\uD800\uDCEF")
            }
            MENU_CLICK -> makeToast(widgetContext, "hi")
            APP1_CLICK -> if (Top3.size > 0) launchApp(widgetContext, Top3[0].pName)
            APP2_CLICK -> if (Top3.size > 1) launchApp(widgetContext, Top3[1].pName)
            APP3_CLICK -> if (Top3.size > 2) launchApp(widgetContext, Top3[2].pName)
        }
    }





    private fun startMusicActivity(songIndex: Int) {
        val intentMusic = Intent(widgetContext, MusicActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("songIndex", songIndex)
        try {
            widgetContext.startActivity(intentMusic)
        } catch (e: Exception) {
            Log.e(TAG, "startMusicActivity failed", e)
        }
    }







    @SuppressLint("ResourceAsColor")
      
    fun getPreciseEnergyCounter(context: Context) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return

        // getLongProperty() returns Integer.MIN_VALUE for unsupported properties, which produced
        // a nonsensical label and a negative ProgressBar value.
        val raw = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val energy = if (raw in 0..100) raw.toInt() else 0

        remoteViews?.setTextViewText(R.id.tx_battery, energy.toString())
        remoteViews?.setProgressBar(R.id.progressBar_battery, 100, energy, false)

        val greenColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_green_light))
        val redColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_red_light))
        val amberColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_orange_light))

        if (energy > 70) {
            setColorStateList(greenColor)
            remoteViews?.setTextColor(
                R.id.tx_battery,
                ContextCompat.getColor(context, android.R.color.holo_green_dark)
            )
        } else if (energy < 30) {
            setColorStateList(redColor)
            remoteViews?.setTextColor(
                R.id.tx_battery,
                ContextCompat.getColor(context, android.R.color.holo_red_dark)
            )
        } else {
            setColorStateList(amberColor)
            remoteViews?.setTextColor(
                R.id.tx_battery,
                ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            )
        }
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
        // The unchecked `as WifiManager` cast threw instead of producing null, making the
        // former null-check dead code.
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled == true
    }



    private fun shareBitmap(bitmapWidget: Bitmap) {

        val cachePath = File(widgetContext.cacheDir, "images")
        if (!cachePath.exists() && !cachePath.mkdirs()) {
            showException("Unable to create cache directory for sharing")
            return
        }
        val imageFile = File(cachePath, "image_to_share.png")

        try {
            // use {} guarantees the stream is closed even when compress() throws.
            FileOutputStream(imageFile).use { outputStream ->
                bitmapWidget.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
        } catch (ex: Exception) {
            showException(ex.message.toString())
            return  // Handle the error appropriately
        }

        try {
            // getUriForFile() throws IllegalArgumentException if the file is outside the
            // paths declared for the provider, and the chooser can fail on its own.
            val contentUri = FileProvider.getUriForFile(
                widgetContext,
                widgetContext.packageName + ".fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
                .setType("image/*")
                .putExtra(Intent.EXTRA_STREAM, contentUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temporary read permission

            widgetContext.startActivity(
                Intent.createChooser(shareIntent, "Share Image Using")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (ex: Exception) {
            showException(ex.message.toString())
        }
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
        if (phoneNumber.isBlank()) return

        // ACTION_CALL throws SecurityException without the CALL_PHONE runtime permission;
        // ACTION_DIAL needs none, so use it as the fallback.
        val canCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL).apply {
            data = Uri.fromParts("tel", phoneNumber, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "dialPhoneNumber failed", e)
        }
    }


    /**
     * True when every runtime permission backing [feature] is granted.
     * An empty group (permission not applicable on this API level) counts as granted.
     */
    private fun hasFeaturePermission(context: Context, feature: FeaturePermission): Boolean {
        return when (feature) {
            FeaturePermission.USAGE_STATS -> UsageStatsChecker().hasUsageStatsPermission(context)
            FeaturePermission.NOTIFICATIONS -> isNotificationListenerPermissionGranted(context)
            else -> feature.permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun isNotificationListenerPermissionGranted(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        val componentName = ComponentName(context, NotificationService::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) ?: false
    }

    /**
     * A widget cannot request runtime permissions itself, so bounce through
     * MainActivity, which shows the rationale and the system dialog for just
     * this one feature.
     */
    private fun requestFeaturePermission(context: Context, feature: FeaturePermission) {
        try {
            context.startActivity(
                Intent(context, DialogActivity::class.java)
                    .putExtra("requestFeature", feature.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // Must never escape a BroadcastReceiver – that kills the widget host.
            Log.e(TAG, "requestFeaturePermission(${feature.name}) failed", e)
        }
    }


    private fun launchApp(context: Context, pkgName: String) {
        // getLaunchIntentForPackage() returns null for packages with no launcher activity,
        // so the previous !! threw NullPointerException.
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkgName)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for $pkgName")
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "launchApp($pkgName) failed", e)
        }
    }


    private fun readApps() {
        if (!isSharedPreferencesInitialized()) return

        val response: String = sharedPreferences.getString("MUA", "").orEmpty()
        if (response.isNotEmpty()) {
            try {
                // fromJson() throws JsonSyntaxException on corrupt prefs payloads; the old
                // List<App?> token also allowed null elements that NPE'd downstream.
                val parsed: ArrayList<App>? = Gson().fromJson(
                    response,
                    object : TypeToken<ArrayList<App>>() {}.type
                )
                if (parsed != null) {
                    choosenApps = ArrayList(parsed.filterNotNull())
                }
            } catch (ex: Exception) {
                Log.e(TAG, "readApps: unable to parse stored apps", ex)
            }
        }

        sortApps(choosenApps)

        appIndex = 0

    }


    private fun sortApps(apps: List<App>) {
        try {
            // Collections.sort() throws UnsupportedOperationException on immutable lists and
            // ConcurrentModificationException if another thread mutates choosenApps.
            Collections.sort(apps) { p0, p1 -> p1.usage.compareTo(p0.usage) }
        } catch (ex: Exception) {
            Log.e(TAG, "sortApps failed", ex)
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

        private var unlockReceiver: BroadcastReceiver? = null
        var penNote: String = ""
        lateinit var blurWallBitmap: Bitmap
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

        /** True when [i_appWidgetIds] has been assigned and holds at least one widget id. */
        fun isAppWidgetIdsInitialized(): Boolean =
            ::i_appWidgetIds.isInitialized && i_appWidgetIds.isNotEmpty()

        var selectedApps: ArrayList<SelectedApp> = ArrayList()
        lateinit var selectedApp: Bitmap

        // These must hold resolved ARGB ints, never R.color.* resource ids: every consumer
        // (ColorUtil shading, createGradientBitmap, RemoteViews.setTextColor) reads them as colors.
        // Defaults mirror light_blue_900 / bg_light / bg_dark until wallColors() overwrites them.
        var primaryColor = Color.parseColor("#FF01579B")
        var secondaryColor = Color.parseColor("#75FFFFFF")
        var tertianaryColor = Color.parseColor("#75000000")


        var favContacts: ArrayList<Contact> = ArrayList()
        var onEn: Boolean = false
        var remoteViews: RemoteViews? = null
        var lapCount: Int = 0

        // Cache for the expensive scale/crop/blur/round pipeline in wallColors() that produces
        // the imgv_widget_layout background. Without this, every widget tap (which redraws via
        // setUI() -> wallColors()) re-ran two RenderScript blur passes synchronously on the main
        // thread, which is what made onClick feedback feel delayed. Both fields are invalidated
        // together whenever the source wallBitmap reference changes (i.e. a new wallpaper is set).
        private var cachedWidgetBackgroundBitmap: Bitmap? = null
        private var lastWallColorsSourceBitmap: Bitmap? = null

        /** Minimum time between appUsageStats() calls triggered from setUI() on every click. */
        private const val APP_USAGE_STATS_MIN_REFRESH_INTERVAL_MS = 60_000L

        /**
         * True once [recognizeActivityTransitions] has successfully registered with
         * ActivityRecognition. Reset in [onDisabled] so re-enabling the widget re-registers.
         */
        private var activityTransitionsRegistered: Boolean = false


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

            timeOfDay = when {
                currentHour < 6 -> "Night!"
                currentHour < 12 -> "Morni!"
                currentHour < 17 -> "Noon!"
                currentHour < 21 -> "Eve!"
                else -> "Night!"
            }

            timelyWish = timeOfDay

            // The Profile query throws SecurityException without READ_CONTACTS, and the old
            // `c!!.close()` threw NPE whenever query() returned null.
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    context.contentResolver.query(
                        ContactsContract.Profile.CONTENT_URI, null, null, null, null
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIndex = c.getColumnIndex("display_name")
                            // getColumnIndex() returns -1 when the column is absent.
                            if (nameIndex != -1) {
                                gpName = c.getString(nameIndex).orEmpty()
                            }
                        }
                    }
                } catch (ex: Exception) {
                    showException(ex.message.toString())
                }
            } //else DialogActivity().requestFeaturePermission(FeaturePermission.CONTACTS)

            Log.d("gpName - ", gpName)

            timelyWish = when (timeOfDay) {
                "Morni!" -> "\uD83C\uDF3B"//, ${gpName.split(" ").get(0)}!"
                "Noon!" -> "☀\uFE0F"//, ${gpName.split(" ").get(0)}!"
                "Eve!" -> "\uD83C\uDF41"//, ${gpName.split(" ").get(0)}!"
                "Night!" -> "\uD83D\uDCA4"//, ${gpName.split(" ").get(0)}!"
                else -> timelyWish
            }

        }

        private fun showException(exp: String) {
            remoteViews?.setTextViewText(R.id.tx_runner, exp)
        }


        fun loadStepsData() {
            stepsData.clear()
            val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            val currentDay = LocalDate.now().dayOfWeek.name
            // sharedPreferences is lateinit and may not be set yet when a service calls this
            // after a process restart, which threw UninitializedPropertyAccessException.
            val prefsReady = isSharedPreferencesInitialized()
            for (dayKey in days) {
                val count = when {
                    dayKey == currentDay -> stepsToday
                    prefsReady -> sharedPreferences.getInt(dayKey, 0)
                    else -> 0
                }
                stepsData.add(count.toString())
            }
        }


        @SuppressLint("ResourceAsColor")
        fun todaysDate(context: Context) {

            val c: Date = Calendar.getInstance().time
            val dfDate = SimpleDateFormat("d", Locale.getDefault())
            val dfMonth = SimpleDateFormat("MMM", Locale.getDefault())

            val dayOfMonthText = dfDate.format(c)
            val dayOfMonth = dayOfMonthText.trim().toIntOrNull() ?: 0

            // The old length-based branches left day 10 (and any unmatched value) with an
            // empty suffix. A single when over the whole range covers every day.
            val postFixDate = when (dayOfMonth) {
                1, 21, 31 -> "ˢᵗ"
                2, 22 -> "ⁿᵈ"
                3, 23 -> "ʳᵈ"
                else -> "ᵗʰ"
            }

            val now = LocalDate.now()
            val dayName = now.dayOfWeek.name
            val todayFormatted = dayOfMonthText + postFixDate + " " + dfMonth.format(c)

            if (!::formattedDate.isInitialized) {
                formattedDate = todayFormatted
                loadStepsData()
            } else if (formattedDate != todayFormatted) {

                for (i in arrayListHabits)
                    i.isChecked = false

                if (isadapterHabitsInitialized()) adapterHabits.notifyDataSetChanged()
                // Midnight transition detected
                appUsageStats(context)

                // The rollover below writes to the lateinit editor; bail out if prefs are
                // not ready yet instead of throwing UninitializedPropertyAccessException.
                if (!isSharedPreferencesInitialized()) {
                    formattedDate = todayFormatted
                    loadStepsData()
                    return
                }

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
                    // These putInt() calls were never committed because apply() was missing.
                    days.forEach { sharedPreferencesEditor.putInt(it, 0) }
                    sharedPreferencesEditor.apply()

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

                formattedDate = todayFormatted

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


        /**
         * Single source of truth for the speech-service indicator: keeps the glyph and its colour
         * in sync so the on/off state stays readable at a glance.
         *
         * Active uses the wallpaper's tertiary accent, shaded to contrast with the wallpaper it sits
         * on. Inactive falls back to a muted grey, so the state is not conveyed by shape alone.
         */
        fun applyTimeAnnouncementState(isSpeaking: Boolean, isWallpaperDark: Boolean) {
            try {
                val glyph = if (isSpeaking) "\uD83D\uDDE3" else "⊘"

                val color = if (isSpeaking) {
                    val accentShade =
                        if (isWallpaperDark) ColorUtil().lightenColor(tertianaryColor, 0.35f)
                        else ColorUtil().darkenColor(tertianaryColor, 0.45f)
                    ColorUtils.setAlphaComponent(accentShade, 255)
                } else {
                    if (isWallpaperDark) Color.LTGRAY else Color.DKGRAY
                }

                remoteViews?.setTextViewText(R.id.tx_time_announcement, glyph)
                remoteViews?.setTextColor(R.id.tx_time_announcement, color)
            } catch (e: Exception) {
                Log.e("NewAppWidget", "Unable to apply time announcement state", e)
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

        /** Framework actions already handled by super.onReceive() -> onUpdate()/onDeleted() etc. */
        private val STANDARD_WIDGET_ACTIONS = setOf(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_DELETED,
            AppWidgetManager.ACTION_APPWIDGET_DISABLED,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED
        )

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
        private const val PLACE_CLICK = "placeClick"
        private const val WEATHER_CLICK = "weatherClick"
        private const val SCRTIME_CLICK = "scrtimeClick"
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

        private const val LOC_P_REQ = "locPrequest"
        private const val NOT_P_REQ = "notPrequest"
        private const val APP_USAGE_P_REQ = "appUsagePrequest"
        private const val ACT_RECOGNITION_P_REQ = "actRecognitionPrequest"

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

        private const val APP1_CLICK = "app1_click"
        private const val APP2_CLICK = "app2_click"
        private const val APP3_CLICK = "app3_click"

    }


}
