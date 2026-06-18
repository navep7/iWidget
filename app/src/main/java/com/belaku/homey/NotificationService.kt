package com.belaku.homey

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SpeakService.Companion.speakOut


class NotificationService : NotificationListenerService() {

    private lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate() {
        super.onCreate()
     //   setupSpeechRecognizer()
        Log.d("NoteServiceLOG", "onCreate")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NoteServiceLOG", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NoteServiceLOG", "onListenerDisconnected")

        // Request rebind to ensure the service stays active
        requestRebind(ComponentName(this, NotificationService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // Extract app name
        var appName: String
        try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            appName = packageManager.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            appName = "Unknown"
        }

        // Initialize sharedPreferences if not already done (context safe)
        val prefs = applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)

        if (prefs.getBoolean("SPKSERVICE", false)) {
             speakOut(appName)
         //   speakOut("Would you like to hear the content?")
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    handleVoiceCommand(matches[0])
                }
            }
            override fun onError(error: Int) {
                Log.e("Speech", "Error: $error")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {}

        })
    }

    private fun handleVoiceCommand(command: String) {
        makeToast("hello - $command")
        when {
            command.contains("yes", ignoreCase = true) || command.contains("yes", ignoreCase = true) -> {
                speakOut("Ok, will do")
            }
            else -> {
                speakOut("fine")
            }
        }
    }

    private fun listenForVoiceCommand() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }


}