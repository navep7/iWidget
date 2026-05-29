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
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SetWallWorker.Companion.isSharedPreferencesInitialized
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.databinding.ActivityMySpaceBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.properties.Delegates


class MySpaceActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var mySpaceActivityContext: Context
    var addType = "App"
    private var appsShown: ArrayList<InstalledApp> = ArrayList()
    private var boolSelectOrLaunch by Delegates.notNull<Boolean>()
    private lateinit var rvAdapter: AppsAdapter
    private lateinit var recyclerView: RecyclerView
    private var allApps: java.util.ArrayList<InstalledApp> = ArrayList()
    private var mySpaceApps: java.util.ArrayList<InstalledApp> = ArrayList()
    private var mySpaceAppsString: ArrayList<String> = ArrayList()
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabReset: FloatingActionButton
    private lateinit var binding: ActivityMySpaceBinding

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMySpaceBinding.inflate(layoutInflater)

        setContentView(binding.root)

        mySpaceActivityContext = applicationContext
        // makeToast("MySpace - apps, contacts, web links, etc .., of my Own!")

        val rootLayout = findViewById<RelativeLayout>(R.id.my_space_layout)
        try {
            rootLayout.setBackgroundDrawable(
                BitmapDrawable(
                    getResources(),
                    blur(applicationContext, SetWallWorker.wallBitmap)
                )
            )
        } catch (exp: Exception) {
            // makeToast("exp - $exp")
        }

        fabAdd = findViewById<FloatingActionButton>(R.id.fab_myspace)
        fabReset = findViewById<FloatingActionButton>(R.id.fab_reset)

        listeners()


        val launchableAppsResolveInfo = getLaunchableApps()

        for (i in launchableAppsResolveInfo) {
            if (i.activityInfo != null) {
                val appInfo = packageManager.getApplicationInfo(i.activityInfo.packageName, 0)
                allApps.add(
                    InstalledApp(
                        i.activityInfo.loadLabel(packageManager).toString(),
                        i.activityInfo.packageName,
                        packageManager.getApplicationIcon(appInfo)
                    )
                )
            }
        }
        allApps.sortWith { s1: InstalledApp, s2: InstalledApp ->
            s1.name.compareTo(s2.name, true)
        }

        recyclerView = findViewById(R.id.rv_my_space)

        if (!isSharedPreferencesInitialized()) {
            sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
            sharedPreferencesEditor = sharedPreferences.edit()
        }
        sharedPreferences.getStringSet("mySpaceApps", null)?.let { mySpaceAppsString.addAll(it) }

        var SZ = mySpaceAppsString.size


        if (SZ > 0) {
            for (sAs in mySpaceAppsString) {
                for (i in allApps) {
                    if (i.name == sAs) {
                        mySpaceApps.add(i)
                    }
                }
            }

            appsShown.addAll(mySpaceApps)
            boolSelectOrLaunch = false
        }

        rvAdapter = AppsAdapter(appsShown, this)
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = rvAdapter



    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listeners() {

        fabAdd.setOnClickListener {
            showSpinnerDialog()
        }

        fabReset.setOnClickListener {
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

        builder.setPositiveButton("OK") { dialog, which ->
            addType = spinner.selectedItem as String

            if (addType == "App") {

                appsShown.clear()
                appsShown.addAll(allApps)
                boolSelectOrLaunch = true

                rvAdapter.notifyDataSetChanged()
            } else if (addType == "Contact") {
                // makeToast("yet2ImplC")
            } else if (addType == "Web link") {
                // makeToast("yet2ImplWL")
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, which -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()
        dialog.show()

        return addType
    }

    private fun getLaunchableApps(): List<ResolveInfo> {
        val packageManager: PackageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        // Query all activities that can be launched
        val allLaunchableApps: List<ResolveInfo> =
            packageManager.queryIntentActivities(mainIntent, 0)

        // Filter the list to include only Google's apps
        return allLaunchableApps
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemClick(pos: Int) {

        mySpaceActivityContext = applicationContext


        if (boolSelectOrLaunch) {
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


        } else {

            if (appsShown.size > pos) {
                val launchIntent = packageManager.getLaunchIntentForPackage(appsShown[pos].pName)
                startActivity(launchIntent)
            }

        }


    }

}