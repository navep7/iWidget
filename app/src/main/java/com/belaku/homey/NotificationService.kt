package com.belaku.homey

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.belaku.homey.MainActivity.Companion.makeToast
import java.util.Locale

class NotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val speechQueue = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        Log.d("NoteServiceLOG", "onCreate")
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            tts = TextToSpeech(applicationContext, this, "com.google.android.tts")
        } catch (e: Exception) {
            try {
                tts = TextToSpeech(applicationContext, this)
            } catch (e2: Exception) {
                Log.e("NoteServiceLOG", "Failed to initialize TTS", e2)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("NoteServiceLOG", "Language is not supported")
            } else {
                isTtsReady = true
                synchronized(speechQueue) {
                    if (speechQueue.isNotEmpty()) {
                        for (msg in speechQueue) {
                            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "notificationUtterance")
                        }
                        speechQueue.clear()
                    }
                }
            }
        } else {
            Log.e("NoteServiceLOG", "TTS Initialization Failed!")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NoteServiceLOG", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NoteServiceLOG", "onListenerDisconnected")

        val componentName = ComponentName(this, NotificationService::class.java)
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = enabledListeners?.contains(componentName.flattenToString()) == true

        if (isEnabled) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("NoteServiceLOG", "Requesting rebind...")
                    requestRebind(componentName)
                } catch (e: Exception) {
                    Log.e("NoteServiceLOG", "Failed to request rebind", e)
                }
            }, 5000)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        processNotification(sbn)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (sbn == null) return
        processNotification(sbn)
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        Log.d("NoteServiceLOG", "onNotificationPosted received from package: $packageName")

        // Filter out our own app's notifications to prevent infinite speech/processing loops
        if (packageName == applicationContext.packageName) return

        // Toasts must be shown on the Main Thread (UI Looper) to avoid crashing the background thread
        Handler(Looper.getMainLooper()).post {
            try {
                makeToast(applicationContext, "Notification from: $packageName")
            } catch (e: Exception) {
                Log.e("NoteServiceLOG", "Error showing toast", e)
            }
        }

        // Extract app name
        var appName: String
        try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            appName = packageManager.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            appName = "Unknown"
        }

        // Initialize sharedPreferences context safely
        val prefs = applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)

        if (prefs.getBoolean("SPKSERVICE", false)) {
            synchronized(speechQueue) {
                if (isTtsReady) {
                    tts?.speak(appName, TextToSpeech.QUEUE_FLUSH, null, "notificationUtterance")
                } else {
                    speechQueue.add(appName)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional override for completeness
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        // Optional override for completeness
    }

    private fun setupSpeechRecognizer() {
        val serviceComponent = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.voicesearch.service.SpeechRecognitionService")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext, serviceComponent)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
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
        synchronized(speechQueue) {
            val reply = if (command.contains("yes", ignoreCase = true)) "Ok, will do" else "fine"
            if (isTtsReady) {
                tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "notificationUtterance")
            } else {
                speechQueue.add(reply)
            }
        }
    }

    private fun listenForVoiceCommand() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("NoteServiceLOG", "Error during TTS shutdown", e)
        }
        tts = null
        isTtsReady = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}
