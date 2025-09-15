package com.belaku.homey

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.WallpaperManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.util.Calendar
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.JOB_SCHEDULER_SERVICE
import androidx.lifecycle.lifecycleScope
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.delayUnit
import com.belaku.homey.MainActivity.Companion.makeSnack
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.queryType
import com.belaku.homey.MainActivity.Companion.randomWallIndex
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.MainActivity.Companion.updateTime
import com.belaku.homey.MainActivity.Companion.wallDelay
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.screenHeight
import com.belaku.homey.NewAppWidget.Companion.screenWidth
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL
import kotlin.random.Random


class SetWallWorker(context: Context?, workerParams: WorkerParameters?) :
    Worker(context!!, workerParams!!) {


    private val appWidM: AppWidgetManager = AppWidgetManager.getInstance(appContx)

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

        WifiState()
   //     BluetoothState()

        return Result.success()
    }


    private fun WifiState() {
        var wTAG = "WifiState ~"

        var networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onLost(network: Network) {
                remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_off)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                Log.d(TAG, "WifiState called from onLost")
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onUnavailable() {
                remoteViews?.setColorInt(
                    R.id.fab_wifi,
                    "setColorFilter",
                    Color.YELLOW,
                    Color.YELLOW
                )
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                Log.d(wTAG, "WifiState OFF")
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onLosing(network: Network, maxMsToLive: Int) {
                remoteViews?.setColorInt(R.id.fab_wifi, "setColorFilter", Color.RED, Color.RED)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                Log.d(wTAG, "WifiState called from onLosing")
            }

            override fun onAvailable(network: Network) {
                Log.d(wTAG, "WifiState ON")
                remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_on)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                //record wi-fi connect event
            }
        }

        val connectivityManager =
            appContx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }


    companion object {

        lateinit var wallBitmap: Bitmap
        var boolNewLap: Boolean = false

        @kotlin.jvm.JvmField
        var stepsToday = 0
        var initialSteps = 0
        val TAG: String = "SetWallWorkerLOG7"
        var wallDesc: String = ""
        var wallDescs: ArrayList<String> = ArrayList()
        var urls: ArrayList<String> = ArrayList()
        lateinit var wm: WallpaperManager


        fun setWall(b: Boolean) {

            wm = WallpaperManager.getInstance(appContx)
            wm.setWallpaperOffsetSteps(1F, 1F);
            wm.suggestDesiredDimensions(screenWidth, screenHeight)

            try {

                urls = ArrayList(sharedPreferences.getStringSet("walls", null)!!)
                urls.sort()
                wallDescs = ArrayList(sharedPreferences.getStringSet("wallDescs", null)!!)
                wallDescs.sort()


                randomWallIndex = Random.Default.nextInt(urls.size)
                wallDesc = wallDescs.get(randomWallIndex)


                wallBitmap = BitmapFactory.decodeStream(
                    URL(
                        urls[randomWallIndex].substring(
                            4,
                            urls[randomWallIndex].length
                        )
                    ).openConnection().getInputStream()
                )

                val scaledBitmap =
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
                Log.d(TAG, "Set successfully")
                pD.dismiss()
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)

                var wD = wallDesc.split("+")[1]
                var qT = queryType
                var dU = delayUnit
                var uT = updateTime
                remoteViews?.setTextViewText(
                    R.id.tx_desc_walltype,
                    Html.fromHtml(
                        wD + "<br>" + qT.split(" ")[0].substring(0, 1)
                            .uppercase() + qT.split(" ")[0].substring(1) + "..,\t ||| \t" + dU + " mins, once.\t ||| \t" + "↺ @ $uT",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )

                val intent = Intent(appContx, NewAppWidget::class.java)
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                newAppWidget = ComponentName(appContx, NewAppWidget::class.java)
                //   intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, newAppWidget)
                appContx.sendBroadcast(intent)

                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    makeSnack("$queryType wallpapers Set, updates every $wallDelay mins. Add the Widget to see more of the Magic!")
                }, 1000)


            } catch (e: IOException) {
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                Log.d(TAG, "setWallEx2 - $e")
            }
            //   newAppWidget = ComponentName(appContx, NewAppWidget::class.java)
            appWidM.updateAppWidget(newAppWidget, remoteViews)

        }
    }

}


