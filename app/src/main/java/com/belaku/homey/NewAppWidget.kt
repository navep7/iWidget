package com.belaku.homey


// Weather Key - 9fa8e101240ab18615e3133b051e767e

import android.Manifest
import android.R.attr.colorPrimary
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStats
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
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.getWeatherData
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.newsIndex
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.MainActivity.Companion.twitterProfileName
import com.belaku.homey.SetWallWorker.Companion.boolNewLap
import com.belaku.homey.SetWallWorker.Companion.initialSteps
import com.belaku.homey.SetWallWorker.Companion.steps
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import kotlin.properties.Delegates


class NewAppWidget : AppWidgetProvider() {


    private var randomTweetIndex : Int = 0
    private var newsStr: String = ""
    private lateinit var wifiManager: WifiManager
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
    var choosenApps: ArrayList<App> = ArrayList()
    lateinit var gpName: String


    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        appContx = context!!
        onEn = true
        Log.d("onEnabled! - ", favContacts.size.toString())
        getWeatherData()

    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        appContx = context!!
        makeToast("onDisabled!")
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

            /* val intentSD = Intent(
                 context,
                 DialogWidgetStepsActivity::class.java
             )
             val pendingIntent = PendingIntent.getActivity(context, 0, intentSD,
                 PendingIntent.FLAG_IMMUTABLE)
             remoteViews?.setOnClickPendingIntent(R.id.imgv_steps, pendingIntent)*/


            val intentSTH = Intent(context, MainActivity::class.java)
            val strSTH = "Set Twitter Handle"
            intentSTH.putExtra("STH", strSTH)


