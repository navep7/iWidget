package com.belaku.homey

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.belaku.homey.StepsService.Companion.mLocationResult
import com.belaku.homey.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {

    private var boolStreetMarkerClicked: Boolean = false
    private var cAddrs: MutableList<Address> = mutableListOf()
    private var boolMapReady: Boolean = false

    private lateinit var mSupportMapFragment: SupportMapFragment
    private lateinit var binding: ActivityMapsBinding
    private lateinit var webViewStreet: WebView

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            if (ismGoogleMapInitialized()) {
                @SuppressLint("MissingPermission")
                mGoogleMap.isMyLocationEnabled = true
            }
            locationUpdates()
        }
    }

    companion object {
        lateinit var mGoogleMap: GoogleMap
        lateinit var mStreetViewPanorama: com.google.android.gms.maps.StreetViewPanorama

        fun ismGoogleMapInitialized(): Boolean {
            return ::mGoogleMap.isInitialized
        }

        fun isStreetViewInitialized(): Boolean {
            return try {
                ::mStreetViewPanorama.isInitialized
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e("MapsSDK", "Failed to initialize maps renderer", e)
        }

        webViewStreet = findViewById(R.id.webview_streetview)
        webViewStreet.settings.javaScriptEnabled = true
        webViewStreet.settings.domStorageEnabled = true
        webViewStreet.webViewClient = WebViewClient()

        mSupportMapFragment =
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mSupportMapFragment.getMapAsync(this@MapsActivity)

        val mapView = findViewById<View>(R.id.map)
        val keyMap = intent.extras?.getString("keyMap")
        if (keyMap == "Street") {
            webViewStreet.visibility = View.VISIBLE
            mapView.visibility = View.GONE
        } else {
            webViewStreet.visibility = View.GONE
            mapView.visibility = View.VISIBLE
        }

        binding.fabMapOStreet.setOnClickListener {
            if (mapView.visibility == View.VISIBLE) {
                webViewStreet.visibility = View.VISIBLE
                mapView.visibility = View.GONE
            } else {
                boolStreetMarkerClicked = false
                webViewStreet.visibility = View.GONE
                mapView.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {
        val location = mLocationResult?.lastLocation
        if (location != null) {
            applyLocationToMap(location.latitude, location.longitude)
        } else {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                LocationServices.getFusedLocationProviderClient(this).lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            applyLocationToMap(loc.latitude, loc.longitude)
                        } else if (boolMapReady) {
                            applyLocationToMap(13.0382, 77.5126)
                        }
                    }
                    .addOnFailureListener {
                        if (boolMapReady) {
                            applyLocationToMap(13.0382, 77.5126)
                        }
                    }
            } else if (boolMapReady) {
                applyLocationToMap(13.0382, 77.5126)
            }
        }
    }

    private var currentMarker: Marker? = null

    private fun applyLocationToMap(lat: Double, lng: Double) {
        getAddress(lat, lng)
        applyLocationToStreet(lat, lng)

        if (boolMapReady) {
            mGoogleMap.setOnMapClickListener(this@MapsActivity)
            mGoogleMap.setOnMarkerClickListener(this)
        }
    }

    fun addPresentMarker(ltlng: LatLng, addrs: String) {
        var icon: BitmapDescriptor? = null
        try {
            val icnGenerator = IconGenerator(this)
            val bmp: Bitmap = icnGenerator.makeIcon(addrs)
            icon = BitmapDescriptorFactory.fromBitmap(bmp)
        } catch (e: Exception) {
            Log.e("MapsActivity", "Error generating icon marker", e)
        }
        val mLatLng = LatLng(ltlng.latitude, ltlng.longitude)
        val markerOptions = MarkerOptions().position(mLatLng).title(addrs)
        if (icon != null) {
            markerOptions.icon(icon)
        }

        currentMarker?.remove()
        if (boolMapReady && ismGoogleMapInitialized()) {
            currentMarker = mGoogleMap.addMarker(markerOptions)
        }
        val cameraPosition =
            CameraPosition.Builder()
                .target(mLatLng)
                .zoom(18f)
                .bearing(0f)
                .tilt(0f)
                .build()

        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun getAddress(lat: Double, lng: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val gcd = Geocoder(applicationContext, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    gcd.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cAddrs = addresses
                            val address = cAddrs.firstOrNull()
                            val markerLabel = address?.subLocality ?: address?.locality ?: address?.countryName ?: "Location"
                            val snackbarText = address?.locality ?: address?.subLocality ?: address?.countryName ?: "Location"

                            lifecycleScope.launch(Dispatchers.Main) {
                                if (boolMapReady && ismGoogleMapInitialized()) {
                                    addPresentMarker(LatLng(lat, lng), markerLabel)
                                }
                                showSnackbar(snackbarText)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.e("MapsActivity", "Geocode error: $errorMessage")
                            lifecycleScope.launch(Dispatchers.Main) {
                                if (boolMapReady && ismGoogleMapInitialized()) {
                                    addPresentMarker(LatLng(lat, lng), "Location")
                                }
                                showSnackbar("Location")
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("MapsActivity", "Geocode exception", e)
                }
            } else {
                var markerLabel = "Location"
                var snackbarText = "Location"
                try {
                    @Suppress("DEPRECATION")
                    val addresses = gcd.getFromLocation(lat, lng, 1)
                    cAddrs = addresses?.toMutableList() ?: mutableListOf()
                    val address = cAddrs.firstOrNull()
                    if (address != null) {
                        markerLabel = address.subLocality ?: address.locality ?: address.countryName ?: "Location"
                        snackbarText = address.locality ?: address.subLocality ?: address.countryName ?: "Location"
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    if (boolMapReady && ismGoogleMapInitialized()) {
                        addPresentMarker(LatLng(lat, lng), markerLabel)
                    }
                    showSnackbar(snackbarText)
                }
            }
        }
    }

    private fun showSnackbar(label: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Snackbar.make(
                window.decorView.rootView,
                label,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun applyLocationToStreet(lat: Double, lng: Double) {
        val embedHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    html, body, iframe { width: 100%; height: 100%; margin: 0; padding: 0; border: none; overflow: hidden; background: #000; }
                </style>
            </head>
            <body>
                <iframe src="https://maps.google.com/maps?q=&layer=c&cbll=$lat,$lng&cbp=12,0,0,0,0&output=svembed" allowfullscreen></iframe>
            </body>
            </html>
        """.trimIndent()
        webViewStreet.loadDataWithBaseURL("https://maps.google.com", embedHtml, "text/html", "UTF-8", null)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            googleMap.isMyLocationEnabled = true
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        googleMap.isBuildingsEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        mGoogleMap = googleMap
        boolMapReady = true
        locationUpdates()
    }

    override fun onMapClick(p0: LatLng) {
        getAddress(p0.latitude, p0.longitude)
        applyLocationToStreet(p0.latitude, p0.longitude)
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 50
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = 1
        binding.fabMapOStreet.startAnimation(anim)
        boolStreetMarkerClicked = true
        applyLocationToStreet(p0.position.latitude, p0.position.longitude)
        return true
    }
}
