package com.belaku.homey


// Weather Key - 9fa8e101240ab18615e3133b051e767e

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Html
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.AdapterView
import android.widget.AnalogClock
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.RECEIVER_NOT_EXPORTED
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.MainActivity.Companion.BitmapRotated
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.beginCal
import com.belaku.homey.MainActivity.Companion.cityLat
import com.belaku.homey.MainActivity.Companion.cityLng
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.endCal
import com.belaku.homey.MainActivity.Companion.mBluetoothAdapter
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.tempC
import com.belaku.homey.MainActivity.Companion.tempKind
import com.belaku.homey.MainActivity.Companion.weatherIconID
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.MusicActivity.Companion.pDatalistSongs
import com.belaku.homey.MusicService.Companion.boolMusicServiceRunning
import com.belaku.homey.MusicService.Companion.mMediaPlayer
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.RemindersActivity.Companion.adapterHabits
import com.belaku.homey.RemindersActivity.Companion.arrayListHabits
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.dayChange
import com.belaku.homey.SetWallWorker.Companion.dayIndex
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
import com.belaku.homey.SetWallWorker.Companion.stepsToday
import com.belaku.homey.SetWallWorker.Companion.wallBitmap
import com.belaku.homey.StepsService.Companion.choosenApps
import com.belaku.homey.StepsService.Companion.presentActivityState
import com.belaku.homey.StepsService.Companion.totalUsage
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Date
import java.util.Locale


class NewAppWidget : AppWidgetProvider() {


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

