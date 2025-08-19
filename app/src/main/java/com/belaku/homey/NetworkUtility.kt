package com.belaku.homey

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class NetworkUtility {
    @Throws(IOException::class)
    fun getInputStreamFromUrl(urlString: String?): InputStream {

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val url = URL(urlString)
        var urlConnection: HttpURLConnection? = null
        try {
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET" // Or "POST", "PUT", etc. as needed
            urlConnection!!.connect()

            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                return urlConnection.inputStream
            } else {
                // Handle non-OK responses (e.g., 404, 500)
                throw IOException("HTTP error code: " + urlConnection.responseCode)
            }
        } finally {
            // It's good practice to close the connection in a finally block
            // if you are not returning the InputStream directly
            // If returning, the caller is responsible for closing the InputStream
            // and implicitly, the connection.
        }
    }
}