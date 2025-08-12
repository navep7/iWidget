package com.belaku.homey

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Explode
import android.transition.Fade
import android.transition.Slide
import android.view.Window
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.ui.AppBarConfiguration
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.randomWallIndex
import com.belaku.homey.databinding.ActivityAppsBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition


class AppsActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAppsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppsBinding.inflate(layoutInflater)

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        window.enterTransition = Fade()
        setContentView(binding.root)

        var url = SetWallWorker.urls[randomWallIndex].split(" ")[2]
        val rootLayout = findViewById<ConstraintLayout>(R.id.apps_layout)


        Glide.with(this)
            .load(url) // Replace with your image URL
            .into(object : CustomTarget<Drawable?>() {


                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    // Set the drawable as the background of your root layout
                    resource.alpha = 128
                    rootLayout.background = resource
                }

                override fun onLoadCleared(@Nullable placeholder: Drawable?) {
                    // Handle placeholder or cleared state if needed
                }
            })


    }

}