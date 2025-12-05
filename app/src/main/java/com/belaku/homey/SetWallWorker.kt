package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.cDate
import com.belaku.homey.MainActivity.Companion.cMonth
import com.belaku.homey.MainActivity.Companion.cYear
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.delayUnit
import com.belaku.homey.MainActivity.Companion.fabMain
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.queryType
import com.belaku.homey.MainActivity.Companion.randomWallIndex
import com.belaku.homey.MainActivity.Companion.rlStatus
import com.belaku.homey.MainActivity.Companion.twitterProfileName
import com.belaku.homey.MainActivity.Companion.txStatus
import com.belaku.homey.MainActivity.Companion.updateTime
import com.belaku.homey.MainActivity.Companion.wallDelay
import com.belaku.homey.NewAppWidget.Companion.arrayListUsageStats
import com.belaku.homey.NewAppWidget.Companion.choosenApps
import com.belaku.homey.NewAppWidget.Companion.dU
import com.belaku.homey.NewAppWidget.Companion.dayOfTheWeek
import com.belaku.homey.NewAppWidget.Companion.formattedDate
import com.belaku.homey.NewAppWidget.Companion.getScreenTime
import com.belaku.homey.NewAppWidget.Companion.greeting
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.qT
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.screenHeight
import com.belaku.homey.NewAppWidget.Companion.screenWidth
import com.belaku.homey.NewAppWidget.Companion.tW
import com.belaku.homey.NewAppWidget.Companion.timelyWish
import com.belaku.homey.NewAppWidget.Companion.uT
import com.belaku.homey.NewAppWidget.Companion.wD
import com.belaku.homey.RemindersActivity.Companion.adapterHabits
import com.belaku.homey.RemindersActivity.Companion.arrayListHabits
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.io.IOException
import java.net.URL
import java.util.Collections
import java.util.Locale
import kotlin.random.Random


