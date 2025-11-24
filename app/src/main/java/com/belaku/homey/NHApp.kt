package com.belaku.homey

// MyApplication.kt
import android.app.Application
import com.google.android.material.color.DynamicColors

class NHApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}