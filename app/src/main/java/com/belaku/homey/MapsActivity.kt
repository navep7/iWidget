package com.belaku.homey

import android.R.attr.button
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.StepsService.Companion.mLocationResult
import com.belaku.homey.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import java.io.IOException
import java.util.Locale



class MapsActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback, OnMapReadyCallback, GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {

    private var boolStreetMarkerClicked: Boolean = false
    // Initialised eagerly: a lateinit read before the first successful geocode
    // would throw UninitializedPropertyAccessException.
    private var cAddrs: MutableList<Address> = mutableListOf()
    private var boolMapReady: Boolean = false

    private lateinit var mSupportMapFragment: SupportMapFragment

    private var boolstreetViewPanorama: Boolean = false
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mStreetViewPanoramaView: StreetViewPanoramaView

    companion object {
        lateinit var mGoogleMap: GoogleMap
        lateinit var mStreetViewPanorama: StreetViewPanorama

        fun ismGoogleMapInitialized(): Boolean {
            return ::mGoogleMap.isInitialized
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)


        MapsInitializer.initialize(
            applicationContext,
            MapsInitializer.Renderer.LATEST,
            OnMapsSdkInitializedCallback { renderer ->
                Log.d("MapsSDK", "Renderer initialized: ${renderer.name}")
            }
        )

        mStreetViewPanoramaView = findViewById(R.id.streetviewpanorama)
        mSupportMapFragment =
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mSupportMapFragment.getMapAsync(this@MapsActivity)


        mStreetViewPanoramaView.onCreate(savedInstanceState);
        mStreetViewPanoramaView.getStreetViewPanoramaAsync(this);

      //  locationUpdates()

        binding.fabMapOStreet.setOnClickListener { view ->

            if (mSupportMapFragment.isVisible) {
                mStreetViewPanoramaView.visibility = View.VISIBLE
                mSupportMapFragment.view?.visibility = View.INVISIBLE
            } else {
                boolStreetMarkerClicked = false
                mStreetViewPanoramaView.visibility = View.INVISIBLE
                mSupportMapFragment.view?.visibility = View.VISIBLE
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {

                val location = mLocationResult?.lastLocation
                if (location != null) {
                    getAddress(location.latitude, location.longitude)

                    if (boolMapReady) {

                        var addrs = ""
                        // cAddrs is empty whenever geocoding failed.
                        val first = cAddrs.firstOrNull()
                        if (first != null) {
                            if (first.maxAddressLineIndex > 0)
                                for (i in 0 until first.maxAddressLineIndex) {
                                    addrs += first.getAddressLine(i)
                                }
                            else addrs = first.subLocality ?: ""
                        }


                        if (addrs.isEmpty())
                            addrs = "unknown"

                        addPresentMarker(LatLng(location.latitude, location.longitude), addrs)

                        mGoogleMap.setOnMapClickListener(this@MapsActivity)
                        mGoogleMap.setOnMarkerClickListener(this)
                    }


        }


    }

    fun addPresentMarker(ltlng: LatLng, addrs: String) {
        var icon: BitmapDescriptor? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val icnGenerator: IconGenerator = IconGenerator(this)
            // Bitmap bmp = icnGenerator.makeIcon(Html.fromHtml("<b><font color=\"#000000\">" + mAddresses[0] + mAddresses[1] + mAddresses[2] + "\n" + mAddresses[3] + mAddresses[4] + "</font></b>"));
            val bmp: Bitmap = icnGenerator.makeIcon(
                addrs
            )
            icon = BitmapDescriptorFactory.fromBitmap(bmp)
        }
        var mLatLng = LatLng(ltlng.latitude, ltlng.longitude)
        var markerOptions = MarkerOptions().position(mLatLng).icon(icon).title(addrs)

        //    marker = googleMap.addMarker(markerOptions);
        mGoogleMap.addMarker(markerOptions)
        val cameraPosition =
            CameraPosition.Builder().target(mLatLng).tilt(55f).zoom(20f).bearing(0f)
                .build()

        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun getAddress(lat: Double, lng: Double) {
        val gcd = Geocoder(applicationContext)
        Locale.getDefault()
        try {
            // getFromLocation() may return null or an empty list, and the
            // individual address fields are often null (e.g. remote areas).
            val addresses = gcd.getFromLocation(lat, lng, 1)
            cAddrs = addresses?.toMutableList() ?: mutableListOf()

            val address = cAddrs.firstOrNull() ?: return
            val label = address.subLocality
                ?: address.locality
                ?: address.countryName
                ?: return

            Snackbar.make(
                window.decorView.rootView,
                label,
                Snackbar.LENGTH_INDEFINITE
            ).show()
        } catch (e: IOException) {
            // Backend service unreachable / no geocoder available.
            e.printStackTrace()
            // makeToast("GCD - IOException \n $e")
        } catch (e: Exception) {
            // Geocoder throws IllegalArgumentException for invalid coordinates.
            e.printStackTrace()
        }

    }

    override fun onStreetViewPanoramaReady(streetViewPanorama: StreetViewPanorama) {
        mStreetViewPanorama = streetViewPanorama
        boolstreetViewPanorama = true
        streetUpdates()
    }

    private fun streetUpdates() {
        if (boolstreetViewPanorama) {

            val location = mLocationResult?.lastLocation

            if (location != null) {
            if (!boolStreetMarkerClicked)
                mStreetViewPanorama.setPosition(
                    LatLng(
                        location.latitude,
                        location.longitude
                    )
                )


            //do something
            val handler = Handler(Looper.getMainLooper()) // For UI updates
            val runnable: Runnable = object : Runnable {
                override fun run() {
                    var mStreetViewPanoramaCamera = StreetViewPanoramaCamera.Builder()
                        .zoom(mStreetViewPanorama.panoramaCamera.zoom)
                        .tilt(mStreetViewPanorama.panoramaCamera.tilt)
                        .bearing(mStreetViewPanorama.panoramaCamera.bearing - 60)
                        .build()
                    mStreetViewPanorama.animateTo(mStreetViewPanoramaCamera, 1000)

                    handler.postDelayed(this, 3000) // 1000 milliseconds = 1 second
                }
            }
            handler.post(runnable);

            if (mStreetViewPanorama.location != null)

                getAddress(
                    mStreetViewPanorama.location.position.latitude,
                    mStreetViewPanorama.location.position.longitude
                )

        }
            }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
        googleMap.isBuildingsEnabled = true
        mGoogleMap = googleMap
        boolMapReady = true
        locationUpdates()
    }

    override fun onMapClick(p0: LatLng) {
        addPresentMarker(p0, "Street")
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        // makeToast(p0.title.toString())
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 50 //You can manage the blinking time with this parameter
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = 1
        binding.fabMapOStreet.startAnimation(anim)
        boolStreetMarkerClicked = true
        mStreetViewPanorama.setPosition(p0.position)
        return true
    }

}