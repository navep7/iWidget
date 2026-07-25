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
import android.util.Log
import com.belaku.homey.SpeakService.Companion.speakOut


class NotificationService : NotificationListenerService() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isConnected = false

    override fun onCreate() {
        super.onCreate()
        Log.d("NoteServiceLOG", "onCreate")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d("NoteServiceLOG", "onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d("NoteServiceLOG", "onListenerDisconnected")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val componentName = ComponentName(this, NotificationService::class.java)
            val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            val isEnabled = enabledListeners?.contains(componentName.flattenToString()) == true

            if (isEnabled) {
                // Delay rebind to avoid "Service not registered" IllegalArgumentException
                // which happens if we request rebind while the system is still unbinding.
                // Increasing delay to 5s to give the system more time to clean up.
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isConnected) {
                        try {
                            Log.d("NoteServiceLOG", "Requesting rebind...")
                            requestRebind(componentName)
                        } catch (e: Exception) {
                            Log.e("NoteServiceLOG", "Failed to request rebind", e)
                        }
                    }
                }, 5000)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isConnected) return
        
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
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
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
        when {
            command.contains("yes", ignoreCase = true) -> {
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
        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }
}