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
        if (tts == null) {
            initializeTTS()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tts == null) {
            initializeTTS()
        }
        return START_STICKY
    }

    private fun initializeTTS() {
        try {
            // Try to use the Google TTS engine explicitly
            tts = TextToSpeech(this, this, "com.google.android.tts")
        } catch (e: Exception) {
            try {
                // Fallback to default engine
                tts = TextToSpeech(this, this)
            } catch (e2: Exception) {
                Log.e("SpeakService", "Failed to initialize TTS", e2)
            }
        }
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("SpeakService", "Error during TTS shutdown", e)
        }
        tts = null
        isReady = false
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("TTS", "This Language is not supported")
            } else {
                isReady = true
                synchronized(speechQueue) {
                    if (speechQueue.isNotEmpty()) {
                        Log.d("SpeakService", "TTS Ready, flushing ${speechQueue.size} queued messages")
                        for (msg in speechQueue) {
                            tts?.speak(msg, TextToSpeech.QUEUE_ADD, null, "utteranceId")
                        }
                        speechQueue.clear()
                    }
                }
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }


    companion object {
        private var tts: TextToSpeech? = null
        private var isReady = false
        private val speechQueue = mutableListOf<String>()

        fun speakOut(spk: String) {
            val currentTts = tts
            if (isReady && currentTts != null) {
                try {
                    // Using QUEUE_ADD to avoid cutting off concurrent notifications
                    currentTts.speak(spk, TextToSpeech.QUEUE_ADD, null, "utteranceId")
                } catch (e: Exception) {
                    Log.e("SpeakService", "Error during speakOut", e)
                }
            } else {
                synchronized(speechQueue) {
                    speechQueue.add(spk)
                }
                Log.d("SpeakService", "TTS not ready or service not running, queued: $spk")
            }
        }
    }
}
