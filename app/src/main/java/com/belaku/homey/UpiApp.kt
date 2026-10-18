package com.belaku.homey

import android.graphics.drawable.Drawable

data class UpiApp(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val offersCount: Int
)