        makeToast("Expand the widget to full screen dimens for better visibility")
        sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        if (ismActInitialized())
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mAct)

        recognizeActivityTransitions()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private fun recognizeActivityTransitions() {

        makeToast("!recognizeActivityTransitions")
        val receiver = ActivityTransitionReceiver()
        val filter = IntentFilter("com.belaku.homey.CUSTOM_ACTION") // Use a unique action string
        widgetContext.registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)

        val intent = Intent(widgetContext, ActivityTransitionReceiver::class.java)
        val requestCodeAT = 57
        val pendingIntent = PendingIntent.getBroadcast(
            widgetContext,
            requestCodeAT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val transitions = ArrayList<ActivityTransition>()
        transitions.apply {
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

        val transitionRequest = ActivityTransitionRequest(transitions)

        // myPendingIntent is the instance of PendingIntent where the app receives callbacks.
        val task = ActivityRecognition.getClient(widgetContext)
            .requestActivityTransitionUpdates(transitionRequest, pendingIntent)

        task.addOnSuccessListener {
            // Handle success
            //    makeToast("Task added successfully")
        }

        task.addOnFailureListener {
            // Handle error
            //    makeToast("Error adding task")
        }

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

        getPreciseEnergyCounter(widgetContext)

        i_appWidgetIds = appWidgetIds

        for (appWidgetId in appWidgetIds) {

            widgetContext = context

            recognizeActivityTransitions()
            setUI()
            readApps()
            getFavoriteContacts()
            setACAdapter()


            //  Create an intent to launch MainActivity

            setOnClickPendingIntents(context)



            appWidM = AppWidgetManager.getInstance(context)
            appWidM.updateAppWidget(appWidgetId, remoteViews)
        }



        super.onUpdate(context, appWidgetManager, appWidgetIds)

    }


    private fun setOnClickPendingIntents(context: Context) {
        // Create a PendingIntent


        remoteViews?.setOnClickPendingIntent(
            R.id.tx_nextstate,
            getPendingSelfIntent(context, NEXT_STATE)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_prevstate,
            getPendingSelfIntent(context, PREV_STATE)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.fl_speed,
            getPendingSelfIntent(context, SPEED_INFO)
        )

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
            R.id.imgv_p_album,
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
            R.id.clock,
            getPendingSelfIntent(context, TIME_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.a_clock,
            getPendingSelfIntent(context, TIME_CLICKED)
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
            R.id.imgv_steps, PendingIntent.getActivity(
                context, 15,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "stepsInfo"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )
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

            remoteViews?.setTextViewText(R.id.tx_place, cityname)
            remoteViews?.setTextViewText(R.id.tx_weather, tempC.split(".")[0] + "°C, " + tempKind)
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


    @RequiresApi(Build.VERSION_CODES.S)
    private fun setUI() {

        val spkServiceRunning = sharedPreferences.getBoolean("SPKSERVICE", false)
        //   makeToast("spkServiceRunning : $spkServiceRunning")
        if (spkServiceRunning)
            remoteViews?.setTextViewText(R.id.tx_time_announcement, "\uD83D\uDDE3")
        else remoteViews?.setTextViewText(R.id.tx_time_announcement, "⊘")

        if (isDataListInitialized() && pDatalistSongs.size > songIndex) {
            remoteViews?.setTextViewText(
                R.id.tx_music_details,
                pDatalistSongs[songIndex].title + " | " + pDatalistSongs[songIndex].album.title + " | " + pDatalistSongs[songIndex].artist.name
            )

            mMediaPlayer?.let {
                if (it.isPlaying)
                    remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.pause_m)
                else remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.play_m)
            }

        }

        remoteViews?.setTextViewText(R.id.tx_breathe_count, sharedPreferences.getInt("breatheCount", 0).toString())
        remoteViews?.setTextViewText(R.id.tx_drink_count, sharedPreferences.getInt("drinkCount", 0).toString())

        //    googleAccountInfo()
        if (isPinNoteInitialized())
            remoteViews?.setTextViewText(R.id.tx_runner, pinNote)

        seekWifiState()
        seekBluetoothState()
        getScreenTime(widgetContext)
        todaysDate()
        locationTxUpdate(widgetContext)
        wallColors()
        //   getWeatherDraws()
        setSomeTwAndWallDescUI()


    }

    /*   private fun googleAccountInfo() {

           val accountManager = AccountManager.get(widgetContext)

           // To get all Google accounts
           val googleAccounts = accountManager.getAccountsByType("com.google")
           // To get all accounts of any type
           val allAccounts = accountManager.accounts
           for (account in googleAccounts) {
               val accountName = account.name
            //   makeToast(accountName)
           }

       }*/


    private fun glossyOverlay(
        originalBitmap: Bitmap
    ): Bitmap {
        val resultBitmap =
            originalBitmap.copy(Bitmap.Config.ARGB_8888, true) // Must be ARGB_8888 and mutable
        val canvas = Canvas(resultBitmap)


        // Define colors: Top (semi-transparent white), Middle (more transparent), Bottom (fully transparent)
        val colors = intArrayOf(
            Color.parseColor("#99FFFFFF"),  // Top: ~60% white transparency
            Color.parseColor("#44FFFFFF"),  // Middle: ~25% white transparency
            Color.TRANSPARENT // Bottom: fully transparent
        )


// Define positions for the colors (optional, can be null for even distribution)
        val positions = floatArrayOf(0.0f, 0.5f, 1.0f)

        val gradient = LinearGradient(
            0f, 0f, 0f, canvas.height.toFloat(), // Start X, Y; End X, Y (vertical gradient)
            colors,
            positions,
            Shader.TileMode.CLAMP
        )

        val paint = Paint()
        paint.setShader(gradient)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)

        return resultBitmap

    }

    /*   private fun blurBitmap(originalBitmap: Bitmap) : Bitmap {

           val rs = RenderScript.create(widgetContext)

           val input = Allocation.createFromBitmap(
               rs,
               originalBitmap
           ) //use this constructor for best performance, because it uses USAGE_SHARED mode which reuses memory
           val output = Allocation.createTyped(rs, input.type)
           val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
           script.setRadius(8f);
           script.setInput(input);
           script.forEach(output);
           output.copyTo(originalBitmap);

           return originalBitmap

       }*/

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

            //  findViewById<View>(R.id.myLayout).background = gradientDrawable


            if (ColorUtil().isColorDark(primaryColor)) {

                makeToast("Dark")

                remoteViews?.setImageViewBitmap(R.id.imgv_rl_controls, drawableToBitmap(widgetContext,
                    widgetContext.getDrawable(R.drawable.gradient_glass_list)!!
                ))


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
                }

                remoteViews?.setInt(
                    R.id.tx_myspace,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.imgbtn_lock,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.imgbtn_qr,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.rl_setwall,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.imgv_conf,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )

                remoteViews?.setInt(
                    R.id.imgv_ps,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.imgbtn_g_apps,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.imgbtn_speech,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.imgv_dialler,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )


                remoteViews?.setColorInt(
                    R.id.imgv_ps,
                    "setColorFilter",
                    Color.WHITE,
                    Color.WHITE
                )

                remoteViews?.setColorInt(
                    R.id.imgbtn_speech,
                    "setColorFilter",
                    Color.WHITE,
                    Color.WHITE
                )
                remoteViews?.setColorInt(
                    R.id.imgv_dialler,
                    "setColorFilter",
                    Color.WHITE,
                    Color.WHITE
                )

                remoteViews?.setTextColor(
                    R.id.clock,
                    widgetContext.resources.getColor(android.R.color.holo_red_light)
                )

            } else {

                makeToast("Light")

                remoteViews?.setImageViewBitmap(R.id.imgv_rl_controls, drawableToBitmap(widgetContext,
                    widgetContext.getDrawable(R.drawable.gradient_glass_dark)!!
                ))

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
                }

                remoteViews?.setInt(
                    R.id.tx_myspace,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.imgbtn_lock,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.imgbtn_qr,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.rl_setwall,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )
                remoteViews?.setInt(
                    R.id.imgv_conf,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_dark
                )

                remoteViews?.setInt(
                    R.id.imgv_ps,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.imgbtn_g_apps,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.imgbtn_speech,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )
                remoteViews?.setInt(
                    R.id.imgv_dialler,
                    "setBackgroundResource",
                    R.drawable.gradient_glass_light
                )


                remoteViews?.setColorInt(
                    R.id.imgv_ps,
                    "setColorFilter",
                    Color.BLACK,
                    Color.BLACK
                )

                remoteViews?.setColorInt(
                    R.id.imgbtn_speech,
                    "setColorFilter",
                    Color.BLACK,
                    Color.BLACK
                )
                remoteViews?.setColorInt(
                    R.id.imgv_dialler,
                    "setColorFilter",
                    Color.BLACK,
                    Color.BLACK
                )

                remoteViews?.setTextColor(
                    R.id.clock,
                    widgetContext.resources.getColor(android.R.color.holo_red_dark)
                )

            }

        } else Log.d("wallColors", "NULL")


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
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "\uD83D\uDC41\uFE0FAD!")
                remoteViews?.setOnClickPendingIntent(
                    R.id.imgbtn_set, PendingIntent.getActivity(
                        widgetContext,
                        18,
                        Intent(widgetContext, DialogActivity::class.java).putExtra(
                            "DialogIntent",
                            "AD"
                        ),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
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

        widgetContext = context

        Log.d(TAG, "!onReceive")
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
        newAppWidget = ComponentName(context, NewAppWidget::class.java)
        sharedPreferences = widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        //    setUI()
        handleIntentActions(intent)

        appWidM = AppWidgetManager.getInstance(widgetContext)
        appWidM.updateAppWidget(newAppWidget, remoteViews)

    }


    @SuppressLint("InflateParams", "ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleIntentActions(intent: Intent) {

        if (BATTERY_INFO == intent.action) {
            val powerUsageIntent = Intent("android.intent.action.POWER_USAGE_SUMMARY")
            if (powerUsageIntent.resolveActivity(widgetContext.getPackageManager()) != null) {
                powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                widgetContext.startActivity(powerUsageIntent)
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
                    makeToast(choosenApps[position].name)
                    val launchIntent: Intent =
                        widgetContext.packageManager.getLaunchIntentForPackage(
                            choosenApps[position].pName
                        )!!

                    // Optional: Add flags for desired behavior (e.g., to ensure a new task is created)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    widgetContext.startActivity(launchIntent)
                } else if (viewID == 1)
                    makeToast("Remove App - ${apps[position].name}")
            } else makeToast("INvalid Pos - $position")
        } else if (FAB_SHARE == intent.action) {

            val inflater =
                widgetContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val appWidgetView: View = inflater.inflate(R.layout.new_app_widget, null)

            makeToast("Yet2IMPL")
            //     loadWidgetToShare(appWidgetView)
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
                //  return
            }
            val cameraManager =
                widgetContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var cameraId: String? = null
            try {
                cameraId = cameraManager.cameraIdList[0] // Typically the back camera
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                //   return
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
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }


        } else if (STEPS_NOW == intent.action) {
            boolNewLap = !boolNewLap

            sharedPreferencesEditor.putBoolean("newLap", boolNewLap).apply()

        } else if (BREATHE_INC == intent.action) {
            makeToast("!BREATHE_INC")
            var bC = sharedPreferences.getInt("breatheCount", 0)
            bC++
            sharedPreferencesEditor.putInt("breatheCount", bC).apply()
            sharedPreferencesEditor.commit()
            makeToast("setting $bC")
            remoteViews?.setTextViewText(R.id.tx_breathe_count, bC.toString())
        } else if (DRINK_INC == intent.action) {
            makeToast("!DRINK_INC")
            var dC = sharedPreferences.getInt("drinkCount", 0)
            dC++
            sharedPreferencesEditor.putInt("drinkCount", dC).apply()
            sharedPreferencesEditor.commit()
            remoteViews?.setTextViewText(R.id.tx_drink_count, dC.toString())
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
        } else if (NEXT_STATE == intent.action) {

            if (sharedPreferences.getString("actState", "STILL") == "STILL") {
                sharedPreferencesEditor.putString("actState", "WALKING").commit()
                remoteViews?.setTextViewText(R.id.tx_activity_state, "WALKING")
                remoteViews?.setViewVisibility(R.id.rl_still_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking_state, View.VISIBLE)
                remoteViews?.setTextViewText(R.id.tx_stepstoday, "Steps ~ $stepsToday")
                remoteViews?.setTextViewText(R.id.tx_steps_km_today, "Distance ~ " + String.format("%.1f",  stepsToday * 74f / 100000f) + " Km")
                remoteViews?.setViewVisibility(R.id.fl_speed, View.INVISIBLE)
            } else if (sharedPreferences.getString("actState", "STILL") == "WALKING") {
                sharedPreferencesEditor.putString("actState", "INVEHICLE").commit()
                remoteViews?.setTextViewText(R.id.tx_activity_state, "IN VEHICLE")
                remoteViews?.setViewVisibility(R.id.rl_still_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.fl_speed, View.VISIBLE)
            } else if (sharedPreferences.getString("actState", "STILL") == "INVEHICLE") {
                sharedPreferencesEditor.putString("actState", "STILL").commit()
                remoteViews?.setTextViewText(R.id.tx_activity_state, "STILL")
                var dC = sharedPreferences.getInt("drinkCount", 0)
                remoteViews?.setTextViewText(R.id.tx_drink_count, dC.toString())
                var bC = sharedPreferences.getInt("breatheCount", 0)
                remoteViews?.setTextViewText(R.id.tx_breathe_count, bC.toString())
                remoteViews?.setViewVisibility(R.id.rl_still_state, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.fl_speed, View.INVISIBLE)
            }

        } else if (PREV_STATE == intent.action) {

            if (presentActivityState == "STILL") {
                remoteViews?.setViewVisibility(R.id.rl_still_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.fl_speed, View.VISIBLE)
            } else if (presentActivityState == "WALKING") {
                remoteViews?.setViewVisibility(R.id.rl_still_state, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.fl_speed, View.INVISIBLE)
            } else if (presentActivityState == "INVEHICLE") {
                remoteViews?.setViewVisibility(R.id.rl_still_state, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.rl_walking_state, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.fl_speed, View.INVISIBLE)
            }

        } else if (TIME_CLICKED == intent.action) {

            if (sharedPreferences.getBoolean("AnalogV", true)) {
                sharedPreferencesEditor.putBoolean("AnalogV", false).apply()

                remoteViews?.setViewVisibility(R.id.a_clock, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.clock, View.VISIBLE)
            } else {
                sharedPreferencesEditor.putBoolean("AnalogV", true).apply()

                remoteViews?.setViewVisibility(R.id.a_clock, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.clock, View.INVISIBLE)
            }

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
            //    makeToast("TIME_A_CLICKED, spkServiceState : $boolSpkService")
            val speakIntent = Intent(widgetContext, SpeakService::class.java)
            if (!boolSpkService) {
                widgetContext.startService(speakIntent)
                remoteViews?.setTextViewText(R.id.tx_time_announcement, "\uD83D\uDDE3")
                makeToast("Change in Hour & notification app name will be announced!")
                sharedPreferencesEditor.putBoolean("SPKSERVICE", true).apply()
            } else {
                widgetContext.stopService(speakIntent)
                remoteViews?.setTextViewText(R.id.tx_time_announcement, "⊘")
                //    makeToast("stopingSPKservice")
                sharedPreferencesEditor.putBoolean("SPKSERVICE", false).apply()
            }

        }
    }

    fun RotateBitmap(source: Bitmap, angle: Float): Bitmap? {
        val matrix: Matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source,
            0,
            0,
            90,
            90,
            matrix,
            true
        )
    }

    private fun startMusicActivity(songIndex: Int) {
        var intentMusic = Intent(widgetContext, MusicActivity::class.java)
        intentMusic.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        makeToast("songIndex ~ " + songIndex)
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

    }

    @SuppressLint("Range")
    private fun getFavoriteContacts() {

        favContacts = ArrayList()

        val queryUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
            .build()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val selection = ContactsContract.Contacts.STARRED + "='1'"

        val cursor = widgetContext.contentResolver.query(
            queryUri, projection, selection, null, null
        )

        while (cursor!!.moveToNext()) {
            val contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            var phoneNumber: String = "7"

            if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                val phones: Cursor? = widgetContext.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactID,
                    null,
                    null
                )
                while (phones!!.moveToNext()) {
                    phoneNumber =
                        phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    phoneNumber = phoneNumber.filter { !it.isWhitespace() }
                }
            }

            var contactBitmap: Bitmap?

            contactBitmap =
                ContactPhotoHelper.retrieveContactPhoto(widgetContext, contactID.toLong())
            val cNme = cursor.getString(
                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            )
            if (contactBitmap == null)
                contactBitmap = CharacterToBitmapConverter.getBitmapFromCharacter(
                    cNme[0], 100, 100, 70, Color.BLACK
                )
            val c = Contact(contactID, cNme, phoneNumber, contactBitmap)
            if (c.number.length > 7)
                favContacts.add(c)
        }
        saveContacts()
        cursor.close()
    }

    private fun saveContacts() {
        val key = "CTS"
        val gson = Gson()
        val json = gson.toJson(favContacts)
        sharedPreferencesEditor.remove(key).commit()
        sharedPreferencesEditor.putString(key, json).commit()
    }


    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.S)
    fun getPreciseEnergyCounter(context: Context): Long {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        //     makeToast("Juice ~ $energy")

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


        return if (energy != Long.MIN_VALUE) {
            energy // Energy remaining in microampere-hours (µAh)
        } else {
            0L
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

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            return wifiInfo != null && wifiInfo.isConnected
        }
        return false
    }

    @SuppressLint("SetTextI18n")
    private fun loadWidgetToShare(appWidgetView: View) {


        appWidgetView.findViewById<ImageView>(R.id.imgv_widget_layout).setImageBitmap(
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
                ), Color.WHITE, 50
            )
        )

        appWidgetView.findViewById<TextView>(
            R.id.btn_screentime
        ).text = "${totalUsage}"
        greeting()
        appWidgetView.findViewById<TextView>(R.id.tx_wish).text = timelyWish
        var ampm = Calendar.getInstance()[Calendar.AM_PM].toString()

        when (ampm) {
            "0" -> ampm = "AM"
            "1" -> ampm = "PM"
        }
        appWidgetView.findViewById<TextView>(R.id.clock).text = "${
            java.util.Calendar.getInstance().get(Calendar.HOUR)
        }:${java.util.Calendar.getInstance().get(Calendar.MINUTE)} $ampm"
        val mSpannableStringLoc = SpannableString(cityname)
        mSpannableStringLoc.setSpan(UnderlineSpan(), 0, mSpannableStringLoc.length, 0)
        appWidgetView.findViewById<TextView>(R.id.tx_place).text = "⚲ " + cityname
        appWidgetView.findViewById<TextView>(R.id.tx_steps).text = "$stepsToday"
        //    appWidgetView.findViewById<TextView>(R.id.tx_weather).text = tempC.substring(0, 2) + "°C, " + weatherIconState

        appWidgetView.findViewById<LinearLayout>(R.id.ll_apps).visibility = View.INVISIBLE
        appWidgetView.findViewById<LinearLayout>(R.id.ll_contacts).visibility = View.INVISIBLE
        appWidgetView.findViewById<TextView>(R.id.tx_apps).visibility = View.VISIBLE
        appWidgetView.findViewById<TextView>(R.id.tx_calls).visibility = View.VISIBLE

        /* appWidgetView.findViewById<TextView>(R.id.tx_weather_icon_temp).setText(
             MainActivity.tempC.substring(
                 0,
                 2
             ) + "°C"
         )
         appWidgetView.findViewById<TextView>(R.id.tx_weather_icon_state).text =
             MainActivity.weatherIconState */
        appWidgetView.findViewById<TextView>(R.id.tx_day_date).text =
            SimpleDateFormat("EEE", Locale.getDefault()).format(Calendar.getInstance().time) +
                    ", " + formattedDate

        readApps()

        appWidgetView.findViewById<TextView>(R.id.tx_walldesc).text = wD
        appWidgetView.findViewById<TextView>(R.id.tx_walltype_updateinfo).setText(
            Html.fromHtml(
                qT.split(" ")[0].substring(0, 1)
                    .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                Html.FROM_HTML_MODE_LEGACY
            )
        )


        appWidgetView.findViewById<AnalogClock>(R.id.a_clock).visibility = View.INVISIBLE
    }

    fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius) // Set the blur radius (0 < radius <= 25)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)
        rs.destroy() // Release RenderScript resources
        return bitmap
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
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
        } catch (e: IOException) {
            e.printStackTrace()
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

    private fun selectContact() {

        makeToast("pickContact!")
        MainActivity.pickContact()

    }


    private fun shareWidget(context: Context, bitmap: Bitmap) {
        val bitmapPath = MediaStore.Images.Media.insertImage(
            widgetContext.getContentResolver(), bitmap, "title", ""
        )
        val uri = Uri.parse(bitmapPath)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("image/*")
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "App")
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Currently a new version of KiKi app is available.")
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(
            Intent.createChooser(shareIntent, "Share").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
        var arrayListUsageStats: HashSet<AppUsage> = HashSet()
        lateinit var dayOfTheWeek: String
        var vpStepsPos: Int = 0
        var noRewards: Int = 0
        var tW: String = "..."
        lateinit var appWidM: AppWidgetManager



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


        private fun Bitmap.getCircledBitmap(): Bitmap {
            val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint()
            val rect = Rect(0, 0, this.width, this.height)
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(this.width / 2f, this.height / 2f, this.width / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(this, rect, rect, paint)
            return output
        }

        fun getAppIconFromPkg(context: Context, packageName: String?): Drawable {
            try {
                val icon: Drawable =
                    context.packageManager.getApplicationIcon(packageName.toString())
                return icon
            } catch (e: NameNotFoundException) {
                e.printStackTrace()
                return AppCompatResources.getDrawable(context, R.drawable.calls)!!
            }
        }


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

            }

            //    remoteViews?.setImageViewBitmap(R.id.imgbtn_n_apps, gpBitmap)

            Log.d("gpName - ", gpName)
            c!!.close()

            if (timeOfDay == "Morni!")
                timelyWish = "\uD83C\uDF3B$timeOfDay "//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Noon!")
                timelyWish = "☀\uFE0F$timeOfDay "//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Eve!")
                timelyWish = "\uD83C\uDF41$timeOfDay "//, ${gpName.split(" ").get(0)}!"
            else if (timeOfDay == "Night!")
                timelyWish = "\uD83D\uDCA4$timeOfDay "//, ${gpName.split(" ").get(0)}!"

        }

        fun getScreenTime(applicationContext: Context) {

            if (sharedPreferences == null)
                sharedPreferences =
                    widgetContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
            if (sharedPreferencesEditor == null)
                sharedPreferencesEditor = sharedPreferences.edit()

            val usageStatsManager =
                applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1) // Query for the last 24 hours
            val startTime = calendar.timeInMillis

            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                1 -> {
                    dayOfTheWeek = "Monday"
                    vpStepsPos = 0
                    sharedPreferencesEditor.putInt("Monday", stepsToday).apply()
                    sharedPreferencesEditor.putInt("Tuesday", 0).apply()
                    sharedPreferencesEditor.putInt("Wednesday", 0).apply()
                    sharedPreferencesEditor.putInt("Thursday", 0).apply()
                    sharedPreferencesEditor.putInt("Friday", 0).apply()
                    sharedPreferencesEditor.putInt("Saturday", 0).apply()
                    sharedPreferencesEditor.putInt("Sunday", 0).apply()
                }

                2 -> {
                    vpStepsPos = 1
                    dayOfTheWeek = "Tuesday"
                    sharedPreferencesEditor.putInt("Tuesday", stepsToday).apply()
                    sharedPreferencesEditor.putInt("Wednesday", 0).apply()
                    sharedPreferencesEditor.putInt("Thursday", 0).apply()
                    sharedPreferencesEditor.putInt("Friday", 0).apply()
                    sharedPreferencesEditor.putInt("Saturday", 0).apply()
                    sharedPreferencesEditor.putInt("Sunday", 0).apply()
                }

                3 -> {
                    vpStepsPos = 2
                    dayOfTheWeek = "Wednesday"
                    sharedPreferencesEditor.putInt("Wednesday", stepsToday).apply()

                    sharedPreferencesEditor.putInt("Thursday", 0).apply()
                    sharedPreferencesEditor.putInt("Friday", 0).apply()
                    sharedPreferencesEditor.putInt("Saturday", 0).apply()
                    sharedPreferencesEditor.putInt("Sunday", 0).apply()
                }

                4 -> {
                    vpStepsPos = 3
                    dayOfTheWeek = "Thursday"
                    sharedPreferencesEditor.putInt("Thursday", stepsToday).apply()

                    sharedPreferencesEditor.putInt("Friday", 0).apply()
                    sharedPreferencesEditor.putInt("Saturday", 0).apply()
                    sharedPreferencesEditor.putInt("Sunday", 0).apply()
                }

                5 -> {
                    vpStepsPos = 4
                    dayOfTheWeek = "Friday"
                    sharedPreferencesEditor.putInt("Friday", stepsToday).apply()

                    sharedPreferencesEditor.putInt("Saturday", 0).apply()
                    sharedPreferencesEditor.putInt("Sunday", 0).apply()
                }

                6 -> {
                    vpStepsPos = 5
                    dayOfTheWeek = "Saturday"
                    sharedPreferencesEditor.putInt("Saturday", stepsToday).apply()

                    sharedPreferencesEditor.putInt("Sunday", 0).apply()
                }

                7 -> {
                    vpStepsPos = 6
                    dayOfTheWeek = "Sunday"
                    sharedPreferencesEditor.putInt("Sunday", stepsToday).apply()
                }
            }

            if (sharedPreferences.getString("day", "someday") != dayOfTheWeek) {
                stepsToday = 0
                sharedPreferencesEditor.putInt("breatheCount", 0).apply()
                sharedPreferencesEditor.putInt("drinkCount", 0).apply()
                dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

                if (arrayListHabits.size > 0) {
                    for (i in arrayListHabits)
                        i.isChecked = false
                    adapterHabits.notifyDataSetChanged()
                }
                dayChange = true
                sharedPreferencesEditor.putString("day", dayOfTheWeek).apply()
            }

            // Get a map of package names to UsageStats objects
            val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
                beginCal.timeInMillis,
                endCal.timeInMillis
            )

            var totalScreenTimeInMillis: Long = 0
            for (usageStats in usageStatsMap.values) {
                totalScreenTimeInMillis += usageStats.totalTimeInForeground
            }

            // Convert to desired units (e.g., minutes, hours)
            totalScreenTimeInHours = totalScreenTimeInMillis / (1000 * 60 * 60) / 6

            val currentHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
            var ampm = Calendar.getInstance()[Calendar.AM_PM].toString()

            when (ampm) {
                "0" -> ampm = "AM"
                "1" -> ampm = "PM"
            }

            if (totalUsage.split(":")[0].isNotEmpty()) {
                var sT = totalUsage.split(":")
                var hour = ""

                if (sT[0][0] == '0')
                    hour = sT[0].drop(1)
                else hour = sT[0]

                remoteViews?.setTextViewText(
                    R.id.btn_screentime,
                    "$hour+ H"
                )
            }


        }

        fun todaysDate() {

            val c: Date = Calendar.getInstance().time
            val dfDate = SimpleDateFormat("d", Locale.getDefault())
            val dfMonth = SimpleDateFormat("MMM", Locale.getDefault())

            var postFixDate = ""



            if (dfDate.format(c).length == 1) {
                when (dfDate.format(c).toInt()) {
                    1 -> postFixDate = "ˢᵗ"
                    2 -> postFixDate = "ⁿᵈ"
                    3 -> postFixDate = "ʳᵈ"
                    in 4..9 -> postFixDate = "ᵗʰ"

                }
            } else {
                when (dfDate.format(c).toInt()) {
                    in 11..20 -> postFixDate = "ᵗʰ"
                    21, 31 -> postFixDate = "ˢᵗ"
                    22 -> postFixDate = "ⁿᵈ"
                    23 -> postFixDate = "ʳᵈ"
                    in 24..30 -> postFixDate = "ᵗʰ"

                }
            }


            formattedDate = dfDate.format(c) + postFixDate + " " + dfMonth.format(c)


            // remoteViews?.setTextViewText(R.id.tx_date, formattedDate)
            sharedPreferencesEditor.putBoolean("DateSet", true).apply()
            sharedPreferencesEditor.putString("fD", formattedDate).apply()
            if (stepsToday != 0)
                remoteViews?.setTextViewText(R.id.tx_steps, "$stepsToday")
            //   remoteViews?.setTextViewText(R.id.n_tx_steps, "Now, $newLapSteps")
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
                        gpName.split(" ")[0].substring(0, 1) + gpName.split(" ")[1].substring(0, 1)
                    )
            } catch (ex: Exception) {
                gpName = ""
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
        private const val BATTERY_INFO = "batteryInfo"
        private const val GET_WEATHER = "getWeather"
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
        private const val TIME_CLICKED = "timeClick"
        private const val C_CLICKED = "CClicked"
        private const val A_CLICKED = "AClicked"
        private const val ACTION_LIST_CONTACTITEM_CLICK = "Contact_Item_Click"
        private const val ACTION_LIST_APPITEM_CLICK = "App_Item_Click"
        const val EXTRA_CONTACTITEM_POSITION = "Contact_Item_Pos"
        const val EXTRA_APPITEM_POSITION = "App_Item_Pos"
        const val EXTRA_CONTACTVIEW_ID = "CID"
        const val EXTRA_APPVIEW_ID = "AID"

    }


}