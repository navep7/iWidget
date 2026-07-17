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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // Set immediate background to avoid grey screen
        if (SetWallWorker.isWallBitmapInitialized()) {
            binding.mySpaceLayout.background = BitmapDrawable(resources, SetWallWorker.wallBitmap)
        }

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
            // Task 1: Background Blur (Parallel)
            launch(Dispatchers.IO) {
                if (SetWallWorker.isWallBitmapInitialized()) {
                    try {
                        val blurredBitmap = blur(applicationContext, SetWallWorker.wallBitmap)
                        withContext(Dispatchers.Main) {
                            binding.mySpaceLayout.background = BitmapDrawable(resources, blurredBitmap)
                        }
                    } catch (e: Exception) {}
                }
            }

            // Task 2: Incremental Apps Loading
            launch(Dispatchers.IO) {
                val launchableApps = getLaunchableApps()
                val loadedMySpaceApps = ArrayList<InstalledApp>()
                
                // Priority pass: Only load icons for choice MySpace apps for instant display
                if (savedAppNames.isNotEmpty()) {
                    for (info in launchableApps) {
                        val label = info.loadLabel(packageManager).toString()
                        if (savedAppNames.contains(label)) {
                            try {
                                val appInfo = packageManager.getApplicationInfo(info.activityInfo.packageName, 0)
                                loadedMySpaceApps.add(
                                    InstalledApp(label, info.activityInfo.packageName, packageManager.getApplicationIcon(appInfo))
                                )
                            } catch (e: Exception) {}
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    mySpaceApps.clear()
                    mySpaceApps.addAll(loadedMySpaceApps)
                    appsShown.clear()
                    appsShown.addAll(mySpaceApps)
                    boolSelectOrLaunch = false
                    rvAdapter.notifyDataSetChanged()
                }

                // Full background pass: Load all other apps (needed only for selection dialog)
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
                allLoadedApps.sortWith { s1: InstalledApp, s2: InstalledApp ->
                    s1.name.compareTo(s2.name, true)
                }

                withContext(Dispatchers.Main) {
                    allApps.clear()
                    allApps.addAll(allLoadedApps)
                    // If no chosen apps, notify loading finished
                    if (savedAppNames.isEmpty()) {
                        rvAdapter.notifyDataSetChanged()
                    }
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

    fun blur(context: Context?, image: Bitmap): Bitmap {

        val BITMAP_SCALE = 0.1f; 
        val BLUR_RADIUS = 25f;

        val width = Math.max(1, Math.round(image.width * BITMAP_SCALE).toInt())
        val height = Math.max(1, Math.round(image.height * BITMAP_SCALE).toInt())

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
        
        rs.destroy()

        return outputBitmap
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
