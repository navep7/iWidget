package com.belaku.homey


// Weather Key - 9fa8e101240ab18615e3133b051e767e

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
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
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.appWidM
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.getWeatherData
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.mBluetoothAdapter
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.newsIndex
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.MainActivity.Companion.twitterProfileName
import com.belaku.homey.MainActivity.Companion.weatherIconID
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.initialSteps
import com.belaku.homey.SetWallWorker.Companion.stepsToday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates


class NewAppWidget : AppWidgetProvider() {


    private lateinit var dNews: Drawable
    private var randomTweetIndex: Int = 0
    private lateinit var formattedDate: String
    private var timelyWish: String = ""
    private val TAG: String = "NewAppWidget LOG7"
    private lateinit var wD: String
    private lateinit var qT: String
    private lateinit var uT: String
    private lateinit var dU: String
    private var tW: String = "..."

    private lateinit var mp: MediaPlayer


    private var currentHour by Delegates.notNull<Int>()
    private var currentMin by Delegates.notNull<Int>()
    lateinit var gpName: String



    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        appContx = context!!
        onEn = true
        dNews = appContx.resources.getDrawable(R.drawable.face_holder)
        Log.d("onEnabled! - ", favContacts.size.toString())
        getWeatherData()
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
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_reminders,
                remindersPendingIntent
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_speech,
                PendingIntent.getActivity(
                    context,
                    25,
                    Intent(context, MainActivity::class.java).putExtra("STT", "SpeechToText"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val intentSTH = Intent(context, MainActivity::class.java)
            val strSTH = "Set Twitter Handle"
            intentSTH.putExtra("STH", strSTH)

            remoteViews?.setOnClickPendingIntent(
                R.id.twSettings,
                PendingIntent.getActivity(
                    context,
                    21,
                    intentSTH,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.fab_share,
                getPendingSelfIntent(context, FAB_SHARE)
            )



            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_news_next,
                getPendingSelfIntent(context, NEWS_NEXT)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgbtn_news_prev,
                getPendingSelfIntent(context, NEWS_PREV)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_news,
                getPendingSelfIntent(context, NEWS_CLICK)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.fab_wifi,
                getPendingSelfIntent(context, WIFI_AUTO)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.fab_torch,
                getPendingSelfIntent(context, TORCH_STATE)
            )


            val intent = Intent(context, MainActivity::class.java)
            val pendingIntentBluetooth = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews?.setOnClickPendingIntent(R.id.fab_blue, pendingIntentBluetooth)

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_steps,
                getPendingSelfIntent(context, STEPS_NOW)
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

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact,
                getPendingSelfIntent(context, C_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact1,
                getPendingSelfIntent(context, C1_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact2,
                getPendingSelfIntent(context, C2_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact3,
                getPendingSelfIntent(context, C3_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact4,
                getPendingSelfIntent(context, C4_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact5,
                getPendingSelfIntent(context, C5_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact6,
                getPendingSelfIntent(context, C6_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact7,
                getPendingSelfIntent(context, C7_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_contact8,
                getPendingSelfIntent(context, C8_CLICKED)
            )


            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }



        appWidgetManager.updateAppWidget(newAppWidget, remoteViews)

    }


    @SuppressLint("ResourceAsColor", "ResourceType")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub

        super.onReceive(context, intent)
        remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)

        Log.d(TAG, "onReceive ${intent.action}")




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
                tW = listTweets[randomTweetIndex]
                sharedPreferencesEditor.putString("tW", tW).apply()
            } else {
                tW = sharedPreferences.getString("tW", "").toString()
                if (tW.length == 0)
                    tW = listTweets[0]
            }
        }

        appContx = context
        readContacts()
        readApps()

        appIndex = 0

        var now = Calendar.getInstance()

        currentHour = now[Calendar.HOUR_OF_DAY]
        currentMin = now[Calendar.MINUTE]

        if (currentHour == 0)
            stepsToday = 0

        getScreenTime()

        val aiIntent = Intent(context, AiActivity::class.java)
        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        aiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val aiPendingIntent = PendingIntent.getActivity(
            context,
            0,
            aiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_reminders,
            remindersPendingIntent
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_speech,
            PendingIntent.getActivity(
                context,
                25,
                Intent(context, MainActivity::class.java).putExtra("STT", "SpeechToText"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val intentSTH = Intent(context, MainActivity::class.java)
        val strSTH = "Set Twitter Handle"
        intentSTH.putExtra("STH", strSTH)

        remoteViews?.setOnClickPendingIntent(
            R.id.twSettings,
            PendingIntent.getActivity(
                context,
                21,
                intentSTH,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.fab_share,
            getPendingSelfIntent(context, FAB_SHARE)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_news_next,
            getPendingSelfIntent(context, NEWS_NEXT)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgbtn_news_prev,
            getPendingSelfIntent(context, NEWS_PREV)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_news,
            getPendingSelfIntent(context, NEWS_CLICK)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.fab_wifi,
            getPendingSelfIntent(context, WIFI_AUTO)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.fab_torch,
            getPendingSelfIntent(context, TORCH_STATE)
        )

        val intentBluetooth = Intent(context, MainActivity::class.java)
        if (mBluetoothAdapter.isEnabled)
            intentBluetooth.putExtra("BLUE", "disable")
        else intentBluetooth.putExtra("BLUE", "enable")
        val pendingIntentBluetooth = PendingIntent.getActivity(
            context,
            0,
            intentBluetooth,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(R.id.fab_blue, pendingIntentBluetooth)

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_steps,
            getPendingSelfIntent(context, STEPS_NOW)
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact,
            getPendingSelfIntent(context, C_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact1,
            getPendingSelfIntent(context, C1_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact2,
            getPendingSelfIntent(context, C2_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact3,
            getPendingSelfIntent(context, C3_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact4,
            getPendingSelfIntent(context, C4_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact5,
            getPendingSelfIntent(context, C5_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact6,
            getPendingSelfIntent(context, C6_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact7,
            getPendingSelfIntent(context, C7_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_contact8,
            getPendingSelfIntent(context, C8_CLICKED)
        )

        var timeOfDay = if (currentHour >= 6 && currentHour < 12) {
            "Morni.."
        } else if (currentHour >= 12 && currentHour < 17) {
            "Noon.."
        } else if (currentHour >= 17 && currentHour < 21) {
            "Eve..,"
        } else {
            "Night.."
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            greeting(context, remoteViews!!, timeOfDay)
        }

        if (newsList.size > 1) {
            remoteViews?.setTextViewText(
                R.id.tx_news,
                Html.fromHtml(
                    "<u>" + (newsIndex + 1).toString() + ". " + newsList[newsIndex] + "</u>",
                    Html.FROM_HTML_MODE_LEGACY
                )
            )


            dNews = BitmapDrawable(newsBitmaps[newsIndex])


            remoteViews?.setImageViewBitmap(
                R.id.imgv_news,
                drawableToBitmap(context, dNews)
            )


        }


        remoteViews?.setTextViewText(
            R.id.tx_desc_walltype,
            Html.fromHtml(
                wD + "<br>" + qT.split(" ")[0].substring(0, 1)
                    .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                Html.FROM_HTML_MODE_LEGACY
            )
        )

        remoteViews?.setTextViewText(
            R.id.tx_tweets,
            "@" + twitterProfileName + "\t ~ \t" + tW
        )
        //🖍


        todaysDate(context)


        if (FAB_SHARE == intent.action) {

        }





        if (GET_WEATHER == intent.action) {
            remoteViews?.setTextViewText(R.id.tx_weather_icon_temp, "")
            getWeatherData()
            remoteViews?.setTextViewText(
                R.id.tx_weather_icon_temp,
                MainActivity.tempC.substring(
                    0,
                    2
                ) + "°C"
            )
        }

        if (NEWS_CLICK == intent.action) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newsLinks[newsIndex]))
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContx.startActivity(browserIntent)
        }

        if (NEWS_NEXT == intent.action) {

            if (newsIndex < newsList.size - 1)
                newsIndex++
            else newsIndex = 0

            remoteViews?.setTextViewText(
                R.id.tx_news,
                Html.fromHtml(
                    "<u>" + (newsIndex + 1) + ". " + newsList[newsIndex] + "</u>",
                    Html.FROM_HTML_MODE_LEGACY
                )
            )


            dNews = BitmapDrawable(newsBitmaps[newsIndex])


            remoteViews?.setImageViewBitmap(
                R.id.imgv_news,
                drawableToBitmap(context, dNews)
            )

        }

        if (NEWS_PREV == intent.action) {

            if (newsIndex > 0)
                newsIndex--
            else newsIndex = newsList.size - 1

            remoteViews?.setTextViewText(
                R.id.tx_news,
                Html.fromHtml(
                    "<u>" + (newsIndex + 1) + ". " + newsList[newsIndex] + "</u>",
                    Html.FROM_HTML_MODE_LEGACY
                )
            )


            dNews = BitmapDrawable(newsBitmaps[newsIndex])


            remoteViews?.setImageViewBitmap(
                R.id.imgv_news,
                drawableToBitmap(context, dNews)
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
                return
            }
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var cameraId: String? = null
            try {
                cameraId = cameraManager.cameraIdList[0] // Typically the back camera
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                return
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
                remoteViews?.setTextViewText(R.id.tx_n_steps, "")
                remoteViews?.setViewVisibility(R.id.vertical_divider, View.INVISIBLE)
                //  remoteViews?.setTextViewText(R.id.tx_add_remove_newlap, "+")
            } else {
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

            var deviceManger =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            var compName = ComponentName(context, DeviceAdmin::class.java)
            val active: Boolean = deviceManger.isAdminActive(compName)

            if (active)
                deviceManger.lockNow()
        }

        if (SET_CLICKED == intent.action) {
            val launchIntent: Intent =
                context.packageManager.getLaunchIntentForPackage("com.belaku.homey")!!
            context.startActivity(launchIntent)
        }


        if (WALL_CHANGE == intent.action) {


            remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.VISIBLE)
            remoteViews?.setViewVisibility(R.id.imgbtn_set, View.INVISIBLE)

            for (i in 0 until selectedApps.size) {
                if (i == 0)
                    remoteViews?.setImageViewBitmap(R.id.imgv_app6, selectedApps[i].icon.getCircledBitmap())
                else if (i == 1)
                    remoteViews?.setImageViewBitmap(R.id.imgv_app7, selectedApps[i].icon.getCircledBitmap())
                else if (i == 2)
                    remoteViews?.setImageViewBitmap(R.id.imgv_app8, selectedApps[i].icon.getCircledBitmap())
                else if (i == 3)
                    remoteViews?.setImageViewBitmap(R.id.imgv_app9, selectedApps[i].icon.getCircledBitmap())
            }
       //     appWidM.updateAppWidget(newAppWidget, remoteViews)

            Thread {
                SetWallWorker.setWall(true)
            }.start()

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
                context.startActivity(Intent(context, AppChooserDialog::class.java).putExtra("id", 6).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        if (APP7_CLICKED == intent.action) {
            if (selectedApps.size > 1) {
                var app = selectedApps[1]
                Log.d("APP7_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(Intent(context, AppChooserDialog::class.java).putExtra("id", 7).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }


        if (APP8_CLICKED == intent.action) {
            if (selectedApps.size > 2) {
                var app = selectedApps[2]
                Log.d("APP8_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(Intent(context, AppChooserDialog::class.java).putExtra("id", 8).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        if (APP9_CLICKED == intent.action) {
            if (selectedApps.size > 3) {
                var app = selectedApps[3]
                Log.d("APP9_CLICKED", app.pName)
                launchApp(context, app.pName)
            } else {
                context.startActivity(Intent(context, AppChooserDialog::class.java).putExtra("id", 9).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        if (C_CLICKED == intent.action) {
            val intentContacts = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            intentContacts.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContx.startActivity(intentContacts)
        }
        if (C1_CLICKED == intent.action) {
            dialPhoneNumber(context, favContacts.get(0).number)
        }
        if (C2_CLICKED == intent.action) {
            dialPhoneNumber(context, favContacts.get(1).number)
        }
        if (C3_CLICKED == intent.action) {
            dialPhoneNumber(context, favContacts.get(2).number)
        }
        if (C4_CLICKED == intent.action) {
            dialPhoneNumber(context, favContacts.get(3).number)
        }
        if (C5_CLICKED == intent.action) {
            if (favContacts.size > 4)
              dialPhoneNumber(context, favContacts.get(4).number)
            else selectContact()
        }
        if (C6_CLICKED == intent.action) {
            if (favContacts.size > 5)
            dialPhoneNumber(context, favContacts.get(5).number)
            else selectContact()
        }
        if (C7_CLICKED == intent.action) {
            if (favContacts.size > 6)
            dialPhoneNumber(context, favContacts.get(6).number)
            else selectContact()
        }
        if (C8_CLICKED == intent.action) {
            if (favContacts.size > 7)
            dialPhoneNumber(context, favContacts.get(8).number)
            else selectContact()
        }


        /*{
                context.startActivity(Intent(context, AppChooserDialog::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }*/

        for (i in 0 until selectedApps.size) {
            if (i == 0)
                remoteViews?.setImageViewBitmap(R.id.imgv_app6, selectedApps[i].icon.getCircledBitmap())
            else if (i == 1)
                remoteViews?.setImageViewBitmap(R.id.imgv_app7, selectedApps[i].icon.getCircledBitmap())
            else if (i == 2)
                remoteViews?.setImageViewBitmap(R.id.imgv_app8, selectedApps[i].icon.getCircledBitmap())
            else if (i == 3)
                remoteViews?.setImageViewBitmap(R.id.imgv_app9, selectedApps[i].icon.getCircledBitmap())
        }

        try {
            if (sharedPreferences.getBoolean("Blue", false))
                remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_on)
            else remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_off)
        } catch (ex: Exception) {
            makeToast("EXXx ${ex.message}")
        }


        newAppWidget = ComponentName(context, NewAppWidget::class.java)
        AppWidgetManager.getInstance(context).updateAppWidget(newAppWidget, remoteViews)


    }

    private fun selectContact() {

        makeToast("yet2Impl")
        MainActivity.pickContact()

    }


    private fun getScreenTime() {

        val usageStatsManager =
            appContx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Query for the last 24 hours
        val startTime = calendar.timeInMillis


        // Get a map of package names to UsageStats objects
        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        var totalScreenTimeInMillis: Long = 0
        for (usageStats in usageStatsMap.values) {
            totalScreenTimeInMillis += usageStats.totalTimeInForeground
        }

        // Convert to desired units (e.g., minutes, hours)
        val totalScreenTimeInMinutes = totalScreenTimeInMillis / (1000 * 60 * 60)

        var ampm: String

        if (currentHour < 12)
            ampm = "am"
        else ampm = "pm"

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
        context.startActivity(Intent.createChooser(shareIntent, "Share").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }


    /*   private fun clickSound(context: Context) {

           mp = MediaPlayer.create(context, R.raw.click)
           mp.start()
           Handler(Looper.getMainLooper()).postDelayed(Runnable { mp.release() }, 3000)

       }*/


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
            remoteViews?.setTextViewText(
                R.id.tx_place,
                "⚲ " + cityname
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
            getWeatherData()
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
                remoteViews?.setTextViewText(
                    R.id.tx_place,
                    cityname
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
        remoteViews?.setTextViewText(R.id.tx_steps, "Today, " + stepsToday.toString())
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

    private fun readContacts() {

        val gson = Gson()
        val response: String = sharedPreferences.getString("CTS", "").toString()
        favContacts = gson.fromJson(
            response,
            object : TypeToken<List<Contact?>?>() {}.type
        )

        conIndex = 0

        addContactInWidget(appContx, favContacts)
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

        if (timeOfDay.equals("Morni.."))
            timelyWish = "\uD83C\uDF3B$timeOfDay "//, ${gpName.split(" ").get(0)}!"
        else if (timeOfDay.equals("Noon.."))
            timelyWish = "\uFE0F$timeOfDay "//, ${gpName.split(" ").get(0)}!"
        else if (timeOfDay.equals("Eve..,"))
            timelyWish = "\uD83C\uDF41$timeOfDay "//, ${gpName.split(" ").get(0)}!"
        else if (timeOfDay.equals("Night.."))
            timelyWish = "\uD83D\uDCA4$timeOfDay "//, ${gpName.split(" ").get(0)}!"

    }


    protected fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.setAction(action)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
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
        var Apps: ArrayList<App> = ArrayList()
        var lapCount: Int = 0


        fun addContactInWidget(context: Context, favC: ArrayList<Contact>) {

            var bm: Bitmap
            var d: Drawable

            for (i in 0 until favC.size) {

                val contentResolver: ContentResolver =
                    appContx.getContentResolver() // Or getContext().getContentResolver()
                val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                    contentResolver,
                    Uri.parse(favC[i].image)
                )

                if (inputStream != null) {
                    bm = BitmapFactory.decodeStream(inputStream)
                    d = BitmapDrawable(bm)

                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    d = appContx.resources.getDrawable(R.drawable.face_holder)
                }


                if (i == 0) {
                    remoteViews!!.setViewVisibility(
                        R.id.rl_contact1,
                        View.VISIBLE
                    )
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_contact1,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    remoteViews!!.setTextViewText(R.id.tx_c1, favC[0].name)
                } else if (i == 1) {
                    remoteViews!!.setViewVisibility(
                        R.id.rl_contact2,
                        View.VISIBLE
                    )
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_contact2,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    remoteViews!!.setTextViewText(R.id.tx_c2, favC[1].name)
                } else if (i == 2) {
                    remoteViews!!.setViewVisibility(
                        R.id.rl_contact3,
                        View.VISIBLE
                    )
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_contact3,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    remoteViews!!.setTextViewText(R.id.tx_c3, favC[2].name)
                } else if (i == 3) {
                    remoteViews!!.setViewVisibility(
                        R.id.rl_contact4,
                        View.VISIBLE
                    )
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_contact4,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    remoteViews!!.setTextViewText(R.id.tx_c4, favC[3].name)
                } else if (i == 4) {
                    remoteViews!!.setViewVisibility(
                        R.id.rl_contact5,
                        View.VISIBLE
                    )
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_contact5,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    remoteViews!!.setTextViewText(R.id.tx_c5, favC[4].name)
                }

            }
        }

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
        private const val STEPS_NOW = "resetSteps"
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
        private const val C1_CLICKED = "C1Clicked"
        private const val C2_CLICKED = "C2Clicked"
        private const val C3_CLICKED = "C3Clicked"
        private const val C4_CLICKED = "C4Clicked"
        private const val C5_CLICKED = "C5Clicked"
        private const val C6_CLICKED = "C6Clicked"
        private const val C7_CLICKED = "C7Clicked"
        private const val C8_CLICKED = "C8Clicked"
    }


}