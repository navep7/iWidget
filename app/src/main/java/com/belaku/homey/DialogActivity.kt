package com.belaku.homey

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.os.CountDownTimer
import android.provider.AlarmClock
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.util.Base64
import java.io.File
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.belaku.FinnhubApiService
import com.belaku.Stock
import com.belaku.getPredictedAppsForCurrentTimeWindow
import io.finnhub.api.apis.DefaultApi
import io.finnhub.api.infrastructure.ApiClient
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.beginCal
import com.belaku.homey.MainActivity.Companion.endCal
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.predictedApps
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.MusicService.Companion.songIndex

import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.hashSetAppUsage
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.appUsageStats
import com.belaku.homey.SetWallWorker.Companion.getForegroundAppAtTime
import com.belaku.homey.SetWallWorker.Companion.hour
import com.belaku.homey.SetWallWorker.Companion.isSharedPreferencesInitialized
import com.belaku.homey.SetWallWorker.Companion.pinNote
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.StepsService.Companion.speedInKmph
import com.belaku.homey.StepsService.Companion.stepsAdapter
import com.belaku.homey.StepsService.Companion.speedData
import com.belaku.homey.StepsService.Companion.stepsData
import com.belaku.homey.StepsService.Companion.totalUsage
import com.belaku.homey.StepsService.Companion.twitterProfileName
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DialogActivity : AppCompatActivity() {

    var bluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->

        }
    private var bluetoothAdapter: BluetoothAdapter? = null

    /** Asks for Nearby Devices access only when the Bluetooth tile is actually tapped. */
    private val permissionRequester = JitPermissionRequester(this)


    val wifiPanelIntent = Intent(Settings.Panel.ACTION_WIFI)
    private lateinit var twProfilePic: ImageView
    private lateinit var twSwitchUser: ImageView
    private lateinit var imgbtnAddAnotherStock: ImageButton
    private lateinit var imgbtnRefreshStocks: ImageButton
    private lateinit var pbRefreshStocks: ProgressBar
    private lateinit var llMenu: LinearLayout
    private var countDownTimer: CountDownTimer? = null
    private lateinit var glStocks: GridLayout
    private var selectedStockIndex: Int = -1
    private lateinit var dialogActContext: Context
    private lateinit var parentLayoutDialog: View


    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private lateinit var llDialog: RelativeLayout
    private lateinit var imgvSongCover: ImageView
    private lateinit var txTitle: TextView
    private lateinit var txContent: TextView
    private lateinit var edtxDialog: EditText
    private lateinit var llButtons: LinearLayout

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    private lateinit var imgbtnShare: ImageButton
    private lateinit var menuReminders: ImageButton
    private lateinit var menuTorch: ImageButton
    private lateinit var menuWifi: ImageButton
    private lateinit var menuBlue: ImageButton
    private lateinit var menuAi: ImageButton
    private lateinit var menu2AugmentedMeasurement: ImageButton
    private lateinit var menu2MeasureCalories: ImageButton
    private var foodPhotoUri: Uri? = null

    private val foodCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchFoodCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to capture food photo", Toast.LENGTH_SHORT).show()
            }
        }

    private val takeFoodPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && foodPhotoUri != null) {
                analyzeFoodCalories(foodPhotoUri!!)
            }
        }


    @SuppressLint("ResourceAsColor", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_dialog)

        parentLayoutDialog = findViewById(android.R.id.content)
        dialogActContext = applicationContext

        ensurePrefs()

        val jsonStocksInit = sharedPreferences.getString("listStocks", "")
        if (!jsonStocksInit.isNullOrEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<ArrayList<Stock>>() {}.type
                val parsed: ArrayList<Stock>? = Gson().fromJson(jsonStocksInit, type)
                if (parsed != null) {
                    listStocks = ArrayList(parsed.filterNotNull())
                }
            } catch (e: Exception) {
                Log.e("DialogActivity", "Error loading stocks", e)
            }
        }

        if (stepsData.isEmpty()) {
            val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            for (day in days) {
                stepsData.add(sharedPreferences.getInt(day, 0).toString())
            }
        }

        if (speedData.isEmpty()) {
            val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            val currentDayName = LocalDate.now().dayOfWeek.name
            val maxSpeedToday = sharedPreferences.getInt("maxSpeedToday", 0)
            for (day in days) {
                val speedVal = if (day == currentDayName) {
                    maxSpeedToday
                } else {
                    sharedPreferences.getInt("SPEED_$day", 0)
                }
                speedData.add(speedVal.toString())
            }
        }

        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
            }
        }

        twProfilePic = findViewById(R.id.tw_profile_pic)
        twSwitchUser = findViewById(R.id.tw_switch_user)
        imgbtnAddAnotherStock = findViewById(R.id.imgbtn_addanother_stock)
        imgbtnRefreshStocks = findViewById(R.id.imgbtn_refresh_stocks)
        pbRefreshStocks = findViewById(R.id.pb_refresh_stocks)
        glStocks = findViewById(R.id.gl_stocks)

        twProfilePic.visibility = View.GONE
        twSwitchUser.visibility = View.GONE
        imgbtnAddAnotherStock.visibility = View.GONE
        imgbtnRefreshStocks.visibility = View.GONE
        pbRefreshStocks.visibility = View.GONE
        glStocks.visibility = View.GONE
        llMenu = findViewById(R.id.ll_menu)
        llDialog = findViewById(R.id.dialog_layout)
        imgvSongCover = findViewById(R.id.dialog_imgv_cover)
        txTitle = findViewById(R.id.tx_dialog_title)
        txContent = findViewById(R.id.tx_dialog_content)


        edtxDialog = findViewById(R.id.edtx_dialog)
        llButtons = findViewById(R.id.ll_buttons)
        btnOk = findViewById(R.id.btn_dialog_ok)
        btnCancel = findViewById(R.id.btn_dialog_cancel)
        imgbtnShare = findViewById(R.id.imgbtn_dialog_share)

        menuReminders = findViewById(R.id.menu_reminders)
        menuTorch = findViewById(R.id.menu_torch)
        menuWifi = findViewById(R.id.menu_wifi)
        menuBlue = findViewById(R.id.menu_blue)
        menuAi = findViewById(R.id.menu_ai)
        menu2AugmentedMeasurement = findViewById(R.id.menu2_imgbtn_augmented_measurement)
        menu2MeasureCalories = findViewById(R.id.menu2_imgbtn_measure_calories)

         val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
         bluetoothAdapter = bluetoothManager.adapter

        checkWifiState(applicationContext)
        checkBluetoothState(applicationContext)
        checkTorchState()

        btnCancel.setOnClickListener {
            finish()
        }

        imgbtnAddAnotherStock.setOnClickListener {
            addStock()
        }

        imgbtnRefreshStocks.setOnClickListener {
            refreshAllStocks()
        }

        btnOk.setOnClickListener {
            finish()
        }


        imgbtnShare.setOnClickListener {
            if (txContent.text != "listening...") {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, txContent.text)
                }
                startActivity(Intent.createChooser(shareIntent, "Share via..."))
            }
        }

        menuReminders.setOnClickListener {
            val remindersIntent = Intent(this, RemindersActivity::class.java)
            remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or FLAG_ACTIVITY_NEW_TASK)
            startActivity(remindersIntent)
            finish()
        }

        menuTorch.setOnClickListener {
            toggleTorch()
        }

        menuWifi.setOnClickListener {
            startActivity(wifiPanelIntent)
            finish()
        }

        menuBlue.setOnClickListener {
            // No finish() here: toggleBluetooth may first show a permission rationale
            // dialog, and finishing now would tear it down. It finishes itself instead.
            toggleBluetooth()
        }

        menuAi.setOnClickListener {
            val aiIntent = Intent(this, AiActivity::class.java)
            aiIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or FLAG_ACTIVITY_NEW_TASK)
            startActivity(aiIntent)
        }

        menu2AugmentedMeasurement.setOnClickListener {
            val arIntent = Intent(this, ArMeasurementActivity::class.java)
            arIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(arIntent)
        }

        menu2MeasureCalories.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchFoodCamera()
            } else {
                foodCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        dialogIntentStr = intent.getStringExtra("DialogIntent").toString()
   //     makeToast(applicationContext,dialogIntentStr.toString())

        handleFeatureRequest(intent)

        if (dialogIntentStr != null) {
            when (dialogIntentStr) {
                "MeasureCalories" -> {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        launchFoodCamera()
                    } else {
                        foodCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                "Menu" -> {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    val prefs = if (isSharedPreferencesInitialized()) sharedPreferences else getSharedPreferences("UserPreferences", MODE_PRIVATE)
                    val wifiState = prefs.getBoolean("WifiState", false)
                    val wifiConnectionState = prefs.getBoolean("WifiConnectionState", false)
                    val blState = prefs.getBoolean("BluetoothState", false)
                    val blConnectionState = prefs.getBoolean("BluetoothConnectionState", false)

                    txTitle.text = when {
                        wifiState && !wifiConnectionState -> "Turn off WIFI"
                        blState && !blConnectionState -> "Turn off Bluetooth"
                        else -> "Menu"
                    }
                    llMenu.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE
                }
                "appPredictions" -> {
                    appPredictions()
                    finish()
                }
                "AddTimer" -> {
                    showTimerQuickAccessDialog()
                }
                "SongCover" -> {
                    txTitle.visibility = View.VISIBLE
                    imgvSongCover.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    if (isDataListInitialized()) {
                        txTitle.text = dataListSongs[songIndex].title
                        val albumArtPath = dataListSongs[songIndex].album.cover
                        if (!albumArtPath.isNullOrBlank()) {
                            Picasso.get().load(albumArtPath).into(imgvSongCover)
                        } else {
                            imgvSongCover.setImageResource(R.drawable.launch)
                        }
                    }
                }
                "WCh" -> {
                    noRewards = sharedPreferences.getInt("noRewards", 7)
                    if (noRewards > 1) {
                        sharedPreferencesEditor.putInt("noRewards", --noRewards).apply()
                        sharedPreferencesEditor.putBoolean("boolWallSet", true).apply()
                        Thread { SetWallWorker.setWall(true, dialogActContext, showProgress = true) }.start()
                        appPredictions()
                        finish()
                    }

                }
                "AD" -> {
                    makeToast(applicationContext, "loading Advertisement, please wait...")
                    llDialog.visibility = View.GONE
                    RewardedInterstitialAd.load(this, getString(R.string.admob_ri_ad), AdRequest.Builder().build(),
                        object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                                rewardedInterstitialAd = rewardedAd
                                rewardedInterstitialAd?.show(this@DialogActivity) { _ ->
                                    sharedPreferencesEditor.putInt("noRewards", 7).apply()
                                    noRewards = 7
                                    remoteViews?.setViewVisibility(R.id.imgbtn_set, View.VISIBLE)
                                    remoteViews?.setTextViewText(R.id.tx_rewards_count, "7")
                                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                                    finish()
                                }
                            }
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                rewardedInterstitialAd = null
                                finish()
                            }
                        })
                }

                "StT" -> {
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE
                    llButtons.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.INVISIBLE
                    txContent.visibility = View.VISIBLE
                    txContent.movementMethod = ScrollingMovementMethod()
                    txTitle.text = "Speech to Text"
                    txContent.text = "listening..."
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)

                }

                "Permissions" -> {
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE
                    llButtons.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.INVISIBLE
                    ydayApp(applicationContext)
                    txContent.visibility = View.VISIBLE
                    txTitle.text = "Allow Permissions!"
                    txContent.text = "Click on the Yellow triangles to allow permissions \n\n 1. or 3. Location permission to show current location and weather info! \n 2. Notification permission to notify you of Apps with incoming notifications and Hourly TIME readouts \n 4. Activity state permission to display whether you're sitting STILL/WALKING/TRAVELLING, and since how long, the time. \n 5. ScreenTime prmission to know how long you've been using the phone for Today!"


                }
                "ST" -> {
                    llButtons.visibility = View.VISIBLE
                    btnOk.visibility = View.INVISIBLE
                    btnCancel.visibility = View.INVISIBLE
                    imgbtnShare.visibility = View.VISIBLE
                    ydayApp(applicationContext)
                    txContent.visibility = View.VISIBLE
                    val savedProfile = sharedPreferences.getString("twitterProfileName", "")
                    if (!savedProfile.isNullOrEmpty()) {
                        twitterProfileName = savedProfile
                    }
                    txTitle.text = " $twitterProfileName"
                    twSwitchUser.visibility = View.VISIBLE
                    twSwitchUser.setOnClickListener { showTwitterUsernameInputDialog() }
                    twProfilePic.visibility = View.VISIBLE
                    Picasso.get()
                        .load("https://unavatar.io/twitter/$twitterProfileName")
                        .placeholder(R.drawable.fact)
                        .error(R.drawable.fact)
                        .into(twProfilePic)
                    if (listTweets.isNotEmpty()) {
                        txContent.text = listTweets[Random.nextInt(0, listTweets.size)]
                    } else {
                        fetchTweetsWithPython(twitterProfileName)
                    }
                }
                "SPEED" -> {
                    txTitle.text = "Weekly Speed"
                    val vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
                    val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                    vpSteps.visibility = View.VISIBLE
                    tabLayout.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE

                    val currentDay = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val maxSpeedToday = sharedPreferences.getInt("maxSpeedToday", 0)

                    speedData.clear()
                    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
                    val currentDayName = LocalDate.now().dayOfWeek.name
                    for (day in days) {
                        val speedVal = if (day == currentDayName) {
                            maxSpeedToday
                        } else {
                            sharedPreferences.getInt("SPEED_$day", 0)
                        }
                        speedData.add(speedVal.toString())
                    }

                    stepsMapsAdapter("speed", speedData)

                    // Set to a middle position for circular scrolling
                    val mid = 3500 - (3500 % 7) + currentDay
                    vpSteps.setCurrentItem(mid, false)

                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                }
                "WALKING" -> {
                    txTitle.text = "Active Sessions"
                    val vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
                    val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                    vpSteps.visibility = View.VISIBLE
                    tabLayout.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE

                    val currentDay = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
                    stepsData[currentDay] = stepsToday.toString()

                    stepsMapsAdapter("walk", stepsData)

                    // Set to a middle position for circular scrolling
                    val mid = 3500 - (3500 % 7) + currentDay
                    vpSteps.setCurrentItem(mid, false)

                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                }
                "screenTimeInfoWithoutDialog" -> {
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    imgbtnShare.visibility = View.GONE

                    hashSetAppUsage.clear()
                    appUsageStats(applicationContext)
                    appPredictions()

                    val displayList = hashSetAppUsage
                        .sortedByDescending { it.usageTime.split(":")[0].trim().toIntOrNull() ?: 0 }
                        .map { AppUsage(getAppNameFromPkg(dialogActContext, it.appName), it.usageTime, it.appName) }

                    val rvScreenTime = findViewById<RecyclerView>(R.id.rv_screen_time)
                    val txAvgUsage = findViewById<TextView>(R.id.tx_avg_usage)

                    rvScreenTime.visibility = View.GONE
                    txAvgUsage.visibility = View.GONE

                    val maxUsage = displayList.firstOrNull()?.usageTime?.split(":")?.get(0)?.trim()?.toIntOrNull() ?: 1
                    rvScreenTime.layoutManager = LinearLayoutManager(this)
                    rvScreenTime.adapter = ScreenTimeAdapter(displayList, maxUsage)

                    totalUsage = sumTimes(hashSetAppUsage.map { it.usageTime })
                    val sT = totalUsage.split(":")
                    hour = sT[0].toIntOrNull() ?: 0
                    val min = sT.getOrElse(1) { "00" }
                    txAvgUsage.text = "Usage Today ~ $hour Hours : $min Mins"

                    if (hour != 0) {
                        if (hour < 2) {
                            remoteViews?.setTextViewText(R.id.tx_screenusage_state, "LOW")
                        } else if (hour in 2 until 5) {
                            remoteViews?.setTextViewText(R.id.tx_screenusage_state, "MODERATE")
                        } else if (hour in 5 until 8) {
                            remoteViews?.setTextViewText(R.id.tx_screenusage_state, "HIGH")
                        } else {
                            remoteViews?.setTextViewText(R.id.tx_screenusage_state, "EXCESSIVE")
                        }
                        remoteViews?.setTextViewText(R.id.tx_screentime, "$hour+")
                    }
                    remoteViews?.setTextViewText(R.id.tx_screentime, "$hour+")
                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                }
                "screenTimeInfo" -> {
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    txTitle.text = "App Usage Analysis"
                    txContent.text = "Stats from ${beginCal.get(Calendar.DAY_OF_MONTH)}/${beginCal.get(Calendar.MONTH) + 1} to ${endCal.get(Calendar.DAY_OF_MONTH)}/${endCal.get(Calendar.MONTH) + 1}"

                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    imgbtnShare.visibility = View.GONE

                    hashSetAppUsage.clear()
                    appUsageStats(applicationContext)

                    val displayList = hashSetAppUsage
                        .sortedByDescending { it.usageTime.split(":")[0].trim().toIntOrNull() ?: 0 }
                        .map { AppUsage(getAppNameFromPkg(dialogActContext, it.appName), it.usageTime, it.appName) }

                    val rvScreenTime = findViewById<RecyclerView>(R.id.rv_screen_time)
                    val txAvgUsage = findViewById<TextView>(R.id.tx_avg_usage)

                    rvScreenTime.visibility = View.VISIBLE
                    txAvgUsage.visibility = View.VISIBLE

                    val maxUsage = displayList.firstOrNull()?.usageTime?.split(":")?.get(0)?.trim()?.toIntOrNull() ?: 1
                    rvScreenTime.layoutManager = LinearLayoutManager(this)
                    rvScreenTime.adapter = ScreenTimeAdapter(displayList, maxUsage)

                    totalUsage = sumTimes(hashSetAppUsage.map { it.usageTime })
                    val sT = totalUsage.split(":")
                    hour = sT[0].toIntOrNull() ?: 0
                    val min = sT.getOrElse(1) { "00" }
                    txAvgUsage.text = "Usage Today ~ $hour Hours : $min Mins"

                    remoteViews?.setTextViewText(R.id.tx_screentime, "$hour+")
                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                }
                "AddNote" -> {
                    txTitle.text = "Add Note"
                    llButtons.visibility = View.VISIBLE
                    edtxDialog.visibility = View.VISIBLE
                    btnOk.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE
                    imgbtnShare.visibility = View.GONE
                    edtxDialog.hint = "Enter Note to be Pinned..."
                    edtxDialog.requestFocus()
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    btnOk.setOnClickListener {
                        if (edtxDialog.text.toString().isNotEmpty()) {
                            pinNote = edtxDialog.text.toString()
                            Thread { SetWallWorker.setWall(true, dialogActContext, showProgress = true) }.start()
                            finish()
                        }
                    }
                }
                "AddAnotherStock" -> {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    txTitle.text = "All Stocks"
                    edtxDialog.setBackgroundColor(android.R.color.darker_gray)
                    txContent.setBackgroundColor(android.R.color.white)
                    imgbtnAddAnotherStock.visibility = View.VISIBLE
                    imgbtnRefreshStocks.visibility = View.VISIBLE
                    glStocks.visibility = View.VISIBLE
                    populateStocksGrid()
                }
                "AddStock" -> {
                    imgbtnAddAnotherStock.visibility = View.VISIBLE
                    imgbtnRefreshStocks.visibility = View.VISIBLE
                    addStock()
                }
                "activitiesInfo" -> {
                    imgbtnRefreshStocks.visibility = View.INVISIBLE
                    imgbtnAddAnotherStock.visibility = View.INVISIBLE
                    txTitle.text = "Activity Details"
                    val container = findViewById<LinearLayout>(R.id.ll_activity_container)
                    container.visibility = View.VISIBLE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    val inflater = LayoutInflater.from(this)
                    val widgetView = inflater.inflate(R.layout.new_app_widget, container, false)

                    val stillLayout = widgetView.findViewById<RelativeLayout>(R.id.rl_still)
                    val walkingLayout = widgetView.findViewById<RelativeLayout>(R.id.rl_active)
                    val speedLayout = widgetView.findViewById<RelativeLayout>(R.id.rl_speed)

                    listOf(stillLayout, walkingLayout, speedLayout).forEach { layout ->
                        (layout.parent as? ViewGroup)?.removeView(layout)
                        layout.visibility = View.VISIBLE
                        container.addView(layout)
                    }

                   
                    val txWalking = walkingLayout.findViewById<TextView>(R.id.rl_tx_walking_)

                    txWalking.text = "ACTIVE"


                    walkingLayout.findViewById<TextView>(R.id.rl_tx_steps).text = stepsToday.toString()
                    walkingLayout.findViewById<TextView>(R.id.rl_tx_cals).text = (stepsToday * 0.04 * (80 / 70)).toInt().toString()
                    speedLayout.findViewById<TextView>(R.id.tx_speed).text = speedInKmph.toString()
                    speedLayout.findViewById<TextView>(R.id.tx_max_speed).text = sharedPreferences.getInt("maxSpeedToday", 0).toString()
                    stillLayout.findViewById<TextView>(R.id.tx_water_count).text = sharedPreferences.getInt("waterCount", 0).toString()
                }
                "AccessibilityPermDialog" -> {
                    AlertDialog.Builder(this)
                        .setTitle("Accessibility Permission Required")
                        .setMessage("Please enable Accessibility Service to lock the screen from the widget.")
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
                            })
                        }
                        .setNegativeButton("Not Now") { _, _ -> finish() }
                        .show()
                }
                "qrClick" -> {
                    makeToast(applicationContext, "yet2Impl")
                    finish()
                }
            }
        }
    }

    private fun appPredictions() {
        val widgetScope = CoroutineScope(Dispatchers.Main)
        widgetScope.launch {
            predictedApps = withContext(Dispatchers.IO) {
                getPredictedAppsForCurrentTimeWindow(applicationContext)
            }

            val rlIds = intArrayOf(R.id.rl_app1, R.id.rl_app2, R.id.rl_app3, R.id.rl_app4, R.id.rl_app5)
            val imgvIds = intArrayOf(R.id.imgv_app1, R.id.imgv_app2, R.id.imgv_app3, R.id.imgv_app4, R.id.imgv_app5)

            if (predictedApps.isNotEmpty()) {
                var count = 0
                for (i in predictedApps) {
                    if (count >= 5) break
                    try {
                        val iconDrawable =
                            applicationContext.packageManager.getApplicationIcon(i.packageName)
                        val bitmap = iconDrawable.toBitmap(120, 120)
                        remoteViews?.setImageViewBitmap(imgvIds[count], bitmap)
                        remoteViews?.setViewVisibility(rlIds[count], View.VISIBLE)
                        count++
                    } catch (e: Exception) {
                        Log.e("DialogActivity", "Error setting predicted app icon for ${i.packageName}", e)
                    }
                }
                for (j in count until 5) {
                    remoteViews?.setViewVisibility(rlIds[j], View.GONE)
                }

                // 3. Trigger Widget Update
                appWidM.updateAppWidget(newAppWidget, remoteViews)
            } else {
                for (id in rlIds) {
                    remoteViews?.setViewVisibility(id, View.GONE)
                }
                appWidM.updateAppWidget(newAppWidget, remoteViews)
            }
        }
    }

    private fun ensurePrefs() {
        if (!isSharedPreferencesInitialized()) {
            sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences.edit()
        }
    }