            remoteViews?.setOnClickPendingIntent(
                R.id.twSettings,
                PendingIntent.getActivity(context, 21, intentSTH, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.twShare,
                getPendingSelfIntent(context, TW_SHARE)
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
                R.id.rl_clocks,
                getPendingSelfIntent(context, RL_INVERT)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_steps,
                getPendingSelfIntent(context, STEPS_NOW)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.tx_placeandweather,
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
                R.id.imgv_add1,
                getPendingSelfIntent(context, APP1_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_add2,
                getPendingSelfIntent(context, APP2_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_add3,
                getPendingSelfIntent(context, APP3_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_add4,
                getPendingSelfIntent(context, APP4_CLICKED)
            )

            remoteViews?.setOnClickPendingIntent(
                R.id.imgv_add5,
                getPendingSelfIntent(context, APP5_CLICKED)
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

        remoteViews?.setColorInt(R.id.imgbtn_lock, "setColorFilter", primaryColor, tertianaryColor)
        remoteViews?.setColorInt(R.id.imgbtn_conf, "setColorFilter", tertianaryColor, primaryColor)
        remoteViews?.setColorInt(R.id.imgbtn_set, "setColorFilter", primaryColor, tertianaryColor)


        sharedPreferences = context.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        wD = sharedPreferences.getString("wD", "").toString()
        qT = sharedPreferences.getString("qT", "").toString()
        dU = sharedPreferences.getString("dU", "").toString()
        uT = sharedPreferences.getString("uT", "").toString()


        if (listTweets.size > 0) {
            if (intent.action.equals("newsNext") || intent.action.equals("newsPrev") || intent.action.equals("twShare"))
                tW = sharedPreferences.getString("tW", "").toString()
            else if (!intent.action.equals("wallChange")){
                Log.d(TAG + "TwAct", intent.action.toString())
                randomTweetIndex = (0..listTweets.size - 1).random()
                tW = listTweets[randomTweetIndex]
                sharedPreferencesEditor.putString("tW", tW).apply()
            }
        }

        appContx = context
        readContacts()
        readApps()

        appIndex = 0

        var now = Calendar.getInstance()

        currentHour = now[Calendar.HOUR_OF_DAY]
        currentMin = now[Calendar.MINUTE]


        val intentSTH = Intent(context, MainActivity::class.java)
        val strSTH = "Set Twitter Handle"
        intentSTH.putExtra("STH", strSTH)


        remoteViews?.setOnClickPendingIntent(
            R.id.twSettings,
            PendingIntent.getActivity(context, 21, intentSTH, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )


        remoteViews?.setOnClickPendingIntent(
            R.id.twShare,
            getPendingSelfIntent(context, TW_SHARE)
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
            R.id.rl_clocks,
            getPendingSelfIntent(context, RL_INVERT)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_steps,
            getPendingSelfIntent(context, STEPS_NOW)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.tx_placeandweather,
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
            R.id.imgv_add1,
            getPendingSelfIntent(context, APP1_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_add2,
            getPendingSelfIntent(context, APP2_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_add3,
            getPendingSelfIntent(context, APP3_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_add4,
            getPendingSelfIntent(context, APP4_CLICKED)
        )

        remoteViews?.setOnClickPendingIntent(
            R.id.imgv_add5,
            getPendingSelfIntent(context, APP5_CLICKED)
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

        var timeOfDay = if (currentHour >= 6 && currentHour < 12) {
            "Morning"
        } else if (currentHour >= 12 && currentHour < 17) {
            "Afternoon"
        } else if (currentHour >= 17 && currentHour < 21) {
            "Evening"
        } else {
            "Night"
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            greeting(context, remoteViews!!, timeOfDay)
        }

//        for (i in 0 until newsList.size)
        //          newsStr = newsStr + "\t\t\t\t\t | ${newsList.get(i)}"


        if (newsList.size > 1)
            remoteViews?.setTextViewText(
                R.id.tx_news,
                Html.fromHtml("<u>" + newsList[newsIndex] + "</u>", Html.FROM_HTML_MODE_LEGACY)
            )

        remoteViews?.setTextViewText(
            R.id.tx_desc_walltype,
            Html.fromHtml(wD + "\n" + qT.split(" ")[0].substring(0, 1)
                .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",  Html.FROM_HTML_MODE_LEGACY)
        )

        remoteViews?.setTextViewText(
            R.id.tx_tweets,
            tW
        )
        //🖍
        remoteViews?.setTextViewText(R.id.twUser, Html.fromHtml(" @${twitterProfileName}",  Html.FROM_HTML_MODE_LEGACY))


      /*  if (listTweets.size > 0) {

            remoteViews?.setTextViewText(
                R.id.tx_tweets,
                tW
            )
            remoteViews?.setTextViewText(R.id.twUser, Html.fromHtml(" @${twitterProfileName}  \uD83D\uDD8D ",  Html.FROM_HTML_MODE_LEGACY))
        }    else {
            remoteViews?.setTextViewText(
                R.id.twUser,
                "Set Twitter Handle"
            )
        }*/






        todaysDate(context)


        if (TW_SHARE == intent.action) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, tW)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null) // "null" for default title
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContx.startActivity(shareIntent)
        }

        if (GET_WEATHER == intent.action) {
            getWeatherData()
        }

        if (NEWS_CLICK == intent.action) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newsLinks[newsIndex]))
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContx.startActivity(browserIntent)
        }

        if (NEWS_NEXT == intent.action) {

            if (newsIndex == newsList.size - 1)
                newsIndex = 0
            else newsIndex++

            makeToast("showing $newsIndex of ${newsList.size}")

            makeToast(newsIndex.toString() + " n-I " + newsList.size)
            if (newsList.size > 1)
             //   remoteViews?.setTextViewText(R.id.tx_news, Html.fromHtml("<u>" + newsList[newsIndex] + "</u>", Html.FROM_HTML_MODE_LEGACY))
            else MainActivity.getNews()
        }

        if (NEWS_PREV == intent.action) {

            if (newsIndex == 1)
                newsIndex = newsList.size - 1
            else newsIndex--

            makeToast(newsIndex.toString() + " n-I " + newsList.size)
            if (newsList.size > 1)
                remoteViews?.setTextViewText(
                    R.id.tx_news,
                    Html.fromHtml("<u>" + newsList[newsIndex] + "</u>", Html.FROM_HTML_MODE_LEGACY)
                );
            else MainActivity.getNews()
        }

        if (WIFI_AUTO == intent.action) {

            var wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            wifiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContx.startActivity(wifiIntent)

        }




        if (RL_INVERT == intent.action) {
            if (sharedPreferences.getBoolean("dark", false)) {
                sharedPreferencesEditor.putBoolean("dark", false).apply()
                remoteViews?.setTextColor(
                    R.id.tx_news,
                    appContx.resources.getColor(android.R.color.white)
                )
                remoteViews?.setTextColor(
                    R.id.tx_tweets,
                    appContx.resources.getColor(android.R.color.white)
                )
                remoteViews?.setTextColor(
                    R.id.tx_placeandweather,
                    appContx.resources.getColor(android.R.color.white)
                )
                remoteViews?.setTextColor(
                    R.id.tx_day_date,
                    appContx.resources.getColor(android.R.color.white)
                )
                remoteViews?.setTextColor(
                    R.id.clock,
                    appContx.resources.getColor(android.R.color.white)
                )
                remoteViews?.setTextColor(
                    R.id.tx_wish,
                    appContx.resources.getColor(android.R.color.white)
                )
                remoteViews?.setTextColor(
                    R.id.tx_desc_walltype,
                    appContx.resources.getColor(android.R.color.white)
                )
            } else {
                sharedPreferencesEditor.putBoolean("dark", true).apply()

                remoteViews?.setTextColor(
                    R.id.tx_news,
                    appContx.resources.getColor(android.R.color.black)
                )
                remoteViews?.setTextColor(
                    R.id.tx_tweets,
                    appContx.resources.getColor(android.R.color.black)
                )
                remoteViews?.setTextColor(
                    R.id.tx_placeandweather,
                    appContx.resources.getColor(android.R.color.black)
                )
                remoteViews?.setTextColor(
                    R.id.tx_day_date,
                    appContx.resources.getColor(android.R.color.black)
                )
                remoteViews?.setTextColor(
                    R.id.clock,
                    appContx.resources.getColor(android.R.color.black)
                )
                remoteViews?.setTextColor(
                    R.id.tx_wish,
                    appContx.resources.getColor(android.R.color.black)
                )
                remoteViews?.setTextColor(
                    R.id.tx_desc_walltype,
                    appContx.resources.getColor(android.R.color.black)
                )
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
                initialSteps = steps
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

            Thread {
                SetWallWorker.setWall()
            }.start()


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

        if (APP3_CLICKED == intent.action) {
            var app = choosenApps[2]
            Log.d("APP3_CLICKED", app.name)
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


        newAppWidget = ComponentName(context, NewAppWidget::class.java)
        AppWidgetManager.getInstance(context).updateAppWidget(newAppWidget, remoteViews)

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
        val df = SimpleDateFormat("dd MMM", Locale.getDefault())

        if (sharedPreferences.getBoolean("DateSet", false)) {
            var newfD = df.format(c)
            if (sharedPreferences.getString("fD", "") != newfD)
                MainActivity.getWeatherData()

        }

        formattedDate = df.format(c)

        //   remoteViews?.setTextViewText(R.id.tx_cityname, MainActivity.cityname)

        if (MainActivity.tempC.length > 3)
            remoteViews?.setTextViewText(
                R.id.tx_placeandweather,
                cityname + " | " + MainActivity.tempC.substring(
                    0,
                    4
                ) + "° C" + " | " + MainActivity.tempKind
            )
        else {
            MainActivity.getWeatherData()
            if (MainActivity.tempC.length > 3)
                remoteViews?.setTextViewText(
                    R.id.tx_placeandweather,
                    cityname + " | " + MainActivity.tempC.substring(
                        0,
                        4
                    ) + "° C" + " | " + MainActivity.tempKind
                )
        }
        // remoteViews?.setTextViewText(R.id.tx_date, formattedDate)
        sharedPreferencesEditor.putBoolean("DateSet", true).apply()
        sharedPreferencesEditor.putString("fD", formattedDate).apply()
        remoteViews?.setTextViewText(R.id.tx_steps, "Today, " + steps.toString())
        remoteViews?.setTextViewText(
            R.id.tx_day_date,
            SimpleDateFormat("EEE", Locale.getDefault()).format(c) +
                    " | " + formattedDate
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
        c?.close()

        if (timeOfDay.equals("Morning"))
            timelyWish = "$timeOfDay, ${gpName.split(" ").get(0)}  \uD83C\uDF3B "
        else if (timeOfDay.equals("Afternoon"))
            timelyWish = "$timeOfDay, ${gpName.split(" ").get(0)}  ☀\uFE0F "
        else if (timeOfDay.equals("Evening"))
            timelyWish = "$timeOfDay, ${gpName.split(" ").get(0)}  \uD83C\uDF41 "
        else if (timeOfDay.equals("Night"))
            timelyWish = "$timeOfDay, ${gpName.split(" ").get(0)}  \uD83D\uDCA4 "


    }

    @SuppressLint("Range", "Recycle")
    private fun getGoogleProfileInfo(context: Context): String {

        val manager = AccountManager.get(context)
        val accounts = manager.getAccountsByType("com.google")
        val possibleEmails: MutableList<String?> = LinkedList()

        for (account in accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type
            // values.
            possibleEmails.add(account.name)
        }

        if (!possibleEmails.isEmpty() && possibleEmails[0] != null) {
            val email = possibleEmails[0]
            val parts: Array<String?> = email!!.split("@".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            return if (parts.size > 0 && parts[0] != null) parts[1].toString()
            else "null1"
        } else return "null2"
    }

    protected fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.setAction(action)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        //    var newsList: ArrayList<String> = Arrays.asList("News Headlines 1")
        var newsList: ArrayList<String> =
            ArrayList(mutableListOf(""))
        var newsLinks: ArrayList<String> =
            ArrayList(mutableListOf(""))
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
                    // Photo exists, proceed to use the inputStream (e.g., load into an ImageView)
                    // Example: Bitmap photo = BitmapFactory.decodeStream(inputStream);
                    // imageView.setImageBitmap(photo);
                    // Don't forget to close the inputStream when done

                    bm = BitmapFactory.decodeStream(inputStream)
                    d = BitmapDrawable(bm)

                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    // No photo available for this contact, handle accordingly (e.g., display a placeholder image)
                    // Example: imageView.setImageResource(R.drawable.placeholder_contact_photo);
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

        fun addAppInWidget(context: Context, fApps: ArrayList<App>) {

            for (i in 0 until fApps.size) {

                val d: Drawable = getAppIconFromPkg(context, fApps[i].pName)

                if (i == 0) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_add1,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                } else if (i == 1) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_add2,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    //   remoteViews!!.setTextViewText(R.id.tx_c2, fApps[1].name)
                } else if (i == 2) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_add3,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )
                    //     remoteViews!!.setTextViewText(R.id.tx_c3, fApps[2].name)
                } else if (i == 3) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_add4,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )

                    //   remoteViews!!.setTextViewText(R.id.tx_c4, fApps[3].name)
                } else if (i == 4) {
                    remoteViews!!.setImageViewBitmap(
                        R.id.imgv_add5,
                        drawableToBitmap(context, d).getCircledBitmap()
                    )

                    //   remoteViews!!.setTextViewText(R.id.tx_c4, fApps[3].name)
                }

            }
        }


        private fun drawableToBitmap(context: Context, drawable: Drawable): Bitmap {

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



        private const val TW_SHARE = "twShare"
        private const val NEWS_CLICK = "newsClick"
        private const val NEWS_NEXT = "newsNext"
        private const val NEWS_PREV = "newsPrev"
        private const val WIFI_AUTO = "wifiAuto"
        private const val RL_INVERT = "rlInvert"
        private const val GET_WEATHER = "getWeather"
        private const val STEPS_NOW = "resetSteps"
        private const val LOCK_PHONE = "lockPhone"
        private const val WALL_CHANGE = "wallChange"
        private const val SET_CLICKED = "setButtonClick"
        private const val APP1_CLICKED = "App1Clicked"
        private const val APP2_CLICKED = "App2Clicked"
        private const val APP3_CLICKED = "App3Clicked"
        private const val APP4_CLICKED = "App4Clicked"
        private const val APP5_CLICKED = "App5Clicked"

        private const val C1_CLICKED = "C1Clicked"
        private const val C2_CLICKED = "C2Clicked"
        private const val C3_CLICKED = "C3Clicked"
        private const val C4_CLICKED = "C4Clicked"
    }


}