package com.belaku.homey

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.belaku.homey.NewAppWidget.Companion.primaryColor
import com.belaku.homey.NewAppWidget.Companion.secondaryColor
import com.belaku.homey.NewAppWidget.Companion.tertianaryColor
import kotlin.math.min
import kotlin.math.roundToInt

class ColorUtil {

    fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(
            color
        ) + 0.114 * Color.blue(color)) / 255
        return if (darkness < 0.5) {
            false // It's a light color
        } else {
            true // It's a dark color
        }
    }


    fun matchPrimaryColor(): Int {
        return if (isColorDark(primaryColor)) lightenColor(primaryColor, 0.3f)
        else darkenColor(primaryColor, 3f)
    }

    fun matchSecondaryColor(): Int {
        return if (isColorDark(secondaryColor)) lightenColor(secondaryColor, 0.3f)
        else darkenColor(secondaryColor, 3f)
    }

    fun matchTertianaryColor(): Int {
        return if (isColorDark(tertianaryColor)) lightenColor(tertianaryColor, 0.3f)
        else darkenColor(tertianaryColor, 3f)
    }


    fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).roundToInt()
        val g = (Color.green(color) * factor).roundToInt()
        val b = (Color.blue(color) * factor).roundToInt()
        return Color.argb(a, min(r, 255), min(g, 255), min(b, 255))
    }

    fun lightenColor(color: Int, factor: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] =
            min(1.0, (hsl[2] + factor).toDouble()).toFloat() // Increase lightness, cap at 1.0
        return ColorUtils.HSLToColor(hsl)
    }

}
