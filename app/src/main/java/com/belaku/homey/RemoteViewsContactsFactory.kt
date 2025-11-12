package com.belaku.homey

import android.R.attr.height
import android.R.attr.width
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.ContactsContract
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.mAct
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.favContacts
import java.io.InputStream


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


            rvContacts.setImageViewBitmap(R.id.contact_imgv, drawableToBitmap(appContx, appContx.resources.getDrawable(R.drawable.face_holder)))
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




    private fun getBitmapFromName(substring: String): Bitmap {

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        val paint: Paint = Paint()
        paint.setColor(Color.BLACK) // Set text color
        paint.setTextSize(50F) // Set text size
        paint.setTypeface(Typeface.DEFAULT_BOLD) // Optional: set typeface
        paint.setAntiAlias(true) // For smoother text rendering

        return bitmap
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