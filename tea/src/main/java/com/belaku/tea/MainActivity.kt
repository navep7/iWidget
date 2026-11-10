package com.belaku.tea

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform


class MainActivity : AppCompatActivity() {
    private var tweetListView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tweetListView = findViewById<ListView?>(R.id.tweetListView)

        // Initialize Python context
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Run the scraping logic on a background thread to prevent UI freezing
        Thread(Runnable {
            try {
                val py = Python.getInstance()
                val pyModule = py.getModule("twitter_scraper")


                // Call the python function passing ('username', tweet_count)
                val pyResult = pyModule.callAttr("get_user_tweets", "NASA", 15)


                // Convert Python List into a Java List
                val pyList = pyResult.asList()
                val tweets = ArrayList<String?>()
                for (obj in pyList) {
                    tweets.add(obj.toString())
                }

                // Update UI elements back on the Main Thread
                runOnUiThread(Runnable {
                    val adapter = ArrayAdapter<String?>(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        tweets
                    )
                    tweetListView!!.setAdapter(adapter)
                })
            } catch (e: Exception) {
                runOnUiThread(Runnable {
                    Log.d("FTW", "Failed to load tweets ~ ${e.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load tweets ~ ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                )
            }
        }).start()
    }
}
