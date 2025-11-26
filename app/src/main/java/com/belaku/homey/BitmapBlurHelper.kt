package com.belaku.homey

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur


object BitmapBlurHelper {
    private const val BITMAP_SCALE = 0.25f // Scale down the bitmap for faster processing
    private const val BLUR_RADIUS = 10f // Blur radius, max is 25f

    fun blurBitmap(context: Context?, originalBitmap: Bitmap): Bitmap {
        // Scale down the bitmap for faster processing
        val width = Math.round(originalBitmap.width * BITMAP_SCALE)
        val height = Math.round(originalBitmap.height * BITMAP_SCALE)
        val inputBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)

        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)

        tmpOut.copyTo(outputBitmap)

        // Release RenderScript resources
        inputBitmap.recycle() // Recycle the scaled down bitmap
        tmpIn.destroy()
        tmpOut.destroy()
        theIntrinsic.destroy()
        rs.destroy()

        return outputBitmap
    }
}