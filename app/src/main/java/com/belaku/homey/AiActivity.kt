package com.belaku.homey


import AppsAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.databinding.ActivityAiBinding
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.launch
import java.util.Locale


class AiActivity : AppCompatActivity(), AppsAdapter.RvEvent, TextToSpeech.OnInitListener {


    private val generativeModel: GenerativeModel get() = generativeModelInstance
    private val REQUEST_CODE_SPEECH_INPUT: Int = 1
    private lateinit var tts: TextToSpeech
    private lateinit var playAI: ImageButton
    private lateinit var voiceAI: ImageView
    private lateinit var prompt: String
    private lateinit var txAi: TextView
    private lateinit var edtxAi: EditText
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAiBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAiBinding.inflate(layoutInflater)

        setContentView(binding.root)

        tts = TextToSpeech(this, this)


        val rootLayout = findViewById<RelativeLayout>(R.id.ai_layout)
        rootLayout.setBackgroundDrawable(BitmapDrawable(getResources(), blur(applicationContext, SetWallWorker.wallBitmap)))


        voiceAI = findViewById<ImageView>(R.id.imgv_mic)
        playAI = findViewById<ImageButton>(R.id.play_ai)
        edtxAi = findViewById<EditText>(R.id.edtx_ai)
        txAi = findViewById<TextView>(R.id.tx_ai_response)
     //   txAi.setTextColor(NewAppWidget.tertianaryColor)
        txAi.movementMethod = ScrollingMovementMethod()


        voiceAI.setOnClickListener {

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toString())
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, Locale.getDefault().toString())
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT, "Speak now..."
            ) // Optional: prompt for the user
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)



        }


        edtxAi.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                //do what you want on the press of 'done'
                try {
                    generateAIresponse(generativeModel, edtxAi.text.toString())
                } catch (ex: Exception) {
                     makeToast("Gemini Exception - $ex")
                }
            }
            false
        })

    }

    private fun generateAIresponse(generativeModel: GenerativeModel, prompt: String) {
        txAi.setText("Generating AI response, please wait...")
        lifecycleScope.launch {
            try {
                val txtResponse = generativeModel.generateContent(prompt)

                Toast.makeText(applicationContext, txtResponse.text, Toast.LENGTH_LONG).show()
                txAi.setText(txtResponse.text)

                playAI.visibility = View.VISIBLE

                playAI.setOnClickListener(View.OnClickListener {
                    if (tts.isSpeaking) {
                        tts.stop()
                        playAI.setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        speakLongText(txtResponse.text.toString())
                        playAI.setImageResource(android.R.drawable.ic_media_pause)
                    }
                })
            } catch (ex: Exception) {
                 makeToast("Gemini AI exception - $ex")
            }


        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    fun speakLongText(longText: String) {
        tts?.let {
            val maxLength = 4000 // Get max length supported by engine
            if (longText.length <= maxLength) {
                it.speak(longText, TextToSpeech.QUEUE_FLUSH, null, "unique_utterance_id")
            } else {
                // Split the long text into smaller parts
                val chunks = longText.chunked(maxLength)
                for (chunk in chunks) {
                    it.speak(chunk, TextToSpeech.QUEUE_ADD, null, "unique_utterance_id_part_${chunks.indexOf(chunk)}")
                }
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
                    // makeToast(recognizedText)
                    edtxAi.setText(recognizedText)
                    try {
                        generateAIresponse(generativeModel, recognizedText)
                    } catch (ex: Exception) {
                         makeToast("Gemini Exception - $ex")
                    }
                }
            }
        }
    }

    fun blur(context: Context?, image: Bitmap): Bitmap {

        var BITMAP_SCALE = 0.4f; // Scale down bitmap for performance
        var BLUR_RADIUS = 25f; // Adjust blur intensity

        val width = Math.round(image.width * BITMAP_SCALE).toInt()
        val height = Math.round(image.height * BITMAP_SCALE).toInt()

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)

        // Cleanup RenderScript resources to avoid memory leaks
        rs.destroy()
        theIntrinsic.destroy()
        tmpIn.destroy()
        tmpOut.destroy()

        return outputBitmap
    }

    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(apps[pos].pName)
        startActivity(launchIntent)
    }

    override fun onInit(status: Int) {
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.UK)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language Not Supported", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        // Use a singleton for GenerativeModel to reuse the underlying gRPC channel
        // and avoid "Previous channel was garbage collected without being shut down" warnings.
        private val generativeModelInstance: GenerativeModel by lazy {
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-1.5-flash")
        }
    }


}