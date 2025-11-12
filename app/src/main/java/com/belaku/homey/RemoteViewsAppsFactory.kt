package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.NewAppWidget.Companion.choosenApps
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap


class RemoteViewsAppsFactory(private val mContext: Context) :
    RemoteViewsFactory {

    override fun onCreate() {
        // Initialize your data source here

    }

    override fun onDataSetChanged() {
        // Called when the data needs to be updated (e.g., after notifyAppWidgetViewDataChanged)
    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return choosenApps.size
    }

    override fun getViewAt(position: Int): RemoteViews  {
        val rvApps = RemoteViews(mContext.packageName, R.layout.remote_view_layout_app)

        rvApps.setImageViewBitmap(R.id.app_imgv, drawableToBitmap(appContx, appContx.packageManager.getApplicationIcon(choosenApps[position].pName)))
        rvApps.setTextViewText(R.id.app_tx, choosenApps[position].name)

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