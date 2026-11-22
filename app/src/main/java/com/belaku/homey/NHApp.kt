package com.belaku.homey

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class NHApp : Application() {
    companion object {
        lateinit var instance: NHApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            // Set a stable debug secret for App Check to prevent "Firebase app check token is invalid" errors.
            // Register this debug secret ("HomeyAppCheckDebugSecret2025") in the Firebase Console under App Check -> Apps -> Manage debug tokens.
            System.setProperty("com.google.firebase.appcheck.debug.debug_secret", "HomeyAppCheckDebugSecret2025")
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // For production environment
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