class SetWallWorker(context: Context?, workerParams: WorkerParameters?) :
    Worker(context!!, workerParams!!) {


    @RequiresApi(Build.VERSION_CODES.S)
    @NonNull
    override fun doWork(): Result {

        Log.d(TAG, "doWork!")
        appContx = applicationContext
        sharedPreferences = appContx.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        urls = ArrayList(sharedPreferences.getStringSet("walls", null)!!)
        urls.sort()

        wm = WallpaperManager.getInstance(appContx)

        setWall(true)
        getCity()
        DayChanges()
        greeting()

        return Result.success()
    }

    private fun DayChanges() {

        getScreenTime(applicationContext)
        if (sharedPreferences.getString("day", "someday").equals(dayOfTheWeek)) {
            Log.d("DayChange?", "same Day")
        } else {
            Log.d("DayChange?","diff Day")
            dayChange = true
            sharedPreferencesEditor.putInt(dayOfTheWeek, stepsToday).apply()
            stepsToday = 0
            for (i in arrayListHabits)
                i.isChecked = false
            adapterHabits.notifyDataSetChanged()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCity() {
        var locationRequest = LocationRequest.create()
        locationRequest.setInterval(30000)
        locationRequest.setSmallestDisplacement(1f)
        locationRequest.setFastestInterval(10000)
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)

        //instantiating the LocationCallBack
        val locationCallback = object : LocationCallback(), GoogleMap.OnMarkerClickListener {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    getAddress(location.latitude, location.longitude)
                }
            }

            override fun onMarkerClick(p0: Marker): Boolean {
            //    makeToast("nothin")
                return true
            }
        }

        var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mAct)

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun getAddress(latitude: Double, longitude: Double) {
        val gcd = Geocoder(applicationContext)
        Locale.getDefault()
        try {
            cAddrs = gcd.getFromLocation(latitude, longitude, 1)!!
            //   makeToast(cAddrs?.get(0)!!.subLocality)

            cityname = cAddrs?.get(0)!!.subLocality
         //   if (cityname.length > 15)
           //     cityname = cityname.substring(0, 12) + "..,"
            //   makeToast("cityname - " + cityname)


        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            makeToast("GCD - IOException \n $e")
        }

    }




    private fun updateWidget() {
        val intent = Intent(
            applicationContext,
            NewAppWidget::class.java
        )
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        val ids: IntArray = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(ComponentName(applicationContext, NewAppWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        applicationContext.sendBroadcast(intent)
    }


    companion object {
        lateinit var rActOpenedFirst: String
        lateinit var mAct: Activity
        var dayIndex: Int = -1
        var dayChange: Boolean = false
        lateinit var sharedPreferences: SharedPreferences
        lateinit var sharedPreferencesEditor: SharedPreferences.Editor

        var boolWallSet: Boolean = false
        lateinit var cAddrs: List<Address>
        lateinit var wallBitmap: Bitmap
        lateinit var scaledBitmap: Bitmap
        fun isWallBitmapInitialized(): Boolean {
            return this::wallBitmap.isInitialized
        }
        var boolNewLap: Boolean = false

        @kotlin.jvm.JvmField
        var stepsToday = 0

        val TAG: String = "SetWallWorkerLOG7"
        var wallDesc: String = ""
        var wallDescs: ArrayList<String> = ArrayList()
        var urls: ArrayList<String> = ArrayList()
        lateinit var wm: WallpaperManager


        @RequiresApi(Build.VERSION_CODES.S)
        @SuppressLint("SetTextI18n")
        fun setWall(b: Boolean) {

            wm = WallpaperManager.getInstance(appContx)
            wm.setWallpaperOffsetSteps(1F, 1F)


            getScreenTime(appContx)
            greeting()
            val metrics = DisplayMetrics()
            mAct.windowManager.getDefaultDisplay().getMetrics(metrics)
            screenHeight = metrics.heightPixels
            screenWidth = metrics.widthPixels
            wm.suggestDesiredDimensions(screenWidth, screenHeight)

            try {

                urls = ArrayList(sharedPreferences.getStringSet("walls", null)!!)
                urls.sort()
                wallDescs = ArrayList(sharedPreferences.getStringSet("wallDescs", null)!!)
                wallDescs.sort()


                randomWallIndex = Random.Default.nextInt(urls.size)
                wallDesc = wallDescs.get(randomWallIndex)

                Log.d("settingWD", urls[randomWallIndex])

                wallBitmap = BitmapFactory.decodeStream(
                    URL(
                        urls[randomWallIndex].substring(
                            4,
                            urls[randomWallIndex].length
                        )
                    ).openConnection().getInputStream()
                )

                scaledBitmap =
                    Bitmap.createScaledBitmap(wallBitmap, screenWidth, screenHeight, true)

                if (b)
                    wm.setBitmap(scaledBitmap)

                val c = Calendar.getInstance()
                updateTime =
                    "" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(
                        Calendar.SECOND
                    )

                if (b) {
                    sharedPreferencesEditor.putString("wD", wallDesc.split("+")[1]).apply()
                    sharedPreferencesEditor.putString("uT", updateTime).apply()
                }
                Log.d(TAG, "Set successfully $noRewards")
                boolWallSet = true


                wD = wallDesc.split("+")[1]
                qT = queryType
                dU = delayUnit
                uT = updateTime
                tW = listTweets[Random.nextInt(0, listTweets.size)]

                if (MainActivity.mainWindow.decorView.rootView.isShown)
                    if (pD.isShowing) {
                        pD.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            txStatus.text =
                                "\"$queryType\" wallpapers Set, updates every $wallDelay mins. \n Add the HomeScreen Widget to see more of the Magic!"
                            rlStatus.visibility = View.VISIBLE
                            fabMain.text = "How to ?"
                        }, 1000)
                    }



                remoteViews?.setTextViewText(
                    R.id.tx_tweets,
                    "@" + twitterProfileName + "\t ~ \t" + tW
                )
                //🖍
                remoteViews?.setTextViewText(R.id.tx_tweets, tW)
                NewAppWidget.greeting()
                remoteViews?.setTextViewText(R.id.tx_wish, timelyWish)
                NewAppWidget.todaysDate()
                remoteViews?.setTextViewText(
                    R.id.tx_day_date,
                    SimpleDateFormat(
                        "EEE",
                        Locale.getDefault()
                    ).format(Calendar.getInstance().time) +
                            ", " + formattedDate
                )
                remoteViews?.setTextViewText(
                    R.id.tx_desc_walltype,
                    Html.fromHtml(
                        wD + "<br>" + qT.split(" ")[0].substring(0, 1)
                            .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )


                /*val mSpannableStringLoc = SpannableString(cityname)
                mSpannableStringLoc.setSpan(UnderlineSpan(), 0, mSpannableStringLoc.length, 0)*/
                remoteViews?.setTextViewText(R.id.tx_place, cityname)


                val intent = Intent(appContx, NewAppWidget::class.java)
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                newAppWidget = ComponentName(appContx, NewAppWidget::class.java)
                //   intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, newAppWidget)

                appContx.sendBroadcast(intent)


            } catch (e: IOException) {
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                Log.d(TAG, "setWallEx2 - $e")
            }
            updateWidget()

        }

        private fun updateWidget() {
            val intent = Intent(
                appContx,
                NewAppWidget::class.java
            )
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids: IntArray = AppWidgetManager.getInstance(appContx)
                .getAppWidgetIds(
                    ComponentName(
                        MainActivity.Companion.appContx,
                        NewAppWidget::class.java
                    )
                )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            appContx.sendBroadcast(intent)
        }


        fun appUsageStats(applicationContext: Context?) {


            val usageStatsManager =
                applicationContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager // Context.USAGE_STATS_SERVICE);


            val beginCal = Calendar.getInstance()
            val endCal = Calendar.getInstance()

            cYear = Calendar.getInstance().get(Calendar.YEAR)
            cMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            cDate = Calendar.getInstance().get(Calendar.DATE)

            beginCal.set(cYear, cMonth - 1, cDate, 0, 0)
            endCal.set(cYear, cMonth, cDate, 0, 0)

            val queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginCal.timeInMillis,
                endCal.timeInMillis
            )
            println("results for " + beginCal.time + " - " + endCal.time)
            println("QUS SWW - " + queryUsageStats.size)
            sortApps(queryUsageStats)


            var appNames = HashSet<String>()
            for (i in 0 until queryUsageStats.size) {

                var appName =
                    getAppNameFromPkg(applicationContext, queryUsageStats.get(i).packageName)
                var appPname = queryUsageStats.get(i).packageName
                var appUsage = formatMilliseconds(queryUsageStats[i].totalTimeInForeground).substring(0, 2)
            //    var appUsage = arrayListUsageStats.elementAt(i).usageTime

                Log.d(
                    "queryUsageStats",
                    "$appName ... - $i : " + queryUsageStats.get(i).totalTimeInForeground
                )

                if (queryUsageStats.get(i).totalTimeInForeground > 0)
                    if (!appName.contains("Launcher") || !appName.equals("Home"))
                        if (applicationContext.packageManager.getLaunchIntentForPackage(
                                queryUsageStats[i].packageName
                            ) != null
                        )
                            if (appNames.add(appName)) {
                                arrayListUsageStats.add(
                                    AppUsage(
                                        queryUsageStats[i].packageName,
                                        formatMilliseconds(queryUsageStats[i].totalTimeInForeground)
                                    )
                                )
                            //    if (choosenApps.size < 5) {
                                    choosenApps.add(
                                        App(
                                            appName, appPname, appUsage
                                        )
                                    )
                                }
                          //  }
            }

            //   saveApps(choosenApps)

        }

        fun formatMilliseconds(milliseconds: Long): String {
            val totalSeconds = milliseconds / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        private fun getAppNameFromPkg(context: Context, packageName: String?): String {
            val pm: PackageManager = context.getPackageManager()
            var ai = try {
                pm.getApplicationInfo(packageName.toString(), 0)
            } catch (e: NameNotFoundException) {
                null
            }
            val applicationName =
                (if (ai != null) pm.getApplicationLabel(ai) else "(unknown)") as String

            return applicationName
        }

        private fun sortApps(queryUsageStats: List<UsageStats>) {

            Collections.sort<UsageStats>(
                queryUsageStats
            ) { p1: UsageStats, p2: UsageStats ->
                p2.totalTimeInForeground.compareTo(p1.totalTimeInForeground)
                //   p1.name.compareTo(p2.name)
            }

        }
    }

}


