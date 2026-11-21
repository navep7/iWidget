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
    var measuredCm: Float = 0f

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
        measuredCm = 0f
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

            val cm = measuredCm
            val text = if (cm >= 30.48f) {
                val totalInches = cm / 2.54f
                val feet = (totalInches / 12).toInt()
                val inches = (totalInches % 12)
                String.format(Locale.getDefault(), "%d ft %.0f in", feet, inches)
            } else {
                String.format(Locale.getDefault(), "%.1f cm", cm)
            }

            val midX = (p1.x + p2.x) / 2f
            val midY = (p1.y + p2.y) / 2f - 30f

            val textWidth = paintText.measureText(text)
            canvas.drawRect(midX - textWidth / 2f - 20f, midY - 45f, midX + textWidth / 2f + 20f, midY + 15f, paintTextBg)
            canvas.drawText(text, midX, midY, paintText)
        }
    }
}
