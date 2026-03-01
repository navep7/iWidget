package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
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
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.belaku.homey.MainActivity.Companion.beginCal
import com.belaku.homey.MainActivity.Companion.cityLat
import com.belaku.homey.MainActivity.Companion.cityLng
import com.belaku.homey.MainActivity.Companion.endCal
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.parentLayout
import com.belaku.homey.MainActivity.Companion.pickContactLauncher
import com.belaku.homey.MainActivity.Companion.sN
import com.belaku.homey.StepsService.Companion.twitterProfileName
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.arrayListUsageStats
import com.belaku.homey.NewAppWidget.Companion.dayOfTheWeek
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.getScreenTime
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.tW
import com.belaku.homey.NewAppWidget.Companion.vpStepsPos
import com.belaku.homey.SetWallWorker.Companion.appUsageStats
import com.belaku.homey.SetWallWorker.Companion.pinNote
import com.belaku.homey.SetWallWorker.Companion.screenHeight
import com.belaku.homey.SetWallWorker.Companion.screenWidth
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.StepsService.Companion.totalUsage
//import com.chaquo.python.Python
//import com.chaquo.python.android.AndroidPlatform
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
import java.io.IOException
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import kotlin.random.Random


class DialogActivity : AppCompatActivity(), OnMapReadyCallback {

