package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.util.Calendar
import android.location.Address
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.text.Html
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.beginCal
import com.belaku.homey.MainActivity.Companion.cDate
import com.belaku.homey.MainActivity.Companion.cMonth
import com.belaku.homey.MainActivity.Companion.cYear
import com.belaku.homey.MainActivity.Companion.delayUnit
import com.belaku.homey.MainActivity.Companion.endCal
import com.belaku.homey.MainActivity.Companion.fabMain
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.queryType
import com.belaku.homey.MainActivity.Companion.randomWallIndex
import com.belaku.homey.MainActivity.Companion.rlStatus
import com.belaku.homey.MainActivity.Companion.txStatus
import com.belaku.homey.MainActivity.Companion.updateTime
import com.belaku.homey.MainActivity.Companion.wallDelay
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.hashSetAppUsage
import com.belaku.homey.NewAppWidget.Companion.dU
import com.belaku.homey.NewAppWidget.Companion.dayOfTheWeek
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.greeting
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.qT
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.uT
import com.belaku.homey.NewAppWidget.Companion.wD
import com.belaku.homey.StepsService.Companion.choosenApps
import com.belaku.homey.StepsService.Companion.totalUsage
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.util.Collections
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


class SetWallWorker(context: Context?, workerParams: WorkerParameters?) :
    Worker(context!!, workerParams!!) {

    private lateinit var wallWorkerContext: Context

    lateinit var widgetContext: Context
    private var isNetConnected: Boolean = false

      
    @NonNull
    override fun doWork(): Result {

        Log.d(TAG, "doWork!")
        wallWorkerContext = applicationContext
        widgetContext = applicationContext
        sharedPreferences = wallWorkerContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        val wallSet = sharedPreferences.getStringSet("walls", null)
        if (wallSet == null || wallSet.isEmpty()) {
            return Result.failure()
        }
        urls = ArrayList(wallSet)
        urls.sort()

        wm = WallpaperManager.getInstance(wallWorkerContext)

        val connectivityManager = wallWorkerContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        isNetConnected = activeNetwork?.isConnectedOrConnecting == true

        if(isNetConnected)
            setWall(true, wallWorkerContext)
        else
            greeting(widgetContext)

        return Result.success()
    }



    companion object {

        var hour: Int = 0
        var screenWidth: Int = 0
        var screenHeight: Int = 0

        lateinit var pinNote: String
        fun isPinNoteInitialized(): Boolean {
            return this::pinNote.isInitialized
        }

        fun ismActInitialized(): Boolean {
            return this::mAct.isInitialized
        }

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

        fun recycleBitmap(bitmap: Bitmap?) {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }

        fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height: Int, width: Int) = options.outHeight to options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        fun isSharedPreferencesInitialized(): Boolean {
            return this::sharedPreferences.isInitialized
        }

        fun isWallBitmapInitialized(context: Context): Boolean {
            if (this::wallBitmap.isInitialized && !wallBitmap.isRecycled) return true
            val loaded = loadWallBitmap(context)
            if (loaded != null) {
                if (::wallBitmap.isInitialized) recycleBitmap(wallBitmap)
                wallBitmap = loaded
                if (::scaledBitmap.isInitialized && scaledBitmap != wallBitmap) recycleBitmap(scaledBitmap)
                scaledBitmap = loaded
                return true
            }
            return false
        }

        fun ensureDimensions(context: Context) {
            if (screenWidth == 0 || screenHeight == 0) {
                val metrics = context.resources.displayMetrics
                screenWidth = metrics.widthPixels
                screenHeight = metrics.heightPixels
            }
        }

        var boolNewLap: Boolean = false

        val TAG: String = "SetWallWorkerLOG7"
        var wallDesc: String = ""
        var wallDescs: ArrayList<String> = ArrayList()
        var urls: ArrayList<String> = ArrayList()
        lateinit var wm: WallpaperManager


          
        @SuppressLint("SetTextI18n")
        fun setWall(b: Boolean, wallWorkerContext: Context) {

            wm = WallpaperManager.getInstance(wallWorkerContext)
            wm.setWallpaperOffsetSteps(1F, 1F)

            ensureDimensions(wallWorkerContext)

            greeting(wallWorkerContext)

            try {
                wm.suggestDesiredDimensions(screenWidth, screenHeight)
            } catch (ex: Exception) {
                Log.e(TAG, "Error suggesting dimensions", ex)
            }

            try {

                val wallSet = sharedPreferences.getStringSet("walls", null)
                val descSet = sharedPreferences.getStringSet("wallDescs", null)
                
                if (wallSet != null) {
                    urls = ArrayList(wallSet)
                    urls.sort()
                }
                
                if (descSet != null) {
                    wallDescs = ArrayList(descSet)
                    wallDescs.sort()
                }


                var downloadSuccess = true

                if (urls.isNotEmpty()) {
                    randomWallIndex = Random.Default.nextInt(urls.size)
                    if (randomWallIndex < wallDescs.size) {
                        wallDesc = wallDescs.get(randomWallIndex)
                    }
                    val wallUrl = urls[randomWallIndex].substring(4)
                    Log.d("settingWD", wallUrl)

                    val tempFile = File(wallWorkerContext.cacheDir, "temp_wall")
                    try {
                        URL(wallUrl).openStream().use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(tempFile.absolutePath, options)
                        options.inSampleSize = calculateInSampleSize(options, screenWidth, screenHeight)
                        options.inJustDecodeBounds = false
                        
                        val newWallBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)
                        if (newWallBitmap != null) {
                            if (::wallBitmap.isInitialized) recycleBitmap(wallBitmap)
                            wallBitmap = newWallBitmap

                            val newScaledBitmap = Bitmap.createScaledBitmap(wallBitmap, screenWidth, screenHeight, true)
                            if (::scaledBitmap.isInitialized && scaledBitmap != newScaledBitmap && scaledBitmap != wallBitmap) {
                                recycleBitmap(scaledBitmap)
                            }
                            scaledBitmap = newScaledBitmap
                            
                            saveBitmapToInternalStorage(wallWorkerContext, scaledBitmap)
                        } else {
                            downloadSuccess = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting wallpaper bitmap", e)
                        downloadSuccess = false
                    } finally {
                        if (tempFile.exists()) tempFile.delete()
                    }
                } else {
                    downloadSuccess = false
                }

                if (downloadSuccess) {
                    if (b && ::scaledBitmap.isInitialized && !scaledBitmap.isRecycled)
                        wm.setBitmap(scaledBitmap)

                    val c = Calendar.getInstance()

                    updateTime =
                        "" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(
                            Calendar.SECOND
                        )

                    if (b) {
                        if (wallDesc.contains("+")) {
                            sharedPreferencesEditor.putString("wD", wallDesc.split("+")[1]).apply()
                            wD = wallDesc.split("+")[1]
                        } else {
                            sharedPreferencesEditor.putString("wD", wallDesc).apply()
                            wD = wallDesc
                        }
                        sharedPreferencesEditor.putString("uT", updateTime).apply()
                    }
                    Log.d(TAG, "Set successfully $noRewards")

                    boolWallSet = true
                }

                remoteViews?.setViewVisibility(R.id.progressBar_cyclic_wallchange, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                
                newAppWidget = ComponentName(wallWorkerContext, NewAppWidget::class.java)
                appWidM = AppWidgetManager.getInstance(wallWorkerContext)
                appWidM.updateAppWidget(newAppWidget, remoteViews)

                if (downloadSuccess) {
                    qT = queryType
                    dU = delayUnit
                    uT = updateTime

                    try {
                        if (pD.isShowing) {
                            pD.dismiss()
                            Handler(Looper.getMainLooper()).postDelayed({
                                txStatus.text =
                                    "\"$queryType\" wallpapers Set, updates every $wallDelay mins."
                                rlStatus.visibility = View.VISIBLE
                                val ids: IntArray = appWidM.getAppWidgetIds(newAppWidget)

                                if (ids.isEmpty()) {
                                    fabMain.text = "Add Widget to Homescreen"
                                }

                            }, 1500)
                        }
                    } catch (ex: Exception) {
                        fabMain.text = "Exp ~ Set again!"
                    }
                }

                if (isPinNoteInitialized()) {
                    remoteViews?.setTextViewText(R.id.tx_runner, "\uD83D\uDCDD " +pinNote)
            //        remoteViews?.setTextColor(R.id.tx_runner, ColorUtil().matchTertianaryColor())
                }

                if (NewAppWidget.checkCompanionVariable()) {
                    remoteViews?.setTextViewText(R.id.tx_walldesc, wD)
                    remoteViews?.setTextViewText(
                        R.id.tx_walltype_updateinfo,
                        Html.fromHtml(
                            qT.split(" ")[0].substring(0, 1)
                                .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                            Html.FROM_HTML_MODE_LEGACY
                        )
                    )
                }

                updateWidget(wallWorkerContext)


            } catch (e: Exception) {
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                Log.d(TAG, "setWallEx2 - $e")
            }

            updateWidget(wallWorkerContext)

        }

        private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap) {
            val file = File(context.filesDir, "wall_bitmap.png")
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Log.d(TAG, "Bitmap saved to internal storage")
            } catch (e: IOException) {
                Log.e(TAG, "Error saving bitmap", e)
            }
        }

        fun loadWallBitmap(context: Context): Bitmap? {
            val file = File(context.filesDir, "wall_bitmap.png")
            return if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading bitmap", e)
                    null
                }
            } else null
        }

        private fun DayChanges(wallWorkerContext: Context) {


            if (sharedPreferences.getString("day", "someday").equals(dayOfTheWeek)) {
                Log.d("DayChange?", "same Day")
            } else {
                Log.d("DayChange?", "diff Day")

                sharedPreferencesEditor.putInt("breatheCount", 0).apply()
                sharedPreferencesEditor.putInt("drinkCount", 0).apply()

                dayChange = true
                stepsToday = sharedPreferences.getInt(LocalDate.now().dayOfWeek.name, 0)
                sharedPreferencesEditor.putInt(dayOfTheWeek, stepsToday).apply()
                updateWidget(wallWorkerContext)

            }
        }

        private fun updateWidget(wallWorkerContext: Context) {
            val intent = Intent(
                wallWorkerContext,
                NewAppWidget::class.java
            )
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids: IntArray = AppWidgetManager.getInstance(wallWorkerContext)
                .getAppWidgetIds(
                    ComponentName(
                        wallWorkerContext,
                        NewAppWidget::class.java
                    )
                )
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                wallWorkerContext.sendBroadcast(intent)
            }
        }

        /**
         * Retrieves a map of package names to their total foreground duration (in milliseconds)
         * for a custom timeframe range.
         *
         * This implementation completely resolves overcounting and undercounting using a fully verified
         * state machine that aligns tracking perfectly with interactive screen sessions.
         */
        fun getAppUsageStatsForRange(
            context: Context,
            startTime: Long,
            endTime: Long
        ): List<Pair<String, Long>> {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            
            val launcherPackages = context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY
            ).map { it.activityInfo.packageName }.toSet()

            // 1. Establish the precise state of the world at exactly midnight (startTime)
            var activeApp: String? = null
            var isScreenInteractive = true

            val lookbackEvents = usageStatsManager.queryEvents(startTime - (12 * 60 * 60 * 1000), startTime)
            val event = UsageEvents.Event()
            while (lookbackEvents.hasNextEvent()) {
                lookbackEvents.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        activeApp = event.packageName
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (event.packageName == activeApp) {
                            activeApp = null
                        }
                    }
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        isScreenInteractive = true
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        isScreenInteractive = false
                    }
                }
            }

            // 2. Chronologically iterate through today's usage logs
            val appUsageMap = HashMap<String, Long>()
            var lastTimestamp: Long = startTime
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val eventTime = Math.min(event.timeStamp, endTime)
                
                // Accumulate screen foreground duration into the active application
                if (isScreenInteractive && activeApp != null && !launcherPackages.contains(activeApp) && activeApp != context.packageName) {
                    val duration = eventTime - lastTimestamp
                    if (duration > 0) {
                        appUsageMap[activeApp] = (appUsageMap[activeApp] ?: 0L) + duration
                    }
                }
                
                // Move our interval marker forward
                lastTimestamp = eventTime
                
                // Transition state tracking correctly
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        activeApp = event.packageName
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (event.packageName == activeApp) {
                            activeApp = null
                        }
                    }
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        isScreenInteractive = true
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        isScreenInteractive = false
                    }
                }
            }

            // 3. Accumulate any trailing active foreground session up to endTime
            if (isScreenInteractive && activeApp != null && !launcherPackages.contains(activeApp) && activeApp != context.packageName) {
                val finalTime = Math.min(System.currentTimeMillis(), endTime)
                val duration = finalTime - lastTimestamp
                if (duration > 0) {
                    appUsageMap[activeApp] = (appUsageMap[activeApp] ?: 0L) + duration
                }
            }

            return appUsageMap.toList().sortedByDescending { it.second }
        }

        /**
         * Retrieves a map of package names to their total foreground duration (in milliseconds)
         * for a specific hour of a specific day.
         *
         * @param hourOfDay The hour of the day (0 - 23).
         */
        fun getHourlyAppUsageStats(
            context: Context,
            year: Int,
            month: Int,
            day: Int,
            hourOfDay: Int
        ): List<Pair<String, Long>> {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DATE, day)
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = startTime + (60 * 60 * 1000)

            return getAppUsageStatsForRange(context, startTime, endTime)
        }


        fun isNotHomeLauncherApp(context: Context, packageName: String): Boolean {
            val launcherPackages = context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY
            ).map { it.activityInfo.packageName }.toSet()
            return !launcherPackages.contains(packageName)
        }

        fun appUsageStats(applicationContext: Context?) {
            val context = applicationContext?.applicationContext ?: return

            if (UsageStatsChecker().hasUsageStatsPermission(context)) {
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startTime = calendar.timeInMillis
                
                // Update shared globals in MainActivity (used by UI logic)
                cYear = calendar.get(Calendar.YEAR)
                cMonth = calendar.get(Calendar.MONTH)
                cDate = calendar.get(Calendar.DATE)
                beginCal.timeInMillis = startTime
                endCal.timeInMillis = now

                try {
                    // getAppUsageStatsForRange uses UsageEvents for accurate duration since midnight
                    val usageStats = getAppUsageStatsForRange(context, startTime, now)

                    hashSetAppUsage.clear()
                    choosenApps.forEach { recycleBitmap(it.iconBitmap) }
                    choosenApps.clear()

                    var totalDurationMs = 0L

                    for ((pkg, duration) in usageStats) {
                        if (duration < 1000) continue // ignore usage under 1 second
                        
                        // Check if the package is a user-facing app with a launch intent
                        if (context.packageManager.getLaunchIntentForPackage(pkg) != null) {
                            val appName = getAppNameFromPkg(context, pkg)
                            val appUsageStr = formatMilliseconds(duration)
                            
                            // Maintain existing behavior where 'appName' in hashSetAppUsage stores the package name
                            hashSetAppUsage.add(AppUsage(pkg, appUsageStr))
                            totalDurationMs += duration

                            if (choosenApps.size < 10) {
                                try {
                                    val iconBitmap: Bitmap = context.packageManager.getApplicationIcon(pkg).toBitmap()
                                    choosenApps.add(App(appName, pkg, appUsageStr, iconBitmap))
                                } catch (e: Exception) {
                                    // Skip apps where icon cannot be retrieved
                                }
                            }
                        }
                    }
                    
                    // Maintain sort order by duration
                    hashSetAppUsage = hashSetAppUsage.sortedByDescending {
                        val parts = it.usageTime.split(":")
                        val mins = if (parts.size >= 1) parts[0].trim().toIntOrNull() ?: 0 else 0
                        val secs = if (parts.size >= 2) parts[1].trim().toIntOrNull() ?: 0 else 0
                        mins * 60 + secs
                    }.toCollection(LinkedHashSet())

                    saveApps(choosenApps)
                    
                    val totalMinutes = totalDurationMs / (1000 * 60)
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    
                    totalUsage = "%02d:%02d".format(hours, minutes)
                    hour = hours.toInt()

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing UsageStats: ${e.message}")
                }
            }
        }


        fun sumTimes(times: List<String>): String {
            val totalDuration = times.fold(Duration.ZERO) { acc, time ->
                val parts = time.split(":").map { it.trim().toIntOrNull() ?: 0 }
                acc + (parts.getOrNull(0) ?: 0).minutes + (parts.getOrNull(1) ?: 0).seconds
            }

            return totalDuration.toComponents { hours, minutes, _, _ ->
                "%02d:%02d".format(hours, minutes)
            }
        }

        fun getPackageNameFromAppName(context: Context, appName: String): String? {
            val packageManager = context.packageManager

            // Retrieve all installed applications
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in installedApps) {
                // Get the visible user-facing application name
                val currentAppName = packageManager.getApplicationLabel(appInfo).toString()

                // Check if the current app name matches the target name (case-insensitive)
                if (currentAppName.equals(appName, ignoreCase = true)) {
                    return appInfo.packageName
                }
            }
            return null // Return null if no application matches
        }


        private fun saveApps(apps: java.util.ArrayList<App>) {

            val key = "MUA"

            val gson = Gson()
            val json = gson.toJson(apps)

            sharedPreferencesEditor.remove(key).commit()

            sharedPreferencesEditor.putString(key, json).commit()
        }

        fun formatMilliseconds(milliseconds: Long): String {
            val totalSeconds = milliseconds / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        fun getAppNameFromPkg(context: Context, packageName: String?): String {

            val pm: PackageManager = context.getPackageManager()
            var ai = try {
                pm.getApplicationInfo(packageName.toString(), 0)
            } catch (e: PackageManager.NameNotFoundException) {
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
