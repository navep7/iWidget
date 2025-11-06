package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import java.io.IOException


class RemoteViewsContactsFactory(private val mContext: Context, intent: Intent?) :
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

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(mContext.packageName, R.layout.remote_view_layout)

        val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
            appContx.contentResolver,
            Uri.parse(favContacts[position].image)
        )

        val bm: Bitmap
        var rBm: Bitmap
        if (inputStream != null) {
            bm = BitmapFactory.decodeStream(inputStream)

            rBm =
                drawableToBitmap(
                    appContx,
                    RoundedBitmapDrawableFactory.create(appContx.resources, bm)
                )
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            bm = drawableToBitmap(
                appContx,
                appContx.resources.getDrawable(R.drawable.face_holder)
            )
            rBm =
                drawableToBitmap(
                    appContx,
                    RoundedBitmapDrawableFactory.create(appContx.resources, bm)
                )
        }


        rv.setTextViewText(
            R.id.new_tx_id,
            favContacts[position].name.substring(0, 1)
                .uppercase() + favContacts[position].name.substring(1, 2).lowercase()
        )
        rv.setImageViewBitmap(R.id.new_imgv_id, rBm)


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
        rv.setOnClickFillInIntent(R.id.new_imgv_id, fillInIntentDial)

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
        rv.setOnClickFillInIntent(R.id.new_tx_close_id, fillInIntentRemove)


        return rv
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