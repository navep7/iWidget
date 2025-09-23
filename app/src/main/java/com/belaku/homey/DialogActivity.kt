package com.belaku.homey

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.NewAppWidget.Companion.appWidM
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
import kotlin.properties.Delegates

class DialogActivity : AppCompatActivity() {

    private val REQUEST_CODE_SPEECH_INPUT: Int = 100
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var blE by Delegates.notNull<Boolean>()
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
                btnOk.setText("Set")
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
            } else if (dialogIntentStr == "AD") {

                llDialog.visibility = View.INVISIBLE

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