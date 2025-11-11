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
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.choosenApps
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
        val rvContacts = RemoteViews(mContext.packageName, R.layout.remote_view_layout_contact)

        if (favContacts.size > position) {
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

            try {
                rvContacts.setTextViewText(
                    R.id.contact_tx_name,
                    favContacts[position].name
                )
                rvContacts.setImageViewBitmap(R.id.contact_imgv, rBm)
            } catch (_:Exception) {

            }

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