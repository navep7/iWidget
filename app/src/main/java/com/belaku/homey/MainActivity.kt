package com.belaku.homey

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.app.WallpaperManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.icu.util.Calendar
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Html
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.Window
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.arrayListUsageStats
import com.belaku.homey.NewAppWidget.Companion.choosenApps
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.newsBitmaps
import com.belaku.homey.NewAppWidget.Companion.newsLinks
import com.belaku.homey.NewAppWidget.Companion.newsList
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.screenHeight
import com.belaku.homey.NewAppWidget.Companion.screenWidth
import com.belaku.homey.NewAppWidget.Companion.tW
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.material.color.DynamicColors
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
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
import java.io.IOException
import java.net.URL
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.properties.Delegates
import kotlin.random.Random


class MainActivity : AppCompatActivity() {


    private val ALL_PERMISSIONS_REQUEST_CODE: Int = 100
    private lateinit var imageSliderAdapter: ImageSliderAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var imageList: ArrayList<Int>
    private lateinit var btnL: Button
    private lateinit var btnAR: Button
    private lateinit var btnRC: Button
    private lateinit var btnBT: Button
    private lateinit var btnPN: Button
    private lateinit var btnCP: Button
    private lateinit var btnAUS: Button
    private lateinit var btnAS: Button

    private lateinit var iDV: AlertDialog
    private lateinit var instructionsDialogView: View
    private lateinit var instructionsDialogBuilder: AlertDialog.Builder
    private lateinit var llInstructions: LinearLayout
    private val arrayListKeysTxViews: ArrayList<TextView> = ArrayList()
    private val arrayListKeys: ArrayList<String> = ArrayList()
    private var selectedKey: String = "Nature"

    private lateinit var mBluetoothReceiver: BroadcastReceiver
    private lateinit var dialogMessage: EditText
    private val REQUEST_CODE_SPEECH_INPUT: Int = 100
    private lateinit var editTextTwitterHandle: EditText
    private lateinit var twitterHandleDialog: View
    private lateinit var responseTweets: okhttp3.Response
    private lateinit var responseTweetID: okhttp3.Response
    private lateinit var twitterID: String
    private lateinit var twitterPicUrl: String
    private lateinit var connectivityManager: ConnectivityManager
    private val LOC_P: Int = 1
    private val ACTIVITY_RECOGNITION_P: Int = 2
    private val READ_CONTACTS_P: Int = 3
    private val BLUETOOTH_P: Int = 4
    private val NOTIfications_P: Int = 5
    private val CALLPHONE_P: Int = 6

    private val TAG: String = "MainActivity"
    private lateinit var frameMin: FrameLayout
    private lateinit var frameHour: FrameLayout
    private lateinit var frameDay: FrameLayout
    private lateinit var fabMin: FloatingActionButton
    private lateinit var fabHour: FloatingActionButton
    private lateinit var fabDay: FloatingActionButton
    private lateinit var rvAdapter: RvAdapter
    private lateinit var rvImages: RecyclerView
    private lateinit var editTextPrompt: EditText
    private var pexelUrl: String =
        "https://api.pexels.com/v1/search?query=$queryType&per_page=10"
    private lateinit var binding: ActivityMainBinding


    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingPermission", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        mainWindow = this.window

        mAct = this@MainActivity
        appContx = applicationContext

        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        launchers()

        MobileAds.initialize(
            this
        ) { initializationStatus -> //Showing a simple Toast Message to the user when The Google AdMob Sdk Initialization is Completed
            //   Toast.makeText( this@MainActivity, "AdMob Sdk Initialize $initializationStatus", Toast.LENGTH_LONG ).show()
        }

        val metrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(metrics)
        screenHeight = metrics.heightPixels
        screenWidth = metrics.widthPixels



        if (apps.size == 0)
            getApps()

        cDate = Calendar.getInstance().get(Calendar.DATE)
        if (sharedPreferences.getInt("Date", 0) == 0) {

            sharedPreferencesEditor.putInt("Date", cDate).apply()
        } else {
            if (cDate != sharedPreferences.getInt("Date", 0)) {
                sharedPreferencesEditor.putInt("Date", cDate).apply()
            }
        }
        cMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        cYear = Calendar.getInstance().get(Calendar.YEAR)


