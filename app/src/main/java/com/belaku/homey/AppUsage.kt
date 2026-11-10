package com.belaku.homey

data class AppUsage(
    val appName: String,
    val usageTime: String,
    val packageName: String? = null
)
