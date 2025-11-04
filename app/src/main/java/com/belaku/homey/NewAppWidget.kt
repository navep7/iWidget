package com.belaku.homey


// Weather Key - 9fa8e101240ab18615e3133b051e767e

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.getWeatherData
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.mBluetoothAdapter
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pickContact
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.MainActivity.Companion.twitterProfileName
import com.belaku.homey.MainActivity.Companion.weatherIconID
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.initialSteps
import com.belaku.homey.SetWallWorker.Companion.stepsToday
import com.belaku.homey.SetWallWorker.Companion.wallBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates


class NewAppWidget : AppWidgetProvider() {

    private var callIndex: Int = -1
    private var totalScreenTimeInMinutes by Delegates.notNull<Long>()
    private lateinit var calendar: Calendar
    private lateinit var nowCalendar: Calendar
    private lateinit var ampm: String
    private lateinit var pendingIntentD: PendingIntent
    private lateinit var intentD: Intent
    private lateinit var dNews: Drawable
    private var randomTweetIndex: Int = 0
    private lateinit var formattedDate: String
    private var timelyWish: String = ""
    private val TAG: String = "NewAppWidget LOG7"
    private lateinit var wD: String
    private lateinit var qT: String
    private lateinit var uT: String
    private lateinit var dU: String

    private lateinit var mp: MediaPlayer


