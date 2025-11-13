package com.belaku.homey

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint


object CharacterToBitmapConverter {
    fun getBitmapFromCharacter(
        character: Char,
        width: Int,
        height: Int,
        textSize: Int,
        textColor: Int
    ): Bitmap {
        // Create a mutable bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Create a canvas to draw on the bitmap
        val canvas = Canvas(bitmap)
        // Create a paint object for styling the text
        val paint = Paint()
        paint.color = textColor
        paint.textSize = textSize.toFloat()
        paint.isAntiAlias = true

        // Optionally, set a custom font
        // paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        // Calculate text position for centering (optional)
        val x = (width - paint.measureText(character.toString())) / 2
        val y = (height - paint.descent() - paint.ascent()) / 2

        // Draw the character
        canvas.drawText(character.toString(), x, y, paint)

        return bitmap
    }
}