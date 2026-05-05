package com.belaku.homey

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.appUsageStats
import com.belaku.homey.StepsService.Companion.choosenApps


class RemoteViewsContactsFactory(private val mContext: Context) :
    RemoteViewsFactory {

    override fun onCreate() {
        // Initialize your data source here
        if (favContacts.isEmpty())
            NewAppWidget().getFavoriteContacts()

        makeToast("favContactsSZ : ${favContacts.size}")
    }

    override fun onDataSetChanged() {
        // Called when the data needs to be updated (e.g., after notifyAppWidgetViewDataChanged)
    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return favContacts.size + 1
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getViewAt(position: Int): RemoteViews {
        val rvContacts = RemoteViews(mContext.packageName, R.layout.remote_view_layout_contact)

        if (favContacts.size == position) {

                rvContacts.setViewVisibility(R.id.contact_tx_close, View.INVISIBLE)
                rvContacts.setImageViewResource(
                    R.id.contact_imgv,
                    android.R.drawable.ic_menu_add
                )
                rvContacts.setTextViewText(R.id.contact_tx_name, "Add NEW")

                val pickContact = Intent(mContext, DialogActivity::class.java).putExtra("DialogIntent", "PC")
                rvContacts.setOnClickFillInIntent(
                    R.id.contact_imgv, pickContact
                )
            } else {

                val roundedBitmapDrawable: RoundedBitmapDrawable =
                    RoundedBitmapDrawableFactory.create(
                        mContext.resources,
                        favContacts[position].contactBitmap
                    )
                val cornerRadius =
                    (favContacts[position].contactBitmap.width / 0.5) // Example radius in pixels
                roundedBitmapDrawable.cornerRadius = cornerRadius.toFloat()

                // Create the fill-in intent
                val fillInIntentDial = Intent()
                fillInIntentDial.putExtra(
                    NewAppWidget.EXTRA_CONTACTITEM_POSITION,
                    position
                ) // Add item-specific data
                fillInIntentDial.putExtra(
                    NewAppWidget.EXTRA_CONTACTVIEW_ID,
                    0
                )
                // setOnClickFillInIntent is called on the root view of the list item layout


                val fillInIntentRemove = Intent()
                fillInIntentRemove.putExtra(
                    NewAppWidget.EXTRA_CONTACTITEM_POSITION,
                    position
                ) // Add item-specific data
                fillInIntentRemove.putExtra(
                    NewAppWidget.EXTRA_CONTACTVIEW_ID,
                    1
                )

                rvContacts.setImageViewBitmap(
                    R.id.contact_imgv,
                    drawableToBitmap(mContext, roundedBitmapDrawable)
                )
                rvContacts.setTextViewText(R.id.contact_tx_name, favContacts[position].name)
                rvContacts.setOnClickFillInIntent(R.id.contact_imgv, fillInIntentDial)
                rvContacts.setOnClickFillInIntent(R.id.contact_tx_close, fillInIntentRemove)
            }

        return rvContacts
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