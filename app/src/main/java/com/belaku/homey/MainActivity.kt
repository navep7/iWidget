package com.belaku.homey

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.Dialog
import android.app.ProgressDialog
import android.app.WallpaperManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.icu.util.Calendar
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.ContactsContract
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.belaku.homey.AppChooserDialog.Companion.choosenApps
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.newsLinks
import com.belaku.homey.NewAppWidget.Companion.newsList
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.screenHeight
import com.belaku.homey.NewAppWidget.Companion.screenWidth
import com.belaku.homey.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.color.DynamicColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {


    private lateinit var appWidM: AppWidgetManager
    private lateinit var editTextTwitterHandle: EditText
    private lateinit var twitterHandleDialog: View
    private lateinit var responseTweets: okhttp3.Response
    private lateinit var responseTweetID: okhttp3.Response
    private val tweets: ArrayList<String> = ArrayList()
    private lateinit var twitterID: String
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var btnContactsAccess: Button
    private lateinit var btnDialPhone: Button
    private lateinit var btnActRecognition: Button
    private lateinit var btnUsageStats: Button
    private val CONTACTS_P: Int = 1
    private val DIAL_P: Int = 2
    private val ACT_R: Int = 3
    private val LOC_P: Int = 4
    private val TAG: String = "MainActivity"
    private lateinit var pD: ProgressDialog
    private lateinit var frameMin: FrameLayout
    private lateinit var frameHour: FrameLayout
    private lateinit var frameDay: FrameLayout
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMin: FloatingActionButton
    private lateinit var fabHour: FloatingActionButton
    private lateinit var fabDay: FloatingActionButton
    private lateinit var rvAdapter: RvAdapter
    private lateinit var rv: RecyclerView
    private lateinit var editTextPrompt: EditText
    private var pexelUrl: String =
        "https://api.pexels.com/v1/search?query=$queryType&per_page=10"

    private val RESULT_ENABLE: Int = 1
    private val MY_PERMISSIONS_REQUEST_READ_CONTACTS: Int = 1
    private lateinit var binding: ActivityMainBinding


    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAct = this@MainActivity
        appContx = applicationContext

        if (intent != null)
            if (intent.getStringExtra("STH") != null) {
                makeToast(intent.getStringExtra("STH").toString())
                if (intent.getStringExtra("STH").equals("Set Twitter Handle"))
                showTwitterHandleDialog()
                else makeToast("yet2Impl")
            }

        setSupportActionBar(binding.toolbar)


        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()


        parentLayout = findViewById(android.R.id.content);

        pD = ProgressDialog(this@MainActivity)
        pD.setMessage("fetching Walls...")

        val metrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(metrics)
        screenHeight = metrics.heightPixels
        screenWidth = metrics.widthPixels

        DynamicColors.applyToActivitiesIfAvailable(application)

        queryType = sharedPreferences.getString("walltype", "nature").toString()

        sharedPreferences.getStringSet("walls", null)?.let { imgUrls.addAll(it) }
        sharedPreferences.getStringSet("wallDescs", null)?.let { imgDescs.addAll(it) }

        connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        findViewByIds()
        setRV(imgUrls, imgDescs)
        listeners()
        fetchWallpaper(applicationContext)
        GetDisplayDimens()

  /*      Thread {
            //Do some Network Request
            getTweetID("iNaveenPrakash")
            runOnUiThread({
                //Update UI
            })
        }.start()*/


        cDate = Calendar.getInstance().get(Calendar.DATE) - 2
        cMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        cYear = Calendar.getInstance().get(Calendar.YEAR)

        getNews()
        apiTweets()
        pyTweets()

        intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        var compName = ComponentName(this, DeviceAdmin::class.java)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable Admin Access for Lock screen shortcut to work from the App's Widget"
        )

        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {

            }
        }

        // Receiver
        val getResult =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == Activity.RESULT_OK || it.resultCode == Activity.RESULT_CANCELED) {
                    ActivityCompat.requestPermissions(
                        mAct,
                        arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        CONTACTS_P
                    )
                }
            }

        getResult.launch(intent)

        sharedPreferencesEditor.putString("qT", queryType).apply()

        fabMain.setOnClickListener { view ->

            if (fabDay.visibility == View.GONE) {
                Snackbar.make(view, "Auto Update Wallpaper, every ?", Snackbar.ANIMATION_MODE_FADE)
                    .setAction("Action", null)
                    .setAnchorView(R.id.fab_main).show()
                fabDay.visibility = View.VISIBLE
                frameMin.visibility = View.VISIBLE
                frameHour.visibility = View.VISIBLE
                frameDay.visibility = View.VISIBLE
                // Add animation here to expand the menu
            } else {
                fabDay.visibility = View.GONE
                frameMin.visibility = View.GONE
                frameHour.visibility = View.GONE
                frameDay.visibility = View.GONE
                // Add animation here to collapse the menu
            }

        }


    }

    @SuppressLint("MissingInflatedId")
    private fun showTwitterHandleDialog() {
        val factory = LayoutInflater.from(this)
        twitterHandleDialog = factory.inflate(R.layout.twitter_handle_layout, null)
        val twitterDialog = AlertDialog.Builder(this).create()
        twitterDialog.setView(twitterHandleDialog)
        editTextTwitterHandle = twitterHandleDialog.findViewById<EditText>(R.id.edtx_th)
        twitterHandleDialog.findViewById<View>(R.id.btn_ok)
            .setOnClickListener { //your business logic
                getTweetID(editTextTwitterHandle.text.toString(), true)
                twitterDialog.dismiss()

            }
        twitterHandleDialog.findViewById<View>(R.id.btn_cancel)
            .setOnClickListener { twitterDialog.dismiss() }

        twitterDialog.show()
    }

    private fun getTweetID(uname: String, showPD: Boolean) {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://twitter241.p.rapidapi.com/user?username=$uname")
            .get()
            .addHeader("x-rapidapi-key", "9e92cc4f67msh8bb4ede93f53bf7p1ecb22jsn26ea5014a6df")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com")
            .build()


        pD.setTitle("fetching user ID...")
        if (showPD)
        pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
            responseTweetID = client.newCall(request).execute()

            withContext(Dispatchers.Main) {
                // Handle the result and hide the loading indicator
                if (showPD)
                pD.dismiss()
                val responseBodyString = responseTweetID.peekBody(Long.MAX_VALUE).string()
                Log.d("$TAG responseTweetID - ", responseBodyString)

                val jsonObject = JSONObject(responseBodyString)

                if (jsonObject.getJSONObject("result").getJSONObject("data").optString("user")
                        .isNotEmpty()
                ) {
                    twitterID = jsonObject.getJSONObject("result").getJSONObject("data")
                        .getJSONObject("user")
                        .getJSONObject("result").getString("rest_id")
                    twitterProfileName =
                        jsonObject.getJSONObject("result").getJSONObject("data")
                            .getJSONObject("user")
                            .getJSONObject("result").getJSONObject("core").getString("screen_name")
                    Log.d(TAG + "Tw ID - ", twitterID + " - " + twitterProfileName)

                    if (showPD)
                    makeSnack("Add Home Widget and Check Tweets in Widget!")
                    if (showPD)
                    pD.dismiss()


                    listTweets.clear()
                    getTweets(twitterID, showPD)
                } else {
                    if (showPD)
                    pD.dismiss()
                    makeSnack("Twitter User doesn't Exist!")

                }
                // Update UI with result
            }
        }


    }

    private fun getTweets(twitterID: String, showPD: Boolean) {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://twitter241.p.rapidapi.com/user-tweets?user=$twitterID&count=5")
            .get()
            .addHeader("x-rapidapi-key", "9e92cc4f67msh8bb4ede93f53bf7p1ecb22jsn26ea5014a6df")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com")
            .build()

        pD.setTitle("fetching Tweets...")
        if (showPD)
        pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
         responseTweets = client.newCall(request).execute()

            var js: JSONArray = (JSONObject(responseTweets.body?.string()).getJSONObject("result").getJSONObject("timeline")
                .getJSONArray("instructions"))//[2] as JSONObject).getJSONArray("entries")

            for (i in 0 until js.length()) {
                if (js[i].toString().contains("entries"))
                    js = (js[i] as JSONObject).getJSONArray("entries")
            }

            withContext(Dispatchers.Main) {
                if (showPD)
                pD.dismiss()
                for (i in 0 until js.length()) {
                val tw =
                    JSONObject(js[i].toString()).getJSONObject("content")//.getJSONObject("itemContent").getJSONObject("tweet_results").getJSONObject("result")
                //   .getJSONObject("legacy").get("full_text")

                if (tw.optString("itemContent").isNotEmpty()) {
                    val actTw = tw.getJSONObject("itemContent").getJSONObject("tweet_results")
                        .getJSONObject("result")
                        .getJSONObject("legacy").get("full_text")

                    Log.d("Twwtt $i", actTw.toString())
                    listTweets.add(actTw.toString())
                }


            }


                remoteViews = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)

                remoteViews?.setTextViewText(R.id.tx_tweets, listTweets[0])
                remoteViews?.setTextViewText(R.id.twUser, " @$twitterProfileName")
                appWidM = AppWidgetManager.getInstance(appContx)
                appWidM.updateAppWidget(newAppWidget, remoteViews)

            }
        }

        Log.d("result", "res - ${listTweets.size}")
    }

    private fun apiTweets() {


    }

    private fun rawTweets() {

        val dataArray: JSONArray = TweetsJsonParser.parseJsonArrayFromRaw(this, R.raw.np_tweets)!!

        if (dataArray != null) {
            makeToast("TsSize ${dataArray.length()}")
            for (i in 0 until dataArray.length()) {
                try {
                    val item = dataArray.getJSONObject(i)
                    val tweet = item.getString("text")
                    listTweets.add(tweet)

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            makeToast("0 T  -- ${listTweets.get(0)}")
        } else {
            Log.e(TAG, "Failed to parse JSON array from raw folder.")
        }
    }

    private fun pyTweets() {


    }


    @SuppressLint("MissingPermission")
    private fun getCity() {

        val task: Task<Location> =
            LocationServices.getFusedLocationProviderClient(this).lastLocation

        task.addOnSuccessListener { location ->
            if (location != null) {

                cityLat = location.latitude
                cityLng = location.longitude
                val geocoder = Geocoder(this, Locale.getDefault())

                val Adress = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                cityname =
                    Adress?.toString()?.split(",")?.get(2) ?: Adress?.get(0)?.locality.toString()



            }
        }


    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    private fun appUsageStats(applicationContext: Context?) {

        choosenApps.clear()

        val currentHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]

        val timeOfDay = if (currentHour >= 6 && currentHour < 12) {
            "Morning"
        } else if (currentHour >= 12 && currentHour < 17) {
            "Afternoon"
        } else if (currentHour >= 17 && currentHour < 21) {
            "Evening"
        } else {
            "Night"
        }


        val usageStatsManager =
            applicationContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager // Context.USAGE_STATS_SERVICE);


        val beginCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()
        if (timeOfDay.equals("Morning")) {
            beginCal.set(cYear, cMonth - 1, cDate, 9, 0)
            endCal.set(cYear, cMonth, cDate, 12, 0)
        } else if (timeOfDay.equals("Afternoon")) {
            beginCal.set(cYear, cMonth - 1, cDate, 12, 0)
            endCal.set(cYear, cMonth, cDate, 17, 0)
        } else if (timeOfDay.equals("Evening")) {
            beginCal.set(cYear, cMonth - 1, cDate, 17, 0)
            endCal.set(cYear, cMonth, cDate, 21, 0)
        } else if (timeOfDay.equals("Night")) {
            beginCal.set(cYear, cMonth - 1, cDate, 21, 0)
            endCal.set(cYear, cMonth - 1, cDate, 23, 57)
        }

        val queryUsageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            beginCal.timeInMillis,
            endCal.timeInMillis
        )
        println("results for " + beginCal.time.toGMTString() + " - " + endCal.time.toGMTString())
        println("QUS - " + queryUsageStats.size)
        sortApps(queryUsageStats)


        var appNames = HashSet<String>()
        for (i in 0 until queryUsageStats.size) {

            var appName = getAppNameFromPkg(applicationContext, queryUsageStats.get(i).packageName)
            var appPname = queryUsageStats.get(i).packageName

            Log.d(
                "queryUsageStats",
                "$appName ... - $i : " + queryUsageStats.get(i).totalTimeInForeground
            )

            if (queryUsageStats.get(i).totalTimeInForeground > 0)
                if (!appName.contains("Launcher"))
                    if (applicationContext.packageManager.getLaunchIntentForPackage(queryUsageStats[i].packageName) != null)
                        if (appNames.add(appName))
                            if (choosenApps.size < 5) {
                                choosenApps.add(
                                    App(
                                        appName, appPname
                                    )
                                )

                            }
        }
        saveApps(choosenApps)

    }


    @SuppressLint("Range", "UseCompatLoadingForDrawables")
    fun getFavoriteContacts(context: Context) {

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

        val cursor = context.contentResolver.query(
            queryUri,
            projection, selection, null, null
        )

        while (cursor!!.moveToNext()) {
            val contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            var phoneNumber: String = "7"

            if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                val phones: Cursor? = context.getContentResolver().query(
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

            var c = Contact(cNme, phoneNumber, cPhUri)

            favContacts.add(c)
        }

        saveContacts()

        cursor.close()




        for (i in 0 until favContacts.size) {

            Log.d(
                "cLog",
                "cName: ${favContacts.get(i).name}, cPic: ${favContacts.get(i).image}, cNum: ${
                    favContacts.get(i).number
                } "
            )


        }

    }

    private fun saveContacts() {

        val key = "CTS"

        val gson = Gson()
        val json = gson.toJson(favContacts)

        sharedPreferencesEditor.remove(key).commit()
        sharedPreferencesEditor.putString(key, json).commit()
    }

    private fun saveApps(apps: java.util.ArrayList<App>) {

        val key = "MUA"

        val gson = Gson()
        val json = gson.toJson(apps)

        sharedPreferencesEditor.remove(key).commit()
        sharedPreferencesEditor.putString(key, json).commit()
    }

    private fun sortApps(queryUsageStats: List<UsageStats>) {

        Collections.sort<UsageStats>(
            queryUsageStats
        ) { p1: UsageStats, p2: UsageStats ->
            p2.totalTimeInForeground.compareTo(p1.totalTimeInForeground)
            //   p1.name.compareTo(p2.name)
        }

    }


    private fun getAppIconFromPkg(context: Context, packageName: String?): Drawable? {
        try {
            val icon: Drawable =
                context.getPackageManager().getApplicationIcon(packageName.toString())
            return icon
        } catch (e: NameNotFoundException) {
            e.printStackTrace()
            return context.getDrawable(R.drawable.calls)
        }
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

    private fun setWalls(delay: Long) {

        appUsageStats(applicationContext)
        delayUnit = delay.toString()
        sharedPreferencesEditor.putString("dU", delayUnit).apply()
        sharedPreferencesEditor.putString("walltype", queryType).apply()

        startWallWork(delay)

    }

    private fun startWallWork(delay: Long) {


        val periodicWorkRequest =
            PeriodicWorkRequest.Builder(SetWallWorker::class.java, delay, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()

        val workManager = WorkManager.getInstance(applicationContext)

        workManager.enqueueUniquePeriodicWork(
            TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }


    private fun listeners() {

        editTextPrompt.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                //do what you want on the press of 'done'

                imgUrls.clear()
                imgDescs.clear()

                queryType = editTextPrompt.text.toString()
                sharedPreferencesEditor.putString("qT", queryType).apply()
                pD.show()
                sN.dismiss()
           //     fetchWallpaper(applicationContext)
            }
            false
        })

        fabMin.setOnClickListener {
            updateInterval = "min"
     //       makeToast("Wallpaper updates every 15 Mins!")
            wallDelay = 15
            setWalls(15)
            sharedPreferencesEditor.putStringSet("walls", HashSet(imgUrls)).apply()
            sharedPreferencesEditor.putStringSet("wallDescs", HashSet(imgDescs)).apply()
        }

        fabHour.setOnClickListener {
            updateInterval = "hour"
     //       makeToast("Wallpaper updates every 30 Mins!")
            setWalls(30)
            sharedPreferencesEditor.putStringSet("walls", HashSet(imgUrls)).apply()
            sharedPreferencesEditor.putStringSet("wallDescs", HashSet(imgDescs)).apply()
        }

        fabDay.setOnClickListener {
            updateInterval = "day"
    //        makeToast("Wallpaper updates every 60 Mins!")
            setWalls(60)
            sharedPreferencesEditor.putStringSet("walls", HashSet(imgUrls)).apply()
            sharedPreferencesEditor.putStringSet("wallDescs", HashSet(imgDescs)).apply()
        }

    }

    private fun findViewByIds() {

        editTextPrompt = findViewById(R.id.edtx_prompt)
        fabMain = findViewById(R.id.fab_main)
        frameMin = findViewById(R.id.frame_fab1)
        frameHour = findViewById(R.id.frame_fab2)
        frameDay = findViewById(R.id.frame_fab3)
        fabMin = findViewById(R.id.fab_option_1)
        fabHour = findViewById(R.id.fab_option_2)
        fabDay = findViewById(R.id.fab_option_3)

    }

    private fun GetDisplayDimens() {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        sharedPreferencesEditor.putInt("sWidth", displayMetrics.widthPixels).apply()
        sharedPreferencesEditor.putInt("sHeight", displayMetrics.heightPixels).apply()

    }


    override fun onDestroy() {
        super.onDestroy()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if (requestCode == LOC_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {

                }
        } else if (requestCode == CONTACTS_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    getFavoriteContacts(applicationContext)
                    getCity()
                    startStepsService()
                    usageStatsPermissionDialog()
                    getTweetID("Fact", false)
                }
        }
    }


    private fun startStepsService() {
        if (!isMyServiceRunning(StepsService::class.java)) {
            val intentSteps = Intent(this, StepsService::class.java)
            startForegroundService(intentSteps)
        }
    }


    fun fetchWallpaper(context: Context) {


        imgUrls.clear()
        imgDescs.clear()


        imgUrls.sort()
        imgDescs.sort()


        if (imgUrls.size == 0) {

            if (queryType.length != 0) {
                makeSnack("Showing $queryType wallpapers... Search using above Bar if you seek something else.. Or Set!")
                pexelUrl = "https://api.pexels.com/v1/search?query=$queryType&per_page=35"
                val request: StringRequest = @SuppressLint("NotifyDataSetChanged")
                object : StringRequest(

                    com.android.volley.Request.Method.GET, pexelUrl,
                    Response.Listener<String?> { response ->
                        try {
                            val jsonObject = JSONObject(response)

                            val jsonArray = jsonObject.getJSONArray("photos")

                            val length = jsonArray.length()


                            for (i in 0 until length) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val objectImages = jsonObject.getJSONObject("src")
                                imgUrls.add("$i + ${objectImages.getString("original")}")
                                imgDescs.add("$i + ${jsonObject.getString("alt")})")
                            }

                            rvAdapter.notifyItemRangeChanged(0, length)
                            pD.dismiss()


                        } catch (e: JSONException) {
                            makeToast("EXE7 - " + e.message)
                        }


                    }, object : Response.ErrorListener {
                        override fun onErrorResponse(error: VolleyError?) {
                            makeToast("onErrorResponse - " + error.toString())
                        }
                    }) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["Authorization"] =
                            "563492ad6f9170000100000123804538e2a24b5c9381b7c388de9f80"

                        return params
                    }
                }
                val requestQueue = Volley.newRequestQueue(context)
                requestQueue.add(request)
            } else makeToast("Please Search for the Walls using the above search bar..")
        }
    }

    private fun setRV(imgUrls: java.util.ArrayList<String>, imgDescs: ArrayList<String>) {

        rv = findViewById(R.id.rv_images)
        rv.layoutManager = StaggeredGridLayoutManager(2, 1)
        rvAdapter = RvAdapter(applicationContext, imgUrls, imgDescs)
        rv.adapter = rvAdapter
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    companion object {

        var wallDelay: Int = 0
        lateinit var twitterProfileName: String
        var listTweets: ArrayList<String> = ArrayList()
        private var cDate by Delegates.notNull<Int>()
        private var cMonth by Delegates.notNull<Int>()
        private var cYear by Delegates.notNull<Int>()
        private val newSAPIKEY: String = "3fa88b5851974caea39bcc59bd2e5746"
        var newsIndex: Int = 1
        private val TAG: String = "MainActTAG"
        lateinit var launcher: ActivityResultLauncher<Intent>
        var cityname: String = "cN"
        var cityLat: Double = 0.0
        var cityLng: Double = 0.0

        var tempC: String = ""
        var tempKind: String = ""
        lateinit var weatherData: WeatherData
        lateinit var sN: Snackbar
        @SuppressLint("StaticFieldLeak")
        lateinit var mAct: Activity
        @SuppressLint("StaticFieldLeak")
        lateinit var parentLayout: View
        @SuppressLint("StaticFieldLeak")
        lateinit var appContx: Context
        var delayUnit: String = ""
        var queryType: String = "Material Design"
        var updateTime: String = "00:00"
        var updateInterval: String? = null
        lateinit var sharedPreferences: SharedPreferences
        lateinit var sharedPreferencesEditor: SharedPreferences.Editor
        var randomNumber: Int = 0
        val imgUrls: ArrayList<String> = ArrayList()
        var imgDescs: ArrayList<String> = ArrayList()



        fun usageStatsPermissionDialog() {
            val alertDialog: AlertDialog = AlertDialog.Builder(mAct).create()
            alertDialog.setTitle("Permission Request")
            alertDialog.setMessage("App needs permission to get Usage stats to suggest you apps to use.. Permit ?")
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, "OK"
            ) { dialog, which ->
                val intent1 = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                appContx.startActivity(intent1.setFlags(FLAG_ACTIVITY_NEW_TASK))
                dialog.dismiss()
            }

            if (!getAdminStatus())
                alertDialog.show()

        }

        private fun getAdminStatus(): Boolean {
            val appOps = appContx.getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContx.packageName
            )
            return if (mode == AppOpsManager.MODE_DEFAULT) {
                appContx.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            } else {
                mode == MODE_ALLOWED
            }
        }


        fun makeToast(s: String) {
            Toast.makeText(appContx, s, Toast.LENGTH_SHORT).show()
            Log.d("makeToastinG", s)
        }

        fun makeSnack(s: String) {
            sN = Snackbar.make(parentLayout, s, Snackbar.LENGTH_INDEFINITE)
            sN.show()
            Log.d("makeToastinG", s)
        }


        fun showSelected(adapterPosition: Int) {

            var url = imgUrls[adapterPosition]
            url = url.split("+ ")[1]

            val dialog = Dialog(mAct)
            dialog.setContentView(R.layout.imgv_dialog_layout)
            dialog.setTitle("Title...")

            var image: ImageView = dialog.findViewById(R.id.imgv_dialog)
            var txt: TextView = dialog.findViewById(R.id.tx_dialog)
            var set: Button = dialog.findViewById(R.id.btn_set_dialog)

            set.setOnClickListener(View.OnClickListener {
                Thread {
                    val inputStream = URL(url).openStream()
                    WallpaperManager.getInstance(appContx).setStream(inputStream)
                }.start()
                Handler(Looper.getMainLooper()).postDelayed(Runnable { makeToast("Set!") }, 1000)

            })

            txt.text = imgDescs[adapterPosition].substring(4, imgDescs[adapterPosition].length)


            Glide.with(appContx)
                .load(url)
                .into(image)

            dialog.show()
        }

        @OptIn(DelicateCoroutinesApi::class)
        fun getWeatherData() {


            // Replace "CityName" with the desired city
            try {
                val weatherService = Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/data/2.5/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherService::class.java)

                GlobalScope.launch(Dispatchers.IO) {
                    val openWeatherApiKey = "9fa8e101240ab18615e3133b051e767e"
                    weatherData = weatherService.getWeather(
                        cityLat.toString(),
                        cityLng.toString(), openWeatherApiKey
                    )
                    withContext(Dispatchers.Main) {
                        //  updateUI(weatherData)
                        tempC = "${weatherData.main.temp - 273}°C"
                        tempKind = weatherData.weather.get(0).description

                        Log.d("weatherInfo", tempC + " - " + tempKind)
                       // makeToast("weatherInfo - " + tempC + " - " + tempKind)

                        sharedPreferencesEditor.putString(
                            "weatherTemp",
                            tempC
                        ).apply()
                    }
                }
            } catch (ex: Exception) {
                Log.d("WD Excep7 - ", ex.toString())
            }

         //   makeToast(tempC)

        }


        fun getNews() {

            newsList.toMutableList().clear()

            ApiUtilities.getApiInterface()
                ?.getNews("bangalore", "$cYear-$cMonth-$cDate", "publishedAt", "en", newSAPIKEY)
                ?.enqueue(object : Callback<MainNews> {

                    override fun onFailure(call: Call<MainNews>, t: Throwable) {

                        makeToast("onFailure - " + t.message)
                    }

                    override fun onResponse(
                        call: Call<MainNews>,
                        response: retrofit2.Response<MainNews>
                    ) {
                        //  newsList.toMutableList().clear()
                        if (response.isSuccessful) {
                            // makeToast("SZ - " + response.raw())
                            // makeToast("SZ - " + response.body()?.totalResults)
                            // makeToast("SZ - " + response.body()?.articles!!.size)
                            for (i in 0 until response.body()?.articles!!.size) {
                                newsList.add(response.body()?.articles!!.get(i).title)
                                newsLinks.add(response.body()?.articles!!.get(i).url)
                            }
                        }
                   //     makeToast("News - " + newsList.size)
                    }
                })

        }



    }

    // handle sensor event

}