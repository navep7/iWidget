package com.belaku.homey

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


object TweetsJsonParser {
    fun parseJsonArrayFromRaw(context: Context, resourceId: Int): JSONArray? {
        var jsonArray: JSONArray? = null
        var jsonString: String? = null
        var inputStream: InputStream? = null
        var reader: BufferedReader? = null

        try {
            inputStream = context.resources.openRawResource(resourceId)
            reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                stringBuilder.append(line)
            }
            jsonString = stringBuilder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        if (jsonString != null) {
            try {
                jsonArray = JSONArray(jsonString)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return jsonArray
    }
}