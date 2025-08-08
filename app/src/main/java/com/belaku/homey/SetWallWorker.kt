package com.belaku.homey

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorManager
import android.icu.util.Calendar
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeSnack
import com.belaku.homey.MainActivity.Companion.queryType
import com.belaku.homey.MainActivity.Companion.randomNumber
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.MainActivity.Companion.updateTime
import com.belaku.homey.MainActivity.Companion.wallDelay
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import java.io.IOException
import java.net.URL
import kotlin.random.Random


class SetWallWorker(context: Context?, workerParams: WorkerParameters?) :
    Worker(context!!, workerParams!!) {


    private val appWidM: AppWidgetManager = AppWidgetManager.getInstance(appContx)

    @NonNull
    override fun doWork(): Result {

        Log.d(TAG, "doWork!")
        sharedPreferences = applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        urls = ArrayList(sharedPreferences.getStringSet("walls", null)!!)
        urls.sort()

        appContx = applicationContext
        wm = WallpaperManager.getInstance(appContx)
        setWall()

        WifiState()

        return Result.success()
    }

    private fun WifiState() {
        var wTAG = "WifiState ~"

         var networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onLost(network: Network) {
                remoteViews?.setImageViewResource(R.id.fab_wifi, R.drawable.wifi_off)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                Log.d(TAG, "NetworkCallback called from onLost")
            }
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onUnavailable() {
                remoteViews?.setColorInt(R.id.fab_wifi, "setColorFilter", Color.YELLOW, Color.YELLOW)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                Log.d(wTAG,"NetworkCallback OFF")
            }
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onLosing(network: Network, maxMsToLive: Int) {
                remoteViews?.setColorInt(R.id.fab_wifi, "setColorFilter", Color.RED, Color.RED)
                appWidM.updateAppWidget(newAppWidget, remoteViews)
                Log.d(wTAG,"NetworkCallback called from onLosing")
            }
            override fun onAvailable(network: Network) {
                Log.d(wTAG,"NetworkCallback ON")
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

        var boolNewLap: Boolean = false
        @kotlin.jvm.JvmField
        var steps = 0
        var initialSteps = 0
        val TAG: String = "SetWallWorker LOG7"
        var wallDesc: String = ""
        var wallDescs: ArrayList<String> = ArrayList()
        var urls: ArrayList<String> = ArrayList()
        lateinit var wm: WallpaperManager



        fun setWall() {
            wm = WallpaperManager.getInstance(appContx)

            try {

                urls = ArrayList(sharedPreferences.getStringSet("walls", null)!!)
                urls.sort()
                wallDescs = ArrayList(sharedPreferences.getStringSet("wallDescs", null)!!)
                wallDescs.sort()


                randomNumber = Random.Default.nextInt(urls.size)
                wallDesc = wallDescs.get(randomNumber)


                try {
                    val inputStream =
                        URL(urls[randomNumber].substring(4, urls[randomNumber].length)).openStream()
                    wm.setStream(inputStream)
                    // Uri.parse(urls[randomNumber].substring(4, urls[randomNumber].length))
                    val newurl = URL(urls[randomNumber].substring(4, urls[randomNumber].length))
                   /* val bitmapOptions = BitmapFactory.Options()
                    bitmapOptions.inSampleSize = 4
                    bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                    var mIcon_val = BitmapFactory.decodeStream(newurl.openConnection().getInputStream(), null, bitmapOptions)
                   remoteViews?.setImageViewBitmap(R.id.rl_widget_bg, mIcon_val)*/
                    var c = Calendar.getInstance()
                    updateTime =
                        "" + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(
                            Calendar.SECOND
                        )
                    sharedPreferencesEditor.putString("wD", wallDesc.split("+")[1]).apply()
                    sharedPreferencesEditor.putString("uT", updateTime).apply()
                    Log.d(TAG, "Set successfully")
                    remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)

                    val intent = Intent(appContx, NewAppWidget::class.java)
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    newAppWidget = ComponentName(appContx, NewAppWidget::class.java)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, newAppWidget)
                    appContx.sendBroadcast(intent)

                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        makeSnack("$queryType wallpapers Set, updates every $wallDelay mins. Add the Widget to see more of the Magic!")
                    }, 1000)


                } catch (ex: Exception) {
                    remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.INVISIBLE)
                    remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                    Log.d(TAG, "setWallEx1 - $ex")
                }

            } catch (e: IOException) {
                remoteViews?.setViewVisibility(R.id.progressBar_cyclic, View.INVISIBLE)
                remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                Log.d(TAG, "setWallEx2 - $e")
            }
            newAppWidget = ComponentName(appContx, NewAppWidget::class.java)
            AppWidgetManager.getInstance(appContx)
                .updateAppWidget(newAppWidget, remoteViews)

        }
    }

}


