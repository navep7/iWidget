package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.belaku.homey.MainActivity.Companion.appWidM
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.choosenApps
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.selectedApp
import com.belaku.homey.NewAppWidget.Companion.selectedApps
import java.util.Collections


class AppChooserDialog : Activity() {

    private var appViewID: Int = 0
    private var appID: Int = 0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_chooser_dialog)

        var gridView: GridView = findViewById(R.id.grid_view)


        if (intent.extras != null)
        appID = intent.extras!!.getInt("id")
        makeToast("appID - " + appID)

        if (appID == 6)
            appViewID = R.id.imgv_app6
        else if (appID == 7)
            appViewID = R.id.imgv_app7
        else if (appID == 8)
            appViewID = R.id.imgv_app8
        else if (appID == 9)
            appViewID = R.id.imgv_app9

        getApps(applicationContext)

        //   list.add(App("DSA", R.drawable.calls))

     //   appLists.add(SelectedApp("n", applicationContext.resources.getDrawable(R.drawable.launch_e)))
        val adapter = GridViewAdapter(this, appLists)
        gridView.adapter = adapter


        gridView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            Toast.makeText(applicationContext, appLists.get(position).name, Toast.LENGTH_SHORT).show()
            makeToast("b4 - ${choosenApps.size}")
            makeToast("a4 - ${choosenApps.size}")
            selectedApp = appLists.get(position).icon
            selectedApps.add(SelectedApp(appLists[position].name, appLists[position].pName, selectedApp))
            remoteViews?.setImageViewBitmap(appViewID, selectedApp)
            appWidM.updateAppWidget(newAppWidget, remoteViews)
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
                var appIcon: Drawable = packageManager.getApplicationIcon(apps[i])
                appNames.add(appName)
                appIcons.add(appIcon)
                val bitmap = Bitmap.createBitmap(
                    appIcon.getIntrinsicWidth(),
                    appIcon.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
                )
                val canvas: Canvas = Canvas(bitmap)
                appIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
                appIcon.draw(canvas)
                appLists.add(SelectedApp(appName, apps[i].packageName, bitmap))
                sortApps(appLists)
            }
        }

     //   sortApps(appNames, appIcons)

    }

    private fun sortApps(list: java.util.ArrayList<SelectedApp>) {

        Collections.sort<SelectedApp>(
            list
        ) { p1: SelectedApp, p2: SelectedApp ->
            p1.name.compareTo(p2.name)
        }

    }

    companion object {
        val appLists: ArrayList<SelectedApp> = ArrayList()
    }


}