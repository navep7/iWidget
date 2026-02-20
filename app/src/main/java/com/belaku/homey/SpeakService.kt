package com.belaku.homey

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import java.util.Locale


class SpeakService : Service(), OnInitListener {


    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        // TODO Auto-generated method stub
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        tts = TextToSpeech(this, this)
     //   speakOut(1)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("TTS", "This Language is not supported")
            }
        //    speakOut(1)
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }



    companion object {
        lateinit var spoken: String
        lateinit var tts: TextToSpeech

        fun isSpokenInitialized(): Boolean {
            return ::spoken.isInitialized // Works inside the companion object
        }

        fun speakOut(spk: String) {

            if (isSpokenInitialized()) {
                if (spk != spoken) {
                    spoken = spk
                    tts.speak(spk, TextToSpeech.QUEUE_FLUSH, null)
                }
            } else {
                spoken = spk
                tts.speak(spk, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }
}