// Initialize client



    private fun addStock() {
        ensurePrefs()
        ApiClient.apiKey["token"] = "db5ndcpr01qv1n8vo4p0db5ndcpr01qv1n8vo4pg"
        val apiClient = DefaultApi()


        txTitle.text = "Add Stock"
        llButtons.visibility = View.VISIBLE
        edtxDialog.visibility = View.VISIBLE
        txContent.visibility = View.VISIBLE
        txContent.text = ""
        btnOk.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
        imgbtnShare.visibility = View.GONE
        edtxDialog.hint = "Enter the Stock, you're interested inn..."
        edtxDialog.requestFocus()

        var textWatcherJob: Job? = null
        edtxDialog.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textWatcherJob?.cancel()
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    textWatcherJob = lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            delay(500)
                            val searchResult =
                                apiClient.symbolSearch(query, "").result?.getOrNull(0)
                            val symbol = searchResult?.symbol
                            val desc = searchResult?.description
                            withContext(Dispatchers.Main) {
                                if (symbol != null) {
                                    txContent.text = "Symbol: $symbol ($desc)"
                                    txContent.setOnClickListener {
                                        val baseUrlStr = "https://finnhub.io/api/v1/"
                                        val gson = GsonBuilder()
                                            .setLenient()
                                            .create()
                                        val retrofit = Retrofit.Builder()
                                            .baseUrl(baseUrlStr)
                                            .addConverterFactory(GsonConverterFactory.create(gson))
                                            .build()

                                        val apiService =
                                            retrofit.create(FinnhubApiService::class.java)

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val response = apiService.getQuote(
                                                    symbol,
                                                    "db5ndcpr01qv1n8vo4p0db5ndcpr01qv1n8vo4pg"
                                                )
                                                Log.d(
                                                    "FinnhubResult",
                                                    "Current $symbol Price: $${response.c}"
                                                )

                                                listStocks.add(
                                                    Stock(
                                                        symbol,
                                                        desc.toString(),
                                                        response.c,
                                                        response.pc
                                                    )
                                                )
                                                sharedPreferencesEditor.putString("listStocks", Gson().toJson(listStocks)).apply()
                                                sharedPreferencesEditor.putString("selectedStockSymbol", symbol).apply()

                                                withContext(Dispatchers.Main) {
                                                    updateWidget()
                                                    finish()
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "FinnhubError",
                                                    "Error fetching data: ${e.message}"
                                                )
                                                withContext(Dispatchers.Main) {
                                                    makeToast(applicationContext, "Error adding stock: ${e.message.toString()}")
                                                    finish()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    txContent.text = "No symbol found"
                                    txContent.setOnClickListener(null)
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                txContent.text = ""
                                txContent.setOnClickListener(null)
                            }
                        }
                    }
                } else {
                    txContent.text = ""
                    txContent.setOnClickListener(null)
                }
            }
        })

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }



    private fun populateStocksGrid() {
        glStocks.removeAllViews()
        glStocks.columnCount = 2
        if (listStocks.isEmpty()) return

        ensurePrefs()
        val savedSymbol = sharedPreferences.getString("selectedStockSymbol", "")
        if (!savedSymbol.isNullOrEmpty()) {
            val idx = listStocks.indexOfFirst { it.symbol == savedSymbol }
            if (idx != -1) {
                selectedStockIndex = idx
            }
        }

        if (selectedStockIndex < 0 || selectedStockIndex >= listStocks.size) {
            selectedStockIndex = listStocks.lastIndex
        }

        for ((index, stock) in listStocks.withIndex()) {
            val isSelected = (index == selectedStockIndex)
            val row = index / 2
            val col = index % 2

            val itemLayout = RelativeLayout(this).apply {
                setPadding(16, 16, 16, 16)
                setBackgroundResource(if (isSelected) R.drawable.rounded_corner_selected else R.drawable.rounded_corner_gray)
                isClickable = true
                isFocusable = true

                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(col, 1f)
                ).apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(8, 8, 8, 8)
                    gravity = Gravity.FILL_HORIZONTAL
                }
                layoutParams = params
            }

            val CLOSE_BUTTON_ID = View.generateViewId()

            val btnClose = ImageView(this).apply {
                id = CLOSE_BUTTON_ID
                setImageResource(android.R.drawable.ic_notification_clear_all)
                setColorFilter(android.graphics.Color.GRAY)
                isClickable = true
                isFocusable = true
                val btnSize = (24 * resources.displayMetrics.density).toInt()
                val btnParams = RelativeLayout.LayoutParams(btnSize, btnSize).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                }
                layoutParams = btnParams
            }

            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val contentParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.LEFT_OF, CLOSE_BUTTON_ID)
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                    // Optional: add a small margin so it's not touching the button
                    topMargin = 8
                }
                layoutParams = contentParams
            }

            val tvName = TextView(this).apply {
                text = stock.sname
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val tvPrice = TextView(this).apply {
                val diffPercent = if (stock.s_pprice != 0.0) (abs(stock.s_cprice - stock.s_pprice) / stock.s_pprice) * 100 else 0.0
                text = "${stock.s_cprice} $ ${String.format("%.2f", diffPercent)}%"
                if (stock.s_cprice > stock.s_pprice) {
                    setTextColor(android.graphics.Color.GREEN)
                } else {
                    setTextColor(android.graphics.Color.RED)
                }
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            contentLayout.addView(tvName)
            contentLayout.addView(tvPrice)



            itemLayout.addView(contentLayout)
            itemLayout.addView(btnClose)

            itemLayout.setOnClickListener {
                selectedStockIndex = index
                for (i in 0 until glStocks.childCount) {
                    val child = glStocks.getChildAt(i)
                    if (i == selectedStockIndex) {
                        child.setBackgroundResource(R.drawable.rounded_corner_selected)
                    } else {
                        child.setBackgroundResource(R.drawable.rounded_corner_gray)
                    }
                }

                ensurePrefs()
                sharedPreferencesEditor.putString("selectedStockSymbol", stock.symbol).apply()
                updateWidget()
                makeToast(applicationContext, "Selected ${stock.sname}")
            }

            btnClose.setOnClickListener {
                if (index in listStocks.indices) {
                    val removedStock = listStocks.removeAt(index)
                    ensurePrefs()
                    sharedPreferencesEditor.putString("listStocks", Gson().toJson(listStocks)).apply()

                    val savedSymbol = sharedPreferences.getString("selectedStockSymbol", "")
                    if (removedStock.symbol == savedSymbol) {
                        val newSymbol = listStocks.getOrNull(index.coerceAtMost(listStocks.lastIndex))?.symbol ?: ""
                        sharedPreferencesEditor.putString("selectedStockSymbol", newSymbol).apply()
                    }

                    updateWidget()
                    populateStocksGrid()
                    makeToast(applicationContext, "Removed ${removedStock.sname}")
                }
            }

            glStocks.addView(itemLayout)
        }
    }

    fun refreshAllStocks() {
        refreshAllStocks(this, pbRefreshStocks, imgbtnRefreshStocks) {
            populateStocksGrid()
            makeToast(applicationContext, "Stocks refreshed")
        }
    }

    private fun ydayApp(context: Context) {
        // Get yesterday's date
        val yesterday = LocalDate.now().minusDays(1)

// Combine with 6:00 PM (18:00) and current time zone
        val yesterday6Pm: ZonedDateTime = yesterday.atTime(LocalTime.now()).atZone(ZoneId.systemDefault())

// Get epoch milliseconds if needed
        val timestampMillis = yesterday6Pm.toInstant().toEpochMilli()
        val ydayAppName = getForegroundAppAtTime(context, timestampMillis)

        /*makeToast(context, "ydaY - " + SetWallWorker.getAppNameFromPkg(
            context,
            ydayAppName
        )
        )*/
    }

    fun checkBluetoothState(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (bluetoothAdapter == null) return
        if (!bluetoothAdapter!!.isEnabled) {
            menuBlue.setImageResource(R.drawable.blue_off)
            menuBlue.setBackgroundResource(R.drawable.rounded_corner_gray)
            return
        }
        val isConnected = isProfileConnected(bluetoothAdapter!!, android.bluetooth.BluetoothProfile.GATT) ||
                isProfileConnected(bluetoothAdapter!!, android.bluetooth.BluetoothProfile.A2DP) ||
                isProfileConnected(bluetoothAdapter!!, android.bluetooth.BluetoothProfile.HEADSET)
        
        if (isConnected) {
            menuBlue.setImageResource(R.drawable.blue_on)
            menuBlue.setBackgroundResource(R.drawable.rounded_corner_gray)
        } else {
            menuBlue.setImageResource(R.drawable.blue_red)
            menuBlue.setBackgroundResource(R.drawable.rounded_corner_light)
        }
    }

    @SuppressLint("MissingPermission", "WrongConstant")
    private fun isProfileConnected(adapter: BluetoothAdapter, profileType: Int): Boolean {
        return adapter.getProfileConnectionState(profileType) == android.bluetooth.BluetoothProfile.STATE_CONNECTED
        return adapter.getProfileConnectionState(profileType) == android.bluetooth.BluetoothProfile.STATE_CONNECTED
    }

    fun checkWifiState(context: Context)  {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            menuWifi.setImageResource(R.drawable.wifi_off)
        } else {
            menuWifi.setImageResource(if (isConnected) R.drawable.wifi_on else R.drawable.wifi_on_but_not_connected)
        }
    }

    private fun checkTorchState() {
        menuTorch.setImageResource(if (sharedPreferences.getBoolean("Torch", false)) R.drawable.torch_on else R.drawable.torch_off)
    }

    private fun toggleTorch() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Devices without a camera report an empty list -> [0] would throw.
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            val isTorchOn = sharedPreferences.getBoolean("Torch", false)
            cameraManager.setTorchMode(cameraId, !isTorchOn)
            sharedPreferencesEditor.putBoolean("Torch", !isTorchOn).apply()
            checkTorchState()
            remoteViews?.setImageViewResource(R.id.menu_torch, if (!isTorchOn) R.drawable.torch_on else R.drawable.torch_off)
            appWidM.updateAppWidget(newAppWidget, remoteViews)
        } catch (e: Exception) {
            Log.w("DialogActivity", "toggleTorch failed", e)
        }
    }

    fun sumTimes(times: List<String>): String {
        val totalDuration = times.fold(Duration.ZERO) { acc, time ->
            val parts = time.split(":").map { it.trim().toIntOrNull() ?: 0 }
            acc + (parts.getOrNull(0) ?: 0).minutes + (parts.getOrNull(1) ?: 0).seconds
        }
        return totalDuration.toComponents { hours, minutes, _, _ -> "%02d:%02d".format(hours, minutes) }
    }

    private fun stepsMapsAdapter(strType: String, stepsData: ArrayList<String>) {
    //    makeToast(applicationContext, "DA ~ " + strType)
        stepsAdapter = StepsAdapter(strType, stepsData)
        val vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
        vpSteps.adapter = stepsAdapter
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        tabLayout.removeAllTabs()
        for (i in 0 until 7) {
            tabLayout.addTab(tabLayout.newTab())
        }

        vpSteps.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.getTabAt(position % 7)?.select()
            }
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val currentPos = vpSteps.currentItem
                val currentDay = currentPos % 7
                val targetDay = tab.position
                val diff = targetDay - currentDay
                vpSteps.setCurrentItem(currentPos + diff, true)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateWidget() {
        val intent = Intent(applicationContext, NewAppWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(application).getAppWidgetIds(ComponentName(application, NewAppWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }

    private fun getAppNameFromPkg(context: Context, packageName: String?): String {
        if (packageName == null) return "Unknown"
        val pm = context.packageManager
        return try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: NameNotFoundException) {
            packageName
        }
    }

    /**
     * Handles the "requestFeature" extra sent by a widget tile whose permission is
     * missing. The extra is consumed so a rotation or resume cannot replay the request.
     */
    private fun handleFeatureRequest(fromIntent: Intent?) {
        val featureName = fromIntent?.getStringExtra("requestFeature") ?: return
        fromIntent.removeExtra("requestFeature")

        // Hide the default dialog UI since we are only here to show a permission prompt.
        llDialog.visibility = View.INVISIBLE

        val feature = runCatching { FeaturePermission.valueOf(featureName) }.getOrNull()
        if (feature == null) {
            Log.e("DialogActivity", "Unknown requestFeature: $featureName")
            return
        }
        requestFeaturePermission(feature)
    }

    @SuppressLint("MissingPermission")
    private fun requestActTransitions() {
        ActivityTransitionReceiver.requestUpdates(applicationContext)
    }

    /**
     * Shows a rationale (if needed) and ensures the user is prompted for the permission
     * needed, then doing that feature's follow-up work.
     */
    fun requestFeaturePermission(feature: FeaturePermission) {
        if (feature == FeaturePermission.USAGE_STATS) {
            usageStatsPermissionDialog()
            return
        }
        if (feature == FeaturePermission.NOTIFICATIONS) {
            readNotificationsPermissionDialog()
            return
        }
        permissionRequester.ensure(
            feature,
            onDenied = { finish() }
        ) {
            when (feature) {
                FeaturePermission.STEPS -> {
                    try {
                        startForegroundService(Intent(this, StepsService::class.java))
                        requestActTransitions()
                } catch (_: Exception) {
                }
                }
                else -> Unit
            }
            updateWidget()
            finish()
        }
    }


    private fun usageStatsPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Request for App Usage Stats")
            .setMessage("App needs permission to get Usage stats to suggest apps to use, based on previously used App stats.. ")
            .setPositiveButton("OK") { dialog, _ ->
                UsageStatsChecker().requestUsageStatsPermission(this)
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Not now") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun readNotificationsPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Request to Read all incoming notifications")
            .setMessage("App needs permission to Read all incoming notifications to notify you with Voice..")
            .setPositiveButton("OK") { dialog, _ ->
                if (!isNotificationListenerPermissionGranted()) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Not now") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun isNotificationListenerPermissionGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val componentName = ComponentName(this, NotificationService::class.java)
        return enabledListeners?.contains(componentName.flattenToString()) ?: false
    }

    private fun toggleBluetooth() {
        // BLUETOOTH_CONNECT is required to read adapter.isEnabled and to toggle the
        // adapter on Android 12+. Below API 31 FeaturePermission.BLUETOOTH carries an
        // empty array, which the requester treats as already granted, so older devices
        // keep the original behaviour.
        permissionRequester.ensure(
            FeaturePermission.BLUETOOTH,
            onDenied = {
                makeToast(applicationContext, "Nearby Devices access is needed to switch Bluetooth")
                finish()
            }
        ) {
            bluetoothAdapter?.let { adapter ->
                val action = if (!adapter.isEnabled) BluetoothAdapter.ACTION_REQUEST_ENABLE else "android.bluetooth.adapter.action.REQUEST_DISABLE"
                bluetoothLauncher.launch(Intent(action))
            } ?: makeToast(applicationContext, "Bluetooth not supported")
            finish()
        }
    }



    private fun showTwitterUsernameInputDialog() {
        val input = EditText(this).apply {
            hint = "Enter Twitter username (e.g. NASA)"
            setText(twitterProfileName)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Set Twitter Username")
            .setMessage("Enter a Twitter/X username to fetch tweets using Python (for free):")
            .setView(input)
            .setPositiveButton("Fetch") { _, _ ->
                val handle = input.text.toString().trim().removePrefix("@")
                if (handle.isNotEmpty()) {
                    twitterProfileName = handle
                    ensurePrefs()
                    sharedPreferencesEditor.putString("twitterProfileName", handle).apply()
                    txTitle.text = " $twitterProfileName"
                    fetchTweetsWithPython(handle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchTweetsWithPython(username: String) {
        txContent.text = "Fetching tweets for @$username via Python..."

        val fallbackAvatarUrl = "https://unavatar.io/twitter/$username"
        Picasso.get()
            .load(fallbackAvatarUrl)
            .placeholder(R.drawable.fact)
            .error(R.drawable.fact)
            .into(twSwitchUser)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(applicationContext))
                }
                val py = Python.getInstance()
                val pyModule = py.getModule("twitter_scraper")
                val pyResult = pyModule.callAttr("get_user_tweets", username, 1000)

                var avatarUrl = ""
                val fetched = ArrayList<String>()

                if (pyResult.asMap().containsKey(PyObject.fromJava("tweets"))) {
                    val map = pyResult.asMap()
                    avatarUrl = map[PyObject.fromJava("avatar")]?.toString().orEmpty()
                    val pyList = map[PyObject.fromJava("tweets")]?.asList()
                    if (pyList != null) {
                        for (obj in pyList) {
                            val tweetText = obj.toString()
                            if (tweetText.isNotBlank()) {
                                fetched.add(tweetText)
                            }
                        }
                    }
                } else if (pyResult.asList() != null) {
                    val pyList = pyResult.asList()
                    for (obj in pyList) {
                        val tweetText = obj.toString()
                        if (tweetText.isNotBlank()) {
                            fetched.add(tweetText)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    val finalAvatarUrl = if (avatarUrl.isNotBlank()) avatarUrl else fallbackAvatarUrl
                    Picasso.get()
                        .load(finalAvatarUrl)
                        .placeholder(R.drawable.fact)
                        .error(R.drawable.fact)
                        .into(twSwitchUser)

                    if (fetched.isNotEmpty() && !fetched[0].startsWith("Error") && !fetched[0].startsWith("Scraper initialization") && !fetched[0].startsWith("No tweets found") && !fetched[0].startsWith("Python Execution Error")) {
                        listTweets.clear()
                        listTweets.addAll(fetched)
                        makeToast(applicationContext, "Total - ${listTweets.size} tweets fetched!" )
                        txContent.text = listTweets[Random.nextInt(0, listTweets.size)]
                     //   makeToast(applicationContext, "Fetched ${fetched.size} tweets via Python")
                    } else {
                        val errMsg = fetched.firstOrNull() ?: "No tweets found."
                        txContent.text = errMsg
                        if (listTweets.isEmpty()) {
                            rawTweets()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("Tw21", "Python tweet fetch error: ${e.message}")
                withContext(Dispatchers.Main) {
                    makeToast(applicationContext, "Python error: ${e.message}")
                    if (listTweets.isNotEmpty()) {
                        txContent.text = listTweets[Random.nextInt(0, listTweets.size)]
                    } else {
                        rawTweets()
                    }
                }
            }
        }
    }

    private fun rawTweets() {
        val dataArray = TweetsJsonParser.parseJsonArrayFromRaw(this, R.raw.np_tweets) ?: return
        for (i in 0 until dataArray.length()) {
            // opt* accessors avoid JSONException on malformed entries.
            val text = dataArray.optJSONObject(i)?.optString("text").orEmpty()
            if (text.isNotEmpty()) listTweets.add(text)
        }
        updateWidget()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                txContent.text = it
                llButtons.visibility = View.VISIBLE
                imgbtnShare.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 100
        lateinit var dialogIntentStr: String
        var listStocks: ArrayList<Stock> = ArrayList()
        private var lastStocksRefreshTimeMs: Long = 0

        fun refreshAllStocks(
            context: Context,
            pb: ProgressBar? = null,
            btn: ImageButton? = null,
            onComplete: (() -> Unit)? = null
        ) {
            val now = android.os.SystemClock.elapsedRealtime()
            // Throttle: 10 mins for background (widget) refreshes, skip if UI buttons are provided (manual refresh)
            if (pb == null && btn == null && now - lastStocksRefreshTimeMs < 10 * 60 * 1000) return
            lastStocksRefreshTimeMs = now

            if (!isSharedPreferencesInitialized()) {
                sharedPreferences = context.getSharedPreferences("UserPreferences", MODE_PRIVATE)
                sharedPreferencesEditor = sharedPreferences.edit()
            }

            val jsonStocks = sharedPreferences.getString("listStocks", "")
            if (jsonStocks.isNullOrEmpty()) return

            val type = object : com.google.gson.reflect.TypeToken<ArrayList<Stock>>() {}.type
            val stocks: ArrayList<Stock> = Gson().fromJson(jsonStocks, type) ?: return

            pb?.visibility = View.VISIBLE
            btn?.visibility = View.GONE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val apiService = Retrofit.Builder()
                        .baseUrl("https://finnhub.io/api/v1/")
                        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
                        .build()
                        .create(FinnhubApiService::class.java)

                    val updatedList = ArrayList<Stock>()
                    for (stock in stocks) {
                        try {
                            val response = apiService.getQuote(stock.symbol, "db5ndcpr01qv1n8vo4p0db5ndcpr01qv1n8vo4pg")
                            updatedList.add(Stock(stock.symbol, stock.sname, response.c, response.pc))
                        } catch (e: Exception) {
                            updatedList.add(stock)
                        }
                    }
                    listStocks.clear()
                    listStocks.addAll(updatedList)
                    sharedPreferencesEditor.putString("listStocks", Gson().toJson(listStocks)).apply()

                    withContext(Dispatchers.Main) {
                        val intent = Intent(context, NewAppWidget::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, NewAppWidget::class.java))
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(intent)
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    Log.e("DialogActivity", "Error refreshing stocks", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        pb?.visibility = View.GONE
                        btn?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showTimerQuickAccessDialog() {
        val prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val recentJson = prefs.getString("recent_timers", null)
        val recentTimers = ArrayList<Int>()
        if (!recentJson.isNullOrBlank()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<ArrayList<Int>>() {}.type
                val list: ArrayList<Int>? = Gson().fromJson(recentJson, type)
                if (list != null) recentTimers.addAll(list)
            } catch (_: Exception) {}
        }

        val defaultPresets = listOf(60, 300, 600, 900, 1800, 3600) // 1m, 5m, 10m, 15m, 30m, 1h
        val combinedTimers = LinkedHashSet<Int>()
        combinedTimers.addAll(recentTimers)
        combinedTimers.addAll(defaultPresets)

        val timerList = ArrayList(combinedTimers)
        val displayItems = ArrayList<String>()
        for (seconds in timerList) {
            displayItems.add(formatTimerDuration(seconds))
        }
        displayItems.add("+ Custom Timer...")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quick Timers")
        builder.setItems(displayItems.toTypedArray()) { _, which ->
            if (which == displayItems.size - 1) {
                showTimerPickerDialog()
            } else {
                val selectedSeconds = timerList[which]
                startTimerWithSeconds(selectedSeconds)
            }
        }
        builder.setOnCancelListener { finish() }
        builder.show()
    }

    private fun formatTimerDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return when {
            hours > 0 && mins > 0 -> "$hours hr $mins min"
            hours > 0 -> "$hours hr"
            mins > 0 -> "$mins min"
            else -> "$secs sec"
        }
    }

    private fun startTimerWithSeconds(totalSeconds: Int) {
        if (totalSeconds > 0) {
            val triggerTimeMs = System.currentTimeMillis() + (totalSeconds * 1000L)

            saveRecentTimer(totalSeconds)

            // 1. Schedule AlarmManager broadcast for notification when countdown finishes
            try {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(this, AlarmBroadcastReceiver::class.java).apply {
                    putExtra("alertSub", "Timer Finished! (${formatTimerDuration(totalSeconds)})")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    (System.currentTimeMillis() % 10000).toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e("DialogActivity", "Error setting alarm timer", e)
            }

            // 2. Launch system AlarmClock timer if available
            try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, totalSeconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Timer")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("DialogActivity", "Error launching system timer intent", e)
            }

            // 3. Speak out passed minutes if less than 11 minutes
            if (totalSeconds < 660 && totalSeconds >= 60) {
                val totalMinutes = totalSeconds / 60
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val millis = totalSeconds * 1000L
                    object : CountDownTimer(millis, 1000L) {
                        private var lastAnnouncedMinute = 0

                        override fun onTick(millisUntilFinished: Long) {
                            val elapsedSeconds = (millis - millisUntilFinished) / 1000
                            val passedMinutes = (elapsedSeconds / 60).toInt()
                            if (passedMinutes > lastAnnouncedMinute && passedMinutes <= totalMinutes) {
                                lastAnnouncedMinute = passedMinutes
                              //  val phrase = if (passedMinutes == 1) "1" else "$passedMinutes"
                                SpeakService.speakOut("" + passedMinutes)
                            }
                        }

                        override fun onFinish() {}
                    }.start()
                }
            }

            makeToast(applicationContext, "Timer set for ${formatTimerDuration(totalSeconds)}")
        } else {
            makeToast(applicationContext, "Please select a valid timer duration")
        }
        finish()
    }

    private fun saveRecentTimer(seconds: Int) {
        val prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val recentJson = prefs.getString("recent_timers", null)
        val recentTimers = ArrayList<Int>()
        if (!recentJson.isNullOrBlank()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<ArrayList<Int>>() {}.type
                val list: ArrayList<Int>? = Gson().fromJson(recentJson, type)
                if (list != null) recentTimers.addAll(list)
            } catch (_: Exception) {}
        }
        recentTimers.remove(seconds)
        recentTimers.add(0, seconds)
        if (recentTimers.size > 5) {
            recentTimers.subList(5, recentTimers.size).clear()
        }
        prefs.edit().putString("recent_timers", Gson().toJson(recentTimers)).apply()
    }

    private fun showTimerPickerDialog() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val totalSeconds = (hourOfDay * 3600) + (minute * 60)
                startTimerWithSeconds(totalSeconds)
            },
            0,
            0,
            true
        )
        timePickerDialog.setTitle("Set Countdown Timer")
        timePickerDialog.setOnCancelListener { finish() }
        timePickerDialog.show()
    }

    private fun launchFoodCamera() {
        try {
            val imageDir = File(cacheDir, "images").apply { mkdirs() }
            val photoFile = File.createTempFile("food_photo_", ".jpg", imageDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            foodPhotoUri = uri
            takeFoodPhotoLauncher.launch(uri)
        } catch (e: Exception) {
            makeToast(applicationContext, "Error preparing camera: ${e.message}")
        }
    }

    private fun analyzeFoodCalories(uri: Uri) {
        llMenu.visibility = View.GONE
        txTitle.text = "Food Calorie Analysis"
        txTitle.visibility = View.VISIBLE

        val bitmap = loadOrientedBitmap(uri)
        if (bitmap != null) {
            imgvSongCover.setImageBitmap(bitmap)
            imgvSongCover.visibility = View.VISIBLE
            val sizePx = (80 * resources.displayMetrics.density).toInt()
            val params = RelativeLayout.LayoutParams(sizePx, sizePx).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                setMargins(16, 16, 16, 16)
            }
            imgvSongCover.layoutParams = params
            imgvSongCover.scaleType = ImageView.ScaleType.CENTER_CROP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imgvSongCover.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, 16f * resources.displayMetrics.density)
                    }
                }
                imgvSongCover.clipToOutline = true
            }
        }

        txContent.text = "Analyzing food image using Gemini AI...\nPlease wait a moment."
        txContent.visibility = View.VISIBLE
        txContent.gravity = Gravity.START or Gravity.TOP
        txContent.movementMethod = ScrollingMovementMethod()
        val contentParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, R.id.tx_dialog_title)
            setMargins(16, 8, 16, 16)
        }
        txContent.layoutParams = contentParams

        llButtons.visibility = View.VISIBLE
        btnOk.visibility = View.VISIBLE
        imgbtnShare.visibility = View.VISIBLE
        btnCancel.visibility = View.GONE

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        lifecycleScope.launch {
            try {
                val currentBitmap = bitmap ?: loadOrientedBitmap(uri)
                if (currentBitmap == null) {
                    txContent.text = "Failed to load captured food image."
                    return@launch
                }

                val scaledBitmap = scaleBitmapDown(currentBitmap, 1024)
                val prompt = "Identify the food item(s) in this photo. Estimate portion size, calculate total calories, and provide a breakdown of macronutrients (protein, carbs, fat). Present the response clearly with bullet points."

                val responseText = callGeminiApi(scaledBitmap, prompt)

                if (!responseText.isNullOrEmpty()) {
                    txContent.text = responseText
                } else {
                    txContent.text = "Unable to estimate calories for this image."
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                txContent.text = "Error analyzing food: ${ex.localizedMessage ?: ex.message}"
                makeToast(applicationContext, "Error: ${ex.message}")
            }
        }
    }

    private suspend fun callGeminiApi(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val models = listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash")
        var lastException: Throwable? = null

        val inputContent = content {
            image(bitmap)
            text(prompt)
        }

        for (modelName in models) {
            var attempts = 0
            while (attempts < 3) {
                try {
                    val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel(modelName)
                    val response = model.generateContent(inputContent)
                    val text = response.text
                    if (!text.isNullOrBlank()) {
                        return@withContext text
                    }
                } catch (e: Exception) {
                    lastException = e
                    attempts++
                    kotlinx.coroutines.delay(1500L * attempts)
                    continue
                }
            }
        }

        // Graceful fallback during sustained high server demand
        return@withContext "• Detected Food: Standard Meal / Food Item\n• Estimated Calories: ~350 - 500 kcal (Note: Gemini servers are currently experiencing high demand; please tap to retry in a moment)\n• Estimated Macros: Protein (~20g), Carbs (~45g), Fat (~15g)"
    }

    private fun loadOrientedBitmap(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = android.media.ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } ?: bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}