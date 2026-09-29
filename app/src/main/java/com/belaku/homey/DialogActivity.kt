package com.belaku.homey

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
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
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.belaku.FinnhubApiService
import com.belaku.Stock
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.beginCal
import com.belaku.homey.MainActivity.Companion.endCal
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.hashSetAppUsage
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.penNote
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
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.squareup.picasso.Picasso
import io.finnhub.api.apis.DefaultApi
import io.finnhub.api.infrastructure.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
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
    private lateinit var imgbtnAddAnotherStock: ImageButton
    private lateinit var imgbtnRefreshStocks: ImageButton
    private lateinit var llMenu: LinearLayout
    private lateinit var glStocks: GridLayout
    private lateinit var dialogActContext: Context
    private lateinit var parentLayoutDialog: View
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult? ->
            if (result?.contents != null) {
                // Handle the scan result
                val scannedUrl = result.contents
                val upiUri = Uri.parse(scannedUrl)
                val upiIntent = Intent(Intent.ACTION_VIEW)
                upiIntent.data = upiUri
                val chooser = Intent.createChooser(upiIntent, "Pay with")
                if (chooser.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                } else {
                    // Handle the case where no UPI apps are installed
                     makeToast(applicationContext, "No UPI app found. Please install one to proceed.")
                }
            }
        }
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private lateinit var llDialog: RelativeLayout
    private lateinit var imgvSongCover: ImageView
    private lateinit var txTitle: TextView
    private lateinit var txContent: TextView
    private lateinit var edtxDialog: EditText

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    private lateinit var imgbtnShare: ImageButton
    private lateinit var menuReminders: ImageButton
    private lateinit var menuTorch: ImageButton
    private lateinit var menuWifi: ImageButton
    private lateinit var menuBlue: ImageButton
    private lateinit var menuAi: ImageButton


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
            stepsData.add(sharedPreferences.getInt("Monday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Tuesday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Wednesday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Thursday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Friday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Saturday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Sunday", 0).toString())
        }

        if (speedData.isEmpty()) {
            speedData.add(sharedPreferences.getInt("Monday", 0).toString())
            speedData.add(sharedPreferences.getInt("Tuesday", 0).toString())
            speedData.add(sharedPreferences.getInt("Wednesday", 0).toString())
            speedData.add(sharedPreferences.getInt("Thursday", 0).toString())
            speedData.add(sharedPreferences.getInt("Friday", 0).toString())
            speedData.add(sharedPreferences.getInt("Saturday", 0).toString())
            speedData.add(sharedPreferences.getInt("Sunday", 0).toString())
        }

        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
            }
        }

        imgbtnAddAnotherStock = findViewById(R.id.imgbtn_addanother_stock)
        imgbtnRefreshStocks = findViewById(R.id.imgbtn_refresh_stocks)
        glStocks = findViewById(R.id.gl_stocks)
        llMenu = findViewById(R.id.ll_menu)
        llDialog = findViewById(R.id.dialog_layout)
        imgvSongCover = findViewById(R.id.dialog_imgv_cover)
        txTitle = findViewById(R.id.tx_dialog_title)
        txContent = findViewById(R.id.tx_dialog_content)


        edtxDialog = findViewById(R.id.edtx_dialog)
        btnOk = findViewById(R.id.btn_dialog_ok)
        btnCancel = findViewById(R.id.btn_dialog_cancel)
        imgbtnShare = findViewById(R.id.imgbtn_dialog_share)

        menuReminders = findViewById(R.id.menu_reminders)
        menuTorch = findViewById(R.id.menu_torch)
        menuWifi = findViewById(R.id.menu_wifi)
        menuBlue = findViewById(R.id.menu_blue)
        menuAi = findViewById(R.id.menu_ai)

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

        dialogIntentStr = intent.getStringExtra("DialogIntent").toString()
   //     makeToast(applicationContext,dialogIntentStr.toString())

        handleFeatureRequest(intent)

        if (dialogIntentStr != null) {
            when (dialogIntentStr) {
                "setNote" -> {
                    edtxDialog.visibility = View.VISIBLE
                    btnOk.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.INVISIBLE
                    txTitle.text = "Pin a Note"

                    btnOk.setOnClickListener {
                        if (edtxDialog.text.isNotEmpty()) {
                            penNote = edtxDialog.text.toString()
                            remoteViews?.setTextViewText(R.id.tx_runner, "\uD83D\uDCDD $penNote")
                            appWidM.updateAppWidget(newAppWidget, remoteViews)
                        }
                        finish()
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
                        remoteViews?.setViewVisibility(R.id.imgbtn_set, View.INVISIBLE)
                        remoteViews?.setViewVisibility(R.id.progressBar_cyclic_wallchange, View.VISIBLE)
                        appWidM.updateAppWidget(newAppWidget, remoteViews)
                        Thread { SetWallWorker.setWall(true, dialogActContext) }.start()
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
                "ST" -> {

                    ydayApp(applicationContext)
                    txContent.visibility = View.VISIBLE
                    txTitle.text = " $twitterProfileName"
                    findViewById<ImageButton>(R.id.tw_config).apply {
                        visibility = View.VISIBLE
                        setOnClickListener { makeToast(applicationContext, "Paid Feature, coming soon!") }
                    }
                    if (listTweets.isNotEmpty()) {
                        txContent.text = listTweets[Random.nextInt(0, listTweets.size)]
                    } else {
                        txContent.text = "fetching Data.. visit again later, please"
                        rawTweets()
                    }
                }
                "SPEED" -> {
                    txTitle.text = "Weekly Speed"
                    val vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
                    val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                    vpSteps.visibility = View.VISIBLE
                    tabLayout.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE

                    val currentDay = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

                    val maxSpeedToday = sharedPreferences.getInt("maxSpeedToday", SpeedService.maxSpeed)
                    speedData[currentDay] = "$maxSpeedToday"

                    stepsMapsAdapter("speed", speedData)

                    // Set to a middle position for circular scrolling
                    val mid = 3500 - (3500 % 7) + currentDay
                    vpSteps.setCurrentItem(mid, false)

                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                }
                "WALKING" -> {
                    txTitle.text = "Weekly Steps"
                    val vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
                    val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                    vpSteps.visibility = View.VISIBLE
                    tabLayout.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE

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
                    edtxDialog.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.INVISIBLE
                    edtxDialog.hint = "Enter Note to be Pinned..."
                    edtxDialog.requestFocus()
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    btnOk.setOnClickListener {
                        if (edtxDialog.text.toString().isNotEmpty()) pinNote = edtxDialog.text.toString()
                        Thread { SetWallWorker.setWall(true, dialogActContext) }.start()
                        finish()
                    }
                }
                "AddAnotherStock" -> {
                    txTitle.text = "All Stocks"
                    edtxDialog.setBackgroundColor(android.R.color.darker_gray)
                    txContent.setBackgroundColor(android.R.color.white)
                    populateStocksGrid()
                }
                "AddStock" -> {

                    addStock()

                }
                "activitiesInfo" -> {
                    txTitle.text = "Activity Details"
                    val container = findViewById<LinearLayout>(R.id.ll_activity_container)
                    container.visibility = View.VISIBLE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    val inflater = LayoutInflater.from(this)
                    val widgetView = inflater.inflate(R.layout.new_app_widget, container, false)

                    val stillLayout = widgetView.findViewById<RelativeLayout>(R.id.rl_still)
                    val walkingLayout = widgetView.findViewById<RelativeLayout>(R.id.rl_walking)
                    val speedLayout = widgetView.findViewById<RelativeLayout>(R.id.rl_speed)

                    listOf(stillLayout, walkingLayout, speedLayout).forEach { layout ->
                        (layout.parent as? ViewGroup)?.removeView(layout)
                        layout.visibility = View.VISIBLE
                        container.addView(layout)
                    }

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

                    val options = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("Scan a QR code")
                    }

                    // Launch the scanner
                    barcodeLauncher.launch(options)
                }
            }
        }
    }

    private fun ensurePrefs() {
        if (!isSharedPreferencesInitialized()) {
            sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences.edit()
        }
    }

    private fun addStock() {
        ensurePrefs()
        ApiClient.apiKey["token"] = "datp3ahr01quegbh5300datp3ahr01quegbh530g"
        val apiClient = DefaultApi()


        txTitle.text = "Add Stock"
        edtxDialog.visibility = View.VISIBLE
        txContent.visibility = View.VISIBLE
        txContent.text = ""
        btnOk.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
        imgbtnShare.visibility = View.INVISIBLE
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
                                apiClient.symbolSearch(query, "US").result?.getOrNull(0)
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
                                                    "datp3ahr01quegbh5300datp3ahr01quegbh530g"
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
                                                    makeToast(applicationContext, "Error adding stock: ${e.message}")
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
        for (stock in listStocks) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.rounded_corner_gray)
                val params = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params
            }

            val tvName = TextView(this).apply {
                text = stock.sname
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
            }

            val tvPrice = TextView(this).apply {
                text = stock.s_cprice.toString()
                if (stock.s_cprice > stock.s_pprice) {
                    setTextColor(android.graphics.Color.GREEN)
                } else {
                    setTextColor(android.graphics.Color.RED)
                }
            }

            itemLayout.addView(tvName)
            itemLayout.addView(tvPrice)
            glStocks.addView(itemLayout)
        }
    }

    private fun refreshAllStocks() {
        ensurePrefs()
        val baseUrlStr = "https://finnhub.io/api/v1/"
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrlStr)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val apiService = retrofit.create(FinnhubApiService::class.java)

        lifecycleScope.launch(Dispatchers.IO) {
            val updatedList = ArrayList<Stock>()
            for (stock in listStocks) {
                try {
                    val response = apiService.getQuote(stock.symbol, "datp3ahr01quegbh5300datp3ahr01quegbh530g")
                    updatedList.add(Stock(stock.symbol, stock.sname, response.c, response.pc))
                } catch (e: Exception) {
                    updatedList.add(stock)
                }
            }
            listStocks.clear()
            listStocks.addAll(updatedList)
            sharedPreferencesEditor.putString("listStocks", Gson().toJson(listStocks)).apply()
            withContext(Dispatchers.Main) {
                populateStocksGrid()
                updateWidget()
                makeToast(applicationContext, "Stocks refreshed")
            }
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

        makeToast(context, "ydaY - " + SetWallWorker.getAppNameFromPkg(
            context,
            ydayAppName
        )
        )
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

            val intentActivityTransitionReceiver =
                Intent(applicationContext, ActivityTransitionReceiver::class.java).setAction("action.TRANSITIONS_DATA")
            val requestCodeAT = 57
            val pendingIntentActivityTransitions = PendingIntent.getBroadcast(
                applicationContext,
                requestCodeAT,
                intentActivityTransitionReceiver,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val activityTransitions = ArrayList<ActivityTransition>()
            activityTransitions.apply {
                add(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
                )

                add(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
                )

                add(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
                )

                add(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
                )

                add(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
                )

                add(
                    ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
                )
            }

            val activityTransitionRequest = ActivityTransitionRequest(activityTransitions)

            // myPendingIntent is the instance of PendingIntent where the app receives callbacks.
            try {
                ActivityRecognition.getClient(applicationContext)
                    .requestActivityTransitionUpdates(
                        activityTransitionRequest,
                        pendingIntentActivityTransitions
                    )
                    .addOnSuccessListener { Log.d("TAG", "Activity transition updates registered") }
                    .addOnFailureListener { e -> Log.e("TAG", "Activity transition updates failed", e) }
            } catch (e: Exception) {
                Log.e("TAG", "requestActivityTransitionUpdates threw", e)
            }

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
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 100
        lateinit var dialogIntentStr: String
        var listStocks: ArrayList<Stock> = ArrayList()
    }
}