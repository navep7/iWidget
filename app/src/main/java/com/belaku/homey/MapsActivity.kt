package com.belaku.homey

import com.google.maps.android.ui.IconGenerator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
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
import java.io.IOException
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback, OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private lateinit var cAddrs: MutableList<Address>
    private var boolMapReady: Boolean = false
    private lateinit var mGoogleMap: GoogleMap

    private lateinit var mSupportMapFragment: SupportMapFragment
    private lateinit var mStreetViewPanorama: StreetViewPanorama
    private var boolstreetViewPanorama: Boolean = false
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mStreetViewPanoramaView: StreetViewPanoramaView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        mStreetViewPanoramaView = findViewById(R.id.streetviewpanorama)
        mSupportMapFragment =
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mSupportMapFragment.getMapAsync(this@MapsActivity)


        mStreetViewPanoramaView.onCreate(savedInstanceState);
        mStreetViewPanoramaView.getStreetViewPanoramaAsync(this);

        locationUpdates()

        binding.fabMapOStreet.setOnClickListener { view ->

            if (mSupportMapFragment.isVisible) {
                mStreetViewPanoramaView.visibility = View.VISIBLE
                mSupportMapFragment.view?.visibility = View.INVISIBLE
            } else {
                mStreetViewPanoramaView.visibility = View.INVISIBLE
                mSupportMapFragment.view?.visibility = View.VISIBLE
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun locationUpdates() {
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
                    if (boolstreetViewPanorama) {
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
                                    .zoom(mStreetViewPanorama.getPanoramaCamera().zoom)
                                    .tilt(mStreetViewPanorama.getPanoramaCamera().tilt)
                                    .bearing(mStreetViewPanorama.getPanoramaCamera().bearing - 60)
                                    .build()
                                mStreetViewPanorama.animateTo(mStreetViewPanoramaCamera, 1000)

                                handler.postDelayed(this, 3000) // 1000 milliseconds = 1 second
                            }
                        }
                        handler.post(runnable);

                        if (mStreetViewPanorama.location != null)
                            makeToast(
                                getAddress(
                                    mStreetViewPanorama.location.position.latitude,
                                    mStreetViewPanorama.location.position.longitude
                                ).toString()
                            )
                    }
                    if (boolMapReady) {

                        var addrs = ""
                        if (cAddrs[0].maxAddressLineIndex > 0)
                        for (i in 0 until cAddrs[0].maxAddressLineIndex) {
                            addrs = addrs + cAddrs[0].getAddressLine(i)
                        }
                        else addrs = cAddrs[0].subLocality
                        addPresentMarker(LatLng(location.latitude, location.longitude), addrs)

                        mGoogleMap.setOnMapClickListener(this@MapsActivity)
                        mGoogleMap.setOnMarkerClickListener(this)
                    }
                }
            }

            override fun onMarkerClick(p0: Marker): Boolean {
                makeToast(p0.title.toString())
                mStreetViewPanorama.setPosition(p0.position)
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

    private fun addPresentMarker(ltlng: LatLng, addrs: String) {
        var icon: BitmapDescriptor? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val icnGenerator: IconGenerator = IconGenerator(this)
            // Bitmap bmp = icnGenerator.makeIcon(Html.fromHtml("<b><font color=\"#000000\">" + mAddresses[0] + mAddresses[1] + mAddresses[2] + "\n" + mAddresses[3] + mAddresses[4] + "</font></b>"));
            val bmp: Bitmap = icnGenerator.makeIcon(
              addrs
            )
            icon = BitmapDescriptorFactory.fromBitmap(bmp)
        }
        var mLatLng: LatLng = LatLng(ltlng.latitude, ltlng.longitude)
        var markerOptions = MarkerOptions().position(mLatLng).icon(icon).title(addrs)

        //    marker = googleMap.addMarker(markerOptions);
        var markerAddress = mGoogleMap.addMarker(markerOptions)
        val cameraPosition =
            CameraPosition.Builder().target(mLatLng).tilt(55f).zoom(20f).bearing(0f)
                .build()

        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun getAddress(lat: Double, lng: Double) {
        val gcd = Geocoder(applicationContext)
        Locale.getDefault()
        try {
            cAddrs = gcd.getFromLocation(lat, lng, 1)!!
            //   makeToast(cAddrs?.get(0)!!.subLocality)
            Snackbar.make(
                window.decorView.rootView,
                cAddrs?.get(0)!!.subLocality,
                Snackbar.LENGTH_INDEFINITE
            ).show()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            makeToast("GCD - IOException \n $e")
        }

    }

    override fun onStreetViewPanoramaReady(streetViewPanorama: StreetViewPanorama) {
        mStreetViewPanorama = streetViewPanorama
        boolstreetViewPanorama = true
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.isMyLocationEnabled = true
        googleMap.isBuildingsEnabled = true
        mGoogleMap = googleMap
        boolMapReady = true
    }

    override fun onMapClick(p0: LatLng) {
        addPresentMarker(p0, "here")
    }

}