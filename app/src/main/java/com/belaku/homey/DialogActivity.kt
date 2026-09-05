package com.belaku.homey

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.MainActivity.Companion.beginCal
import com.belaku.homey.MainActivity.Companion.endCal
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.pickContactLauncher
import com.belaku.homey.MainActivity.Companion.sN
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.hashSetAppUsage
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.penNote
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.vpStepsPos
import com.belaku.homey.SetWallWorker.Companion.appUsageStats
import com.belaku.homey.SetWallWorker.Companion.getFavoriteContacts
import com.belaku.homey.SetWallWorker.Companion.hour
import com.belaku.homey.SetWallWorker.Companion.isSharedPreferencesInitialized
import com.belaku.homey.SetWallWorker.Companion.pinNote
import com.belaku.homey.SetWallWorker.Companion.screenHeight
import com.belaku.homey.SetWallWorker.Companion.screenWidth
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.StepsService.Companion.speedInKmph
import com.belaku.homey.StepsService.Companion.stepsAdapter
import com.belaku.homey.StepsService.Companion.stepsData
import com.belaku.homey.StepsService.Companion.totalUsage
import com.belaku.homey.StepsService.Companion.twitterProfileName
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.Calendar
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DialogActivity : AppCompatActivity() {

    var bluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }
    private var bluetoothAdapter: BluetoothAdapter? = null
    val wifiPanelIntent = Intent(Settings.Panel.ACTION_WIFI)
    private lateinit var llMenu: LinearLayout
    private lateinit var dialogAct: AlertDialog
    private var boolFetchingTweets: Boolean = false
    private lateinit var dialogActContext: Context
    private lateinit var parentLayoutDialog: View
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult? ->
            if (result?.contents == null) {

            } else {
                // Handle the scan result
                var scannedUrl = result.contents
                val upiUri = Uri.parse(scannedUrl)
                val upiIntent = Intent(Intent.ACTION_VIEW)
                upiIntent.setData(upiUri)
                val chooser = Intent.createChooser(upiIntent, "Pay with")
                if (chooser.resolveActivity(getPackageManager()) != null) {
                    startActivity(chooser);
                } else {
                    // Handle the case where no UPI apps are installed
                     makeToast(applicationContext, "No UPI app found. Please install one to proceed.")
                }
            }
        }
    private var stepsVPpos: Int = 0
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

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ResourceAsColor", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog)

        parentLayoutDialog = findViewById(android.R.id.content)
        dialogActContext = applicationContext

        if (isSharedPreferencesInitialized())
        if (stepsData.isEmpty()) {
            stepsData.add(sharedPreferences.getInt("Monday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Tuesday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Wednesday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Thursday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Friday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Saturday", 0).toString())
            stepsData.add(sharedPreferences.getInt("Sunday", 0).toString())
        }

        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
            }
        }

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

         val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
         bluetoothAdapter = bluetoothManager.adapter

        checkWifiState(applicationContext)
        checkBluetoothState(applicationContext)
        checkTorchState()

        btnCancel.setOnClickListener {
            finish()
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
            remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            remindersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
            toggleBluetooth()
            finish()
        }

        menuAi.setOnClickListener {
            val aiIntent = Intent(this, AiActivity::class.java)
            aiIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            aiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(aiIntent)
        }

        val dialogIntentStr = intent.getStringExtra("DialogIntent")

        if (dialogIntentStr != null) {
            when (dialogIntentStr) {
                "setNote" -> {
                    edtxDialog.visibility = View.VISIBLE
                    btnOk.visibility = View.VISIBLE
                    txTitle.text = "Pin a Note"

                    btnOk.setOnClickListener {
                        if (edtxDialog.text.isNotEmpty()) {
                            penNote = edtxDialog.text.toString()
                            remoteViews?.setTextViewText(R.id.edtx_pen, penNote)
                            appWidM.updateAppWidget(newAppWidget, remoteViews)
                        }
                        finish()
                    }
                }
                "Menu" -> {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    txTitle.text = "Menu"
                    llMenu.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                }
                "SongCover" -> {
                    txTitle.visibility = View.VISIBLE
                    imgvSongCover.visibility = View.VISIBLE
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
                                rewardedInterstitialAd?.show(this@DialogActivity) { rewardItem ->
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
                "PC" -> {
                    llDialog.visibility = View.GONE
                    getFavoriteContacts(applicationContext)
                    pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val contactUri = result.data?.data
                            if (contactUri != null) {
                                getContactInfo(contactUri)
                            }
                        }
                    }
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    try { pickContactLauncher.launch(intent) } catch (ex: Exception) { finish() }
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
                    txContent.visibility = View.VISIBLE
                    txTitle.text = " " + twitterProfileName
                    findViewById<ImageButton>(R.id.tw_config).apply {
                        visibility = View.VISIBLE
                        setOnClickListener { makeToast(applicationContext, "Paid Feature, coming soon!") }
                    }
                    if (listTweets.isNotEmpty()) {
                        txContent.text = listTweets[Random.nextInt(0, listTweets.size)]
                    } else {
                        txContent.text = "fetching Data.. visit again later, please"
                        rawTweets(false)
                    }
                }
                "stepsInfo" -> {
                    txTitle.text = "Weekly Steps"
                    val vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
                    val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                    vpSteps.visibility = View.VISIBLE
                    tabLayout.visibility = View.VISIBLE
                    imgbtnShare.visibility = View.GONE
                    
                    val currentDay = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
                    stepsData[currentDay] = stepsToday.toString()

                    stepsMapsAdapter(stepsData)
                    
                    // Set to a middle position for circular scrolling
                    val mid = 3500 - (3500 % 7) + currentDay
                    vpSteps.setCurrentItem(mid, false)
                    
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
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
                        .filter { 
                            val mins = it.usageTime.split(":")[0].trim().toIntOrNull() ?: 0
                            mins in 1..500 
                        }
                        .sortedByDescending { it.usageTime.split(":")[0].trim().toInt() }
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
                    txAvgUsage.text = "Avg Usage/Day ~ $hour Hours : $min Mins"

                    remoteViews?.setTextViewText(R.id.tx_screentime, "$hour+")
                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                }
                "AddNote" -> {
                    txTitle.text = "Add Note"
                    edtxDialog.visibility = View.VISIBLE
                    edtxDialog.setHint("Enter Note to be Pinned...")
                    edtxDialog.requestFocus()
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    btnOk.setOnClickListener {
                        if (edtxDialog.text.toString().isNotEmpty()) pinNote = edtxDialog.text.toString()
                        Thread { SetWallWorker.setWall(true, dialogActContext) }.start()
                        finish()
                    }
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

    fun checkBluetoothState(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (bluetoothAdapter == null) return
        if (!bluetoothAdapter!!.isEnabled) {
            menuBlue.setImageResource(R.drawable.blue_off)
            return
        }
        val isConnected = isProfileConnected(bluetoothAdapter!!, android.bluetooth.BluetoothProfile.GATT) ||
                isProfileConnected(bluetoothAdapter!!, android.bluetooth.BluetoothProfile.A2DP) ||
                isProfileConnected(bluetoothAdapter!!, android.bluetooth.BluetoothProfile.HEADSET)
        menuBlue.setImageResource(if (isConnected) R.drawable.blue_on else R.drawable.blue_red)
    }

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
            val cameraId = cameraManager.cameraIdList[0]
            val isTorchOn = sharedPreferences.getBoolean("Torch", false)
            cameraManager.setTorchMode(cameraId, !isTorchOn)
            sharedPreferencesEditor.putBoolean("Torch", !isTorchOn).apply()
            checkTorchState()
            remoteViews?.setImageViewResource(R.id.menu_torch, if (!isTorchOn) R.drawable.torch_on else R.drawable.torch_off)
            appWidM.updateAppWidget(newAppWidget, remoteViews)
        } catch (e: Exception) {}
    }

    fun sumTimes(times: List<String>): String {
        val totalDuration = times.fold(Duration.ZERO) { acc, time ->
            val parts = time.split(":").map { it.trim().toIntOrNull() ?: 0 }
            acc + (parts.getOrNull(0) ?: 0).minutes + (parts.getOrNull(1) ?: 0).seconds
        }
        return totalDuration.toComponents { hours, minutes, _, _ -> "%02d:%02d".format(hours, minutes) }
    }

    private fun stepsMapsAdapter(stepsData: ArrayList<String>) {
        stepsAdapter = StepsAdapter(stepsData)
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

    private fun getContactInfo(contactUri: Uri) {
        contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                val contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                markAsFav(contactId)
                saveContacts()
                updateWidget()
                finish()
            }
        }
    }

    private fun toggleBluetooth() {
        bluetoothAdapter?.let { adapter ->
            val action = if (!adapter.isEnabled) BluetoothAdapter.ACTION_REQUEST_ENABLE else "android.bluetooth.adapter.action.REQUEST_DISABLE"
            bluetoothLauncher.launch(Intent(action))
        } ?: makeToast(applicationContext, "Bluetooth not supported")
    }

    fun markAsFav(contactId: Long) {
        val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, 1) }
        contentResolver.update(ContactsContract.Contacts.CONTENT_URI, values, "${ContactsContract.Contacts._ID} = ?", arrayOf(contactId.toString()))
        getFavoriteContacts(applicationContext)
    }

    private fun saveContacts() {
        val json = Gson().toJson(favContacts)
        sharedPreferencesEditor.putString("CTS", json).apply()
    }

    private fun getTweetID(str: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url("https://twitter241.p.rapidapi.com/user?username=$str")
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com").build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@launch
                val json = JSONObject(responseBody)
                val restId = json.getJSONObject("result").getJSONObject("data").getJSONObject("user").getJSONObject("result").getString("rest_id")
                withContext(Dispatchers.Main) { getTweets(restId, true) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { makeToast(applicationContext, "User not found") }
            }
        }
    }

    private fun getTweets(twitterID: String, b: Boolean) {
        val client = OkHttpClient()
        val request = Request.Builder().url("https://twitter241.p.rapidapi.com/user-tweets?user=$twitterID&count=5")
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com").build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@launch
                val json = JSONObject(responseBody)
                // Parsing logic simplified for brevity
                withContext(Dispatchers.Main) { updateWidget() }
            } catch (e: Exception) {}
        }
    }

    private fun rawTweets(b: Boolean) {
        val dataArray = TweetsJsonParser.parseJsonArrayFromRaw(this, R.raw.np_tweets) ?: return
        for (i in 0 until dataArray.length()) {
            listTweets.add(dataArray.getJSONObject(i).getString("text"))
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
    }
}
