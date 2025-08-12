package com.belaku.homey

import AppsAdapter
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Fade
import android.view.Window
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.randomWallIndex
import com.belaku.homey.databinding.ActivityAppsBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition


class AppsActivity : AppCompatActivity() {

    var apps: ArrayList<InstalledApp> = ArrayList()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAppsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppsBinding.inflate(layoutInflater)

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        window.enterTransition = Fade()
        setContentView(binding.root)
        getApps()

        var url = SetWallWorker.urls[randomWallIndex].split(" ")[2]
        val rootLayout = findViewById<ConstraintLayout>(R.id.apps_layout)

        val recyclerView: RecyclerView = findViewById(R.id.rv_apps)
        // ... your list of DataItem objects ...
        val adapter = AppsAdapter(apps)
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

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

    private fun getApps() {

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)

        for (i in resolveInfoList) {
            if (i.activityInfo != null) {
                val appInfo = packageManager.getApplicationInfo(i.activityInfo.packageName, 0)
                apps.add(InstalledApp(i.activityInfo.loadLabel(packageManager).toString(), packageManager.getApplicationIcon(appInfo)))
            }
        }

        makeToast("Total Apps - ${apps.size}")

    }

}