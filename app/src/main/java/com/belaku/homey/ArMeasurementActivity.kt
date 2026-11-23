package com.belaku.homey

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.hypot

class ArMeasurementActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var measurementOverlay: MeasurementOverlayView
    private lateinit var btnBack: ImageButton
    private lateinit var btnReset: Button
    private lateinit var tvInstructions: TextView
    private lateinit var tvDistanceResult: TextView

    private var latestFocusDiopters: Float = 0f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraSession()
        } else {
            Toast.makeText(this, "Camera permission is required for AR Measurement", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_measurement)

        viewFinder = findViewById(R.id.viewFinder)
        measurementOverlay = findViewById(R.id.measurementOverlay)
        btnBack = findViewById(R.id.btnBack)
        btnReset = findViewById(R.id.btnReset)
        tvInstructions = findViewById(R.id.tvInstructions)
        tvDistanceResult = findViewById(R.id.tvDistanceResult)

        btnBack.setOnClickListener { finish() }
        btnReset.setOnClickListener {
            resetMeasurement()
        }

        measurementOverlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y
                performMeasurementPoint(x, y)
                true
            } else {
                false
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraSession()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun performMeasurementPoint(x: Float, y: Float) {
        if (measurementOverlay.point1 == null) {
            measurementOverlay.point1 = PointF(x, y)
            tvInstructions.text = "Tap second point"
            measurementOverlay.invalidate()
        } else if (measurementOverlay.point2 == null) {
            measurementOverlay.point2 = PointF(x, y)
            tvInstructions.text = "Measurement complete. Tap Reset to start over."
            measurementOverlay.invalidate()
            recalculateMeasurement()
        } else {
            resetMeasurement()
            measurementOverlay.point1 = PointF(x, y)
            tvInstructions.text = "Tap second point"
            measurementOverlay.invalidate()
        }
    }

    private fun recalculateMeasurement() {
        val p1 = measurementOverlay.point1
        val p2 = measurementOverlay.point2
        if (p1 != null && p2 != null) {
            val pixelDist = hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()

            // Improved distance estimation with clamping and realistic default (0.8m / 80cm)
            val distanceMeters = if (latestFocusDiopters > 0f) {
                val d = 1.0f / latestFocusDiopters
                d.coerceIn(0.2f, 5.0f)
            } else {
                0.8f
            }

            // Using ~70 degree typical phone camera horizontal FOV factor (0.7f)
            val viewWidthMeters = 2.0f * distanceMeters * 0.70f
            val widthPx = if (viewFinder.width > 0) viewFinder.width.toFloat() else 1080f
            val cmPerPixel = (viewWidthMeters * 100.0f) / widthPx
            val cm = pixelDist * cmPerPixel

            measurementOverlay.measuredCm = cm

            val distanceText = if (cm >= 30.48f) {
                val totalInches = cm / 2.54f
                val feet = (totalInches / 12).toInt()
                val inches = (totalInches % 12)
                if (inches >= 0.5f) {
                    String.format(Locale.getDefault(), "%d ft %.1f in (%.1f cm / %.2f m)", feet, inches, cm, cm / 100f)
                } else {
                    String.format(Locale.getDefault(), "%d ft (%.1f cm / %.2f m)", feet, cm, cm / 100f)
                }
            } else {
                String.format(Locale.getDefault(), "%.1f cm (%.2f m)", cm, cm / 100f)
            }
            tvDistanceResult.text = "Distance: $distanceText"
            measurementOverlay.invalidate()
        }
    }

    private fun resetMeasurement() {
        measurementOverlay.clearPoints()
        tvInstructions.text = "Tap two points on screen to measure distance"
        tvDistanceResult.text = "Distance: --"
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun startCameraSession() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to initialize camera: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addListener
            }

            val previewBuilder = Preview.Builder()
            val camera2Extender = Camera2Interop.Extender(previewBuilder)
            camera2Extender.setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val focusDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                    if (focusDistance != null && focusDistance > 0f) {
                        latestFocusDiopters = focusDistance
                    }
                }
            })

            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera preview failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}
