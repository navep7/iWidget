package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

data class TransactionItem(
    val title: String,
    val subtitle: String,
    val date: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("subtitle", subtitle)
            put("date", date)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): TransactionItem {
            return TransactionItem(
                json.optString("title"),
                json.optString("subtitle"),
                json.optString("date")
            )
        }
    }
}

sealed class TransactionRow {
    data class Header(val dateText: String) : TransactionRow()
    data class Transaction(val item: TransactionItem) : TransactionRow()
}

class TransactionAdapter(context: Context, rows: ArrayList<TransactionRow>) :
    ArrayAdapter<TransactionRow>(context, 0, rows) {

    fun updateRows(newRows: List<TransactionRow>) {
        clear()
        addAll(newRows)
        notifyDataSetChanged()
    }

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionRow.Header -> 0
            is TransactionRow.Transaction -> 1
            else -> 1
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = getItem(position)
        val inflater = LayoutInflater.from(context)

        return when (row) {
            is TransactionRow.Header -> {
                val view = convertView ?: inflater.inflate(R.layout.item_transaction_header, parent, false)
                val tvHeader = view.findViewById<TextView>(R.id.tv_item_header)
                tvHeader.text = row.dateText
                tvHeader.setTypeface(null, Typeface.BOLD)
                tvHeader.setTextColor(Color.BLACK)
                view
            }
            is TransactionRow.Transaction -> {
                val view = convertView ?: inflater.inflate(R.layout.item_transaction, parent, false)
                val item = row.item
                val tvTitle = view.findViewById<TextView>(R.id.tv_item_title)
                val tvSubtitle = view.findViewById<TextView>(R.id.tv_item_subtitle)
                val tvDate = view.findViewById<TextView>(R.id.tv_item_date)

                tvTitle.text = item.title
                tvTitle.setTypeface(null, Typeface.BOLD)
                tvTitle.setTextColor(Color.BLACK)

                tvSubtitle.text = item.subtitle
                tvSubtitle.setTypeface(null, Typeface.NORMAL)
                tvSubtitle.setTextColor(Color.BLACK)

                tvDate.text = item.date
                tvDate.setTypeface(null, Typeface.NORMAL)
                tvDate.setTextColor(Color.DKGRAY)
                view
            }
            null -> convertView ?: View(context)
        }
    }
}

class QRScannerActivity : AppCompatActivity() {

    private lateinit var tvScannedStatus: TextView
    private lateinit var etPayingFor: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnPayNow: Button
    private lateinit var lvTransactions: ListView