    private var currentHour by Delegates.notNull<Int>()
    private var currentMin by Delegates.notNull<Int>()
    lateinit var gpName: String


    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        appContx = context!!
        onEn = true
        dNews = appContx.resources.getDrawable(R.drawable.face_holder)
//        Log.d("onEnabled! - ", favContacts.size.toString())
        getWeatherData(false)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        appContx = context!!
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        Log.d(TAG, "onUpdate")

        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)

        if (wallpaperColors != null) {
            primaryColor = wallpaperColors.primaryColor.toArgb()

            if (wallpaperColors.secondaryColor != null)
                secondaryColor = wallpaperColors.secondaryColor!!.toArgb()
            else secondaryColor = Color.LTGRAY

            if (wallpaperColors.tertiaryColor != null)
                tertianaryColor = wallpaperColors.tertiaryColor!!.toArgb()
            else tertianaryColor = Color.DKGRAY

        }

        if (!MainActivity.isLocationEnabled(context)) {
            remoteViews?.setTextViewText(R.id.tx_place, "Please Enable Location services!")

            val locIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            val locPendingIntent = PendingIntent.getActivity(
                context,
                21,
                locIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews?.setOnClickPendingIntent(
                R.id.tx_place,
                locPendingIntent
            )
        } else remoteViews?.setTextViewText(
            R.id.tx_place,
            "⚲ " + cityname
        )


        for (appWidgetId in appWidgetIds) {
            remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)
            newAppWidget = ComponentName(context, NewAppWidget::class.java)


            val aiIntent = Intent(context, AiActivity::class.java)
            aiIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            aiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val aiPendingIntent = PendingIntent.getActivity(
                context,
                0,
                aiIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_ai,
                aiPendingIntent
            )


            val remindersIntent = Intent(context, RemindersActivity::class.java)
            remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val remindersPendingIntent = PendingIntent.getActivity(
                context,
                0,
                remindersIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_reminders,
                remindersPendingIntent
            )


            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_speech, PendingIntent.getActivity(
                    context, 1,
                    Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "StT"),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_tweets, PendingIntent.getActivity(
                    context, 2,
                    Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "ST"),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )


            remoteViews?.setOnClickPendingIntent(
                R.id.twSettings, PendingIntent.getActivity(
                    context, 3,
                    Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "STH"),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            val intentBluetooth = Intent(context, DialogActivity::class.java)
            if (mBluetoothAdapter.isEnabled)
                intentBluetooth.putExtra("DialogIntent", "BLUEDisable")
            else intentBluetooth.putExtra("DialogIntent", "BLUEEnable")
            val pendingIntentBluetooth = PendingIntent.getActivity(
                context,
                4,
                intentBluetooth,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews?.setOnClickPendingIntent(R.id.fab_blue, pendingIntentBluetooth)

            remoteViews?.setOnClickPendingIntent(
                R.id.fab_share,
                getPendingSelfIntent(context, FAB_SHARE)
            )


            val intentWifi = Intent(context, DialogActivity::class.java)
            if (isWifiEnabled(context))
                intentWifi.putExtra("DialogIntent", "WifiDisable")
            else intentWifi.putExtra("DialogIntent", "WifiEnable")
            val pendingIntentWifi = PendingIntent.getActivity(
                context,
                6,
                intentWifi,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews?.setOnClickPendingIntent(R.id.fab_wifi, pendingIntentWifi)

            remoteViews?.setOnClickPendingIntent(
                R.id.fab_torch,
                getPendingSelfIntent(context, TORCH_STATE)
            )


            remoteViews?.setOnClickPendingIntent(
                R.id.tx_now_steps,
                getPendingSelfIntent(context, STEPS_NOW)
            )

            val launcherIntentGaps = Intent(context, GapsActivity::class.java)
            launcherIntentGaps.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            launcherIntentGaps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val launcherPendingIntentGaps = PendingIntent.getActivity(
                context,
                0,
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
                11,
                launcherIntentNPs,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_n_apps,
                launcherPendingIntentNPs
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_qr,
                PendingIntent.getActivity(
                    context, 10,
                    Intent(context, DialogActivity::class.java).putExtra(
                        "DialogIntent",
                        "qrClick"
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )


            remoteViews?.setOnClickPendingIntent(
                R.id.tx_screentime_info,
                PendingIntent.getActivity(
                    context, 8,
                    Intent(context, DialogActivity::class.java).putExtra(
                        "DialogIntent",
                        "screenTimeInfo"
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_steps_info, PendingIntent.getActivity(
                    context, 7,
                    Intent(context, DialogActivity::class.java).putExtra(
                        "DialogIntent",
                        "stepsInfo"
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_live_weather_effects, PendingIntent.getActivity(
                    context, 9,
                    Intent(context, DialogActivity::class.java).putExtra(
                        "DialogIntent",
                        "liveWall"
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.weather_icon,
                getPendingSelfIntent(context, GET_WEATHER)
            )


            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_lock,
                getPendingSelfIntent(context, LOCK_PHONE)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_set,
                getPendingSelfIntent(context, WALL_CHANGE)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_conf,
                getPendingSelfIntent(context, SET_CLICKED)
            )


            val mapsIntent = Intent(context, MapsActivity::class.java)
            val mapsPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mapsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_location,
                mapsPendingIntent
            )


            val launcherIntent = Intent(context, AppsActivity::class.java)
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launcherPendingIntent = PendingIntent.getActivity(
                context,
                0,
                launcherIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app3,
                launcherPendingIntent
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app1,
                getPendingSelfIntent(context, APP1_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app2,
                getPendingSelfIntent(context, APP2_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app4,
                getPendingSelfIntent(context, APP4_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app5,
                getPendingSelfIntent(context, APP5_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app6,
                getPendingSelfIntent(context, APP6_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app7,
                getPendingSelfIntent(context, APP7_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app8,
                getPendingSelfIntent(context, APP8_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_app9,
                getPendingSelfIntent(context, APP9_CLICKED)
            )


            appWidM = AppWidgetManager.getInstance(context)
            appWidM.updateAppWidget(appWidgetId, remoteViews)
        }



        appWidM.updateAppWidget(newAppWidget, remoteViews)

    }


    @SuppressLint("ResourceAsColor", "ResourceType")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub

        super.onReceive(context, intent)
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)

        Log.d(TAG, "onReceive ${intent.action}")


        // Keep track of the current frame
        var currentFrameIndex = 0
        val animationFrames =
            intArrayOf( /* ... */)

        checkMusicStatus(context)

        if (!MainActivity.isLocationEnabled(context)) {
            remoteViews?.setTextViewText(R.id.tx_place, "Please Enable Location services!")

            val locIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            val locPendingIntent = PendingIntent.getActivity(
                context,
                21,
                locIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews?.setOnClickPendingIntent(
                R.id.tx_place,
                locPendingIntent
            )
        } else remoteViews?.setTextViewText(
            R.id.tx_place,
            "⚲ " + cityname
        )


        // Create a handler or a timer to update the frames
        val handler = Handler()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                // Update the ImageView with the next frame
                remoteViews?.setImageViewResource(
                    R.id.animation_image,
                    animationFrames[currentFrameIndex]
                )

                // Increment frame index and loop if necessary
                currentFrameIndex = (currentFrameIndex + 1) % animationFrames.size

                // Apply the updates to the widget/notification
                appWidM.updateAppWidget(newAppWidget, remoteViews) // For App Widgets

                // notificationManager.notify(notificationId, notification); // For Notifications

                // Schedule the next frame update
                handler.postDelayed(
                    this,
                    100
                ) // Use the duration defined in XML
            }
        }

        //    handler.post(runnable);


        if (intent.action.equals("android.appwidget.action.APPWIDGET_UPDATE"))
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                Receive(intent, context)
            }, 1000)
        else Receive(intent, context)

    }

    private fun checkMusicStatus(contx: Context) {

        var audioManager = contx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val isMusicActive: Boolean = audioManager.isMusicActive()


        if (isMusicActive) {
            remoteViews?.setTextViewText(R.id.tx_sname, "Music is currently active.")
            //    detectMusic()
        } else {
            remoteViews?.setTextViewText(R.id.tx_sname, "No music is currently active.")
        }
    }

    /* private fun detectMusic() {

         val m = appContx.getSystemService<MediaSessionManager>()!!
         val component = ComponentName(appContx, MusicNotificationListenerService::class.java)
         val sessions = m.getActiveSessions(component)
         Log.d("Sessions", "count: ${sessions.size}")
         sessions.forEach {
             Log.d("Sessions", "$it -- " + (it?.metadata?.keySet()?.joinToString()))
             Log.d("Sessions", "$it -- " + (it?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)))
         }
     }*/

    @SuppressLint("NewApi")
    private fun Receive(intent: Intent, context: Context) {

        sharedPreferences = context.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        wD = sharedPreferences.getString("wD", "").toString()
        qT = sharedPreferences.getString("qT", "").toString()
        dU = sharedPreferences.getString("dU", "").toString()
        uT = sharedPreferences.getString("uT", "").toString()


        if (listTweets.size > 0) {
            if (intent.action.equals("wallChange")) {
                Log.d(TAG + "TwAct", intent.action.toString())
                randomTweetIndex = (0..listTweets.size - 1).random()

                if (noRewards != 0)
                    tW = listTweets[randomTweetIndex]
                else tW = listTweets[0]
                sharedPreferencesEditor.putString("tW", tW).apply()
            } else {
                tW = sharedPreferences.getString("tW", "").toString()
                if (tW.length == 0)
                    tW = listTweets[0]
            }
        }

        appContx = context
        readContacts()
        getFavoriteContacts()
        readApps()

        appIndex = 0

        nowCalendar = Calendar.getInstance()

        currentHour = nowCalendar[Calendar.HOUR_OF_DAY]
        currentMin = nowCalendar[Calendar.MINUTE]
        if (nowCalendar[Calendar.AM_PM] == Calendar.AM)
            ampm = "am"
        else ampm = "pm"


        getScreenTime()

        val aiIntent = Intent(context, AiActivity::class.java)
        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val aiPendingIntent = PendingIntent.getActivity(
            context,
            0,
            aiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_ai,
            aiPendingIntent
        )

        val remindersIntent = Intent(context, RemindersActivity::class.java)
        remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val remindersPendingIntent = PendingIntent.getActivity(
            context,
            0,
            remindersIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_reminders,
            remindersPendingIntent
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_speech, PendingIntent.getActivity(
                context, 1,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "StT"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_tweets, PendingIntent.getActivity(
                context, 2,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "ST"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.twSettings, PendingIntent.getActivity(
                context, 3,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "STH"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        val intentBluetooth = Intent(context, DialogActivity::class.java)
        if (mBluetoothAdapter.isEnabled)
            intentBluetooth.putExtra("DialogIntent", "BLUEDisable")
        else intentBluetooth.putExtra("DialogIntent", "BLUEEnable")
        val pendingIntentBluetooth = PendingIntent.getActivity(
            context,
            4,
            intentBluetooth,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews?.setOnClickPendingIntent(R.id.fab_blue, pendingIntentBluetooth)


        remoteViews?.setOnClickPendingIntent(
            R.id.fab_share,
            getPendingSelfIntent(context, FAB_SHARE)
        )


        val intentWifi = Intent(context, DialogActivity::class.java)
        if (isWifiEnabled(context)) {
            intentWifi.putExtra("DialogIntent", "WifiDisable")
            remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_on)
        } else {
            intentWifi.putExtra("DialogIntent", "WifiEnable")
            remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_off)
        }
        val pendingIntentWifi = PendingIntent.getActivity(
            context,
            6,
            intentWifi,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews?.setOnClickPendingIntent(R.id.fab_wifi, pendingIntentWifi)

        remoteViews?.setOnClickPendingIntent(
            R.id.fab_torch,
            getPendingSelfIntent(context, TORCH_STATE)
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.tx_now_steps,
            getPendingSelfIntent(context, STEPS_NOW)
        )

        val launcherIntentGaps = Intent(context, GapsActivity::class.java)
        launcherIntentGaps.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        launcherIntentGaps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val launcherPendingIntentGaps = PendingIntent.getActivity(
            context,
            0,
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
            11,
            launcherIntentNPs,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_n_apps,
            launcherPendingIntentNPs
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_qr,
            PendingIntent.getActivity(
                context, 10,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "qrClick"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.tx_screentime_info,
            PendingIntent.getActivity(
                context, 8,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "screenTimeInfo"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_steps_info, PendingIntent.getActivity(
                context, 7,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "stepsInfo"
                ).putExtra("day", calendar.get(Calendar.DAY_OF_WEEK)),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_live_weather_effects, PendingIntent.getActivity(
                context, 9,
                Intent(context, DialogActivity::class.java).putExtra(
                    "DialogIntent",
                    "liveWall"
                ),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.weather_icon,
            getPendingSelfIntent(context, GET_WEATHER)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_lock,
            getPendingSelfIntent(context, LOCK_PHONE)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_set,
            getPendingSelfIntent(context, WALL_CHANGE)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_conf,
            getPendingSelfIntent(context, SET_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app1,
            getPendingSelfIntent(context, APP1_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app2,
            getPendingSelfIntent(context, APP2_CLICKED)
        )

        val mapsIntent = Intent(context, MapsActivity::class.java)
        val mapsPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mapsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_location,
            mapsPendingIntent
        )


        val launcherIntent = Intent(context, AppsActivity::class.java)
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val launcherPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launcherIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app3,
            launcherPendingIntent
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app4,
            getPendingSelfIntent(context, APP4_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app5,
            getPendingSelfIntent(context, APP5_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app6,
            getPendingSelfIntent(context, APP6_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app7,
            getPendingSelfIntent(context, APP7_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app8,
            getPendingSelfIntent(context, APP8_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_app9,
            getPendingSelfIntent(context, APP9_CLICKED)
        )


        var timeOfDay = if (currentHour >= 6 && currentHour < 12) {
            "Morni!"
        } else if (currentHour >= 12 && currentHour < 17) {
            "Noon!"
        } else if (currentHour >= 17 && currentHour < 21) {
            "Eve!"
        } else {
            "Night!"
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            greeting(context, remoteViews!!, timeOfDay)
        }


        remoteViews?.setTextViewText(
            R.id.tx_rewards_count,
            "" + sharedPreferences.getInt("noRewards", 5)
        )

        remoteViews?.setTextViewText(
            R.id.tx_tweets,
            "@" + twitterProfileName + "\t ~ \t" + tW
        )

        remoteViews?.setTextViewText(
            R.id.tx_desc_walltype,
            Html.fromHtml(
                wD + "<br>" + qT.split(" ")[0].substring(0, 1)
                    .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                Html.FROM_HTML_MODE_LEGACY
            )
        )


        todaysDate(context)


        if (FAB_SHARE == intent.action) {

            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val appWidgetView: View = inflater.inflate(R.layout.new_app_widget, null)

            loadWidgetToShare(appWidgetView)
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


        }





        if (GET_WEATHER == intent.action) {
            remoteViews?.setTextViewText(R.id.tx_weather_icon_temp, "")
            getWeatherData(true)
            remoteViews?.setTextViewText(
                R.id.tx_weather_icon_temp,
                MainActivity.tempC.substring(
                    0,
                    2
                ) + "°C"
            )
        }


        if (WIFI_AUTO == intent.action) {
            var wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            wifiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContx.startActivity(wifiIntent)
        }

        if (TORCH_STATE == intent.action) {

            val isFlashAvailable =
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            if (!isFlashAvailable) {
                //  return
            }
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
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


        }


        if (STEPS_NOW == intent.action) {
            if (boolNewLap) {
                remoteViews?.setTextViewText(R.id.tx_now_steps, " + ")
                remoteViews?.setTextViewText(R.id.tx_n_steps, "")
                remoteViews?.setViewVisibility(R.id.vertical_divider, View.INVISIBLE)
                //  remoteViews?.setTextViewText(R.id.tx_add_remove_newlap, "+")
            } else {
                remoteViews?.setTextViewText(R.id.tx_now_steps, " x ")
                remoteViews?.setTextViewText(R.id.tx_n_steps, "Now, " + "0")
                remoteViews?.setViewVisibility(R.id.vertical_divider, View.VISIBLE)
                //  remoteViews?.setTextViewText(R.id.tx_add_remove_newlap, "x")
            }
            boolNewLap = !boolNewLap
            if (initialSteps == 0)
                initialSteps = stepsToday
            else initialSteps = 0
        }

        if (LOCK_PHONE == intent.action) {
            LockAccessibilityService.lockScreenAccessibility(appContx)
        }


        if (SET_CLICKED == intent.action) {
            val launchIntent: Intent =
                context.packageManager.getLaunchIntentForPackage("com.belaku.homey")!!
            context.startActivity(launchIntent)
        }


        if (WALL_CHANGE == intent.action) {

            noRewards = sharedPreferences.getInt("noRewards", 5)

            if (noRewards > 0) {
                noRewards--
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "$noRewards")
            } else {
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "$noRewards AD!")
            }
            sharedPreferencesEditor.putInt("noRewards", noRewards).apply()

            if (noRewards != 0) {

                remoteViews?.setTextViewText(
                    R.id.tx_tweets,
                    "@" + twitterProfileName + "\t ~ \t" + tW
                )
                //🖍

                remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.VISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.INVISIBLE)

                //     appWidM.updateAppWidget(newAppWidget, remoteViews)

                Thread {
                    SetWallWorker.setWall(true)
                }.start()

            } else {
                makeToast("Watch an AD to auto change Walls for next 7 times!")
                remoteViews?.setTextViewText(R.id.tx_rewards_count, "\uD83D\uDC41\uFE0FAD!")
                remoteViews?.setOnClickPendingIntent(
                    R.id.tx_rewards_count, PendingIntent.getActivity(
                        context,
                        5,
                        Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "AD"),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }

        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)


        if (wallpaperColors != null) {
            primaryColor = wallpaperColors.primaryColor.toArgb()
            if (wallpaperColors.secondaryColor != null)
                secondaryColor = wallpaperColors.secondaryColor!!.toArgb()
            else secondaryColor = Color.LTGRAY

            if (wallpaperColors.tertiaryColor != null)
                tertianaryColor = wallpaperColors.tertiaryColor!!.toArgb()
            else tertianaryColor = Color.DKGRAY

            remoteViews?.setColorInt(
                R.id.imgbtn_lock,
                "setColorFilter",
                primaryColor,
                secondaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_conf,
                "setColorFilter",
                tertianaryColor,
                primaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_set,
                "setColorFilter",
                primaryColor,
                secondaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_location,
                "setColorFilter",
                secondaryColor,
                primaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_speech,
                "setColorFilter",
                secondaryColor,
                primaryColor
            )
        } else {
            primaryColor = Color.BLACK
            secondaryColor = Color.WHITE
            tertianaryColor = Color.RED
            remoteViews?.setColorInt(
                R.id.imgbtn_lock,
                "setColorFilter",
                primaryColor,
                secondaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_conf,
                "setColorFilter",
                tertianaryColor,
                primaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_set,
                "setColorFilter",
                primaryColor,
                secondaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_location,
                "setColorFilter",
                secondaryColor,
                primaryColor
            )
            remoteViews?.setColorInt(
                R.id.imgbtn_speech,
                "setColorFilter",
                secondaryColor,
                primaryColor
            )
        }

        if (APP1_CLICKED == intent.action) {
            var app = choosenApps[0]
            Log.d("APP1_CLICKED", app.name)
            launchApp(context, app.pName)
        }

        if (APP2_CLICKED == intent.action) {
            var app = choosenApps[1]
            Log.d("APP2_CLICKED", app.name)
            launchApp(context, app.pName)
        }


        if (APP4_CLICKED == intent.action) {
            var app = choosenApps[3]
            Log.d("APP4_CLICKED", app.name)
            launchApp(context, app.pName)
        }

        if (APP5_CLICKED == intent.action) {
            var app = choosenApps[4]
            Log.d("APP5_CLICKED", app.name)
            launchApp(context, app.pName)
        }

        if (APP6_CLICKED == intent.action) {
            if (selectedApps.size > 0) {
                var app = selectedApps[0]
                Log.d("APP6_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(
                    Intent(context, AppChooserDialog::class.java).putExtra(
                        "id",
                        6
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        if (APP7_CLICKED == intent.action) {
            if (selectedApps.size > 1) {
                var app = selectedApps[1]
                Log.d("APP7_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(
                    Intent(context, AppChooserDialog::class.java).putExtra(
                        "id",
                        7
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }


        if (APP8_CLICKED == intent.action) {
            if (selectedApps.size > 2) {
                var app = selectedApps[2]
                Log.d("APP8_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(
                    Intent(context, AppChooserDialog::class.java).putExtra(
                        "id",
                        8
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        if (APP9_CLICKED == intent.action) {
            if (selectedApps.size > 3) {
                var app = selectedApps[3]
                Log.d("APP9_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(
                    Intent(context, AppChooserDialog::class.java).putExtra(
                        "id",
                        9
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }


        if (C_CLICKED == intent.action) {
            val intentContacts = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            intentContacts.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContx.startActivity(intentContacts)
        } else if (C1_CLICK == intent.action)
            dialPhoneNumber(context, favContacts[0].number)
        else if (C2_CLICK == intent.action)
            dialPhoneNumber(context, favContacts[1].number)
        else if (C3_CLICK == intent.action)
            dialPhoneNumber(context, favContacts[2].number)
        else if (C4_CLICK == intent.action)
            dialPhoneNumber(context, favContacts[3].number)
        else if (C5_CLICK == intent.action)
            dialPhoneNumber(context, favContacts[4].number)
        else if (CL1_CLICK == intent.action)
            unMarkAsFav(favContacts[0].id)
        else if (CL2_CLICK == intent.action)
            unMarkAsFav(favContacts[1].id)
        else if (CL3_CLICK == intent.action)
            unMarkAsFav(favContacts[2].id)
        else if (CL4_CLICK == intent.action)
            unMarkAsFav(favContacts[3].id)
        else if (CL5_CLICK == intent.action)
            unMarkAsFav(favContacts[5].id)



        for (i in 0 until selectedApps.size) {
            if (i == 0)
                remoteViews?.setImageViewBitmap(
                    R.id.imgv_app6,
                    selectedApps[i].icon.getCircledBitmap()
                )
            else if (i == 1)
                remoteViews?.setImageViewBitmap(
                    R.id.imgv_app7,
                    selectedApps[i].icon.getCircledBitmap()
                )
            else if (i == 2)
                remoteViews?.setImageViewBitmap(
                    R.id.imgv_app8,
                    selectedApps[i].icon.getCircledBitmap()
                )
            else if (i == 3)
                remoteViews?.setImageViewBitmap(
                    R.id.imgv_app9,
                    selectedApps[i].icon.getCircledBitmap()
                )
        }

        try {
            if (sharedPreferences.getBoolean("Blue", false))
                remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_on)
            else remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_off)
        } catch (ex: Exception) {
            makeToast("EXXx ${ex.message}")
        }

        if (isWifiEnabled(context))
            if (!isWifiConnected(context))
                remoteViews?.setImageViewResource(
                    R.id.fab_wifi,
                    R.drawable.wifi_on_but_not_connected
                )


        if ("TWEET".equals(intent.getAction()))
            makeToast(tW)



        newAppWidget = ComponentName(context, NewAppWidget::class.java)
        appWidM = AppWidgetManager.getInstance(context)
        appWidM.updateAppWidget(newAppWidget, remoteViews)

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun unMarkAsFav(contactId: String) {
        // Replace with the actual contact ID
        val values = ContentValues()
        values.put(ContactsContract.Contacts.STARRED, 0) // 1 for favorite, 0 for not favorite

        appContx.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            ContactsContract.Contacts._ID + " = ?",
            arrayOf<String>(contactId.toString())
        )

        getFavoriteContacts()
        readContacts()

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

        val cursor = appContx.contentResolver.query(
            queryUri, projection, selection, null, null
        )

        while (cursor!!.moveToNext()) {
            val contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            var phoneNumber: String = "7"

            if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                val phones: Cursor? = appContx.getContentResolver().query(
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

            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI, contactID.toString()
            )
            intent.data = uri
            val cPhUri = intent.toUri(0)

            val cNme = cursor.getString(
                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            )

            var c = Contact(contactID, cNme, phoneNumber, cPhUri)

            if (c.number.length > 7)
                favContacts.add(c)

        }

        saveContacts()

        cursor.close()


        appWidM.updateAppWidget(newAppWidget, remoteViews)
    }

    private fun saveContacts() {
        val key = "CTS"
        val gson = Gson()
        val json = gson.toJson(favContacts)
        sharedPreferencesEditor.remove(key).commit()
        sharedPreferencesEditor.putString(key, json).commit()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun readContacts() {

        favContacts?.clear()
        val gson = Gson()
        val response: String = sharedPreferences.getString("CTS", "").toString()
        favContacts = gson.fromJson(
            response,
            object : TypeToken<List<Contact?>?>() {}.type
        )

        conIndex = 0


        if (response.isNotEmpty())
            addContactsInWidget(appContx, favContacts)
        else {
            remoteViews?.removeAllViews(R.id.ll_contacts)
            var childView = RemoteViews(appContx.packageName, R.layout.remote_view_layout)


            childView.setImageViewBitmap(
                R.id.new_imgv_id,
                drawableToBitmap(appContx, appContx.resources.getDrawable(R.drawable.contacts))
            )
            childView.setTextViewText(R.id.new_tx_close_id, "")
            childView.setTextViewText(R.id.new_tx_id, "")

            childView.setViewLayoutMargin(
                R.id.new_imgv_id,
                RemoteViews.MARGIN_TOP,
                10f,
                TypedValue.COMPLEX_UNIT_DIP
            )
            childView.setViewLayoutMargin(
                R.id.new_imgv_id,
                RemoteViews.MARGIN_START,
                10f,
                TypedValue.COMPLEX_UNIT_DIP
            )


            childView.setOnClickPendingIntent(
                R.id.new_imgv_id,
                getPendingSelfIntent(appContx, C_CLICKED)
            )
            remoteViews?.addView(R.id.ll_contacts, childView)

            childView = RemoteViews(appContx.packageName, R.layout.remote_view_layout)

            childView.setViewLayoutMargin(
                R.id.new_imgv_id,
                RemoteViews.MARGIN_TOP,
                10f,
                TypedValue.COMPLEX_UNIT_DIP
            )
            childView.setViewLayoutMargin(
                R.id.new_imgv_id,
                RemoteViews.MARGIN_START,
                10f,
                TypedValue.COMPLEX_UNIT_DIP
            )


            childView.setImageViewResource(
                R.id.new_imgv_id,
                android.R.drawable.ic_input_add
            )

            childView.setOnClickPendingIntent(
                R.id.new_imgv_id, PendingIntent.getActivity(
                    appContx, 2,
                    Intent(appContx, DialogActivity::class.java).putExtra("DialogIntent", "PC"),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            childView.setTextViewText(R.id.new_tx_close_id, "")
            childView.setTextViewText(R.id.new_tx_id, "")


            remoteViews?.addView(R.id.ll_contacts, childView)
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

        val backgroundDrawable = BitmapDrawable(appContx.getResources(), wallBitmap)
        appWidgetView.findViewById<RelativeLayout>(R.id.rl_widget_layout)
            .setBackground(backgroundDrawable)

        appWidgetView.findViewById<TextView>(
            R.id.btn_screentime
        ).setText(
            "Screen time ~ ${totalScreenTimeInMinutes.toString()}+ Hrs"
        )
        appWidgetView.findViewById<TextView>(R.id.tx_wish).setText(timelyWish)

        appWidgetView.findViewById<TextView>(R.id.tx_st_since)
            .setText("since ${currentHour % 12} $ampm, yday...")
        appWidgetView.findViewById<TextView>(R.id.clock)
            .setText("${nowCalendar.get(Calendar.HOUR)}:$currentMin $ampm")
        appWidgetView.findViewById<TextView>(R.id.tx_place).setText("⚲ " + cityname)
        appWidgetView.findViewById<TextView>(R.id.tx_steps)
            .setText("$dayOfTheWeek ~ " + stepsToday.toString())

        appWidgetView.findViewById<ImageView>(R.id.imgv_app3).setImageBitmap(
            drawableToBitmap(
                appContx,
                appContx.resources.getDrawable(R.drawable.launch_e)
            )
        )
        appWidgetView.findViewById<TextView>(R.id.tx_weather_icon_temp).setText(
            MainActivity.tempC.substring(
                0,
                2
            ) + "°C"
        )
        appWidgetView.findViewById<TextView>(R.id.tx_weather_icon_state)
            .setText(MainActivity.weatherIconState + "..,")
        appWidgetView.findViewById<TextView>(R.id.tx_day_date).setText(
            SimpleDateFormat("EEE", Locale.getDefault()).format(Calendar.getInstance().time) +
                    "│" + formattedDate
        )
        appWidgetView.findViewById<TextView>(R.id.tx_steps)
            .setText("$dayOfTheWeek ~ " + stepsToday.toString())

        readApps()




        appWidgetView.findViewById<ImageView>(R.id.imgv_app6).visibility = View.INVISIBLE
        appWidgetView.findViewById<ImageView>(R.id.imgv_app7).visibility = View.INVISIBLE
        appWidgetView.findViewById<ImageView>(R.id.imgv_app8).visibility = View.INVISIBLE
        appWidgetView.findViewById<ImageView>(R.id.imgv_app9).visibility = View.INVISIBLE


        appWidgetView.findViewById<TextView>(R.id.tx_desc_walltype).setText(
            Html.fromHtml(
                wD + "<br>" + qT.split(" ")[0].substring(0, 1)
                    .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                Html.FROM_HTML_MODE_LEGACY
            )
        )
        appWidgetView.findViewById<TextView>(R.id.tx_tweets)
            .setText("@" + twitterProfileName + "\t ~ \t" + tW)


    }

    private fun shareBitmap(bitmapWidget: Bitmap) {

        val cachePath: File = File(appContx.getCacheDir(), "images")
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
            appContx,
            appContx.getApplicationContext().getPackageName() + ".fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("image/*")
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temporary read permission

        appContx.startActivity(
            Intent.createChooser(shareIntent, "Share Image Using")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun selectContact() {

        makeToast("pickContact!")
        MainActivity.pickContact()

    }


    private fun getScreenTime() {

        val usageStatsManager =
            appContx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Query for the last 24 hours
        val startTime = calendar.timeInMillis



        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            1 -> {
                dayOfTheWeek = "Monday"
                sharedPreferencesEditor.putInt("Monday", stepsToday).apply()
                sharedPreferencesEditor.putInt("Tuesday", 0).apply()
                sharedPreferencesEditor.putInt("Wednesday", 0).apply()
                sharedPreferencesEditor.putInt("Thursday", 0).apply()
                sharedPreferencesEditor.putInt("Friday", 0).apply()
                sharedPreferencesEditor.putInt("Saturday", 0).apply()
                sharedPreferencesEditor.putInt("Sunday", 0).apply()
            }

            2 -> {
                dayOfTheWeek = "Tuesday"
                sharedPreferencesEditor.putInt("Tuesday", stepsToday).apply()

                sharedPreferencesEditor.putInt("Wednesday", 0).apply()
                sharedPreferencesEditor.putInt("Thursday", 0).apply()
                sharedPreferencesEditor.putInt("Friday", 0).apply()
                sharedPreferencesEditor.putInt("Saturday", 0).apply()
                sharedPreferencesEditor.putInt("Sunday", 0).apply()
            }

            3 -> {
                dayOfTheWeek = "Wednesday"
                sharedPreferencesEditor.putInt("Wednesday", stepsToday).apply()

                sharedPreferencesEditor.putInt("Thursday", 0).apply()
                sharedPreferencesEditor.putInt("Friday", 0).apply()
                sharedPreferencesEditor.putInt("Saturday", 0).apply()
                sharedPreferencesEditor.putInt("Sunday", 0).apply()
            }

            4 -> {
                dayOfTheWeek = "Thursday"
                sharedPreferencesEditor.putInt("Thursday", stepsToday).apply()

                sharedPreferencesEditor.putInt("Friday", 0).apply()
                sharedPreferencesEditor.putInt("Saturday", 0).apply()
                sharedPreferencesEditor.putInt("Sunday", 0).apply()
            }

            5 -> {
                dayOfTheWeek = "Friday"
                sharedPreferencesEditor.putInt("Friday", stepsToday).apply()

                sharedPreferencesEditor.putInt("Saturday", 0).apply()
                sharedPreferencesEditor.putInt("Sunday", 0).apply()
            }

            6 -> {
                dayOfTheWeek = "Saturday"
                sharedPreferencesEditor.putInt("Saturday", stepsToday).apply()

                sharedPreferencesEditor.putInt("Sunday", 0).apply()
            }

            7 -> {
                dayOfTheWeek = "Sunday"
                sharedPreferencesEditor.putInt("Sunday", stepsToday).apply()
            }
        }



        if (sharedPreferences.getString("day", "someday") != dayOfTheWeek) {
            stepsToday = 0
            sharedPreferencesEditor.putString("day", dayOfTheWeek).apply()
        }

        // Get a map of package names to UsageStats objects
        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        var totalScreenTimeInMillis: Long = 0
        for (usageStats in usageStatsMap.values) {
            totalScreenTimeInMillis += usageStats.totalTimeInForeground
        }

        // Convert to desired units (e.g., minutes, hours)
        totalScreenTimeInMinutes = totalScreenTimeInMillis / (1000 * 60 * 60)


        remoteViews?.setTextViewText(R.id.tx_st_since, "since ${currentHour % 12} $ampm, yday...")
        remoteViews?.setTextViewText(
            R.id.btn_screentime,
            "Screen time ~ ${totalScreenTimeInMinutes.toString()}+ Hrs"
        )

    }


    private fun shareWidget(context: Context, bitmap: Bitmap) {
        val bitmapPath = MediaStore.Images.Media.insertImage(
            appContx.getContentResolver(), bitmap, "title", ""
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


    /*   private fun clickSound(context: Context) {

           mp = MediaPlayer.create(context, R.raw.click)
           mp.start()
           Handler(Looper.getMainLooper()).postDelayed(Runnable { mp.release() }, 3000)

       }*/

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


    private fun todaysDate(context: Context) {

        val c: Date = Calendar.getInstance().time
        val dfDate = SimpleDateFormat("dd", Locale.getDefault())
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


        if (MainActivity.tempC.length > 3) {
            remoteViews?.setTextViewText(
                R.id.tx_weather_icon_temp,
                MainActivity.tempC.substring(
                    0,
                    2
                ) + "°C"
            )
            remoteViews?.setTextViewText(
                R.id.tx_weather_icon_state,
                MainActivity.weatherIconState + "..,"
            )

            if (weatherIconID.startsWith("5"))
                remoteViews?.setImageViewResource(R.id.weather_icon, R.drawable.rain)
            if (weatherIconID.equals("800"))
                remoteViews?.setImageViewResource(R.id.weather_icon, R.drawable.clear_sky)
            if (weatherIconID.equals("801") || weatherIconID.equals("802") || weatherIconID.equals("803") || weatherIconID.equals(
                    "804"
                )
            )
                remoteViews?.setImageViewResource(R.id.weather_icon, R.drawable.clouds)
        } else {
            getWeatherData(false)
            if (MainActivity.tempC.length > 3) {
                remoteViews?.setTextViewText(
                    R.id.tx_weather_icon_temp,
                    MainActivity.tempC.substring(
                        0,
                        2
                    ) + "°C"
                )
                remoteViews?.setTextViewText(
                    R.id.tx_weather_icon_state,
                    MainActivity.weatherIconState + "..,"
                )

                if (weatherIconID.equals("801") || weatherIconID.equals("802") || weatherIconID.equals(
                        "803"
                    ) || weatherIconID.equals("804")
                )
                    remoteViews?.setImageViewResource(R.id.weather_icon, R.drawable.clouds)
            }
        }
        // remoteViews?.setTextViewText(R.id.tx_date, formattedDate)
        sharedPreferencesEditor.putBoolean("DateSet", true).apply()
        sharedPreferencesEditor.putString("fD", formattedDate).apply()
        remoteViews?.setTextViewText(R.id.tx_steps, "$dayOfTheWeek ~ " + stepsToday.toString())
        remoteViews?.setTextViewText(
            R.id.tx_day_date,
            SimpleDateFormat("EEE", Locale.getDefault()).format(c) +
                    "│" + formattedDate
        )

        remoteViews?.setTextViewText(R.id.tx_wish, timelyWish)
    }

    private fun launchApp(context: Context, pkgName: String) {
        val launchIntent: Intent = context.packageManager.getLaunchIntentForPackage(pkgName)!!
        context.startActivity(launchIntent)
    }


    private fun readApps() {

        val gson = Gson()
        val response: String = sharedPreferences.getString("MUA", "").toString()
        choosenApps = gson.fromJson(
            response,
            object : TypeToken<List<App?>?>() {}.type
        )

        appIndex = 0

        addAppInWidget(appContx, choosenApps)
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun addContactsInWidget(context: Context, favC: ArrayList<Contact>) {

        var bm: Bitmap


        remoteViews?.removeAllViews(R.id.ll_contacts)
        var childView = RemoteViews(context.packageName, R.layout.remote_view_layout)


        childView.setImageViewBitmap(
            R.id.new_imgv_id,
            drawableToBitmap(appContx, appContx.resources.getDrawable(R.drawable.contacts))
        )
        childView.setTextViewText(R.id.new_tx_close_id, "")
        childView.setTextViewText(R.id.new_tx_id, "")


        childView.setViewLayoutMargin(
            R.id.new_imgv_id,
            RemoteViews.MARGIN_TOP,
            10f,
            TypedValue.COMPLEX_UNIT_DIP
        )
        childView.setViewLayoutMargin(
            R.id.new_imgv_id,
            RemoteViews.MARGIN_START,
            10f,
            TypedValue.COMPLEX_UNIT_DIP
        )


        childView.setOnClickPendingIntent(
            R.id.new_imgv_id,
            getPendingSelfIntent(context, C_CLICKED)
        )
        remoteViews?.addView(R.id.ll_contacts, childView)


        for (i in 0 until favC.size) {

            val contentResolver: ContentResolver =
                appContx.getContentResolver() // Or getContext().getContentResolver()
            val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                contentResolver,
                Uri.parse(favC[i].image)
            )

            if (inputStream != null) {
                bm = BitmapFactory.decodeStream(inputStream)
                bm = drawableToBitmap(
                    appContx,
                    RoundedBitmapDrawableFactory.create(appContx.resources, bm)
                )

                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                bm = drawableToBitmap(
                    appContx,
                    appContx.resources.getDrawable(R.drawable.face_holder)
                )
            }

            //     rBm = drawableToBitmap(appContx, RoundedBitmapDrawableFactory.create(appContx.resources, bm))

            childView = RemoteViews(context.packageName, R.layout.remote_view_layout)

            if (i == 0) {
                CALL_CLICKED = C1_CLICK
                CLEAR_C_CLICKED = CL1_CLICK
            } else if (i == 1) {
                CALL_CLICKED = C2_CLICK
                CLEAR_C_CLICKED = CL2_CLICK
            } else if (i == 2) {
                CALL_CLICKED = C3_CLICK
                CLEAR_C_CLICKED = CL3_CLICK
            } else if (i == 3) {
                CALL_CLICKED = C4_CLICK
                CLEAR_C_CLICKED = CL4_CLICK
            } else if (i == 4) {
                CALL_CLICKED = C5_CLICK
                CLEAR_C_CLICKED = CL5_CLICK
            }

            childView.setOnClickPendingIntent(
                R.id.new_tx_close_id,
                getPendingSelfIntent(context, CLEAR_C_CLICKED)
            )

            childView.setOnClickPendingIntent(
                R.id.new_imgv_id,
                getPendingSelfIntent(context, CALL_CLICKED)
            )
            childView.setViewLayoutMargin(
                R.id.new_imgv_id,
                RemoteViews.MARGIN_TOP,
                10f,
                TypedValue.COMPLEX_UNIT_DIP
            )
            childView.setViewLayoutMargin(
                R.id.new_imgv_id,
                RemoteViews.MARGIN_START,
                10f,
                TypedValue.COMPLEX_UNIT_DIP
            )

            childView.setImageViewBitmap(
                R.id.new_imgv_id,
                bm.getCircledBitmap()
            )
            childView.setTextViewText(R.id.new_tx_id, favC[i].name.substring(0, 1).uppercase())


            remoteViews?.addView(R.id.ll_contacts, childView)
            appWidM.updateAppWidget(newAppWidget, remoteViews)


        }

        childView = RemoteViews(context.packageName, R.layout.remote_view_layout)

        childView.setViewLayoutMargin(
            R.id.new_imgv_id,
            RemoteViews.MARGIN_TOP,
            10f,
            TypedValue.COMPLEX_UNIT_DIP
        )
        childView.setViewLayoutMargin(
            R.id.new_imgv_id,
            RemoteViews.MARGIN_START,
            10f,
            TypedValue.COMPLEX_UNIT_DIP
        )


        childView.setImageViewResource(
            R.id.new_imgv_id,
            android.R.drawable.ic_input_add
        )

        childView.setTextViewText(R.id.new_tx_close_id, "")
        childView.setTextViewText(R.id.new_tx_id, "")

        childView.setOnClickPendingIntent(
            R.id.new_imgv_id, PendingIntent.getActivity(
                context, 2,
                Intent(context, DialogActivity::class.java).putExtra("DialogIntent", "PC"),
                PendingIntent.FLAG_IMMUTABLE
            )
        )


        remoteViews?.addView(R.id.ll_contacts, childView)
    }


    private fun sortApps(queryUsageStats: List<UsageStats>) {

        Collections.sort<UsageStats>(
            queryUsageStats
        ) { p1: UsageStats, p2: UsageStats ->
            p2.totalTimeInForeground.compareTo(p1.totalTimeInForeground)
            //   p1.name.compareTo(p2.name)
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

    @SuppressLint("Range")
    private fun greeting(context: Context, remoteViews: RemoteViews, timeOfDay: String) {

        timelyWish = timeOfDay

        val c: Cursor? = context.getContentResolver()
            .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null)
        c?.moveToFirst()
        gpName = c!!.getString(c.getColumnIndex("display_name"))
        c.close()

        if (timeOfDay == "Morni!")
            timelyWish = "\uD83C\uDF3B$timeOfDay "//, ${gpName.split(" ").get(0)}!"
        else if (timeOfDay == "Noon!")
            timelyWish = "☀\uFE0F$timeOfDay "//, ${gpName.split(" ").get(0)}!"
        else if (timeOfDay == "Eve!")
            timelyWish = "\uD83C\uDF41$timeOfDay "//, ${gpName.split(" ").get(0)}!"
        else if (timeOfDay == "Night!")
            timelyWish = "\uD83D\uDCA4$timeOfDay "//, ${gpName.split(" ").get(0)}!"

    }


    protected fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.setAction(action)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        var arrayListUsageStats: HashSet<AppUsage> = HashSet()
        lateinit var dayOfTheWeek: String
        var noRewards: Int = 0
        var tW: String = "..."
        lateinit var appWidM: AppWidgetManager
        var choosenApps: ArrayList<App> = ArrayList()
        var selectedApps: ArrayList<SelectedApp> = ArrayList()
        lateinit var selectedApp: Bitmap
        var newsList: ArrayList<String> =
            ArrayList()
        var newsLinks: ArrayList<String> =
            ArrayList()

        var newsBitmaps: ArrayList<Bitmap> =
            ArrayList()
        var primaryColor by Delegates.notNull<Int>()
        var secondaryColor by Delegates.notNull<Int>()
        var tertianaryColor by Delegates.notNull<Int>()
        var screenWidth by Delegates.notNull<Int>()
        var screenHeight by Delegates.notNull<Int>()
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


        @SuppressLint("UseCompatLoadingForDrawables")
        fun addAppInWidget(context: Context, fApps: ArrayList<App>) {

            for (i in 0 until fApps.size) {

                val d: Drawable = getAppIconFromPkg(context, fApps[i].pName)

                if (i == 0) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_app1,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                } else if (i == 1) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_app2,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    //   remoteViews!!.setTextViewText(R.id.tx_c2, fApps[1].name)
                } else if (i == 2) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_app3,
                        drawableToBitmap(
                            context,
                            appContx.resources.getDrawable(R.drawable.launch_e)
                        )
                    )
                    //     remoteViews!!.setTextViewText(R.id.tx_c3, fApps[2].name)
                } else if (i == 3) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_app4,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )

                    //   remoteViews!!.setTextViewText(R.id.tx_c4, fApps[3].name)
                } else if (i == 4) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_app5,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )

                    //   remoteViews!!.setTextViewText(R.id.tx_c4, fApps[3].name)
                }

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


        private var appIndex: Int = 0
        private var conIndex: Int = 0

        lateinit var newAppWidget: ComponentName


        private const val FAB_SHARE = "fabShare"
        private const val NEWS_CLICK = "newsClick"
        private const val NEWS_NEXT = "newsNext"
        private const val NEWS_PREV = "newsPrev"
        private const val WIFI_AUTO = "wifiAuto"
        private const val TORCH_STATE = "torch"

        //    private const val RL_INVERT = "rlInvert"
        private const val GET_WEATHER = "getWeather"
        private const val STEPS_NOW = "newSteps"
        private const val LOCK_PHONE = "lockPhone"
        private const val WALL_CHANGE = "wallChange"
        private const val SET_CLICKED = "setButtonClick"
        private const val APP1_CLICKED = "App1Clicked"
        private const val APP2_CLICKED = "App2Clicked"
        private const val APP4_CLICKED = "App4Clicked"
        private const val APP5_CLICKED = "App5Clicked"
        private const val APP6_CLICKED = "App6Clicked"
        private const val APP7_CLICKED = "App7Clicked"
        private const val APP8_CLICKED = "App8Clicked"
        private const val APP9_CLICKED = "App9Clicked"

        private const val C_CLICKED = "CClicked"

        private var CLEAR_C_CLICKED = "Clear_C_Clicked"
        private val CL1_CLICK = "CL1_CLICK"
        private val CL2_CLICK = "CL2_CLICK"
        private val CL3_CLICK = "CL3_CLICK"
        private val CL4_CLICK = "CL4_CLICK"
        private val CL5_CLICK = "CL5_CLICK"

        private var CALL_CLICKED = "CallClicked"
        private val C1_CLICK = "C1_CLICK"
        private val C2_CLICK = "C2_CLICK"
        private val C3_CLICK = "C3_CLICK"
        private val C4_CLICK = "C4_CLICK"
        private val C5_CLICK = "C5_CLICK"

    }


}