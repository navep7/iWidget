package com.belaku.homey

import AppsAdapter
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Fade
import android.view.View
import android.view.Window
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.randomWallIndex
import com.belaku.homey.databinding.ActivityAppsBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import pl.droidsonroids.gif.GifImageView
import java.util.Collections


class AppsActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAppsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppsBinding.inflate(layoutInflater)

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        window.enterTransition = Fade()
        setContentView(binding.root)


        val recyclerView: RecyclerView = findViewById(R.id.rv_apps)
        val adapter = AppsAdapter(apps, this)
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter


        val rootLayout = findViewById<ConstraintLayout>(R.id.apps_layout)
        rootLayout.setBackgroundDrawable(BitmapDrawable(getResources(), SetWallWorker.wallBitmap))

    }

    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(apps[pos].pName)
        startActivity(launchIntent)
    }

}