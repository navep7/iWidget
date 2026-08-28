package com.belaku.homey

import AppsAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.NewAppWidget.Companion.blurWallBitmap
import com.belaku.homey.NewAppWidget.Companion.primaryColor
import com.belaku.homey.databinding.ActivityGapsBinding

class GapsActivity : AppCompatActivity(), AppsAdapter.RvEvent {

    private var gapps: ArrayList<InstalledApp> = ArrayList()
    private lateinit var binding: ActivityGapsBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val googleLaunchableAppsResolveInfo = getGoogleLaunchableApps()
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

        val recyclerView = binding.rvApps
        val adapter = AppsAdapter(gapps, this)
        val layoutManager = GridLayoutManager(this, 4)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val rootLayout = binding.gappsLayout
        val titleTextView = binding.txT

        try {
            rootLayout.background = BitmapDrawable(resources, blurWallBitmap)
            
            if (ColorUtil().isColorDark(primaryColor)) {
                titleTextView.setTextColor(getColor(R.color.white))
            } else {
                titleTextView.setTextColor(getColor(R.color.black))
            }
        } catch (ex: Exception) {
            // Fallback if blurWallBitmap is not available
        }
    }

    private fun getGoogleLaunchableApps(): List<ResolveInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val allLaunchableApps = packageManager.queryIntentActivities(mainIntent, 0)
        return allLaunchableApps.filter { it.activityInfo.packageName.startsWith("com.google.android") }
    }

    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(gapps[pos].pName)
        startActivity(launchIntent)
    }
}