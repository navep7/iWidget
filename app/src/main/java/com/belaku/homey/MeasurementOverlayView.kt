package com.belaku.homey

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.hypot

class MeasurementOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var point1: PointF? = null
    var point2: PointF? = null
    var focusDiopters: Float = 0f
    var viewWidthPx: Int = 1080

    private val paintPoint = Paint().apply {
        color = 0xFF00FF66.toInt() // Vibrant green
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintLine = Paint().apply {
        color = 0xFF00FF66.toInt()
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val paintTextBg = Paint().apply {
        color = 0xAA000000.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    fun clearPoints() {
        point1 = null
        point2 = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val p1 = point1
        val p2 = point2

        if (p1 != null) {
            canvas.drawCircle(p1.x, p1.y, 20f, paintPoint)
        }

        if (p2 != null) {
            canvas.drawCircle(p2.x, p2.y, 20f, paintPoint)
        }

        if (p1 != null && p2 != null) {
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintLine)

            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val pixelDistance = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            val distanceMeters = if (focusDiopters > 0f) 1.0f / focusDiopters else 0.5f
            val viewWidthMeters = 2.0f * distanceMeters * 0.57735f // tan(30 deg)
            val actualViewWidthPx = if (viewWidthPx > 0) viewWidthPx else width
            val cmPerPixel = if (actualViewWidthPx > 0) (viewWidthMeters * 100.0f) / actualViewWidthPx else 0.15f
            val estimatedCm = pixelDistance * cmPerPixel

            val text = if (estimatedCm >= 30.48f) {
                val totalInches = estimatedCm / 2.54f
                val feet = (totalInches / 12).toInt()
                val inches = (totalInches % 12)
                String.format(Locale.getDefault(), "%d ft %.0f in", feet, inches)
            } else {
                String.format(Locale.getDefault(), "%.1f cm", estimatedCm)
            }

            val midX = (p1.x + p2.x) / 2f
            val midY = (p1.y + p2.y) / 2f - 30f

            val textWidth = paintText.measureText(text)
            canvas.drawRect(midX - textWidth / 2f - 20f, midY - 45f, midX + textWidth / 2f + 20f, midY + 15f, paintTextBg)
            canvas.drawText(text, midX, midY, paintText)
        }
    }
}
