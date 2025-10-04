package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.listTweets
import com.belaku.homey.MainActivity.Companion.makeSnack
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.MainActivity.Companion.twitterProfileName
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.drawableToBitmap
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.noRewards
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.NewAppWidget.Companion.tW
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import kotlin.properties.Delegates


class DialogActivity : AppCompatActivity() {

    private val REQUEST_CODE_SPEECH_INPUT: Int = 100
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var blE by Delegates.notNull<Boolean>()
    private var wifE by Delegates.notNull<Boolean>()
    private lateinit var llDialog: LinearLayout
    private lateinit var txTitle: TextView
    private lateinit var txContent: TextView

    private lateinit var edtxDialog: EditText

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    private lateinit var imgbtnShare: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog)

        rewardedInterstitialAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                 //   Log.d(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    rewardedInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when fullscreen content failed to show.
                 //   Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    rewardedInterstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                 //   Log.d(TAG, "Ad showed fullscreen content.")

                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                //    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdClicked() {
                    // Called when an ad is clicked.
                //    Log.d(TAG, "Ad was clicked.")
                }
            }

        var bluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    if (blE)
                    makeToast("Bluetooth ON")
                    else makeToast("Bluetooth OFF")
                } else {
                    // Bluetooth not enabled by user
                }
            }

        var wifiLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    if (blE)
                        makeToast("Wifi ON")
                    else makeToast("Wifi OFF")
                } else {
                    // Bluetooth not enabled by user
                }
            }

        llDialog = findViewById<LinearLayout>(R.id.dialog_layout)
        txTitle = findViewById<TextView>(R.id.tx_dialog_title)
        txContent = findViewById<TextView>(R.id.tx_dialog_content)
        edtxDialog = findViewById<EditText>(R.id.edtx_dialog)
        btnOk = findViewById<Button>(R.id.btn_dialog_ok)
        btnCancel = findViewById<Button>(R.id.btn_dialog_cancel)
        imgbtnShare = findViewById<ImageButton>(R.id.imgbtn_dialog_share)


        var dialogIntentStr = intent.getStringExtra("DialogIntent")

        if (dialogIntentStr != null) {
            if (dialogIntentStr == "StT") {
                edtxDialog.visibility = View.INVISIBLE
                btnOk.visibility = View.INVISIBLE
                btnCancel.visibility = View.INVISIBLE
                txTitle.setText("Speech to Text")
                txContent.setText("listening...")

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak now..."
                ) // Optional: prompt for the user
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)

                imgbtnShare.setOnClickListener(View.OnClickListener {
                    if (txContent.text != "listening...") {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, txContent.text).putExtra(Intent.EXTRA_SUBJECT, "Sharing via nHome!"), "Share via..."))
                    }
                })

            }
            else  if (dialogIntentStr == "ST") {
                edtxDialog.visibility = View.INVISIBLE
                btnOk.visibility = View.INVISIBLE
                btnCancel.visibility = View.INVISIBLE
                txTitle.setText("Tweet")
                txContent.setText(tW)
                imgbtnShare.setOnClickListener(View.OnClickListener {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, tW).putExtra(Intent.EXTRA_SUBJECT, "Sharing via nHome!"), "Share via..."))
                })
            } else  if (dialogIntentStr == "STH") {
                txTitle.setText("Twitter")
                txContent.visibility = View.INVISIBLE
                edtxDialog.visibility = View.VISIBLE
                btnOk.setText("Set")
                btnOk.setOnClickListener(View.OnClickListener {
                    if (edtxDialog.text.toString().equals("Fact")) {
                        twitterProfileName = "Fact"
                        listTweets.clear()
                        rawTweets(false)
                    } else {
                        getTweetID(edtxDialog.text.toString(), false)
                    }
                })
            } else if (dialogIntentStr == "BLUEEnable") {
                blE = true
                llDialog.visibility = View.GONE
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothLauncher.launch(enableBtIntent)
            } else if (dialogIntentStr == "BLUEDisable") {
                blE = false
                llDialog.visibility = View.GONE
                val disableintent = Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
                bluetoothLauncher.launch(disableintent)
            } else if (dialogIntentStr == "WifiEnable" || dialogIntentStr == "WifiDisable") {
                try {
                    val intent = Intent(Intent.ACTION_MAIN, null)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val cn = ComponentName(
                        "com.android.settings",
                        "com.android.settings.wifi.WifiSettings"
                    )
                    intent.setComponent(cn)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (ignored: ActivityNotFoundException) {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            }  else if (dialogIntentStr == "AD") {

                txTitle.setText("loading Advertisement, please wait...")
                txContent.visibility = View.GONE
                edtxDialog.visibility = View.GONE
                imgbtnShare.visibility = View.GONE
                btnOk.visibility = View.GONE
                btnCancel.visibility = View.GONE

                RewardedInterstitialAd.load(
                    this,
                    getString(R.string.admob_ri_ad),
                    AdRequest.Builder().build(),
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                            makeToast("Ad was loaded.")
                            rewardedInterstitialAd = rewardedAd

                            rewardedInterstitialAd?.show(this@DialogActivity) { rewardItem ->
                                makeToast("User earned the reward.")
                                // Handle the reward.
                                val rewardAmount = rewardItem.amount
                                val rewardType = rewardItem.type
                                sharedPreferencesEditor.putInt("noRewards", 7).apply()
                                noRewards = 7
                                remoteViews?.setTextViewText(R.id.tx_rewards_count, "" + 7)
                                txTitle.setText("swipe outside to continue changing walls.")
                                appWidM.updateAppWidget(newAppWidget, remoteViews)
                            }
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            makeToast("onAdFailedToLoad: ${adError.message}")
                            rewardedInterstitialAd = null
                        }
                    },
                )
            }

        }
    }

    private fun getTweetID(str: String, b: Boolean) {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://twitter241.p.rapidapi.com/user?username=$str")
            .get()
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com")
            .build()


        pD.setTitle("Twitter")
        pD.setMessage("fetching user ID...")
        if (b)
            pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
            var responseTweetID = client.newCall(request).execute()

            withContext(Dispatchers.Main) {
                // Handle the result and hide the loading indicator
                if (b)
                    pD.dismiss()
                val responseBodyString = responseTweetID.peekBody(Long.MAX_VALUE).string()


                val jsonObject = JSONObject(responseBodyString)

                if (jsonObject.getJSONObject("result").getJSONObject("data").optString("user")
                        .isNotEmpty()
                )
                    if (jsonObject.getJSONObject("result").getJSONObject("data")
                            .getJSONObject("user").optString("result")
                            .isNotEmpty()
                    )
                        if (jsonObject.getJSONObject("result").getJSONObject("data")
                                .getJSONObject("user").getJSONObject("result").optString("rest_id")
                                .isNotEmpty()
                        ) {
                            var twitterID = jsonObject.getJSONObject("result").getJSONObject("data")
                                .getJSONObject("user")
                                .getJSONObject("result").getString("rest_id")
                            var twitterPicUrl =
                                jsonObject.getJSONObject("result").getJSONObject("data")
                                    .getJSONObject("user")
                                    .getJSONObject("result").getJSONObject("avatar")
                                    .getString("image_url")

                            twitterProfileName =
                                jsonObject.getJSONObject("result").getJSONObject("data")
                                    .getJSONObject("user")
                                    .getJSONObject("result").getJSONObject("core")
                                    .getString("screen_name")
                            Log.d("TwitterPicUrl - ", twitterPicUrl)

                            remoteViews =
                                RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                            newAppWidget =
                                ComponentName(applicationContext, NewAppWidget::class.java)
                            remoteViews?.setImageViewUri(R.id.twSettings, Uri.parse(twitterPicUrl))

                            appWidM = AppWidgetManager.getInstance(appContx)
                            appWidM.updateAppWidget(newAppWidget, remoteViews)

                        //    Log.d(TAG + "responseTweetID - ", responseBodyString)
                          //  Log.d(TAG + "Tw ID - ", twitterID + " - " + twitterProfileName)

                            if (b)
                                pD.dismiss()

                            getTweets(twitterID, false)
                        } else {
                            if (b)
                                pD.dismiss()
                            makeSnack("Twitter User doesn't Exist!")

                        }
                    else {
                        if (b)
                            pD.dismiss()
                        makeSnack("Twitter User doesn't Exist!")

                    }
                else {
                    if (b)
                        pD.dismiss()
                    makeSnack("Twitter User doesn't Exist!")

                }
                // Update UI with result
            }
        }


    }

    private fun getTweets(twitterID: String, b: Boolean) {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://twitter241.p.rapidapi.com/user-tweets?user=$twitterID&count=5")
            .get()
            .addHeader("x-rapidapi-key", "8521aa6a65mshab927b74fff566dp175607jsn24cd6edd63a7")
            .addHeader("x-rapidapi-host", "twitter241.p.rapidapi.com")
            .build()

        pD.setTitle("Twitter")
        pD.setMessage("fetching Tweets...")
        if (b)
            pD.show()
        lifecycleScope.launch(Dispatchers.IO) {
            var responseTweets = client.newCall(request).execute()

            var js: JSONArray = (JSONObject(responseTweets.body?.string()).getJSONObject("result")
                .getJSONObject("timeline")
                .getJSONArray("instructions"))//[2] as JSONObject).getJSONArray("entries")

            for (i in 0 until js.length()) {
                if (js[i].toString().contains("entries"))
                    js = (js[i] as JSONObject).getJSONArray("entries")
            }

            withContext(Dispatchers.Main) {
                if (b)
                    pD.dismiss()
                if (js.length() > 0)
                    listTweets.clear()
                for (i in 0 until js.length()) {
                    val tw =
                        JSONObject(js[i].toString()).getJSONObject("content")//.getJSONObject("itemContent").getJSONObject("tweet_results").getJSONObject("result")
                    //   .getJSONObject("legacy").get("full_text")

                    if (tw.optString("itemContent").isNotEmpty()) {
                        val actTw = tw.getJSONObject("itemContent").getJSONObject("tweet_results")
                            .getJSONObject("result")
                            .getJSONObject("legacy").get("full_text")

                        Log.d("Twwtt $i", actTw.toString())
                        listTweets.add(actTw.toString())
                    }
                }

                makeSnack("Tweets - ${listTweets.size}")

                remoteViews = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)
                newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)
                remoteViews?.setTextViewText(
                    R.id.tx_tweets,
                    "@" + twitterProfileName + "\t ~ \t" + listTweets[1]
                )

                appWidM = AppWidgetManager.getInstance(appContx)
                appWidM.updateAppWidget(newAppWidget, remoteViews)

            }
        }

        Log.d("result", "res - ${listTweets.size}")
    }

    private fun rawTweets(b: Boolean) {


        if (b) {
            pD.setTitle("Twitter")
            pD.setMessage("fetching Tweets...")
            pD.show()
            Handler().postDelayed(Runnable {
                pD.dismiss()
            }, 1000)
        }

        val dataArray: JSONArray = TweetsJsonParser.parseJsonArrayFromRaw(this, R.raw.np_tweets)!!

        for (i in 0 until dataArray.length()) {
            try {
                val item = dataArray.getJSONObject(i)
                val tweet = item.getString("text")
                listTweets.add(tweet)

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        //       makeSnack("Tweets - ${listTweets.size}")

        var bitmapTwPic: Bitmap =
            drawableToBitmap(applicationContext, resources.getDrawable(R.drawable.walp_icon))

        remoteViews = RemoteViews(applicationContext.packageName, R.layout.new_app_widget)

        lifecycleScope.launch { // Launch a coroutine in the lifecycle scope
            val imageUrl =
                "https://pbs.twimg.com/profile_images/1244657050275151872/BRycNabV_normal.jpg" // Replace with your image URL
            val bitmap = getBitmapFromUrl(imageUrl)
            // Now you have the bitmap, you can display it in an ImageView or process it further
            if (bitmap != null) {
                //     makeToast("TwiPic")
                try {
                    remoteViews?.setTextViewText(
                        R.id.tx_tweets,
                        "@" + twitterProfileName + "\t ~ \t" + listTweets[1]
                    )
                    remoteViews?.setImageViewBitmap(R.id.twSettings, bitmap)
                } catch (ex: Exception) {
                    makeToast("TwiEx - ${ex.message}")
                }
            }
        }

        newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)


        appWidM = AppWidgetManager.getInstance(appContx)
        appWidM.updateAppWidget(newAppWidget, remoteViews)

    }

    suspend fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) { // Switch to the IO dispatcher for network operations
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val inputStream = connection.getInputStream()
                BitmapFactory.decodeStream(inputStream) // Decode the input stream into a Bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null // Return null on error
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (result != null && !result.isEmpty()) {
                    val recognizedText = result[0] // Get the most likely recognized phrase
                    txContent.setText(recognizedText)
                }
            }
        }
    }


}