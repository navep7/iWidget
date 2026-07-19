package com.belaku.homey

import AppsAdapter
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.transition.Fade
import android.transition.Slide
import android.view.Window
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.NewAppWidget.Companion.blurWallBitmap
import com.belaku.homey.NewAppWidget.Companion.primaryColor
import com.belaku.homey.databinding.ActivityAppsBinding


class AppsActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAppsBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val recyclerView: RecyclerView = findViewById(R.id.rv_apps)
        val rootLayout = findViewById<RelativeLayout>(R.id.apps_layout)

        if (ColorUtil().isColorDark(primaryColor)) {
            rootLayout.findViewById<TextView>(R.id.tx_t)
                .setTextColor(applicationContext.getColor(R.color.white))
        } else {
            rootLayout.findViewById<TextView>(R.id.tx_t)
                .setTextColor(applicationContext.getColor(R.color.black))
        }

        val adapter = AppsAdapter(apps, this)
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter




        try {
            rootLayout.setBackgroundDrawable(
                BitmapDrawable(
                    getResources(),
                    blurWallBitmap
                )
            )
        } catch (ex: Exception) {

        }
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


    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(apps[pos].pName)
        startActivity(launchIntent)
    }

}