    private val transactionList = ArrayList<TransactionItem>()
    private lateinit var adapter: TransactionAdapter
    private var isScanning = false
    private var scannedUpiString: String? = null
    private var pendingTransactionRecord: TransactionItem? = null
    private var waitingForPaymentResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrscanner)

        tvScannedStatus = findViewById(R.id.tv_scanned_status)
        etPayingFor = findViewById(R.id.et_paying_for)
        etAmount = findViewById(R.id.et_amount)
        btnPayNow = findViewById(R.id.btn_pay_now)
        lvTransactions = findViewById(R.id.lv_transactions)

        adapter = TransactionAdapter(this, ArrayList())
        lvTransactions.adapter = adapter

        loadTransactions()

        btnPayNow.setOnClickListener {
            processUpiPayment()
        }

        etAmount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                if (btnPayNow.isEnabled) {
                    processUpiPayment()
                }
                true
            } else {
                false
            }
        }

        // Launch Google barcode scanner directly upon opening the screen
        startQRScanner()
    }

    override fun onResume() {
        super.onResume()
        if (waitingForPaymentResult) {
            waitingForPaymentResult = false
            showSuccessConfirmationDialog()
        }
    }

    private fun showSuccessConfirmationDialog() {
        val record = pendingTransactionRecord ?: return
        AlertDialog.Builder(this)
            .setTitle("Payment Confirmation")
            .setMessage("Was the payment completed successfully?")
            .setPositiveButton("Yes, Successful") { _, _ ->
                saveTransaction(record)
                pendingTransactionRecord = null
                Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No / Cancel") { _, _ ->
                pendingTransactionRecord = null
                Toast.makeText(this, "Transaction cancelled", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun startQRScanner() {
        if (isScanning) return
        isScanning = true

        val scanner = GmsBarcodeScanning.getClient(this)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                isScanning = false
                val rawValue = barcode.rawValue
                android.util.Log.d("QRScannerActivity", "Scanned UPI rawValue: $rawValue")
                if (rawValue != null && rawValue.startsWith("upi://pay")) {
                    scannedUpiString = rawValue
                    onQrScannedSuccessfully(rawValue)
                } else {
                    Toast.makeText(this, "Not a valid UPI QR Code", Toast.LENGTH_LONG).show()
                    tvScannedStatus.text = "Status: Invalid QR Code. Tap Scan or reopen."
                }
            }
            .addOnFailureListener { e ->
                isScanning = false
                android.util.Log.e("QRScannerActivity", "Scanning failed or cancelled", e)
                Toast.makeText(this, "Scanning failed or cancelled", Toast.LENGTH_SHORT).show()
                tvScannedStatus.text = "Status: Scan cancelled."
            }
    }

    private fun onQrScannedSuccessfully(upiString: String) {
        var payeeName = "Merchant"
        var payeeVpa = ""

        val queryIndex = upiString.indexOf('?')
        if (queryIndex != -1 && queryIndex < upiString.length - 1) {
            val queryString = upiString.substring(queryIndex + 1)
            val pairs = queryString.split("&")
            for (pair in pairs) {
                val eqIndex = pair.indexOf('=')
                if (eqIndex != -1) {
                    val key = pair.substring(0, eqIndex).trim()
                    val rawValue = pair.substring(eqIndex + 1)
                    val decodedValue = try {
                        java.net.URLDecoder.decode(rawValue, "UTF-8")
                    } catch (_: Exception) {
                        rawValue
                    }
                    if (key.equals("pa", ignoreCase = true)) payeeVpa = decodedValue
                    if (key.equals("pn", ignoreCase = true)) payeeName = decodedValue
                }
            }
        } else {
            val upiUri = Uri.parse(upiString)
            payeeVpa = upiUri.getQueryParameter("pa") ?: ""
            payeeName = upiUri.getQueryParameter("pn") ?: "Merchant"
        }

        tvScannedStatus.text = "Scanned: $payeeName ($payeeVpa)"
        btnPayNow.isEnabled = true

        // Focus et_paying_for and show soft keyboard
        etPayingFor.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etPayingFor, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun processUpiPayment() {
        val upiString = scannedUpiString ?: return
        val payingForText = etPayingFor.text.toString().trim()
        val amountText = etAmount.text.toString().trim()

        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")

        var payeeName = "Merchant"
        var payeeVpa = ""

        val queryIndex = upiString.indexOf('?')
        if (queryIndex != -1 && queryIndex < upiString.length - 1) {
            val queryString = upiString.substring(queryIndex + 1)
            val pairs = queryString.split("&")
            for (pair in pairs) {
                val eqIndex = pair.indexOf('=')
                if (eqIndex != -1) {
                    val key = pair.substring(0, eqIndex).trim()
                    val rawValue = pair.substring(eqIndex + 1)
                    val decodedValue = try {
                        java.net.URLDecoder.decode(rawValue, "UTF-8")
                    } catch (_: Exception) {
                        rawValue
                    }

                    if (key.equals("pa", ignoreCase = true)) payeeVpa = decodedValue
                    if (key.equals("pn", ignoreCase = true)) payeeName = decodedValue

                    // Exclude restrictive or user-override parameters
                    if (!key.equals("tr", ignoreCase = true) &&
                        !key.equals("mam", ignoreCase = true) &&
                        !key.equals("am", ignoreCase = true) &&
                        !key.equals("tn", ignoreCase = true)) {
                        builder.appendQueryParameter(key, decodedValue)
                    }
                }
            }
        } else {
            val upiUri = Uri.parse(upiString)
            payeeVpa = upiUri.getQueryParameter("pa") ?: ""
            payeeName = upiUri.getQueryParameter("pn") ?: "Merchant"
            for (paramName in upiUri.queryParameterNames) {
                if (!paramName.equals("tr", ignoreCase = true) &&
                    !paramName.equals("mam", ignoreCase = true) &&
                    !paramName.equals("am", ignoreCase = true) &&
                    !paramName.equals("tn", ignoreCase = true)) {
                    for (v in upiUri.getQueryParameters(paramName)) {
                        builder.appendQueryParameter(paramName, v)
                    }
                }
            }
        }

        // Apply user overrides for amount and paying for note if provided
        if (amountText.isNotEmpty()) {
            builder.appendQueryParameter("am", amountText)
        }
        if (payingForText.isNotEmpty()) {
            builder.appendQueryParameter("tn", payingForText)
        }

        val sanitizedUri = builder.build()
        android.util.Log.d("QRScannerActivity", "Final UPI URI: $sanitizedUri")

        // Prepare transaction record to be saved only upon user confirmation
        val displayAmount = if (amountText.isNotEmpty()) "₹$amountText" else "Custom Amount"
        val displayNote = if (payingForText.isNotEmpty()) payingForText else "UPI Payment"
        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val title = "To: $payeeName ($payeeVpa)"
        val subtitle = "Amount: $displayAmount | For: $displayNote"
        pendingTransactionRecord = TransactionItem(title, subtitle, timestamp)
        waitingForPaymentResult = true

        // Launch UPI payment app chooser
        val upiIntent = Intent(Intent.ACTION_VIEW, sanitizedUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(upiIntent, "Pay with...")
        if (upiIntent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            waitingForPaymentResult = false
            Toast.makeText(this, "No UPI apps found.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveTransaction(record: TransactionItem) {
        transactionList.add(0, record)
        refreshGroupedAdapter()

        val prefs = getSharedPreferences("upi_prefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in transactionList) {
            jsonArray.put(item.toJson())
        }
        prefs.edit().putString("transactions", jsonArray.toString()).apply()
    }

    private fun loadTransactions() {
        val prefs = getSharedPreferences("upi_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("transactions", null) ?: return
        try {
            val jsonArray = JSONArray(jsonStr)
            transactionList.clear()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                transactionList.add(TransactionItem.fromJson(jsonObj))
            }
            refreshGroupedAdapter()
        } catch (_: Exception) {
        }
    }

    private fun refreshGroupedAdapter() {
        val groupedRows = buildGroupedRows(transactionList)
        adapter.updateRows(groupedRows)
    }

    private fun buildGroupedRows(rawTransactions: List<TransactionItem>): List<TransactionRow> {
        val groupedMap = LinkedHashMap<String, MutableList<TransactionItem>>()
        val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        for (tx in rawTransactions) {
            val datePart = tx.date.substringBefore(",").trim()
            val key = if (datePart.equals(todayStr, ignoreCase = true)) "Today" else datePart

            if (!groupedMap.containsKey(key)) {
                groupedMap[key] = ArrayList()
            }
            groupedMap[key]?.add(tx)
        }

        val rows = ArrayList<TransactionRow>()
        for ((dateHeader, items) in groupedMap) {
            rows.add(TransactionRow.Header(dateHeader))
            for (item in items) {
                rows.add(TransactionRow.Transaction(item))
            }
        }
        return rows
    }
}
