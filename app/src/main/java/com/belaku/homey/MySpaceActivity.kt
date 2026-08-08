package com.belaku.homey

import AppsAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.content.Context
import android.content.Intent
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
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.NewAppWidget.Companion.blurWallBitmap
import com.belaku.homey.NewAppWidget.Companion.primaryColor
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.databinding.ActivityMySpaceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MySpaceActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var mySpaceActivityContext: Context
    var addType = "App"
    private var appsShown: ArrayList<InstalledApp> = ArrayList()
    private var boolSelectOrLaunch: Boolean = false
    private lateinit var rvAdapter: AppsAdapter
    private lateinit var recyclerView: RecyclerView
    private var allApps: java.util.ArrayList<InstalledApp> = ArrayList()
    private var mySpaceApps: java.util.ArrayList<InstalledApp> = ArrayList()
    private var mySpaceAppsString: ArrayList<String> = ArrayList()
    private lateinit var binding: ActivityMySpaceBinding

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMySpaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mySpaceActivityContext = applicationContext


        if (ColorUtil().isColorDark(primaryColor)) {
            findViewById<TextView>(R.id.tx_t)
                .setTextColor(applicationContext.getColor(R.color.white))
        } else {
            findViewById<TextView>(R.id.tx_t)
                .setTextColor(applicationContext.getColor(R.color.black))
        }

        // Set immediate background to avoid grey screen while loading
        try {
            if (SetWallWorker.isWallBitmapInitialized(applicationContext)) {
                binding.mySpaceLayout.background = BitmapDrawable(resources, SetWallWorker.wallBitmap)
            }
        } catch (e: Exception) {}

        recyclerView = binding.rvMySpace
        rvAdapter = AppsAdapter(appsShown, this)
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = rvAdapter

        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        listeners()

        loadData()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun loadData() {
        val savedAppNames = sharedPreferences.getStringSet("mySpaceApps", null) ?: emptySet()
        mySpaceAppsString.clear()
        mySpaceAppsString.addAll(savedAppNames)

        lifecycleScope.launch {
            // Task 1: Background Blur (Parallel) - Updates background when ready
            launch(Dispatchers.IO) {
                try {
                    if (SetWallWorker.isWallBitmapInitialized(applicationContext)) {
                      //  val blurredBitmap = blur(applicationContext, SetWallWorker.wallBitmap)
                        withContext(Dispatchers.Main) {
                            binding.mySpaceLayout.background = BitmapDrawable(resources,
                                blurWallBitmap)
                        }
                    }
                } catch (e: Exception) {}
            }

            // Task 2: Incremental Apps Loading
            launch(Dispatchers.IO) {
                val launchableApps = getLaunchableApps()

                // First pass: Only load icons for choice MySpace apps for instant display
                val priorityApps = ArrayList<InstalledApp>()
                if (savedAppNames.isNotEmpty()) {
                    for (info in launchableApps) {
                        if (info.activityInfo == null) continue
                        val label = info.loadLabel(packageManager).toString()
                        if (savedAppNames.contains(label)) {
                            try {
                                val appInfo = packageManager.getApplicationInfo(info.activityInfo.packageName, 0)
                                priorityApps.add(
                                    InstalledApp(label, info.activityInfo.packageName, packageManager.getApplicationIcon(appInfo))
                                )
                            } catch (e: Exception) {}
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    mySpaceApps.clear()
                    mySpaceApps.addAll(priorityApps)
                    appsShown.clear()
                    appsShown.addAll(mySpaceApps)
                    boolSelectOrLaunch = false
                    rvAdapter.notifyDataSetChanged()
                }

                // Second pass: Load all other apps in background (needed only for selection spinner)
                val allLoadedApps = ArrayList<InstalledApp>()
                for (i in launchableApps) {
                    if (i.activityInfo != null) {
                        try {
                            val appInfo = packageManager.getApplicationInfo(i.activityInfo.packageName, 0)
                            allLoadedApps.add(
                                InstalledApp(
                                    i.activityInfo.loadLabel(packageManager).toString(),
                                    i.activityInfo.packageName,
                                    packageManager.getApplicationIcon(appInfo)
                                )
                            )
                        } catch (e: Exception) {}
                    }
                }
                allLoadedApps.sortWith { s1, s2 -> s1.name.compareTo(s2.name, true) }

                withContext(Dispatchers.Main) {
                    allApps.clear()
                    allApps.addAll(allLoadedApps)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listeners() {

        binding.fabMyspace.setOnClickListener {
            showSpinnerDialog()
        }

        binding.fabReset.setOnClickListener {
            mySpaceAppsString.clear()
            mySpaceApps.clear()
            appsShown.clear()
            sharedPreferencesEditor.putStringSet("mySpaceApps", HashSet(mySpaceAppsString)).commit()
            rvAdapter.notifyDataSetChanged()
        }


    }

    @SuppressLint("MissingInflatedId")
    private fun showSpinnerDialog() : String {
        val builder: AlertDialog.Builder = Builder(this@MySpaceActivity)
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_spinner_layout, null)
        builder.setView(dialogView)

        val spinner = dialogView.findViewById<Spinner>(R.id.dialogSpinner)
        val items: MutableList<String> = ArrayList()
        items.add("App")
        items.add("Contact")
        items.add("Web link")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        builder.setPositiveButton("OK") { dialog, _ ->
            addType = spinner.selectedItem as String

            if (addType == "App") {

                appsShown.clear()
                appsShown.addAll(allApps)
                boolSelectOrLaunch = true

                rvAdapter.notifyDataSetChanged()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()
        dialog.show()

        return addType
    }

    private fun getLaunchableApps(): List<ResolveInfo> {
        val packageManager: PackageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        // Query all activities that can be launched
        return packageManager.queryIntentActivities(mainIntent, 0)
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onItemClick(pos: Int) {

        mySpaceActivityContext = applicationContext


        if (boolSelectOrLaunch) {
            if (pos < allApps.size) {
                val n = allApps[pos].name
                val p = allApps[pos].pName
                val i = allApps[pos].icon
                mySpaceApps.add(InstalledApp(n, p, i))
                mySpaceAppsString.add(n)
                sharedPreferencesEditor.putStringSet("mySpaceApps", HashSet(mySpaceAppsString)).commit()

                appsShown.clear()
                appsShown.addAll(mySpaceApps)
                boolSelectOrLaunch = false

                rvAdapter.notifyDataSetChanged()
            }


        } else {

            if (appsShown.size > pos) {
                val launchIntent = packageManager.getLaunchIntentForPackage(appsShown[pos].pName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
                }
            }

        }


    }

}
