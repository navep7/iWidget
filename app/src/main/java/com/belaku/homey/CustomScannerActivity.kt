package com.belaku.homey

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var pendingAmount: String? = null
    private var pendingNote: String? = null

    companion object {
        private const val UPI_PAYMENT_REQUEST_CODE = 1001
    }

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
            edtxPayingFor.visibility = View.VISIBLE
            edtxAmount.visibility = View.VISIBLE
            btnHistory.visibility = View.VISIBLE
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
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("payment_history", null)
        val type = object : TypeToken<ArrayList<PaymentHistory>>() {}.type
        val history: ArrayList<PaymentHistory> = if (json == null) {
            ArrayList()
        } else {
            gson.fromJson(json, type)
        }
        
        val adapter = PaymentHistoryAdapter(this, history)
        lvPaymentHistory.adapter = adapter
        
        historyContainer.visibility = View.VISIBLE
        edtxPayingFor.visibility = View.GONE
        edtxAmount.visibility = View.GONE
        btnHistory.visibility = View.GONE
        barcodeScannerView.pause()
    }

    private fun savePaymentToHistory(amount: String, note: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("payment_history", null)
        val type = object : TypeToken<ArrayList<PaymentHistory>>() {}.type
        val historyList: ArrayList<PaymentHistory> = if (json == null) {
            ArrayList()
        } else {
            gson.fromJson(json, type)
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val finalNote = if (note.isEmpty()) "QR Payment" else note
        val newPayment = PaymentHistory(amount, finalNote, timestamp)
        historyList.add(0, newPayment)

        // Keep only last 50 items
        val truncatedList = if (historyList.size > 50) ArrayList(historyList.subList(0, 50)) else historyList

        sharedPreferences.edit().putString("payment_history", gson.toJson(truncatedList)).apply()
    }

    private fun launchUpi(scannedText: String, amountStr: String, noteStr: String = "") {
        val upiUri = Uri.parse(scannedText)
        val builder = upiUri.buildUpon()
        
        var finalAmount = amountStr
        var finalNote = noteStr

        // Fix for "UPI Risk Policy" error:
        // Avoid duplicate 'am' (amount) parameters which trigger security flags in UPI apps.
        val uriAmount = upiUri.getQueryParameter("am")
        if (amountStr.isNotEmpty() && uriAmount == null) {
            builder.appendQueryParameter("am", amountStr)
        } else if (amountStr.isEmpty() && uriAmount != null) {
            finalAmount = uriAmount
        }
        
        // Append transaction note if provided and not already present in QR
        val uriNote = upiUri.getQueryParameter("tn")
        if (noteStr.isNotEmpty() && uriNote == null) {
            builder.appendQueryParameter("tn", noteStr)
        } else if (noteStr.isEmpty() && uriNote != null) {
            finalNote = uriNote
        }
        
        if (finalAmount.isEmpty()) finalAmount = "0"
        
        // Store details to save to history ONLY if payment succeeds
        pendingAmount = finalAmount
        pendingNote = finalNote
        
        val finalUri = builder.build()
        
        val upiApps = getInstalledUpiApps(this)
        if (upiApps.isEmpty()) {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showUpiAppChooser(this, upiApps, finalUri)
    }

    private fun getInstalledUpiApps(context: Context): List<UpiApp> {
        val upiIntent = Intent(Intent.ACTION_VIEW)
        upiIntent.data = Uri.parse("upi://pay")
        val pm = context.packageManager
        val resolveInfoList = pm.queryIntentActivities(upiIntent, PackageManager.MATCH_DEFAULT_ONLY)
        
        val upiApps = mutableListOf<UpiApp>()
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            
            // Mocking offer counts for demonstration. 
            // In a real app, this could come from a server or local database.
            val mockOffers = when (packageName) {
                "com.google.android.apps.nbu.paisa.user" -> 5
                "com.phonepe.app" -> 3
                "net.one97.paytm" -> 7
                "in.org.npci.upiapp" -> 2
                else -> (1..4).random()
            }
            
            upiApps.add(UpiApp(name, packageName, icon, mockOffers))
        }
        return upiApps
    }

    private fun showUpiAppChooser(context: Context, apps: List<UpiApp>, uri: Uri) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Choose UPI App to pay")
        
        val adapter = UpiAppAdapter(context, apps)
        builder.setAdapter(adapter) { _, which ->
            val selectedApp = apps[which]
            val upiIntent = Intent(Intent.ACTION_VIEW, uri)
            upiIntent.`package` = selectedApp.packageName
            try {
                barcodeScannerView.pause()
                startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(this, "Error launching ${selectedApp.name}", Toast.LENGTH_SHORT).show()
                barcodeScannerView.resume()
            }
        }
        
        builder.setOnCancelListener {
            barcodeScannerView.resume()
        }
        
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            val response = data?.getStringExtra("response") ?: ""
            if (isPaymentSuccessful(response)) {
                savePaymentToHistory(pendingAmount ?: "0", pendingNote ?: "QR Payment")
                Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_SHORT).show()
            }
            // Always finish as we are done with the scan/payment attempt
            finish()
        }
    }

    private fun isPaymentSuccessful(response: String): Boolean {
        if (response.isBlank()) return false
        val parts = response.split("&")
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size == 2 && kv[0].equals("Status", ignoreCase = true)) {
                return kv[1].equals("SUCCESS", ignoreCase = true)
            }
        }
        return false
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
            edtxPayingFor.visibility = View.VISIBLE
            edtxAmount.visibility = View.VISIBLE
            btnHistory.visibility = View.VISIBLE
            barcodeScannerView.resume()
            return true
        }
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}
