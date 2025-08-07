package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Collections


class AppChooserDialog : Activity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_chooser_dialog)

        var gridView: GridView = findViewById(R.id.grid_view)

        getApps(applicationContext)

        //   list.add(App("DSA", R.drawable.calls))

        val adapter = GridViewAdapter(this, appLists)
        gridView.adapter = adapter


        gridView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            Toast.makeText(applicationContext, appLists.get(position).name, Toast.LENGTH_SHORT).show()
      //      NewAppWidget.addAppInWidget(applicationContext, appLists.get(position))
            choosenApps.add(appLists.get(position))
            goHome()
        }




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SaveAppsToSharedP(choosenApps)
    }

    private fun SaveAppsToSharedP(choosenApps: ArrayList<App>) {

        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("app1", choosenApps.get(0).name)
        editor.apply()

    }

    private fun goHome() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(startMain)
    }

    private fun getApps(applicationContext: Context) {

        val packageManager = applicationContext.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        var appNames: ArrayList<String> = ArrayList()
        var appIcons: ArrayList<Drawable> = ArrayList()

        for (i in 0 until apps.size) {
            if ((apps.get(i).flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                var appName = apps.get(i).loadLabel(packageManager).toString()
                var appIcon: Drawable = packageManager.getApplicationIcon(apps.get(i))
                appNames.add(appName)
                appIcons.add(appIcon)
          //      appLists.add(App(apps.get(i).loadLabel(packageManager).toString(), appIcon))
                sortApps(appLists)
            }
        }

     //   sortApps(appNames, appIcons)

    }

    private fun sortApps(list: java.util.ArrayList<App>) {

        Collections.sort<App>(
            list
        ) { p1: App, p2: App ->
            p1.name.compareTo(p2.name)
        }

    }

    companion object {
        val appLists: ArrayList<App> = ArrayList()
        val choosenApps: ArrayList<App> = ArrayList()
    }


}