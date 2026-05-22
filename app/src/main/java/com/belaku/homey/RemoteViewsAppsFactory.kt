package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.hashSetAppUsage
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.SetWallWorker.Companion.appUsageStats
import com.belaku.homey.StepsService.Companion.choosenApps


class RemoteViewsAppsFactory(private val mContext: Context) :
    RemoteViewsFactory {

    override fun onCreate() {
        // Initialize your data source here
        if (choosenApps.isEmpty())
            appUsageStats(mContext)

      //  makeToast("Most Used Apps : ${choosenApps.size}")
    }

    override fun onDataSetChanged() {
        // Called when the data needs to be updated (e.g., after notifyAppWidgetViewDataChanged)
    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return choosenApps.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val rvApps = RemoteViews(mContext.packageName, R.layout.remote_view_layout_app)

        try {
            rvApps.setImageViewBitmap(
                R.id.app_imgv,
                drawableToBitmap(
                    mContext,
                    mContext.packageManager.getApplicationIcon(choosenApps[position].pName)
                )
            )
            rvApps.setTextViewText(R.id.app_tx, choosenApps[position].name)
         //   rvApps.setTextViewText(R.id.app_time, choosenApps[position].usage)

            if (Integer.parseInt(choosenApps[position].usage.split(":")[0]) < 60)
                rvApps.setTextViewText(R.id.app_time, choosenApps[position].usage.split(":")[0])
            else if (Integer.parseInt(choosenApps[position].usage.split(":")[0]) < 300)
                rvApps.setTextViewText(R.id.app_time, (Integer.parseInt(choosenApps[position].usage.split(":")[0]) / 60).toString() + "+")
            else {
                Log.d("H3r7 - ", choosenApps[position].name + " - " + choosenApps[position].usage)
                if (hashSetAppUsage.removeIf { it.appName == choosenApps[position].pName }) {
                    Log.d("H3r7", "YES")
                    hashSetAppUsage.forEach { appUsage ->
                        Log.d("H3r7", "Rrd User details: ${appUsage.appName}") }
                } else hashSetAppUsage.forEach { appUsage ->
                    Log.d("H3r7", "User details: ${appUsage.appName}")
                }
                rvApps.setTextViewText(R.id.app_time, "?+")
            }


            // Create the fill-in intent
            val fillInIntentApp = Intent()
            fillInIntentApp.putExtra(
                NewAppWidget.EXTRA_APPITEM_POSITION,
                position
            ) // Add item-specific data
            fillInIntentApp.putExtra(
                NewAppWidget.EXTRA_APPVIEW_ID,
                0
            )
            // setOnClickFillInIntent is called on the root view of the list item layout
            rvApps.setOnClickFillInIntent(R.id.app_imgv, fillInIntentApp)

        } catch (ex: Exception) {
            Log.d("EX3P - ", ex.message.toString(), ex)
        }
        return rvApps
    }

    override fun getLoadingView(): RemoteViews? {
        return null // You can provide a custom loading view
    }

    override fun getViewTypeCount(): Int {
        return 1 // Number of different layout types in your list
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}