        var bluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    makeToast("Bluetooth enabled by user")
                } else {
                    // Bluetooth not enabled by user
                }
            }

        if (intent != null) {
            var intentStr = intent.getStringExtra("intent2Main")
            if (intentStr != null)
                makeToast(intentStr)

            if (intentStr.equals("BLUEDisable")) {
                val disableintent = Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
                bluetoothLauncher.launch(disableintent)
            } else if (intentStr.equals("BLUEEnable")) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothLauncher.launch(enableBtIntent)
            }

        }


        setSupportActionBar(binding.toolbar)


        parentLayout = findViewById(android.R.id.content);

        pD = ProgressDialog(this@MainActivity)
        pD.setMessage("fetching Walls...")

        DynamicColors.applyToActivitiesIfAvailable(application)

        queryType = sharedPreferences.getString("walltype", "Nature").toString()

        sharedPreferences.getStringSet("walls", null)?.let { imgUrls.addAll(it) }
        sharedPreferences.getStringSet("wallDescs", null)?.let { imgDescs.addAll(it) }

        connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


        findViewByIds()
        populateKeys()
        setRV(imgUrls, imgDescs)
        listeners()
        fetchWallpaper(applicationContext)
        GetDisplayDimens()

        //    getNews(cDate - 1)

        if ((ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PERMISSION_GRANTED)
        ) {
            getCity()
        }


        instructionsDialogBuilder = AlertDialog.Builder(this@MainActivity)
        val inflater = LayoutInflater.from(this@MainActivity)
        instructionsDialogView = inflater.inflate(R.layout.instructions_dialog, null)
        instructionsDialogBuilder.setView(instructionsDialogView)

        iDV = instructionsDialogBuilder.create()

        iDV.setCanceledOnTouchOutside(false) // Prevent dismissal on outside touch
        iDV.setCancelable(false)

        viewPager = instructionsDialogView.findViewById<ViewPager2>(R.id.viewPager);
        tabLayout = instructionsDialogView.findViewById(R.id.tabLayout);

        imageList = ArrayList<Int>()
        imageList.add(R.drawable.nhome_widget)
        imageList.add(R.drawable.widget_i)
        imageList.add(R.drawable.widget_i)

        imageSliderAdapter = ImageSliderAdapter(imgUrls, applicationContext)
        viewPager.adapter = imageSliderAdapter

        viewPager.offscreenPageLimit = 2

        viewPager.post { viewPager.currentItem = 17 }


        TabLayoutMediator(
            tabLayout, viewPager
        ) { tab: TabLayout.Tab?, position: Int -> }.attach()

        if (nPermissions())
            iDV.show()

        llInstructions = instructionsDialogView.findViewById<LinearLayout>(R.id.ll_instructions)
        llInstructions.orientation = LinearLayout.VERTICAL
        //    val messageView = dialogView.findViewById<TextView>(R.id.dialog_message)
        //    messageView.movementMethod = ScrollingMovementMethod()

        //all Ps at once



        addPermissionCards()


        addPermissionCard(
            "<b> Permission Request for App Usage Stats </b>- \n  to suggest \"④ Frequent Apps\" to use, based on previously used App stats.. ",
            "Permit",
            "AUS"
        )
        addPermissionCard(
            "<b> Requisition for Accessibility Service permission </b>- \n  to smoothly lock Phone screen from Widget shortcut.",
            "Permit",
            "AS"
        )

        var btnDone = Button(applicationContext)
        btnDone.text = "Done"

        btnDone.setOnClickListener {
            if (!nPermissions()) {
                rawTweets(false)
                getFavoriteContacts(appContx)
                iDV.dismiss()
            } else makeToast("Ensure all Ps are Granted")
        }
        llInstructions.addView(btnDone)



        instructionsDialogBuilder.setTitle("nHome Widget Highlights ~ underlined words in the below pic, explain...!")




        sharedPreferencesEditor.putString("qT", queryType).apply()

        fabMain.setOnClickListener { view ->

            if (fabMain.text == "Set")
                if (fabDay.visibility == View.GONE) {

                    fabDay.visibility = View.VISIBLE
                    frameMin.visibility = View.VISIBLE
                    frameHour.visibility = View.VISIBLE
                    frameDay.visibility = View.VISIBLE
                    TxAutoUpdate.visibility = View.VISIBLE
                    // Add animation here to expand the menu

                    if (newsList.size == 0) {
                        pDNews = ProgressDialog(this@MainActivity)
                        pDNews.setCancelable(false)
                        //    pDNews.setTitle("fetching News...")
                        //    pDNews.show()
                        val jobScheduler =
                            appContx.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
                        val serviceComponent = ComponentName(appContx, DailyJobService::class.java)
                        // Optional: persist across reboots

                        GlobalScope.launch(Dispatchers.IO) {
                            val builder = JobInfo.Builder(1, serviceComponent)
                                .setPeriodic(AlarmManager.INTERVAL_DAY) // Schedule to run daily
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) // Optional: require network
                                .setPersisted(true)
                            jobScheduler.schedule(builder.build())
                        }
                    }
                } else {
                    fabDay.visibility = View.GONE
                    frameMin.visibility = View.GONE
                    frameHour.visibility = View.GONE
                    frameDay.visibility = View.GONE
                    TxAutoUpdate.visibility = View.GONE
                    // Add animation here to collapse the menu
                }
            else {
                val builder = AlertDialog.Builder(this)

                // Set the dialog's title and message
                builder.setTitle("How to Add nHome Widget to HomeScreen")
                builder.setMessage(
                    "1. Goto Device Home Screen.\n" +
                            "2. Long press on empty region.\n" +
                            "3. Scroll down till you see nHome widget and long press the widget to HomeScreen"
                )


                // Set a positive button and its click listener
                builder.setPositiveButton("OK") { dialog, id ->
                    // User clicked OK button
                    dialog.dismiss() // Dismiss the dialog
                }


                // Create the AlertDialog object and show it
                val dialog = builder.create()
                dialog.show()
            }

        }

        BluetoothState()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        appContx.registerReceiver(mBluetoothReceiver, filter)


    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addPermissionCards() {
        addPermissionCard(
            " <b><u>Permissions needed...</u><b> <br><b> Device location </b>- to display \"① Place Info\" in the Widget",
            "Permit ACCESS_FINE_LOCATION permission",
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        addPermissionCard(
            "<b> Physical Activity </b>- to recognise walking state and display \"② Steps Count\" in the Widget",
            "Permit ACTIVITY_RECOGNITION permission",
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        addPermissionCard(
            "<b> Contacts </b>- to show your \"③ Favorite Contacts\" in the Widget, to dial easily",
            "Permit CONTACTS permission",
            Manifest.permission.READ_CONTACTS
        )


        addPermissionCard(
            "<b> Nearby Devices </b>- for indicating Bluetooth connection status in the Widget",
            "Permit BLUETOOTH_CONNECT permission",
            Manifest.permission.BLUETOOTH_CONNECT
        )
        addPermissionCard(
            "<b> Notifications </b>- to notify of set Reminders",
            "Permit POST_NOTIFICATIONS Access",
            Manifest.permission.POST_NOTIFICATIONS
        )
        addPermissionCard(
            "<b> Make Phone calls </b>- to quickly dial your \"Favorite Contacts\"",
            "Permit CALL_PHONE Access",
            Manifest.permission.CALL_PHONE
        )

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.CALL_PHONE
        )
        var buttonAll = Button(applicationContext)
        buttonAll.text = "Allow ALL"
        buttonAll.setOnClickListener {
            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS_REQUEST_CODE);
            } else {
                // Permissions already granted, proceed with functionality
            }
        }
        llInstructions.addView(buttonAll)
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun nPermissions(): Boolean {

        return (!(sharedPreferences.getBoolean("LP", false) && sharedPreferences.getBoolean(
            "ARP",
            false
        ) && sharedPreferences.getBoolean("RCP", false) && sharedPreferences.getBoolean(
            "BP",
            false
        ) && sharedPreferences.getBoolean("PNP", false) && sharedPreferences.getBoolean(
            "CPP",
            false
        ) && sharedPreferences.getBoolean("AUS", false) && sharedPreferences.getBoolean("AS", false)
                ))
    }

    private fun addPermissionCard(tx: String, bTx: String, rPermission: String) {

            val cardP = CardView(applicationContext)

        val layoutParamsCard = LayoutParams(
            LayoutParams.MATCH_PARENT,  // or WRAP_CONTENT, or specific dimension
            LayoutParams.WRAP_CONTENT // or specific dimension
        )

        val radiusInPixels = resources.displayMetrics.density * 8 // Convert 8dp to pixels
        cardP.radius = radiusInPixels
        val marginInPxX: Int = dpToPx(8, appContx) // Example: 16dp margin
        val marginInPxY: Int = dpToPx(16, appContx)
        layoutParamsCard.setMargins(marginInPxX, marginInPxY, marginInPxX, marginInPxY)
        cardP.layoutParams = layoutParamsCard


        val llP = LinearLayout(applicationContext)
        llP.orientation = LinearLayout.VERTICAL

        val txP = TextView(applicationContext)
        txP.text = Html.fromHtml(tx)
        llP.addView(txP)


        var requestCode: Int = 25
        if (rPermission == Manifest.permission.ACCESS_FINE_LOCATION) {
            requestCode = LOC_P
            btnL = Button(applicationContext)
            btnL.text = bTx
            btnL.setOnClickListener {
                ActivityCompat.requestPermissions(
                    mAct,
                    arrayOf(rPermission),
                    requestCode
                )
            }
            llP.addView(btnL)
        } else if (rPermission == Manifest.permission.ACTIVITY_RECOGNITION) {
            requestCode = ACTIVITY_RECOGNITION_P
            btnAR = Button(applicationContext)
            btnAR.text = bTx
            btnAR.setOnClickListener {
                ActivityCompat.requestPermissions(
                    mAct,
                    arrayOf(rPermission),
                    requestCode
                )
            }
            llP.addView(btnAR)
        } else if (rPermission == Manifest.permission.READ_CONTACTS) {
            var rPs = arrayOf(rPermission, Manifest.permission.WRITE_CONTACTS)
            requestCode = READ_CONTACTS_P
            btnRC = Button(applicationContext)
            btnRC.text = bTx
            btnRC.setOnClickListener {
                ActivityCompat.requestPermissions(
                    mAct,
                    rPs,
                    requestCode
                )
            }
            llP.addView(btnRC)
        } else if (rPermission == Manifest.permission.BLUETOOTH_CONNECT) {
            requestCode = BLUETOOTH_P
            btnBT = Button(applicationContext)
            btnBT.text = bTx
            btnBT.setOnClickListener {
                ActivityCompat.requestPermissions(
                    mAct,
                    arrayOf(rPermission),
                    requestCode
                )
            }
            llP.addView(btnBT)
        } else if (rPermission == Manifest.permission.POST_NOTIFICATIONS) {
            requestCode = NOTIfications_P
            btnPN = Button(applicationContext)
            btnPN.text = bTx
            btnPN.setOnClickListener {
                ActivityCompat.requestPermissions(
                    mAct,
                    arrayOf(rPermission),
                    requestCode
                )
            }
            llP.addView(btnPN)
        } else if (rPermission == Manifest.permission.CALL_PHONE) {
            requestCode = CALLPHONE_P
            btnCP = Button(applicationContext)
            btnCP.text = bTx
            btnCP.setOnClickListener {
                ActivityCompat.requestPermissions(
                    mAct,
                    arrayOf(rPermission),
                    requestCode
                )
            }
            llP.addView(btnCP)
        } else if (rPermission == "AUS") {

            btnAUS = Button(applicationContext)
            btnAUS.text = bTx
            btnAUS.setOnClickListener {
                usageStatsPermissionDialog()
            }
            llP.addView(btnAUS)

        } else if (rPermission == "AS") {

            btnAS = Button(applicationContext)
            btnAS.text = bTx
            btnAS.setOnClickListener {
                AccessibilityServicePermissionDialog()

            }
            llP.addView(btnAS)

        }


        cardP.addView(llP)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                "$rPermission"
            ) != PERMISSION_GRANTED
        )
            llInstructions.addView(cardP)

    }

    fun dpToPx(dp: Int, context: Context): Int {
        val density = context.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    fun usageStatsPermissionDialog() {
        val alertDialog: AlertDialog = AlertDialog.Builder(mAct).create()
        alertDialog.setTitle("Permission Request for App Usage Stats")
        alertDialog.setMessage("App needs permission to get Usage stats to suggest apps to use, based on previously used App stats.. ")
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL, "OK"
        ) { dialog, which ->
            UsageStatsChecker().requestUsageStatsPermission(appContx)
            dialog.dismiss()
        }

        alertDialog.show()

    }

    private fun populateKeys() {

        sharedPreferences.getStringSet("wallKeys", null)?.let { arrayListKeys.addAll(it) }

        if (arrayListKeys.size == 0) {
            arrayListKeys.add("Nature")
            addWallKey("Nature", true)
            arrayListKeys.add("Material Design")
            addWallKey("Material Design", false)
            arrayListKeys.add("iPhone Wallpapers")
            addWallKey("iPhone Wallpapers", false)
            arrayListKeys.add("Monsoon Birds")
            addWallKey("Monsoon Birds", false)
            arrayListKeys.add("CountrySide")
            addWallKey("CountrySide", false)
            arrayListKeys.add("Low Angle Photography")
            addWallKey("Low Angle Photography", false)
            arrayListKeys.add("Cherry Blossoms")
            addWallKey("Cherry Blossoms", false)
            arrayListKeys.add("Colorful Bokeh Lights")
            addWallKey("Colorful Bokeh Lights", false)
        }


    }

    fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService?>
    ): Boolean {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (enabledService in enabledServices) {
            val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName == context.packageName && enabledServiceInfo.name == service.name) return true
        }

        return false
    }

    private fun addWallKey(s: String, boolSelection: Boolean) {
        val txKey = TextView(this)
        txKey.text = s
        txKey.setTypeface(null, Typeface.BOLD)
        if (boolSelection) {
            for (i in arrayListKeysTxViews)
                i.setBackgroundResource(R.drawable.circular_tx_border)
            txKey.setBackgroundResource(R.drawable.circular_tx_selected)
        } else txKey.setBackgroundResource(R.drawable.circular_tx_border)
        txKey.setOnClickListener {
            WallKeyClick(txKey)
        }

        arrayListKeysTxViews.add(txKey)
        llKeywords.addView(txKey)
    }

    private fun launchers() {

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

    }

    fun getContactInfo(contactUri: Uri) {
        val contentResolver = contentResolver
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                contactUri!!,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts._ID,  // Add other desired columns like HAS_PHONE_NUMBER, PHOTO_URI, etc.
                ),
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)

                if (displayNameIndex != -1) {
                    val displayName = cursor.getString(displayNameIndex)
                    val contactId = cursor.getLong(contactIdIndex)
                    getContactDetails(displayName, contactId)

                    // Now you have the display name and ID for the contact
                    // You can use the contactId to query for phone numbers, email addresses, etc.
                    // using ContactsContract.CommonDataKinds.Phone or ContactsContract.CommonDataKinds.Email
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
    updateWidget()
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


    fun getContactDetails(displayName: String, contactId: Long) {
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


    private fun showSTTDialog() {
        val customDialog = Dialog(this@MainActivity)
        customDialog.setContentView(R.layout.stt_dialog)

        val dialogTitle = customDialog.findViewById<TextView>(R.id.dialogTitle)
        dialogMessage = customDialog.findViewById<EditText>(R.id.dialogMessage)
        val dialogButton = customDialog.findViewById<Button>(R.id.dialogButton)
        val imgBtnShare = customDialog.findViewById<ImageButton>(R.id.imgbtn_stt_share)

        dialogTitle.text = "Speech To Text"
        dialogMessage.setText("...")

        dialogButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak now..."
            ) // Optional: prompt for the user
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        }

        imgBtnShare.setOnClickListener {
            var textToShare = dialogMessage.text.toString()
            if (textToShare.length > 5) {
                val shareIntent = Intent()
                shareIntent.setAction(Intent.ACTION_SEND)
                shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare)
                shareIntent.setType("text/plain")
                startActivity(Intent.createChooser(shareIntent, "Share text using:"))
            }
        }

        customDialog.show()
    }

    private fun showTweetDialog(twInD: String) {
        val customDialog = Dialog(this)
        customDialog.setContentView(R.layout.tweet_dialog)

        val dialogTitle = customDialog.findViewById<TextView>(R.id.dialogTitle)
        dialogMessage = customDialog.findViewById<EditText>(R.id.dialogMessage)
        dialogMessage.visibility = View.INVISIBLE
        val dialogButton = customDialog.findViewById<Button>(R.id.dialogButton)
        dialogButton.visibility = View.INVISIBLE
        val imgBtnShare = customDialog.findViewById<ImageButton>(R.id.imgbtn_stt_share)

        dialogTitle.text = tW

        customDialog.show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (result != null && !result.isEmpty()) {
                    val recognizedText = result[0] // Get the most likely recognized phrase
                    dialogMessage.setText(recognizedText)
                }
            }
        }
    }

    private fun getApps() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)
        for (i in resolveInfoList) {
            if (i.activityInfo != null) {
                val appInfo = packageManager.getApplicationInfo(i.activityInfo.packageName, 0)
                apps.add(
                    InstalledApp(
                        i.activityInfo.loadLabel(packageManager).toString(),
                        i.activityInfo.packageName,
                        packageManager.getApplicationIcon(appInfo)
                    )
                )
            }
        }
        apps.sortWith { s1: InstalledApp, s2: InstalledApp ->
            s1.name.compareTo(s2.name, true)
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showTwitterHandleDialog() {
        val factory = LayoutInflater.from(this)
        twitterHandleDialog = factory.inflate(R.layout.twitter_handle_layout, null)
        val twitterDialog = AlertDialog.Builder(this).create()
        twitterDialog.setView(twitterHandleDialog)
        editTextTwitterHandle = twitterHandleDialog.findViewById<EditText>(R.id.edtx_th)
        editTextTwitterHandle.setText("")
        twitterHandleDialog.findViewById<View>(R.id.btn_ok)
            .setOnClickListener { //your business logic

                if (editTextTwitterHandle.text.toString().equals("Fact")) {
                    twitterProfileName = "Fact"
                    listTweets.clear()
                    rawTweets(true)
                } else {
                    getTweetID(editTextTwitterHandle.text.toString(), true)
                }
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
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com")
            .build()


        pD.setTitle("Twitter")
        pD.setMessage("fetching user ID...")
        if (showPD)
            pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
            responseTweetID = client.newCall(request).execute()

            withContext(Dispatchers.Main) {
                // Handle the result and hide the loading indicator
                if (showPD)
                    pD.dismiss()
                val responseBodyString = responseTweetID.peekBody(Long.MAX_VALUE).string()


                val jsonObject = JSONObject(responseBodyString)

                if (jsonObject.getJSONObject("result").getJSONObject("data").optString("user")
                        .isNotEmpty()
                )
                    if (jsonObject.getJSONObject("result").getJSONObject("data")
                            .getJSONObject("user").optString("result")
                            .isNotEmpty()
                    )
                        if (jsonObject.getJSONObject("result").getJSONObject("data")
                                .getJSONObject("user").getJSONObject("result").optString("rest_id")
                                .isNotEmpty()
                        ) {
                            twitterID = jsonObject.getJSONObject("result").getJSONObject("data")
                                .getJSONObject("user")
                                .getJSONObject("result").getString("rest_id")
                            twitterPicUrl = jsonObject.getJSONObject("result").getJSONObject("data")
                                .getJSONObject("user")
                                .getJSONObject("result").getJSONObject("avatar")
                                .getString("image_url")

                            twitterProfileName =
                                jsonObject.getJSONObject("result").getJSONObject("data")
                                    .getJSONObject("user")
                                    .getJSONObject("result").getJSONObject("core")
                                    .getString("screen_name")
                            Log.d("TwitterPicUrl - ", twitterPicUrl)

                            remoteViews =
                                RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                            newAppWidget =
                                ComponentName(applicationContext, NewAppWidget::class.java)
                            remoteViews?.setImageViewUri(R.id.twSettings, Uri.parse(twitterPicUrl))

                       updateWidget()

                            Log.d(TAG + "responseTweetID - ", responseBodyString)
                            Log.d(TAG + "Tw ID - ", twitterID + " - " + twitterProfileName)

                            if (showPD)
                                pD.dismiss()

                            getTweets(twitterID, showPD)
                        } else {
                            if (showPD)
                                pD.dismiss()
                            makeSnack("Twitter User doesn't Exist!")

                        }
                    else {
                        if (showPD)
                            pD.dismiss()
                        makeSnack("Twitter User doesn't Exist!")

                    }
                else {
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
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com")
            .build()

        pD.setTitle("Twitter")
        pD.setMessage("fetching Tweets...")
        if (showPD)
            pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
            responseTweets = client.newCall(request).execute()

            var js: JSONArray = (JSONObject(responseTweets.body?.string()).getJSONObject("result")
                .getJSONObject("timeline")
                .getJSONArray("instructions"))//[2] as JSONObject).getJSONArray("entries")

            for (i in 0 until js.length()) {
                if (js[i].toString().contains("entries"))
                    js = (js[i] as JSONObject).getJSONArray("entries")
            }

            withContext(Dispatchers.Main) {
                if (showPD)
                    pD.dismiss()
                if (js.length() > 0)
                    listTweets.clear()
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

                makeSnack("Tweets - ${listTweets.size}")

                remoteViews = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)
                remoteViews?.setTextViewText(
                    R.id.tx_tweets,
                    "@" + twitterProfileName + "\t ~ \t" + listTweets[1]
                )

           updateWidget()

            }
        }

        Log.d("result", "res - ${listTweets.size}")
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

    private fun rawTweets(boolpD: Boolean) {


        if (boolpD) {
            pD.setTitle("Twitter")
            pD.setMessage("fetching Tweets...")
            pD.show()
            Handler().postDelayed(Runnable {
                pD.dismiss()
            }, 1000)
        }

        val dataArray: JSONArray = TweetsJsonParser.parseJsonArrayFromRaw(this, R.raw.np_tweets)!!

        for (i in 0 until dataArray.length()) {
            try {
                val item = dataArray.getJSONObject(i)
                val tweet = item.getString("text")
                if (!containsLinkRegex(tweet))
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
                        R.id.tx_tweets,
                        "@" + twitterProfileName + "\t ~ \t" + listTweets[1]
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

    fun containsLinkRegex(text: String?): Boolean {
        // A common regex for URLs, including http, https, ftp, and file schemes.
        // This regex is a simplified example and might need adjustment for specific edge cases.
        val urlRegex =
            "\\b((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
        val pattern: Pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val matcher: Matcher = pattern.matcher(text)
        return matcher.find() // Returns true if a match is found
    }


    @SuppressLint("MissingPermission")
    private fun getCity() {

        if (!isLocationEnabled(applicationContext)) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            applicationContext.startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
        }

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
                makeToast("nothin")
                return true
            }
        }

        var fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun getAddress(lat: Double, lng: Double) {
        val gcd = Geocoder(applicationContext)
        Locale.getDefault()
        try {
            var cAddrs = gcd.getFromLocation(lat, lng, 1)!!
            //   makeToast(cAddrs?.get(0)!!.subLocality)

            cityname = cAddrs?.get(0)!!.subLocality
      //      if (cityname.length > 15)
        //        cityname = cityname.substring(0, 12) + "..,"
            //   makeToast("cityname - " + cityname)

        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            makeToast("GCD - IOException \n $e")
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

        //   choosenApps.clear()

        val currentHour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]


        NewAppWidget.timeOfDay = if (currentHour < 6) {
            "Night!"
        } else if (currentHour < 12) {
            "Morni!"
        } else if (currentHour < 17) {
            "Noon!"
        } else if (currentHour < 21) {
            "Eve!"
        } else {
            "Night!"
        }


        val usageStatsManager =
            applicationContext?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager // Context.USAGE_STATS_SERVICE);


        val beginCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()

        beginCal.set(cYear, cMonth - 1, cDate, 0, 0)
        endCal.set(cYear, cMonth, cDate, 0, 0)

        val queryUsageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            beginCal.timeInMillis,
            endCal.timeInMillis
        )
        println("results for " + beginCal.time + " - " + endCal.time)
        println("QUS - MA" + queryUsageStats.size)
        sortApps(queryUsageStats)


        var appNames = HashSet<String>()
        for (i in 0 until queryUsageStats.size) {

            var appName = getAppNameFromPkg(applicationContext, queryUsageStats.get(i).packageName)
            var appPname = queryUsageStats.get(i).packageName
            var appUsage = formatMilliseconds(queryUsageStats[i].totalTimeInForeground).substring(0, 2)

            Log.d(
                "queryUsageStats",
                "$appName ... - $i : " + queryUsageStats[i].totalTimeInForeground
            )

            if (queryUsageStats[i].totalTimeInForeground > 0)
                if (!appName.contains("Launcher") || !appName.equals("Home"))
                    if (applicationContext.packageManager.getLaunchIntentForPackage(queryUsageStats[i].packageName) != null)
                        if (appNames.add(appName)) {
                            arrayListUsageStats.add(
                                AppUsage(
                                    queryUsageStats[i].packageName,
                                    formatMilliseconds(queryUsageStats[i].totalTimeInForeground)
                                )
                            )
                         //   if (choosenApps.size < 10) {
                                choosenApps.add(
                                    App(
                                        appName, appPname, appUsage
                                    )
                                )
                         //   }
                        }
        }

        saveApps(choosenApps)

    }

    fun formatMilliseconds(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }


    @SuppressLint("Range", "UseCompatLoadingForDrawables", "Recycle")
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

                val phones: Cursor? = context.contentResolver.query(
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

            val cNme = cursor.getString(
                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            )

            val color = Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            var contactBitmap: Bitmap?

            contactBitmap = ContactPhotoHelper.retrieveContactPhoto(appContx, contactID.toLong())

            if (contactBitmap == null)
                contactBitmap = CharacterToBitmapConverter.getBitmapFromCharacter(
                    cNme[0], 100, 100, 70, color)

            var c = Contact(contactID, cNme, phoneNumber, contactBitmap)

            if (c.number.length > 7)
                favContacts.add(c)

        }



        if (favContacts.size > 0)
            saveContacts()
        else {
            makeToast("You've got no Contacts marked as Favorute!.. Go ahead add some to dial from the widget directly.")
            val builder = AlertDialog.Builder(this)

            builder.setTitle("Favorites") // Set the title of the dialog
            builder.setMessage("You've got no Contacts marked as Favorite!.. You can add some to dial from the widget directly, by clicking on + contact button in the Widget.") // Set the message of the dialog

            // Set the Positive Button and its action
            builder.setPositiveButton("Ok") { dialog: DialogInterface, which: Int ->
                dialog.dismiss() // Dismiss the dialog
            }

            // Create and show the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }

        cursor.close()
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setWalls(delay: Long) {

        appUsageStats(applicationContext)
        delayUnit = delay.toString()
        sharedPreferencesEditor.putString("dU", delayUnit).apply()
        sharedPreferencesEditor.putString("walltype", queryType).apply()

        pD.setTitle("Wallpaper...")
        pD.setMessage("Setting \"${queryType}\" wallpaper...")
        pD.show()

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
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )

    }


    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {

        editTextPrompt.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                //do what you want on the press of 'done'


                queryType = editTextPrompt.text.toString()
                sharedPreferencesEditor.putString("qT", queryType).apply()
                pD.setTitle("Wallpaper")
                pD.setMessage("fetching wallpapers,please wait...")
                pD.show()

                fetchWallpaper(applicationContext)
                editTextPrompt.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    android.R.drawable.ic_input_add,
                    0
                )
            }
            false
        })

        editTextPrompt.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if the touch event is within the bounds of the drawableRight

                if (editTextPrompt.compoundDrawables.get(2) != null)
                    if (event.x > (editTextPrompt.width - editTextPrompt.paddingRight - editTextPrompt.compoundDrawables
                            .get(2).bounds.width())
                    ) {
                        // Change the drawable
                        var newKey = editTextPrompt.text.toString()
                        if (!arrayListKeys.contains(newKey)) {
                            arrayListKeys.add(newKey)
                            addWallKey(newKey, true)
                            populateSelection(newKey)
                        }
                        return@OnTouchListener true // Consume the event
                    }
            }
            false // Let the event propagate if not a drawable click
        })

        fabMin.setOnClickListener {
            updateInterval = "min"
            //       makeToast("Wallpaper updates every 15 Mins!")
            wallDelay = 15
            setWalls(15)
            sharedPreferencesEditor.putStringSet("walls", HashSet(imgUrls)).apply()
            sharedPreferencesEditor.putStringSet("wallDescs", HashSet(imgDescs)).apply()

            //    throw RuntimeException("TestCraash")
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

        scrollKeys = findViewById(R.id.scroll_keys)
        llKeywords = findViewById(R.id.ll_keys)
        rlStatus = findViewById(R.id.rl_status)
        txStatus = findViewById(R.id.tx_status)
        editTextPrompt = findViewById(R.id.edtx_prompt)
        editTextPrompt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        fabMain = findViewById(R.id.fab_main)
        TxAutoUpdate = findViewById(R.id.tx_autoupdate)
        frameMin = findViewById(R.id.frame_fab1)
        frameHour = findViewById(R.id.frame_fab2)
        frameDay = findViewById(R.id.frame_fab3)
        fabMin = findViewById(R.id.fab_option_1)
        fabHour = findViewById(R.id.fab_option_2)
        fabDay = findViewById(R.id.fab_option_3)
        rvImages = findViewById(R.id.rv_images)

    }

    private fun GetDisplayDimens() {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        sharedPreferencesEditor.putInt("sWidth", displayMetrics.widthPixels).apply()
        sharedPreferencesEditor.putInt("sHeight", displayMetrics.heightPixels).apply()

    }


    override fun onDestroy() {
        super.onDestroy()

        if (pD.isShowing()) {
            pD.dismiss()
        }

        sharedPreferencesEditor.putStringSet("wallKeys", HashSet(arrayListKeys)).apply()
        sharedPreferencesEditor.putStringSet("walls", HashSet(imgUrls)).apply()
        sharedPreferencesEditor.putStringSet("wallDescs", HashSet(imgDescs)).apply()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if (requestCode == ALL_PERMISSIONS_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                makeToast("All permissions granted")

                    sharedPreferencesEditor.putBoolean("LP", true).apply()
                    getCity()
                    btnL.text = "Granted"


                    sharedPreferencesEditor.putBoolean("ARP", true).apply()
                    startStepsService()
                    btnAR.text = "Granted"


                    sharedPreferencesEditor.putBoolean("RCP", true).apply()
                    getFavoriteContacts(applicationContext)
                    btnRC.text = "Granted"


                    sharedPreferencesEditor.putBoolean("BP", true).apply()
                    btnBT.text = "Granted"


                    sharedPreferencesEditor.putBoolean("PNP", true).apply()
                    btnPN.text = "Granted"


                    sharedPreferencesEditor.putBoolean("CPP", true).apply()
                    btnCP.text = "Granted"



            } else {
                makeToast("Some permissions denied")
            }
        } else if (requestCode == LOC_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    sharedPreferencesEditor.putBoolean("LP", true).apply()
                    getCity()
                    btnL.text = "Granted"
                }

        } else if (requestCode == ACTIVITY_RECOGNITION_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    sharedPreferencesEditor.putBoolean("ARP", true).apply()
                    startStepsService()
                    btnAR.text = "Granted"
                }
        } else if (requestCode == READ_CONTACTS_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    sharedPreferencesEditor.putBoolean("RCP", true).apply()
                    getFavoriteContacts(applicationContext)
                    btnRC.text = "Granted"
                }
        } else if (requestCode == BLUETOOTH_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    sharedPreferencesEditor.putBoolean("BP", true).apply()
                    btnBT.text = "Granted"
                }
        } else if (requestCode == NOTIfications_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    sharedPreferencesEditor.putBoolean("PNP", true).apply()
                    btnPN.text = "Granted"
                }
        } else if (requestCode == CALLPHONE_P) {
            if (grantResults.isNotEmpty())
                if (grantResults[0].equals(PERMISSION_GRANTED)) {
                    sharedPreferencesEditor.putBoolean("CPP", true).apply()
                    btnCP.text = "Granted"
                }
        }

        if (nPermissions())
            instructionsDialogBuilder.create().dismiss()
    }

    private fun AccessibilityServicePermissionDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Requisition for Accessibility Service permission")
        builder.setMessage(
            "Please Enable Accessibility Service to smoothly lock Phone screen from Widget shortcut."
        )

        builder.setPositiveButton("OK") { dialog, id ->
            // User clicked OK button
            dialog.dismiss() // Dismiss the dialog
            val openSettings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            openSettings.addFlags(FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(openSettings)
        }


        // Create the AlertDialog object and show it
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }


    private fun BluetoothState() {
        var wTAG = "BluetoothState ~ "


        mBluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {

                val action = intent.action
                makeSnack("onReceive BLT - " + action)
                appWidM = AppWidgetManager.getInstance(appContx)


                if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_off)
                  updateWidget()
                        }

                        BluetoothAdapter.STATE_TURNING_OFF -> {}
                        BluetoothAdapter.STATE_ON -> {
                            remoteViews?.setImageViewResource(R.id.fab_blue, R.drawable.blue_on)
                   updateWidget()
                        }

                        BluetoothAdapter.STATE_TURNING_ON -> {}
                    }
                }
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
        rvImages.recycledViewPool.clear()
        rvAdapter.notifyItemRangeChanged(0, imgUrls.size)


        if (imgUrls.size == 0) {

            if (queryType.isNotEmpty()) {

                pexelUrl = "https://api.pexels.com/v1/search?query=$queryType&per_page=35"
                val request: StringRequest = @SuppressLint("NotifyDataSetChanged")
                object : StringRequest(

                    com.android.volley.Request.Method.GET, pexelUrl,
                    Response.Listener<String?> { response ->
                        try {
                            val jsonObject = JSONObject(response)

                            val jsonArray = jsonObject.getJSONArray("photos")

                            val length = jsonArray.length()


                            if (length > 0) {
                                txStatus.text =
                                    "Showing $queryType wallpapers...\n1. Search using Top Bar. \n2.Select from Above options, \nif you seek something else.. Or S3t!"
                                selectedKey = queryType
                                unSelectKeys()
                                fabMain.setText("Set")
                                for (i in 0 until length) {
                                    val jsonObject = jsonArray.getJSONObject(i)
                                    val objectImages = jsonObject.getJSONObject("src")
                                    imgUrls.add("$i + ${objectImages.getString("original")}")
                                    imgDescs.add("$i + ${jsonObject.getString("alt")})")
                                }
                                rvAdapter.notifyItemRangeChanged(0, length)
                                imageSliderAdapter.notifyItemRangeChanged(0, length)
                            } else makeSnack("doesn't match any existing set")


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

        rvImages.layoutManager = StaggeredGridLayoutManager(2, 1)

        rvAdapter = RvAdapter(applicationContext, imgUrls, imgDescs)
        rvImages.adapter = rvAdapter
    }

    override fun onResume() {
        super.onResume()

        if (isAccessibilityServiceEnabled(applicationContext, LockAccessibilityService::class.java)) {
            sharedPreferencesEditor.putBoolean("AS", true).apply()
            btnAS.text = "Granted"
        }

        if (UsageStatsChecker().hasUsageStatsPermission(applicationContext)) {
            btnAUS.text = "Granted"
            sharedPreferencesEditor.putBoolean("AUS", true).apply()
        }
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
            R.id.action_settings ->
                {

                    iDV.show()
                    true
                }
            else -> super.onOptionsItemSelected(item)
        }
    }


    companion object {

        lateinit var mainWindow: Window
        lateinit var fabMain: ExtendedFloatingActionButton
        lateinit var TxAutoUpdate: TextView
        lateinit var txStatus: TextView
        lateinit var rlStatus: RelativeLayout
        lateinit var llKeywords: LinearLayout
        lateinit var scrollKeys: HorizontalScrollView
        lateinit var pickContactLauncher: ActivityResultLauncher<Intent>
        private val CPick: Int = 7
        private val REQUEST_CONTACT_PICKER: Int = 9
        lateinit var pDNews: ProgressDialog
        lateinit var pD: ProgressDialog
        private lateinit var newsimgLink: String
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var apps: ArrayList<InstalledApp> = ArrayList()
        var wallDelay: Int = 0
        var twitterProfileName: String = "Fact"
        var listTweets: ArrayList<String> = ArrayList()
        var cDate by Delegates.notNull<Int>()
        var cMonth by Delegates.notNull<Int>()
        var cYear by Delegates.notNull<Int>()
        private val newSAPIKEY: String = "3fa88b5851974caea39bcc59bd2e5746"
        var newsIndex: Int = 0
        private val TAG: String = "MainActTAG"
        lateinit var launcher: ActivityResultLauncher<Intent>
        var cityname: String = "cN"
        var cityLat: Double = 0.0
        var cityLng: Double = 0.0

        var weatherIconState: String = ""
        var weatherIconSubState: String = ""
        var tempC: String = ""
        var tempKind: String = ""
        var weatherIconID: String = ""
        var weatherIconUrl: String = ""
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

        var randomWallIndex: Int = 0
        val imgUrls: ArrayList<String> = ArrayList()
        var imgDescs: ArrayList<String> = ArrayList()

        fun isAccessibilityServiceEnabled(
            context: Context,
            service: Class<out AccessibilityService?>
        ): Boolean {
            val am: AccessibilityManager =
                context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices: List<AccessibilityServiceInfo> =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

            for (enabledService in enabledServices) {
                val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
                if (enabledServiceInfo.packageName.equals(context.packageName) && enabledServiceInfo.name.equals(
                        service.name
                    )
                )
                    return true
            }

            return false
        }




        fun makeToast(s: String) {
            Toast.makeText(appContx, s, Toast.LENGTH_SHORT).show()
            Log.d("makeToastinG", s)
        }

        fun makeSnack(s: String) {
            sN = Snackbar.make(parentLayout, s, Snackbar.LENGTH_LONG)
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
        fun getWeatherData(b: Boolean) {

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
                        weatherIconState = weatherData.weather.get(0).main
                        Log.d("weatherIconSubState",  weatherData.weather.toString())
                        tempKind = weatherData.weather.get(0).description
                        weatherIconID = weatherData.weather.get(0).id
                        weatherIconUrl =
                            "http://openweathermap.org/img/wn/" + weatherIconID + "@2x.png"


                        Log.d("weatherInfo", tempC + " - " + tempKind)
                        if (b)
                            makeToast(
                                "weatherInfo - " + tempC.substring(
                                    0,
                                    4
                                ) + "°C" + " - " + tempKind
                            )

                        remoteViews?.setTextViewText(
                            R.id.tx_weather_icon_temp, tempC.substring(
                                0,
                                2
                            ) + "°C"
                        )
                        remoteViews?.setTextViewText(
                            R.id.tx_weather_icon_state,
                            weatherIconState
                        )
                        if (weatherIconID.startsWith("5"))
                            remoteViews?.setImageViewResource(R.id.weather_icon, R.drawable.rain)
                        if (weatherIconID.equals("800"))
                            remoteViews?.setImageViewResource(
                                R.id.weather_icon,
                                R.drawable.clear_sky
                            )
                        if (weatherIconID.equals("801") || weatherIconID.equals("802") || weatherIconID.equals(
                                "803"
                            ) || weatherIconID.equals("804")
                        )
                            remoteViews?.setImageViewResource(R.id.weather_icon, R.drawable.clouds)

                   updateWidget()

                        sharedPreferencesEditor.putString(
                            "weatherTemp",
                            tempC
                        ).apply()
                    }
                }
            } catch (ex: Exception) {
                Log.d("WD Excep7 - ", ex.toString())
                makeToast("Weather EXP - ${ex.message}")
            }

            //   makeToast(tempC)

        }


        fun updateWidget() {
            val intent = Intent(
                appContx,
                NewAppWidget::class.java
            )
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids: IntArray = AppWidgetManager.getInstance(appContx)
                .getAppWidgetIds(ComponentName(Companion.appContx, NewAppWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            appContx.sendBroadcast(intent)
        }


        fun getNews(tDate: Int) {

            //    makeToast("getNews - $cYear-$cMonth-$tDate")
            newsList.toMutableList().clear()

            ApiUtilities.getApiInterface()
                ?.getNews("bangalore", "$cYear-$cMonth-$tDate", "publishedAt", "en", newSAPIKEY)
                ?.enqueue(object : Callback<MainNews> {

                    override fun onFailure(call: Call<MainNews>, t: Throwable) {
                        makeToast("onFailure - " + t.message)
                        pDNews.dismiss()
                    }

                    override fun onResponse(
                        call: Call<MainNews>,
                        response: retrofit2.Response<MainNews>
                    ) {
                        //  newsList.toMutableList().clear()
                        if (response.isSuccessful) {

                            var newsRespSize = response.body()?.articles!!.size
                            if (newsRespSize > 3) {
                                newsList.clear()
                                newsBitmaps.clear()
                                newsLinks.clear()
                            }

                            for (i in 1 until newsRespSize - 1) {
                                newsList.add(response.body()?.articles!!.get(i).title)
                                newsLinks.add(response.body()?.articles!!.get(i).url)
                                try {
                                    newsimgLink = response.body()?.articles!!.get(i).urlToImage
                                    newsBitmaps.add(
                                        BitmapFactory.decodeStream(
                                            NetworkUtility().getInputStreamFromUrl(
                                                newsimgLink
                                            )
                                        )
                                    )
                                } catch (ex: Exception) {
                                    newsBitmaps.add(
                                        BitmapFactory.decodeResource(
                                            appContx.getResources(),
                                            R.drawable.face_holder
                                        )
                                    )
                                }
                            }
                            makeToast("News Added - " + newsList.size)
                            pDNews.dismiss()
                            Snackbar.make(
                                parentLayout,
                                "Auto Update Wallpaper, every,",
                                Snackbar.LENGTH_SHORT
                            )
                                .setAction("Action", null)
                                .setAnchorView(R.id.fab_main).show()
                        }

                    }
                })


        }


        fun isLocationEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        fun pickContact() {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            try {
                pickContactLauncher.launch(intent)
            } catch (ex: Exception) {
                makeToast("Ex - ${ex.message}")
            }
        }

        fun AccessibilityServicePermissionDialog() {

            val builder = AlertDialog.Builder(appContx)
            builder.setTitle("Requisition for Accessibility Service permission")
            builder.setMessage(
                "Please Enable Accessibility Service to smoothly lock Phone screen from Widget shortcut."
            )

            builder.setPositiveButton("OK") { dialog, id ->
                // User clicked OK button
                dialog.dismiss() // Dismiss the dialog
                val openSettings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                openSettings.addFlags(FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
                appContx.startActivity(openSettings)
            }

            builder.setPositiveButton("OK") { dialog, id ->
                // User clicked OK button
                dialog.dismiss() // Dismiss the dialog
                makeToast("Lock Screen cannot work without access to Accessibility Service!")
            }


            // Create the AlertDialog object and show it
            val dialog = builder.create()
            dialog.show()
        }


    }

    fun WallKeyClick(view: View) {
        selectedKey = (view as TextView).text.toString()
        queryType = selectedKey
        sharedPreferencesEditor.putString("qT", queryType).apply()
        pD.setTitle("Wallpaper")
        pD.setMessage("fetching wallpapers,please wait...")
        pD.show()
        fetchWallpaper(applicationContext)
        unSelectKeys()
    }

    private fun unSelectKeys() {
        for (i in arrayListKeys) {
            if (i == selectedKey)
                populateSelection(i)
        }
    }

    private fun populateSelection(i: String) {

        llKeywords.removeAllViews()

        var swapIndex = arrayListKeys.indexOf(i)
        Collections.swap(arrayListKeys, 0, swapIndex)

        for (i in 0 until arrayListKeys.size)
            if (i == 0)
                addWallKey(arrayListKeys[i], true)
            else addWallKey(arrayListKeys[i], false)


        scrollKeys.scrollTo(0, 0)


    }
}
