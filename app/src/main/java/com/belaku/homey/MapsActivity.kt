package com.belaku.homey

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback, OnMapReadyCallback {

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
        val locationCallback = object : LocationCallback() {
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
                        var hereAmi = LatLng(location.latitude, location.longitude)
                        var addrs: String = ""
                        if (cAddrs[0].maxAddressLineIndex > 0)
                        for (i in 0 until cAddrs[0].maxAddressLineIndex) {
                            addrs = addrs + cAddrs[0].getAddressLine(i)
                        }
                        else addrs = cAddrs[0].subLocality
                        mGoogleMap.addMarker(
                            MarkerOptions().position(hereAmi).title(addrs).icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation))
                        )?.showInfoWindow()
                        val zoomLevel = 17.0f // Desired zoom level
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hereAmi, zoomLevel))

                    }
                }
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

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        boolMapReady = true
    }

}