package com.belaku.homey

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import java.util.Locale

class NotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val speechQueue = mutableListOf<String>()
    private var usingGoogleTtsEngine = true
    private var audioManager: AudioManager? = null

    private var lastProcessedKey: String? = null
    private var lastProcessedTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("NoteServiceLOG", "onCreate")
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            usingGoogleTtsEngine = true
            tts = TextToSpeech(applicationContext, this, "com.google.android.tts")
        } catch (_: Exception) {
            fallbackToDefaultTTS()
        }
    }

    private fun fallbackToDefaultTTS() {
        try {
            Log.w("NoteServiceLOG", "Falling back to default TTS engine")
            usingGoogleTtsEngine = false
            tts?.shutdown()
            tts = TextToSpeech(applicationContext, this)
        } catch (e2: Exception) {
            Log.e("NoteServiceLOG", "Failed to initialize default TTS", e2)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("NoteServiceLOG", "Language is not supported")
            } else {
                Log.d("NoteServiceLOG", "TTS initialized successfully")
                setupTtsAudioAttributes()
                isTtsReady = true
                synchronized(speechQueue) {
                    if (speechQueue.isNotEmpty()) {
                        for (msg in speechQueue) {
                            speakText(msg)
                        }
                        speechQueue.clear()
                    }
                }
            }
        } else {
            Log.e("NoteServiceLOG", "TTS Initialization Failed with status $status")
            if (usingGoogleTtsEngine) {
                fallbackToDefaultTTS()
            }
        }
    }

    private fun setupTtsAudioAttributes() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        } catch (e: Exception) {
            Log.e("NoteServiceLOG", "Error setting audio attributes", e)
        }
    }

    private fun speakText(text: String) {
        if (!isTtsReady || tts == null) {
            synchronized(speechQueue) {
                speechQueue.add(text)
            }
            Log.d("NoteServiceLOG", "TTS not ready yet, queued: $text")
            return
        }

        try {
            requestAudioFocus()

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            Log.d("NoteServiceLOG", "Speaking text loudly: $text")
            tts?.speak(text, try {
                TextToSpeech.QUEUE_FLUSH
            } catch (e: Exception) {
                TODO("Not yet implemented")
            }, params, "notificationUtterance_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("NoteServiceLOG", "Error during tts.speak", e)
        }
    }

    private fun requestAudioFocus() {
        try {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioManager?.requestAudioFocus(focusRequest)
        } catch (e: Exception) {
            Log.e("NoteServiceLOG", "Error requesting audio focus", e)
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

        // Deduplicate rapid duplicate callbacks for the same notification
        val notificationKey = "${packageName}_${sbn.id}_${sbn.postTime}"
        val currentTime = System.currentTimeMillis()
        if (notificationKey == lastProcessedKey && (currentTime - lastProcessedTime) < 1000) {
            Log.d("NoteServiceLOG", "Duplicate notification callback ignored for $notificationKey")
            return
        }
        lastProcessedKey = notificationKey
        lastProcessedTime = currentTime

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
                speakText(appName)
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
            speakText(reply)
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