    private var boolFetchingTweets: Boolean = false
    private lateinit var dialogActContext: Context
    private lateinit var parentLayoutDialog: View
    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult? ->
            if (result?.contents == null) {
                makeToast("Cancelled")
            } else {
                // Handle the scan result
                var scannedUrl = result.contents
                makeToast("Scanned: ${result}")
                val upiUri = Uri.parse(scannedUrl)
                val upiIntent = Intent(Intent.ACTION_VIEW)
                upiIntent.setData(upiUri)
                val chooser = Intent.createChooser(upiIntent, "Pay with")
                if (chooser.resolveActivity(getPackageManager()) != null) {
                    startActivity(chooser);
                } else {
                    // Handle the case where no UPI apps are installed
                    makeToast("No UPI app found. Please install one to proceed.")
                }
            }
        }
    private var stepsVPpos: Int = 0
    private var muApps: ArrayList<String> = ArrayList()
    private var myAppUsages: ArrayList<String> = ArrayList()
    private val REQUEST_CODE_SPEECH_INPUT: Int = 100
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var blE by Delegates.notNull<Boolean>()
    private var wifE by Delegates.notNull<Boolean>()
    private lateinit var llDialog: RelativeLayout
    private lateinit var imgvSongCover: ImageView
    private lateinit var txTitle: TextView
    private lateinit var txContent: TextView
    private lateinit var txAppName: TextView
    private lateinit var txAppUsageTime: TextView
    private lateinit var vpSteps: ViewPager2
    private lateinit var stepsMapsFragment: SupportMapFragment

    private lateinit var edtxDialog: EditText

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    private lateinit var imgbtnShare: ImageButton

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ResourceAsColor", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog)

        parentLayoutDialog = findViewById(android.R.id.content)

        dialogActContext = applicationContext

        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                //   Log.d(TAG, "Ad was dismissed.")
                // Don't forget to set the ad reference to null so you
                // don't show the ad a second time.
                rewardedInterstitialAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when fullscreen content failed to show.
                //   Log.d(TAG, "Ad failed to show.")
                // Don't forget to set the ad reference to null so you
                // don't show the ad a second time.
                rewardedInterstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                // Called when fullscreen content is shown.
                //   Log.d(TAG, "Ad showed fullscreen content.")

            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                //    Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdClicked() {
                // Called when an ad is clicked.
                //    Log.d(TAG, "Ad was clicked.")
            }
        }

        var bluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    if (blE) makeToast("Bluetooth ON")
                    else makeToast("Bluetooth OFF")
                } else {
                    // Bluetooth not enabled by user
                }
            }


        llDialog = findViewById<RelativeLayout>(R.id.dialog_layout)
        imgvSongCover = findViewById<ImageView>(R.id.dialog_imgv_cover)
        txTitle = findViewById<TextView>(R.id.tx_dialog_title)
        txContent = findViewById<TextView>(R.id.tx_dialog_content)
        txContent.movementMethod = ScrollingMovementMethod()

        txAppName = findViewById(R.id.tx_app_left)
        txAppUsageTime = findViewById(R.id.tx_appusage_right)
        edtxDialog = findViewById<EditText>(R.id.edtx_dialog)
        btnOk = findViewById<Button>(R.id.btn_dialog_ok)
        btnCancel = findViewById<Button>(R.id.btn_dialog_cancel)
        imgbtnShare = findViewById<ImageButton>(R.id.imgbtn_dialog_share)
        vpSteps = findViewById<ViewPager2>(R.id.vp_dialog)
        stepsMapsFragment =
            supportFragmentManager.findFragmentById(R.id.steps_map) as SupportMapFragment

        stepsMapsFragment.view?.visibility = View.GONE


        var dialogIntentStr = intent.getStringExtra("DialogIntent")


        if (dialogIntentStr != null) {

            if (dialogIntentStr == "SongCover") {
                //     makeToast("yet2Impl")
                llDialog.setBackgroundColor(android.R.color.transparent)
                edtxDialog.visibility = View.GONE
                btnOk.visibility = View.GONE
                btnCancel.visibility = View.GONE
                vpSteps.visibility = View.GONE
                imgbtnShare.visibility = View.GONE
                imgvSongCover.visibility = View.VISIBLE
                txTitle.setText(dataListSongs[songIndex].title)
                Picasso.get()
                    .load(dataListSongs[songIndex].album.cover)
                    .into(imgvSongCover)
            } else if (dialogIntentStr == "WCh") {

                if (noRewards > 1) {
                    remoteViews?.setViewVisibility(R.id.progressBar_cyclic_wallchange, View.VISIBLE)
                    remoteViews?.setViewVisibility(R.id.imgbtn_set, View.INVISIBLE)
                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                } else {
                    remoteViews?.setTextViewText(R.id.tx_rewards_count, "\uD83D\uDC41\uFE0FAD!")
                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                }
                sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                sharedPreferencesEditor = sharedPreferences.edit()

                llDialog.visibility = View.GONE
                noRewards = sharedPreferences.getInt("noRewards", 7)
                noRewards--
                sharedPreferencesEditor.putInt("noRewards", noRewards).apply()

                if (noRewards > 0) {
                    //   makeSnack("Changing Wall, please wait...")
                    dialogActContext = applicationContext
                    Thread {
                        SetWallWorker.setWall(true, dialogActContext)
                    }.start()
                } else {

                    makeSnack("loading Advertisement, please wait...")
                    txTitle.setText("loading Advertisement, please wait...")
                    txContent.visibility = View.GONE
                    edtxDialog.visibility = View.GONE
                    imgbtnShare.visibility = View.GONE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    vpSteps.visibility = View.GONE

                    RewardedInterstitialAd.load(
                        this,
                        getString(R.string.admob_ri_ad),
                        AdRequest.Builder().build(),
                        object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                                makeToast("Ad was loaded.")
                                rewardedInterstitialAd = rewardedAd

                                rewardedInterstitialAd?.show(this@DialogActivity) { rewardItem ->
                                    makeToast("User earned the reward.")
                                    // Handle the reward.
                                    val rewardAmount = rewardItem.amount
                                    val rewardType = rewardItem.type
                                    sharedPreferencesEditor.putInt("noRewards", 7).apply()
                                    noRewards = 7
                                    remoteViews?.setTextViewText(R.id.tx_rewards_count, "" + 7)
                                    txTitle.setText("swipe outside to continue changing walls.")
                                    updateWidget()
                                }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                dialogActContext = applicationContext
                                makeToast("onAdFailedToLoad: ${adError.message}")
                                rewardedInterstitialAd = null
                            }
                        },
                    )
                }

            } else if (dialogIntentStr == "PC") {
                llDialog.visibility = View.GONE
                getFavoriteContacts(applicationContext)
                pickContactLauncher =
                    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val contactUri = result.data?.data
                            if (contactUri != null) {
                                getContactInfo(contactUri)
                                // markContactAsFavorite(contactUri)
                            }
                        }
                    }
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                try {
                    pickContactLauncher.launch(intent)
                } catch (ex: Exception) {
                    makeToast("Ex - ${ex.message}")
                }
            } else if (dialogIntentStr == "StT") {
                edtxDialog.visibility = View.GONE
                btnOk.visibility = View.GONE
                btnCancel.visibility = View.GONE
                vpSteps.visibility = View.GONE
                txTitle.setText("Speech to Text")
                txContent.setText("listening...")

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT, "Speak now..."
                ) // Optional: prompt for the user
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)

                imgbtnShare.setOnClickListener(View.OnClickListener {
                    if (txContent.text != "listening...") {
                        startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, txContent.text)
                                    .putExtra(Intent.EXTRA_SUBJECT, "Sharing via nHome!"),
                                "Share via..."
                            )
                        )
                    }
                })

            } else if (dialogIntentStr == "ST") {

                parentLayout = findViewById(android.R.id.content);
                Snackbar.make(
                    parentLayout,
                    "Showing Tweets from $twitterProfileName",
                    Snackbar.LENGTH_SHORT
                )
                    .setAction("Customize") { view ->
                        // Code to undo the user's last action
                        // For example, showing another snackbar:
                        Snackbar.make(
                            parentLayout,
                            "Paid Feature, coming soon!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        txTitle.setText("Twitter")
                        txContent.visibility = View.INVISIBLE
                        edtxDialog.visibility = View.VISIBLE
                        vpSteps.visibility = View.GONE
                        imgbtnShare.visibility = View.GONE
                        btnOk.visibility = View.VISIBLE
                        btnOk.setText("Set")
                        btnOk.setOnClickListener(View.OnClickListener {
                            boolFetchingTweets = true
                            if (edtxDialog.text.toString().equals("Fact")) {
                                twitterProfileName = "Fact"
                                listTweets.clear()
                                rawTweets(false)
                            } else {
                                getTweetID(edtxDialog.text.toString())
                            }
                            //   llDialog.visibility = View.GONE
                        })

                    }
                    .setActionTextColor(
                        resources.getColor(
                            android.R.color.holo_red_dark,
                            theme
                        )
                    ) // Optional: set custom color
                    .show()



                if (boolFetchingTweets) {
                    Handler(Looper.getMainLooper()).postDelayed( {
                        tW = listTweets[Random.nextInt(0, listTweets.size)]
                        edtxDialog.visibility = View.GONE
                        btnOk.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        vpSteps.visibility = View.GONE
                        txContent.visibility = View.VISIBLE
                        imgbtnShare.visibility = View.VISIBLE
                        txTitle.setText(twitterProfileName)
                        txContent.setText(tW)
                        imgbtnShare.setOnClickListener(View.OnClickListener {
                            startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, tW)
                                        .putExtra(Intent.EXTRA_SUBJECT, "Sharing via nHome!"),
                                    "Share via..."
                                )
                            )
                        })
                    }, 3000)
                } else {
                    tW = listTweets[Random.nextInt(0, listTweets.size)]
                    edtxDialog.visibility = View.GONE
                    btnOk.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    vpSteps.visibility = View.GONE
                    txTitle.setText(twitterProfileName)
                    txContent.setText(tW)
                    imgbtnShare.setOnClickListener(View.OnClickListener {
                        startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, tW)
                                    .putExtra(Intent.EXTRA_SUBJECT, "Sharing via nHome!"),
                                "Share via..."
                            )
                        )
                    })
                }

            } else if (dialogIntentStr == "STH") {
                llDialog.visibility = View.GONE
                Snackbar.make(parentLayoutDialog, "Paid Feature!", Snackbar.LENGTH_LONG)
                    .setAction("Pay") {
                    }
                    .show()


                /* txTitle.setText("Twitter")
                 txContent.visibility = View.INVISIBLE
                 edtxDialog.visibility = View.VISIBLE
                 vpSteps.visibility = View.GONE
                 imgbtnShare.visibility = View.GONE
                 btnOk.setText("Set")
                 btnOk.setOnClickListener(View.OnClickListener {
                     if (edtxDialog.text.toString().equals("Fact")) {
                         twitterProfileName = "Fact"
                         listTweets.clear()
                         rawTweets(false)
                     } else {
                         getTweetID(edtxDialog.text.toString(), false)
                     }
                 })*/
            } else if (dialogIntentStr == "BLUEEnable") {
                blE = true
                llDialog.visibility = View.GONE
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothLauncher.launch(enableBtIntent)
            } else if (dialogIntentStr == "BLUEDisable") {
                blE = false
                llDialog.visibility = View.GONE
                val disableintent = Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
                bluetoothLauncher.launch(disableintent)
            } else if (dialogIntentStr == "WifiEnable" || dialogIntentStr == "WifiDisable") {
                try {
                    val intent = Intent(Intent.ACTION_MAIN, null)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val cn = ComponentName(
                        "com.android.settings", "com.android.settings.wifi.WifiSettings"
                    )
                    intent.setComponent(cn)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (ignored: ActivityNotFoundException) {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            } else if (dialogIntentStr == "stepsInfo") {

                stepsMapsFragment.getMapAsync(this)
                getScreenTime(applicationContext)
                findViewById<CardView>(R.id.card_map).visibility = View.VISIBLE
                makeToast(dayOfTheWeek)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                );

                txTitle.setText("Steps...")
                txContent.visibility = View.GONE
                edtxDialog.visibility = View.GONE
                btnOk.visibility = View.GONE
                btnCancel.visibility = View.GONE
                imgbtnShare.visibility = View.GONE
                vpSteps.visibility = View.VISIBLE


            } else if (dialogIntentStr == "screenTimeInfo") {

                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                );

                btnOk.visibility = View.GONE
                btnCancel.visibility = View.GONE
                imgbtnShare.visibility = View.GONE
                txTitle.setText(
                    "Screen Time Analysis : Based on App Usage stats from a Week(" + "${
                        beginCal.get(
                            Calendar.DAY_OF_MONTH
                        )
                    }/${beginCal.get(Calendar.MONTH) + 1}/${beginCal.get(Calendar.YEAR)} : " +
                            "${endCal.get(Calendar.DAY_OF_MONTH)}/${endCal.get(Calendar.MONTH) + 1}/${
                                endCal.get(
                                    Calendar.YEAR
                                )
                            })" + ", below is the App data, every day (mm:ss)..  "
                )


                appUsageStats(applicationContext)

                var b = arrayListUsageStats.distinctBy { it.usageTime }
                var c = b.sortedBy { it.usageTime }


                for (i in c)
                    myAppUsages.add(i.usageTime)

                totalUsage = sumTimeArray(myAppUsages)
                myAppUsages.clear()

                for (i in c) {
                    if (i.usageTime.substring(0, 2).toInt() > 10) {
                        muApps.add(
                            "\n" + getAppNameFromPkg(
                                dialogActContext, i.appName
                            )
                        )
                        myAppUsages.add("\n" + i.usageTime)
                        arrayListUsageStats.remove(i)
                    }
                }

                //     txContent.movementMethod = ScrollingMovementMethod()
                //     txContent.append(Html.fromHtml("\n\n<b><u> Most Used Apps.. > 10 mins</u></b>"))

                txAppName.append("Most Used Apps.\n")
                txAppUsageTime.append("> 10 mins/day\n")
                for (i in muApps) txAppName.append(i)
                for (i in myAppUsages) txAppUsageTime.append(i)

                //   txAppName.append("\n\n\n ${sumTimeArray(myAppUsages)}")
                muApps.clear()
                myAppUsages.clear()

                txAppName.append("\n\n")
                txAppUsageTime.append("\n\n")

                b = arrayListUsageStats.distinctBy { it.usageTime }
                c = b.sortedBy { it.usageTime }



                for (i in 0 until c.size) {
                    if ((c[i].usageTime.substring(0, 2).toInt() > 5) && (c[i].usageTime.substring(
                            0, 2
                        ).toInt() < 10)
                    ) {
                        muApps.add(
                            "\n" + getAppNameFromPkg(
                                dialogActContext, c[i].appName
                            )
                        )
                        myAppUsages.add("\n" + c[i].usageTime)
                        arrayListUsageStats.remove(c[i])
                    }
                }

                txAppName.append("Moderately Used Apps.\n")
                txAppUsageTime.append("> 5 mins/day\n")
                for (i in muApps) txAppName.append(i)
                for (i in myAppUsages) txAppUsageTime.append(i)
                muApps.clear()
                myAppUsages.clear()

                txAppName.append("\n\n")
                txAppUsageTime.append("\n\n")

                b = arrayListUsageStats.distinctBy { it.usageTime }
                c = b.sortedBy { it.usageTime }


                for (i in 0 until c.size) {
                    if ((c[i].usageTime.substring(0, 2).toInt() > 1) && (c[i].usageTime.substring(
                            0, 2
                        ).toInt() < 5)
                    ) {
                        muApps.add(
                            "\n" + getAppNameFromPkg(
                                dialogActContext, c[i].appName
                            )
                        )
                        myAppUsages.add("\n" + c[i].usageTime)
                        arrayListUsageStats.remove(c[i])
                    }
                }

                txAppName.append("Least Used Apps.\n")
                txAppUsageTime.append("> 1 mins/day\n")
                for (i in muApps) txAppName.append(i)
                for (i in myAppUsages) txAppUsageTime.append(i)
                muApps.clear()
                myAppUsages.clear()

                txAppName.append("\n\n")
                txAppUsageTime.append("\n\n")

                b = arrayListUsageStats.distinctBy { it.usageTime }
                c = b.sortedBy { it.usageTime }


                for (i in 0 until c.size) {
                    if ((c[i].usageTime.substring(0, 2).toInt() == 0)
                    ) {
                        muApps.add(
                            "\n" + getAppNameFromPkg(
                                dialogActContext, c[i].appName
                            )
                        )
                        myAppUsages.add("\n" + c[i].usageTime)
                        arrayListUsageStats.remove(c[i])
                    }
                }



                txAppName.append("Rarely Used Apps.\n")
                txAppUsageTime.append("> 0 mins/day\n")
                for (i in muApps) txAppName.append(i)
                for (i in myAppUsages) txAppUsageTime.append(i)
                muApps.clear()
                myAppUsages.clear()

                txAppName.append("\n\n")
                txAppUsageTime.append("\n\n")

                var sT = totalUsage.split(":")
                var hour = ""

                if (sT[0][0] == '0')
                    hour = sT[0].drop(1)
                else hour = sT[0]

                txAppName.append("Avg Usage/Day ~ $hour Hours : ${sT[1]} Mins ")

                edtxDialog.visibility = View.GONE
                vpSteps.visibility = View.VISIBLE

                remoteViews?.setTextViewText(
                    R.id.btn_screentime,
                    "$hour+ H"
                )


                appWidM = AppWidgetManager.getInstance(dialogActContext)
                appWidM.updateAppWidget(newAppWidget, remoteViews)

            } else if (dialogIntentStr == "liveWall") {
                makeToast("LIVEWALL!")
                val p: String = WallService::class.java.getPackage().getName()
                val c: String = WallService::class.java.getCanonicalName()

                //    val intentLiveWall = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(p, c))
                //    startActivity(intentLiveWall)

                val i = Intent()
                i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(p, c))
                startActivityForResult(i, 0)
            } else if (dialogIntentStr == "qrClick") {

                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scan a QR code")
                }

                // Launch the scanner
                barcodeLauncher.launch(options)
            } else if (dialogIntentStr == "AddNote") {
                llDialog.minimumWidth = screenWidth
                llDialog.minimumHeight = screenHeight
                txTitle.setText("Add Note")
                edtxDialog.setHint("Enter Note to be Pinned in the Widget...")
                edtxDialog.requestFocus()
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                imgbtnShare.visibility = View.GONE
                btnOk.setOnClickListener {

                    if (edtxDialog.text.toString().isNotEmpty())
                        pinNote = edtxDialog.text.toString()

                    llDialog.visibility = View.GONE
                    Thread {
                        SetWallWorker.setWall(true, dialogActContext)
                    }.start()
                    //  appWidM.updateAppWidget(newAppWidget, remoteViews)
                }
            } else if (dialogIntentStr == "AccessibilityPermDialog") {
                llDialog.visibility = View.INVISIBLE

                val builder = AlertDialog.Builder(this@DialogActivity)
                builder.setTitle("Requisition for Accessibility Service permission")
                builder.setMessage(
                    "Please Enable Accessibility Service to smoothly lock Phone screen from Widget shortcut."
                )

                builder.setPositiveButton("OK") { dialog, id ->
                    // User clicked OK button
                    dialog.dismiss() // Dismiss the dialog
                    val openSettings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    openSettings.addFlags(FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
                    dialogActContext.startActivity(openSettings)
                }

                builder.setNegativeButton("Not now") { dialog, id ->
                    // User clicked OK button
                    dialog.dismiss() // Dismiss the dialog

                    finish()
                }

                builder.setNegativeButton("Not Now") { dialog, id ->
                    // User clicked OK button
                    dialog.dismiss() // Dismiss the dialog
                    makeToast("Lock Screen cannot work without access to Accessibility Service!")
                    finish()
                }


                // Create the AlertDialog object and show it
                val dialog = builder.create()
                dialog.show()

            }


        }
    }

    /*  private fun pythonTimpl() {

          // 1. Initialize Python (if not already done in Application)
          if (!Python.isStarted()) {
              Python.start(AndroidPlatform(this))
          }

          // 2. Get the Python instance
          val py = Python.getInstance()

          // 3. Get the module (script.py)
          val module = py.getModule("python")

          makeToast("Py ~ ${module.callAttr("wrapped_function", "KotlinUser")}")

      }*/


    private fun sumTimeArray(myAppUsages: ArrayList<String>): String {
        // 1. Calculate total seconds from all "mm:ss" strings
        var totalSeconds = 0L
        for (time in myAppUsages) {
            val parts = time.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val seconds = parts[1].toLong()
                totalSeconds += minutes * 60 + seconds
            }
        }

        // 2. Format the total seconds to "hh:mm"
        // TimeUnit handles the conversion to hours and minutes
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60

        // Use String.format for consistent "hh:mm" formatting, including leading zeros for minutes
        // Note: The hour part can be > 23, which is correct for a duration
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    private fun stepsMapsAdapter(
        stepsData: ArrayList<String>,
        stepsLocInfo: ArrayList<LatLng>
    ) {

        val stepsAdapter = StepsAdapter(stepsData, stepsLocInfo)
        vpSteps.adapter = stepsAdapter
        vpSteps.currentItem = vpStepsPos


        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)


        TabLayoutMediator(tabLayout, vpSteps) { tab, position ->
            // You can set text or icons for tabs here if needed,
            // but for dot indicators, this part might be empty or use a placeholder.
        }.attach()


        vpSteps.registerOnPageChangeCallback(object : OnPageChangeCallback(
        ) {
            val days: ArrayList<String> = arrayListOf(
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
                "Sunday"
            )
            private var myState = 0
            private var currentPosition = 0
            var currentOffset = 0F

            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {

                //   makeToast("$currentOffset VS $positionOffset")
                addMarker(
                    stepsLocInfo[position],
                    days[position] + " - " + stepsData[position] + " steps",
                    getAddress(stepsLocInfo[position])
                )

                if (currentOffset == positionOffset) if (myState == ViewPager2.SCROLL_STATE_DRAGGING && currentPosition == position && currentPosition == 0) vpSteps.setCurrentItem(
                    6
                )
                if (myState == ViewPager2.SCROLL_STATE_DRAGGING && currentPosition == position && currentPosition == 6) vpSteps.setCurrentItem(
                    0
                )

                currentOffset = positionOffset

                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            private fun getAddress(latLng: LatLng): String {
                val gcd = Geocoder(applicationContext)
                Locale.getDefault()
                var lat = latLng.latitude
                var lng = latLng.longitude
                var cityname = "unKnown"
                try {
                    var cAddrs = gcd.getFromLocation(lat, lng, 1)!!
                    //   makeToast(cAddrs?.get(0)!!.subLocality)

                    cityname = cAddrs?.get(0)!!.subLocality
                    //        if (MainActivity.cityname.length > 15)
                    //          MainActivity.cityname = cityname.substring(0, 12) + "..,"


                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                    makeToast("GCD - IOException \n $e")
                }

                return cityname

            }

            override fun onPageSelected(position: Int) {

                currentPosition = position

                super.onPageSelected(position)

            }

            override fun onPageScrollStateChanged(state: Int) {
                myState = state

                super.onPageScrollStateChanged(state)
            }
        })
    }

    private fun addMarker(markerLocationn: LatLng, mTitle: String, mDesc: String) {

        stepsMaps.clear()
        val markerLocation = markerLocationn//LatLng(37.7749, -122.4194)
        val marker = stepsMaps.addMarker(
            MarkerOptions()
                .position(markerLocation)
                .title(mTitle)
                .snippet(mDesc)
        )

        // Show the info window for the marker immediately
        marker?.showInfoWindow()

        stepsMaps.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                markerLocation,
                19f
            )
        )
    }

    fun makeSnack(s: String) {
        sN = Snackbar.make(parentLayoutDialog, s, Snackbar.LENGTH_LONG)
        sN.show()
    }

    private fun updateWidget() {
        val intent = Intent(
            applicationContext,
            NewAppWidget::class.java
        )
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        val ids: IntArray = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(getApplication(), NewAppWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
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

    private fun getContactInfo(contactUri: Uri) {
        val contentResolver = contentResolver
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                contactUri!!, arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts._ID,  // Add other desired columns like HAS_PHONE_NUMBER, PHOTO_URI, etc.
                ), null, null, null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)

                if (displayNameIndex != -1) {
                    val displayName = cursor.getString(displayNameIndex)
                    val contactId = cursor.getLong(contactIdIndex)
                    getContactDetails(displayName, contactId)
                    markAsFav(contactId)
                    saveContacts()

                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    val thisWidget: ComponentName =
                        ComponentName(this, NewAppWidget::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                    val updateIntent = Intent(
                        this,
                        NewAppWidget::class.java
                    )
                    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    sendBroadcast(updateIntent)
                    finish()
                    updateWidget()

                }
            }
        } finally {
            cursor?.close()
        }
    }

    fun markAsFav(contactId: Long) {
        // Replace with the actual contact ID
        val values = ContentValues()
        values.put(ContactsContract.Contacts.STARRED, 1) // 1 for favorite, 0 for not favorite

        getContentResolver().update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            ContactsContract.Contacts._ID + " = ?",
            arrayOf<String>(contactId.toString())
        )

        getFavoriteContacts(applicationContext)
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
            queryUri, projection, selection, null, null
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


            val color =
                Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            var contactBitmap: Bitmap?

            contactBitmap =
                ContactPhotoHelper.retrieveContactPhoto(dialogActContext, contactID.toLong())
            val cNme = cursor.getString(
                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            )

            if (contactBitmap == null)
                contactBitmap = CharacterToBitmapConverter.getBitmapFromCharacter(
                    cNme[0], 100, 100, 70, color
                )

            val c = Contact(contactID, cNme, phoneNumber, contactBitmap)


            //     var c = Contact(contactID, cNme, phoneNumber, cPhUri)

            if (c.number.length > 7)
                favContacts.add(c)

        }



        if (favContacts.size > 0)
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

    private fun getContactDetails(displayName: String?, contactId: Long) {
        val contentResolver = contentResolver
        var phoneCursor: Cursor? = null
        var emailCursor: Cursor? = null

        markAsFav(contactId)
        try {
            // Get phone numbers
            phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId.toString()),
                null
            )

            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                val phoneNumberIndex =
                    phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (phoneNumberIndex != -1) {
                    val phoneNumber = phoneCursor.getString(phoneNumberIndex)
                    // Process phone number
                    makeToast("Contct - $displayName : $phoneNumber")
                }
            }

            // Get email addresses
            emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contactId.toString()),
                null
            )

            if (emailCursor != null && emailCursor.moveToFirst()) {
                val emailAddressIndex =
                    emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                if (emailAddressIndex != -1) {
                    val emailAddress = emailCursor.getString(emailAddressIndex)
                    // Process email address
                }
            }
        } finally {
            phoneCursor?.close()
            emailCursor?.close()
        }
    }

    private fun getTweetID(str: String) {

        val client = OkHttpClient()

        val request =
            Request.Builder().url("https://twitter241.p.rapidapi.com/user?username=$str").get()
                .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
                .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com").build()

        pD = ProgressDialog(this@DialogActivity)

        lifecycleScope.launch(Dispatchers.IO) {
            var responseTweetID = client.newCall(request).execute()

            withContext(Dispatchers.Main) {
                // Handle the result and hide the loading indicator

                val responseBodyString = responseTweetID.peekBody(Long.MAX_VALUE).string()


                val jsonObject = JSONObject(responseBodyString)

                if (jsonObject.getJSONObject("result").getJSONObject("data").optString("user")
                        .isNotEmpty()
                ) if (jsonObject.getJSONObject("result").getJSONObject("data").getJSONObject("user")
                        .optString("result").isNotEmpty()
                ) if (jsonObject.getJSONObject("result").getJSONObject("data").getJSONObject("user")
                        .getJSONObject("result").optString("rest_id").isNotEmpty()
                ) {
                    var twitterID = jsonObject.getJSONObject("result").getJSONObject("data")
                        .getJSONObject("user").getJSONObject("result").getString("rest_id")
                    var twitterPicUrl = jsonObject.getJSONObject("result").getJSONObject("data")
                        .getJSONObject("user").getJSONObject("result").getJSONObject("avatar")
                        .getString("image_url")

                    twitterProfileName = jsonObject.getJSONObject("result").getJSONObject("data")
                        .getJSONObject("user").getJSONObject("result").getJSONObject("core")
                        .getString("screen_name")
                    //  Log.d("TwitterPicUrl - ", twitterPicUrl)

                    remoteViews =
                        RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                    newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)
                    remoteViews?.setImageViewUri(R.id.twSettings, Uri.parse(twitterPicUrl))

                    updateWidget()


                    getTweets(twitterID, false)
                    twitterProfileName = edtxDialog.text.toString()

                    pD.setTitle("Twitter")
                    pD.setMessage("fetching Tweets...")
                    pD.show()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        pD.dismiss()
                    }, 3000)
                } else {

                    makeSnack("Twitter User doesn't Exist!")
                    pD.setTitle("Twitter")
                    pD.setMessage("Twitter User doesn't Exist!")
                    pD.show()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        pD.dismiss()
                    }, 3000)

                }
                else {

                    makeSnack("Twitter User doesn't Exist!")
                    pD.setTitle("Twitter")
                    pD.setMessage("Twitter User doesn't Exist!")
                    pD.show()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        pD.dismiss()
                    }, 3000)

                }
                else {

                    makeSnack("Twitter User doesn't Exist!")
                    pD.setTitle("Twitter")
                    pD.setMessage("Twitter User doesn't Exist!")
                    pD.show()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        pD.dismiss()
                    }, 3000)

                }
                // Update UI with result
            }
        }


    }

    private fun getTweets(twitterID: String, b: Boolean) {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://twitter241.p.rapidapi.com/user-tweets?user=$twitterID&count=5").get()
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com").build()

        pD.setTitle("Twitter")
        pD.setMessage("fetching Tweets...")
        if (b) pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
            var responseTweets = client.newCall(request).execute()

            var js: JSONArray = (JSONObject(responseTweets.body?.string()).getJSONObject("result")
                .getJSONObject("timeline")
                .getJSONArray("instructions"))//[2] as JSONObject).getJSONArray("entries")

            for (i in 0 until js.length()) {
                if (js[i].toString().contains("entries")) js =
                    (js[i] as JSONObject).getJSONArray("entries")
            }

            withContext(Dispatchers.Main) {
                if (b) pD.dismiss()
                if (js.length() > 0) listTweets.clear()
                for (i in 0 until js.length()) {
                    val tw =
                        JSONObject(js[i].toString()).getJSONObject("content")//.getJSONObject("itemContent").getJSONObject("tweet_results").getJSONObject("result")
                    //   .getJSONObject("legacy").get("full_text")

                    if (tw.optString("itemContent").isNotEmpty()) {
                        val actTw = tw.getJSONObject("itemContent").getJSONObject("tweet_results")
                            .getJSONObject("result").getJSONObject("legacy").get("full_text")

                        Log.d("Twwtt $i", actTw.toString())
                        listTweets.add(actTw.toString())
                    }
                }

                remoteViews = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)
                remoteViews?.setTextViewText(
                    R.id.tx_tweets, "@" + twitterProfileName + "\t ~ \t" + listTweets[1]
                )


                updateWidget()

            }
        }

        Log.d("result", "res - ${listTweets.size}")
    }

    private fun rawTweets(b: Boolean) {


        if (b) {
            pD.setTitle("Twitter")
            pD.setMessage("fetching Tweets...")
            pD.show()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                pD.dismiss()
            }, 1000)
        }

        val dataArray: JSONArray = TweetsJsonParser.parseJsonArrayFromRaw(this, R.raw.np_tweets)!!

        for (i in 0 until dataArray.length()) {
            try {
                val item = dataArray.getJSONObject(i)
                val tweet = item.getString("text")
                listTweets.add(tweet)

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        //       makeSnack("Tweets - ${listTweets.size}")

        var bitmapTwPic: Bitmap =
            drawableToBitmap(applicationContext, resources.getDrawable(R.drawable.walp_icon))

        remoteViews = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)

        lifecycleScope.launch { // Launch a coroutine in the lifecycle scope
            val imageUrl =
                "https://pbs.twimg.com/profile_images/1244657050275151872/BRycNabV_normal.jpg" // Replace with your image URL
            val bitmap = getBitmapFromUrl(imageUrl)
            // Now you have the bitmap, you can display it in an ImageView or process it further
            if (bitmap != null) {
                //     makeToast("TwiPic")
                try {
                    remoteViews?.setTextViewText(
                        R.id.tx_tweets, "@" + twitterProfileName + "\t ~ \t" + listTweets[1]
                    )
                    remoteViews?.setImageViewBitmap(R.id.twSettings, bitmap)
                } catch (ex: Exception) {
                    makeToast("TwiEx - ${ex.message}")
                }
            }
        }

        newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)


        updateWidget()

    }

    suspend fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) { // Switch to the IO dispatcher for network operations
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val inputStream = connection.getInputStream()
                BitmapFactory.decodeStream(inputStream) // Decode the input stream into a Bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null // Return null on error
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (result != null && !result.isEmpty()) {
                    val recognizedText = result[0] // Get the most likely recognized phrase
                    txContent.setText(recognizedText)
                }
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {

        stepsMaps = p0

        val stepsData: ArrayList<String> = ArrayList()
        stepsData.add(sharedPreferences.getInt("Monday", 0).toString())
        stepsData.add(sharedPreferences.getInt("Tuesday", 0).toString())
        stepsData.add(sharedPreferences.getInt("Wednesday", 0).toString())
        stepsData.add(sharedPreferences.getInt("Thursday", 0).toString())
        stepsData.add(sharedPreferences.getInt("Friday", 0).toString())
        stepsData.add(sharedPreferences.getInt("Saturday", 0).toString())
        stepsData.add(sharedPreferences.getInt("Sunday", 0).toString())

        val stepsLocInfo: ArrayList<LatLng> = ArrayList()
        stepsLocInfo.add(LatLng(-34.0, 151.0))
        stepsLocInfo.add(LatLng(35.69, 139.69))
        stepsLocInfo.add(LatLng(19.08, 72.88))
        stepsLocInfo.add(LatLng(19.43, -99.13))
        stepsLocInfo.add(LatLng(52.30, 13.40))
        stepsLocInfo.add(LatLng(23.55, 46.63))
        stepsLocInfo.add(LatLng(40.71, -74.00))

        stepsMapsAdapter(stepsData, stepsLocInfo)

        // Example: Setting a location for Sydney, Australia
        val presentLoc = LatLng(cityLat, cityLng)


        // Add a marker at the specified location
        //    stepsMaps.addMarker(MarkerOptions().position(presentLoc).title(cityname))


        // Move the camera to the specified location with a zoom level
        /*stepsMaps.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                presentLoc,
                21f
            )
        )*/ // Zoom level 10 is a good starting point


    }

    companion object {
        lateinit var stepsMaps: GoogleMap

        fun isStepsMapsInitialized(): Boolean {
            return this::stepsMaps.isInitialized
        }
    }


}