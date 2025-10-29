package com.belaku.homey

import AppsAdapter
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
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
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.databinding.ActivityGapsBinding


class GapsActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private var gapps: ArrayList<InstalledApp> = ArrayList()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityGapsBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGapsBinding.inflate(layoutInflater)

        setContentView(binding.root)


        val googleLaunchableAppsResolveInfo = getGoogleLaunchableApps()

        makeToast("GoogleApps - ${googleLaunchableAppsResolveInfo.size}")

        gapps = ArrayList()

        for (i in googleLaunchableAppsResolveInfo) {
            if (i.activityInfo != null) {
                val appInfo = packageManager.getApplicationInfo(i.activityInfo.packageName, 0)
                gapps.add(
                    InstalledApp(
                        i.activityInfo.loadLabel(packageManager).toString(),
                        i.activityInfo.packageName,
                        packageManager.getApplicationIcon(appInfo)
                    )
                )
            }
        }
        gapps.sortWith { s1: InstalledApp, s2: InstalledApp ->
            s1.name.compareTo(s2.name, true)
        }

        val recyclerView: RecyclerView = findViewById(R.id.rv_apps)
        val adapter = AppsAdapter(gapps, this)
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter


        val rootLayout = findViewById<RelativeLayout>(R.id.gapps_layout)
        rootLayout.setBackgroundDrawable(BitmapDrawable(getResources(), blur(applicationContext, SetWallWorker.wallBitmap)))

        rootLayout.findViewById<TextView>(R.id.tx_t).setTextColor(NewAppWidget.tertianaryColor)

        /* val blurRadius = 20.0f
         val blurEffect = RenderEffect.createBlurEffect(
             blurRadius,
             blurRadius,
             Shader.TileMode.CLAMP
         )
         rootLayout.setRenderEffect(blurEffect);*/


    }

    private fun getGoogleLaunchableApps(): List<ResolveInfo> {
        val packageManager: PackageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        // Query all activities that can be launched
        val allLaunchableApps: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)

        // Filter the list to include only Google's apps
        return allLaunchableApps.filter { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            // Google's core apps use package names starting with "com.google.android"
            packageName.startsWith("com.google.android")
        }
    }

    fun blur(context: Context?, image: Bitmap): Bitmap {

        var BITMAP_SCALE = 0.4f; // Scale down bitmap for performance
        var BLUR_RADIUS = 25f; // Adjust blur intensity

        val width = Math.round(image.width * BITMAP_SCALE).toInt()
        val height = Math.round(image.height * BITMAP_SCALE).toInt()

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)

        return outputBitmap
    }

    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(gapps[pos].pName)
        startActivity(launchIntent)
    }

}