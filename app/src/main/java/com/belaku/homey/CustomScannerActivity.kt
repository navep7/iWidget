package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class CustomScannerActivity : AppCompatActivity() {
    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private lateinit var edtxPayingFor: EditText
    private lateinit var edtxAmount: EditText
    private lateinit var btnHistory: ImageButton
    
    private lateinit var historyContainer: FrameLayout
    private lateinit var lvPaymentHistory: ListView
    private lateinit var btnCloseHistory: Button
    
    private var lastScannedUpiUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prevent keyboard from showing automatically to avoid obstructing the camera
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        
        setContentView(R.layout.activity_custom_scanner)

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)
        edtxPayingFor = findViewById(R.id.edtx_paying_for)
        edtxAmount = findViewById(R.id.edtx_amount)
        btnHistory = findViewById(R.id.btn_history)
        
        historyContainer = findViewById(R.id.history_container)
        lvPaymentHistory = findViewById(R.id.lv_payment_history)
        btnCloseHistory = findViewById(R.id.btn_close_history)

        btnHistory.setOnClickListener {
            showHistory()
        }
        
        btnCloseHistory.setOnClickListener {
            historyContainer.visibility = View.GONE
            barcodeScannerView.resume()
        }

        // Handle the "Done" action on the keyboard when the user manually enters the amount
        edtxAmount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val amount = edtxAmount.text.toString().trim()
                val note = edtxPayingFor.text.toString().trim()
                if (amount.isNotEmpty() && lastScannedUpiUri != null) {
                    launchUpi(lastScannedUpiUri!!, amount, note)
                    true
                } else {
                    if (amount.isEmpty() && lastScannedUpiUri != null) {
                        edtxAmount.error = "Please enter amount"
                    }
                    false
                }
            } else {
                false
            }
        }
        
        // Improve scanning performance for dense QR codes like PhonePe
        barcodeScannerView.barcodeView.cameraSettings.apply {
            isContinuousFocusEnabled = true
            isAutoFocusEnabled = true
        }
        
        barcodeScannerView.setStatusText("Scan UPI QR code")
        
        capture = object : CaptureManager(this, barcodeScannerView) {
            override fun returnResult(rawResult: BarcodeResult?) {
                val scannedText = rawResult?.text
                if (scannedText != null && scannedText.startsWith("upi://pay", ignoreCase = true)) {
                    val amountStr = edtxAmount.text.toString().trim()
                    val noteStr = edtxPayingFor.text.toString().trim()
                    val upiUri = Uri.parse(scannedText)
                    val uriAmount = upiUri.getQueryParameter("am")

                    // If amount is not in QR and not entered in EditText, prompt user
                    if (amountStr.isEmpty() && uriAmount == null) {
                        lastScannedUpiUri = scannedText
                        runOnUiThread {
                            edtxPayingFor.requestFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showSoftInput(edtxPayingFor, InputMethodManager.SHOW_IMPLICIT)
                            Toast.makeText(this@CustomScannerActivity, "Please enter details to proceed", Toast.LENGTH_SHORT).show()
                        }
                        // Pause scanner to allow typing without re-triggering scans
                        barcodeScannerView.barcodeView.pause()
                        return
                    }

                    launchUpi(scannedText, amountStr, noteStr)
                    return
                }

                // Default result handling for non-UPI or if it falls through
                val intent = resultIntent(rawResult, null)
                val amount = edtxAmount.text.toString().trim()
                val note = edtxPayingFor.text.toString().trim()
                if (amount.isNotEmpty()) {
                    intent.putExtra("SCAN_AMOUNT", amount)
                }
                if (note.isNotEmpty()) {
                    intent.putExtra("SCAN_NOTE", note)
                }
                this@CustomScannerActivity.setResult(RESULT_OK, intent)
                this@CustomScannerActivity.finish()
            }
        }

        capture.initializeFromIntent(intent, savedInstanceState)

        // Enhance decoder hints to ensure dense QRs (like PhonePe) and inverted QRs are captured reliably.
        val hints = mutableMapOf<DecodeHintType, Any>()
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
        
        // scanMode = 2 enables both normal and inverted QR code detection
        barcodeScannerView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE), hints, "UTF-8", 2)

        capture.decode()
    }

    private fun showHistory() {
        val sampleHistory = listOf(
            PaymentHistory("500", "Grocery Store", "2023-10-25 10:30"),
            PaymentHistory("120", "Coffee Shop", "2023-10-24 15:45"),
            PaymentHistory("2500", "Rent Payment", "2023-10-01 09:00"),
            PaymentHistory("450", "Fuel Station", "2023-09-28 18:20"),
            PaymentHistory("99", "Mobile Recharge", "2023-09-25 12:10")
        )
        
        val adapter = PaymentHistoryAdapter(this, sampleHistory)
        lvPaymentHistory.adapter = adapter
        
        historyContainer.visibility = View.VISIBLE
        barcodeScannerView.pause()
    }

    private fun launchUpi(scannedText: String, amountStr: String, noteStr: String = "") {
        val upiUri = Uri.parse(scannedText)
        val builder = upiUri.buildUpon()
        
        // Fix for "UPI Risk Policy" error:
        // Avoid duplicate 'am' (amount) parameters which trigger security flags in UPI apps.
        if (amountStr.isNotEmpty() && upiUri.getQueryParameter("am") == null) {
            builder.appendQueryParameter("am", amountStr)
        }
        
        // Append transaction note if provided and not already present in QR
        if (noteStr.isNotEmpty() && upiUri.getQueryParameter("tn") == null) {
            builder.appendQueryParameter("tn", noteStr)
        }
        
        val finalUri = builder.build()
        
        val upiIntent = Intent(Intent.ACTION_VIEW, finalUri)
        // Use flags to ensure the intent is launched cleanly. 
        // This helps bypass overlay/risk detection in banking apps by starting a fresh task.
        upiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        try {
            startActivity(upiIntent)
            // Finish immediately to ensure this activity isn't seen as an overlay.
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (historyContainer.visibility != View.VISIBLE) {
            capture.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && historyContainer.visibility == View.VISIBLE) {
            historyContainer.visibility = View.GONE
            barcodeScannerView.resume()
            return true
        }
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}
