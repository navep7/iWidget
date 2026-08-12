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
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.icu.util.Calendar
import android.location.Address
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.text.Html
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
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
import com.belaku.homey.MainActivity.Companion.makeSnack
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
import com.belaku.homey.NewAppWidget.Companion.widgetContext
import com.belaku.homey.StepsService.Companion.choosenApps
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.util.Collections
import kotlin.properties.Delegates
import kotlin.random.Random


class SetWallWorker(context: Context?, workerParams: WorkerParameters?) :
    Worker(context!!, workerParams!!) {

    private lateinit var wallWorkerContext: Context

    private var isNetConnected: Boolean = false

    @RequiresApi(Build.VERSION_CODES.S)
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
            greeting()

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


        @RequiresApi(Build.VERSION_CODES.S)
        @SuppressLint("SetTextI18n")
        fun setWall(b: Boolean, wallWorkerContext: Context) {

            wm = WallpaperManager.getInstance(wallWorkerContext)
            wm.setWallpaperOffsetSteps(1F, 1F)

            ensureDimensions(wallWorkerContext)

            greeting()

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
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting wallpaper bitmap", e)
                    } finally {
                        if (tempFile.exists()) tempFile.delete()
                    }
                }

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

                remoteViews?.setViewVisibility(R.id.progressBar_cyclic_wallchange, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                
                newAppWidget = ComponentName(wallWorkerContext, NewAppWidget::class.java)
                appWidM = AppWidgetManager.getInstance(wallWorkerContext)
                appWidM.updateAppWidget(newAppWidget, remoteViews)


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

                            if (ids.size == 0) {
                                fabMain.text = "Add Widget to Homescreen"
                            }

                        }, 1000)
                    }
                } catch (ex: Exception) {

                }

                if (isPinNoteInitialized()) {
                    remoteViews?.setTextViewText(R.id.tx_runner, pinNote)
                }

                remoteViews?.setTextViewText(R.id.tx_walldesc, wD)
                remoteViews?.setTextViewText(
                    R.id.tx_walltype_updateinfo,
                    Html.fromHtml(
                        qT.split(" ")[0].substring(0, 1)
                            .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )

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


        @SuppressLint("Range")
        fun getFavoriteContacts() {

            favContacts.forEach { recycleBitmap(it.contactBitmap) }
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
                    phones?.close()
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


        fun appUsageStats(applicationContext: Context?) {

            if (UsageStatsChecker().hasUsageStatsPermission(applicationContext!!)) {

                cYear = Calendar.getInstance().get(Calendar.YEAR)
                cMonth = Calendar.getInstance().get(Calendar.MONTH)
                cDate = Calendar.getInstance().get(Calendar.DATE)

                beginCal.set(cYear, cMonth, cDate - 7, 0, 0)
                endCal.set(cYear, cMonth, cDate - 1, 0, 0)


                try {
                    StepsService.usageStatsManager =
                        applicationContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager // Context.USAGE_STATS_SERVICE);

                    val queryUsageStats = StepsService.usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        beginCal.timeInMillis,
                        endCal.timeInMillis
                    )

                    if (queryUsageStats != null) {
                        println("results for " + beginCal.time + " - " + endCal.time)
                        println("QUS SWW - " + queryUsageStats.size)
                        sortApps(queryUsageStats)

                        choosenApps.forEach { recycleBitmap(it.iconBitmap) }
                        choosenApps.clear()

                        val appNames = HashSet<String>()
                        for (i in 0 until queryUsageStats.size) {

                            val appName =
                                getAppNameFromPkg(
                                    applicationContext!!,
                                    queryUsageStats.get(i).packageName
                                )
                            val appPname = queryUsageStats.get(i).packageName
                            val appUsage =
                                formatMilliseconds(queryUsageStats[i].totalTimeInForeground)

                            Log.d(
                                "queryUsageStats",
                                "$appName ... - $i : " + queryUsageStats.get(i).totalTimeInForeground
                            )

                            //   if (queryUsageStats.get(i).totalTimeInForeground > 0)
                            if (!appName.contains("Launcher") || !appName.equals("Home"))
                                if (applicationContext.packageManager.getLaunchIntentForPackage(
                                        queryUsageStats[i].packageName
                                    ) != null
                                )
                                    if (appNames.add(appName)) {
                                        if (!hashSetAppUsage.any { it.appName == appName }) {
                                            Log.d("AddedAPP", queryUsageStats[i].packageName)
                                            hashSetAppUsage.add(
                                                AppUsage(
                                                    queryUsageStats[i].packageName,
                                                    formatMilliseconds(queryUsageStats[i].totalTimeInForeground)
                                                )
                                            )
                                        }

                                    }

                        }

                        hashSetAppUsage = hashSetAppUsage.sortedByDescending { 
                            val parts = it.usageTime.split(":")
                            if (parts.size >= 1) parts[0].trim().toIntOrNull() ?: 0 else 0
                        }
                            .toCollection(LinkedHashSet())

                        Log.d("hashSetAppUsagez ~ ", hashSetAppUsage.toString())
                        for (i in hashSetAppUsage) {
                            //   var appName = i.appName
                            //   var appPname = getPackageNameFromAppName(applicationContext!!, appName)

                            val appUsage = i.usageTime
                            val iconBitmap: Bitmap =
                                applicationContext!!.packageManager.getApplicationIcon(i.appName)
                                    .toBitmap()

                            if (choosenApps.size < 10) {
                                if (choosenApps.none {
                                        it.name == getAppNameFromPkg(
                                            applicationContext,
                                            i.appName
                                        )
                                    })
                                    choosenApps.add(
                                        App(
                                            getAppNameFromPkg(applicationContext, i.appName),
                                            i.appName,
                                            appUsage,
                                            iconBitmap
                                        )
                                    )
                            } else {
                                break
                            }
                        }


                        saveApps(choosenApps)
                    }
                } catch (e: Exception) {
                    // This catches the AppSearchException "Invalid cycle detected" which is a system bug
                    // in the AppsIndexer when processing Digital Wellbeing metadata.
                    Log.e(TAG, "System indexing error during UsageStats query: ${e}")
                }
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
