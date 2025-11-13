package com.belaku.homey

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import java.io.IOException


object ContactPhotoHelper {
    fun retrieveContactPhoto(context: Context, contactId: Long): Bitmap? {
        val contactUri =
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
            context.contentResolver,
            contactUri
        )

        if (inputStream != null) {
            val photo = BitmapFactory.decodeStream(inputStream)
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return photo
        } else {
            // Return a default image or null if no photo exists
            return null // Or BitmapFactory.decodeResource(context.getResources(), R.drawable.default_contact_photo);
        }
    }
}