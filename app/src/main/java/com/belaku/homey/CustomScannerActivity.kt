package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomScannerActivity : AppCompatActivity() {
    private lateinit var llInputContainer: LinearLayout
    private lateinit var edtxPayingFor: EditText
    private lateinit var edtxAmount: EditText
    private lateinit var btnPay: Button
    private lateinit var btnHistory: ImageButton

    private lateinit var historyContainer: FrameLayout
    private lateinit var lvPaymentHistory: ListView
    private lateinit var btnCloseHistory: Button

    private var lastScannedUpiUri: String? = null
    private var pendingAmount: String = "0.00"
    private var pendingNote: String = "QR Payment"

    companion object {
        private const val UPI_REQUEST_CODE = 2026
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(R.layout.activity_custom_scanner)

        llInputContainer = findViewById(R.id.ll_input_container)
        edtxPayingFor = findViewById(R.id.edtx_paying_for)
        edtxAmount = findViewById(R.id.edtx_amount)
        btnPay = findViewById(R.id.btn_pay)
        btnHistory = findViewById(R.id.btn_history)

        historyContainer = findViewById(R.id.history_container)
        lvPaymentHistory = findViewById(R.id.lv_payment_history)
        btnCloseHistory = findViewById(R.id.btn_close_history)

        llInputContainer.visibility = View.GONE

        btnHistory.setOnClickListener { showHistory() }
        btnCloseHistory.setOnClickListener {
            historyContainer.visibility = View.GONE
            btnHistory.visibility = View.VISIBLE
            finish()
        }

        btnPay.setOnClickListener {
            val amountStr = edtxAmount.text.toString().trim()
            val noteStr = edtxPayingFor.text.toString().trim()
            val amountVal = amountStr.toDoubleOrNull()

            if (amountVal == null || amountVal <= 0.0) {
                edtxAmount.error = "Please enter a valid amount"
                return@setOnClickListener
            }

            if (lastScannedUpiUri != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(btnPay.windowToken, 0)

                llInputContainer.visibility = View.GONE
                launchUpiChooser(lastScannedUpiUri!!, amountStr, noteStr)
            }
        }

        // Start Google Code Scanner immediately on activity start
        startGoogleCodeScanner()
    }

    private fun startGoogleCodeScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedText = barcode.rawValue
                if (!scannedText.isNullOrEmpty() && scannedText.startsWith("upi://pay", ignoreCase = true)) {
                    lastScannedUpiUri = scannedText
                    val uri = Uri.parse(scannedText)
                    val uriAmount = uri.getQueryParameter("am")
                    val uriNote = uri.getQueryParameter("tn")

                    if (!uriAmount.isNullOrEmpty()) {
                        edtxAmount.setText(uriAmount)
                    }
                    if (!uriNote.isNullOrEmpty()) {
                        edtxPayingFor.setText(uriNote)
                    }

                    runOnUiThread {
                        llInputContainer.visibility = View.VISIBLE
                        edtxAmount.requestFocus()
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showSoftInput(edtxAmount, InputMethodManager.SHOW_IMPLICIT)
                        Toast.makeText(this, "Enter description & amount to proceed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Invalid UPI QR Code or scan cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun launchUpiChooser(scannedText: String, amountStr: String, noteStr: String) {
        val originalUri = Uri.parse(scannedText)
        val builder = originalUri.buildUpon()

        if (originalUri.getQueryParameter("am") == null && amountStr.isNotEmpty()) {
            builder.appendQueryParameter("am", String.format(Locale.US, "%.2f", amountStr.toDoubleOrNull() ?: 1.00))
        }
        if (originalUri.getQueryParameter("tn") == null && noteStr.isNotEmpty()) {
            builder.appendQueryParameter("tn", noteStr)
        }
        if (originalUri.getQueryParameter("cu") == null) {
            builder.appendQueryParameter("cu", "INR")
        }

        val finalUri = builder.build()
        pendingAmount = amountStr.ifEmpty { originalUri.getQueryParameter("am") ?: "1.00" }
        pendingNote = noteStr.ifEmpty { originalUri.getQueryParameter("tn") ?: "QR Payment" }

        val upiIntent = Intent(Intent.ACTION_VIEW, finalUri)
        val chooser = Intent.createChooser(upiIntent, "Pay using").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivityForResult(chooser, UPI_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "No UPI payment app found on device", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPI_REQUEST_CODE) {
            savePaymentHistory(pendingAmount, pendingNote)
            Toast.makeText(this, "Transaction completed & added to history", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showHistory() {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("payment_history", null)
        val type = object : TypeToken<ArrayList<PaymentHistory>>() {}.type
        val history: ArrayList<PaymentHistory> = json?.let { gson.fromJson(it, type) } ?: ArrayList()

        lvPaymentHistory.adapter = PaymentHistoryAdapter(this, history)
        historyContainer.visibility = View.VISIBLE
        llInputContainer.visibility = View.GONE
        btnHistory.visibility = View.GONE
    }

    private fun savePaymentHistory(amount: String, note: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("payment_history", null)
        val type = object : TypeToken<ArrayList<PaymentHistory>>() {}.type
        val historyList: ArrayList<PaymentHistory> = json?.let { gson.fromJson(it, type) } ?: ArrayList()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        historyList.add(0, PaymentHistory(amount, if (note.isEmpty()) "QR Payment" else note, timestamp))
        val truncatedList = if (historyList.size > 50) ArrayList(historyList.subList(0, 50)) else historyList
        sharedPreferences.edit().putString("payment_history", gson.toJson(truncatedList)).apply()
    }
}
