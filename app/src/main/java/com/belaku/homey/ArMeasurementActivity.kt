package com.belaku.homey

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
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
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import java.util.Locale
import kotlin.math.hypot

class ArMeasurementActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var measurementOverlay: MeasurementOverlayView
    private lateinit var btnBack: ImageButton
    private lateinit var btnReset: Button
    private lateinit var tvInstructions: TextView
    private lateinit var tvDistanceResult: TextView

    private var arSession: Session? = null
    private var anchor1: Anchor? = null
    private var anchor2: Anchor? = null
    private var latestFocusDiopters: Float = 0f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraAndArSession()
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

                val session = arSession
                if (session != null) {
                    try {
                        val frame = session.update()
                        val hitResults = frame.hitTest(x, y)
                        val hitResult = hitResults.firstOrNull {
                            val trackable = it.trackable
                            trackable is Plane && trackable.isPoseInPolygon(it.hitPose)
                        } ?: hitResults.firstOrNull()

                        if (hitResult != null) {
                            val anchor = hitResult.createAnchor()
                            if (anchor1 == null) {
                                anchor1 = anchor
                                measurementOverlay.point1 = PointF(x, y)
                                tvInstructions.text = "Tap second point"
                                measurementOverlay.invalidate()
                            } else if (anchor2 == null) {
                                anchor2 = anchor
                                measurementOverlay.point2 = PointF(x, y)
                                tvInstructions.text = "Measurement complete. Tap Reset to start over."
                                measurementOverlay.invalidate()
                                recalculateMeasurement()
                            } else {
                                resetMeasurement()
                                anchor1 = anchor
                                measurementOverlay.point1 = PointF(x, y)
                                tvInstructions.text = "Tap second point"
                                measurementOverlay.invalidate()
                            }
                        } else {
                            fallbackPixelMeasurement(x, y)
                        }
                    } catch (e: Exception) {
                        fallbackPixelMeasurement(x, y)
                    }
                } else {
                    fallbackPixelMeasurement(x, y)
                }
                true
            } else {
                false
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraAndArSession()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun fallbackPixelMeasurement(x: Float, y: Float) {
        if (measurementOverlay.point1 == null) {
            measurementOverlay.point1 = PointF(x, y)
            tvInstructions.text = "Tap second point"
            measurementOverlay.invalidate()
        } else if (measurementOverlay.point2 == null) {
            measurementOverlay.point2 = PointF(x, y)
            tvInstructions.text = "Measurement complete."
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

            val cm = if (anchor1 != null && anchor2 != null) {
                val pA = anchor1!!.pose
                val pB = anchor2!!.pose
                val dx = (pB.tx() - pA.tx()).toDouble()
                val dy = (pB.ty() - pA.ty()).toDouble()
                val dz = (pB.tz() - pA.tz()).toDouble()
                val meters = hypot(hypot(dx, dy), dz).toFloat()
                meters * 100f
            } else {
                val distanceMeters = if (latestFocusDiopters > 0f) 1.0f / latestFocusDiopters else 0.25f
                val viewWidthMeters = 2.0f * distanceMeters * 0.57735f
                val widthPx = if (viewFinder.width > 0) viewFinder.width.toFloat() else 1080f
                val cmPerPixel = (viewWidthMeters * 100.0f) / widthPx
                pixelDist * cmPerPixel
            }

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
        anchor1?.detach()
        anchor2?.detach()
        anchor1 = null
        anchor2 = null
        measurementOverlay.clearPoints()
        tvInstructions.text = "Move phone slowly to detect surface, then tap two points"
        tvDistanceResult.text = "Distance: --"
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun startCameraAndArSession() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val previewBuilder = Preview.Builder()
            val extender = Camera2Interop.Extender(previewBuilder)
            extender.setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val focusDist = result.get(android.hardware.camera2.CaptureResult.LENS_FOCUS_DISTANCE)
                    if (focusDist != null && focusDist > 0f) {
                        latestFocusDiopters = focusDist
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

        try {
            if (arSession == null) {
                arSession = Session(this)
                val config = Config(arSession).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                arSession?.configure(config)
            }
            arSession?.resume()
        } catch (e: Exception) {
            arSession = null
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            arSession?.resume()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
    }
}
