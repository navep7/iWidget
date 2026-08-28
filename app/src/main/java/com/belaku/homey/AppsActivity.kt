package com.belaku.homey

import AppsAdapter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.NewAppWidget.Companion.blurWallBitmap
import com.belaku.homey.NewAppWidget.Companion.primaryColor
import com.belaku.homey.databinding.ActivityAppsBinding


class AppsActivity : AppCompatActivity(), AppsAdapter.RvEvent {

    private lateinit var binding: ActivityAppsBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView: RecyclerView = binding.rvApps
        val rootLayout: View = binding.appsLayout

        val titleTextView: TextView = binding.txT
        
        if (ColorUtil().isColorDark(primaryColor)) {
            titleTextView.setTextColor(applicationContext.getColor(R.color.white))
        } else {
            titleTextView.setTextColor(applicationContext.getColor(R.color.black))
        }

        val adapter = AppsAdapter(apps, this)
        val layoutManager = GridLayoutManager(this, 4) // Reduced span count for better Material look
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        try {
            rootLayout.background = BitmapDrawable(resources, blurWallBitmap)
        } catch (ex: Exception) {
            // Handle error or set default background
        }
    }

    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(apps[pos].pName)
        startActivity(launchIntent)
    }

}