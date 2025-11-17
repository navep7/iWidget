package com.belaku.homey

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts


class RemoteViewsContactsFactory(private val mContext: Context) :
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

        return favContacts.size
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getViewAt(position: Int): RemoteViews {
        val rvContacts = RemoteViews(mContext.packageName, R.layout.remote_view_layout_contact)

        if (favContacts.size > position) {


            val roundedBitmapDrawable: RoundedBitmapDrawable =
                RoundedBitmapDrawableFactory.create(mContext.resources, favContacts[position].contactBitmap)
            val cornerRadius = (favContacts[position].contactBitmap.width / 0.5) // Example radius in pixels
            roundedBitmapDrawable.cornerRadius = cornerRadius.toFloat()
            rvContacts.setImageViewBitmap(R.id.contact_imgv, drawableToBitmap(mContext, roundedBitmapDrawable))

            rvContacts.setTextViewText(R.id.contact_tx_name, favContacts[position].name)

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
                   rvContacts.setOnClickFillInIntent(R.id.contact_imgv, fillInIntentDial)

            val fillInIntentRemove = Intent()
            fillInIntentRemove.putExtra(
                NewAppWidget.EXTRA_CONTACTITEM_POSITION,
                position
            ) // Add item-specific data
            fillInIntentRemove.putExtra(
                NewAppWidget.EXTRA_CONTACTVIEW_ID,
                1
            )

            // setOnClickFillInIntent is called on the root view of the list item layout
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