package com.belaku.homey

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback {

    private lateinit var mStreetViewPanorama: StreetViewPanorama
    private var boolstreetViewPanorama: Boolean = false
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mStreetViewPanoramaView: StreetViewPanoramaView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        mStreetViewPanoramaView = findViewById(R.id.streetviewpanorama);
        mStreetViewPanoramaView.onCreate(savedInstanceState);
        mStreetViewPanoramaView.getStreetViewPanoramaAsync(this);

        locationUpdates()

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
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
        var locationCallback = object : LocationCallback() {
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
                            makeToast(getAddress(mStreetViewPanorama.location.position.latitude, mStreetViewPanorama.location.position.longitude).toString())


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
            val cAddrs = gcd.getFromLocation(lat, lng, 1)
         //   makeToast(cAddrs?.get(0)!!.subLocality)
            Snackbar.make(window.decorView.rootView, cAddrs?.get(0)!!.subLocality, Snackbar.LENGTH_INDEFINITE).show()
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


}