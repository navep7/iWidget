package com.belaku.homey

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

class App(
    var name: String,
    var pName: String,
    var usage: String,
    var iconBitmap: Bitmap




) {
    override fun toString(): String {
        return "App(name='$name', pName='$pName', usage='$usage', iconBitmap=$iconBitmap)"
    }
}