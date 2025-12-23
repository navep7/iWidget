package com.belaku.homey

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.cityname
import com.belaku.homey.MainActivity.Companion.fabMain
import com.belaku.homey.MainActivity.Companion.imgDescs
import com.belaku.homey.MainActivity.Companion.imgUrls
import com.belaku.homey.MainActivity.Companion.isLocationEnabled
import com.belaku.homey.MainActivity.Companion.mainWindow
import com.belaku.homey.MainActivity.Companion.makeSnack
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.queryType
import com.belaku.homey.MainActivity.Companion.txStatus
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.getScreenTime
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.todaysDate
import com.belaku.homey.SetWallWorker.Companion.isPinNoteInitialized
import com.belaku.homey.SetWallWorker.Companion.isWallBitmapInitialized
import com.belaku.homey.SetWallWorker.Companion.mAct
import com.belaku.homey.SetWallWorker.Companion.pinNote
import com.belaku.homey.SetWallWorker.Companion.scaledBitmap
import com.belaku.homey.SetWallWorker.Companion.screenHeight
import com.belaku.homey.SetWallWorker.Companion.screenWidth
import com.belaku.homey.SetWallWorker.Companion.setWall
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SetWallWorker.Companion.wallBitmap
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.random.Random

class LauncherActivity : AppCompatActivity() {
    private lateinit var pexelUrl: String
    private val ALL_PERMISSIONS_REQUEST_CODE: Int = 100
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CALL_PHONE
    )
    private lateinit var imgvBg: ImageView


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()

        if (UsageStatsChecker().hasUsageStatsPermission(applicationContext))
            if (isWallBitmapInitialized()) {
                scaledBitmap =
                    Bitmap.createScaledBitmap(wallBitmap, screenWidth, screenHeight, true)
                imgvBg.setImageBitmap(
                    applyThinFilmOverlay(
                        drawableToBitmap(
                            appContx, RoundedBitmapDrawableFactory.create(
                                appContx.resources, BitmapBlurHelper.blurBitmap(
                                    appContx,
                                    Bitmap.createBitmap(
                                        scaledBitmap,
                                        10,
                                        25,
                                        screenWidth - 20,
                                        screenHeight - 150
                                    )
                                )
                            )
                        ), Color.WHITE, 50
                    )
                )
            }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_launcher)

        appContx = applicationContext
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars())

        mAct = this@LauncherActivity
        mainWindow = this.window

        sharedPreferences = appContx.getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        val metrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(metrics)
        screenHeight = metrics.heightPixels
        screenWidth = metrics.widthPixels


        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS_REQUEST_CODE);
        } else {
            // Permissions already granted, proceed with functionality
            setUI()
        }




    }

    @RequiresApi(Build.VERSION_CODES.S)
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

                //   AccessibilityServicePermissionDialog()


                usageStatsPermissionDialog()

                sharedPreferencesEditor.putBoolean("LP", true).apply()
                getCity()





                sharedPreferencesEditor.putBoolean("ARP", true).apply()
                startStepsService()



                sharedPreferencesEditor.putBoolean("RCP", true).apply()
                getFavoriteContacts(applicationContext)


                sharedPreferencesEditor.putBoolean("BP", true).apply()
                sharedPreferencesEditor.putBoolean("PNP", true).apply()
                sharedPreferencesEditor.putBoolean("CPP", true).apply()

            } else {
                makeToast("Some permissions denied")
            }
        }


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

            val color =
                Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            var contactBitmap: Bitmap?

            contactBitmap = ContactPhotoHelper.retrieveContactPhoto(appContx, contactID.toLong())

            if (contactBitmap == null)
                contactBitmap = CharacterToBitmapConverter.getBitmapFromCharacter(
                    cNme[0], 100, 100, 70, color
                )

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


    private fun startStepsService() {
        if (!isMyServiceRunning(StepsService::class.java)) {
            val intentSteps = Intent(this, StepsService::class.java)
            startForegroundService(intentSteps)
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

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun fetchWallpaper(context: Context) {


        imgUrls.clear()
        imgDescs.clear()



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

                            makeToast("TotalWALLS - $length")

                            if (length > 0) {

                                for (i in 0 until length) {
                                    val jsonObject = jsonArray.getJSONObject(i)
                                    val objectImages = jsonObject.getJSONObject("src")
                                    imgUrls.add("$i + ${objectImages.getString("original")}")
                                    imgDescs.add("$i + ${jsonObject.getString("alt")})")
                                }

                                makeToast("adding IMGURLs")
                                sharedPreferencesEditor.putStringSet("walls", HashSet(imgUrls))
                                    .apply()
                                sharedPreferencesEditor.putStringSet("wallDescs", HashSet(imgDescs))
                                    .apply()

                            } else makeSnack("doesn't match any existing set")


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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setUI() {

        findViewByIds()

        fetchWallpaper(appContx)


        //    googleAccountInfo()
        if (isPinNoteInitialized())
            remoteViews?.setTextViewText(R.id.txrun, pinNote)
        liquidGlassEffects()
        /*   seekWifiState()
           seekBluetoothState()
           getScreenTime(appContx)
           todaysDate()
           locationTxUpdate(appContx)
           wallColors()
           setSomeTwAndWallDescUI()*/
    }

    private fun findViewByIds() {

        imgvBg = findViewById(R.id.imgv_widget_layout)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun liquidGlassEffects() {

        if (!isWallBitmapInitialized())
            Thread {
                setWall(true)
            }.start()

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            if (isWallBitmapInitialized()) {
                scaledBitmap =
                    Bitmap.createScaledBitmap(wallBitmap, screenWidth, screenHeight, true)
                imgvBg.setImageBitmap(
                    applyThinFilmOverlay(
                        drawableToBitmap(
                            appContx, RoundedBitmapDrawableFactory.create(
                                appContx.resources, BitmapBlurHelper.blurBitmap(
                                    appContx,
                                    Bitmap.createBitmap(
                                        scaledBitmap,
                                        10,
                                        25,
                                        screenWidth - 20,
                                        screenHeight - 150
                                    )
                                )
                            )
                        ), Color.WHITE, 50
                    )
                )
            }
        }, 3000)


    }

    private fun applyThinFilmOverlay(
        originalBitmap: Bitmap,
        filmColor: Int,
        filmAlpha: Int
    ): Bitmap {
        // Create a mutable bitmap for drawing
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)

        // Draw the original bitmap
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // Create a paint object for the "film" effect
        val paint = Paint()
        paint.color = filmColor
        // Set the transparency (0 = fully transparent, 255 = fully opaque)
        paint.alpha = filmAlpha

        // Draw the semi-transparent color over the entire canvas
        canvas.drawRect(
            0f,
            0f,
            originalBitmap.width.toFloat(),
            originalBitmap.height.toFloat(),
            paint
        )

        return resultBitmap
    }
}