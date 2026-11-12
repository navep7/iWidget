package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UpiTransaction(
    val id: String = UUID.randomUUID().toString(),
    val payeeName: String,
    val payeeVpa: String,
    val amount: String,
    val description: String = "",
    val place: String = "",
    val txnId: String,
    val status: String = "SUCCESS",
    val date: String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
    val timestamp: Long = System.currentTimeMillis()
)

class ScanAndPayActivity : AppCompatActivity() {

    private lateinit var btnScanQr: Button
    private lateinit var lvTransactions: ListView
    private lateinit var tvEmptyHistory: View
    private lateinit var tvTxnCount: TextView

    private val transactionList = mutableListOf<UpiTransaction>()
    private lateinit var adapter: TransactionsAdapter

    private var pendingPayeeName: String = ""
    private var pendingPayeeVpa: String = ""
    private var pendingAmount: String = ""
    private var pendingDescription: String = ""
    private var pendingTxnRef: String = ""

    private val upiPaymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleUpiPaymentResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_and_pay)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnScanQr = findViewById(R.id.btnScanQr)
        lvTransactions = findViewById(R.id.lvTransactions)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        tvTxnCount = findViewById(R.id.tvTxnCount)

        adapter = TransactionsAdapter(this, transactionList)
        lvTransactions.adapter = adapter

        loadSavedTransactions()

        btnScanQr.setOnClickListener {
            startQrScanner()
        }

        // Auto-start scanner if launched fresh or from widget intent
        if (savedInstanceState == null) {
            startQrScanner()
        }
    }

    private fun startQrScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrEmpty() && rawValue.startsWith("upi://pay", ignoreCase = true)) {
                    processUpiQr(rawValue)
                } else {
                    Toast.makeText(this, "Not a valid UPI QR code", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCanceledListener {
                // User canceled scanner - stays on history screen
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Scanner error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processUpiQr(upiUriString: String) {
        val uri = Uri.parse(upiUriString)
        val payeeVpa = uri.getQueryParameter("pa") ?: ""
        val payeeName = uri.getQueryParameter("pn") ?: "UPI User"
        val merchantCode = uri.getQueryParameter("mc")
        val prefilledAmount = uri.getQueryParameter("am") ?: uri.getQueryParameter("mam") ?: ""
        val prefilledNote = uri.getQueryParameter("tn") ?: uri.getQueryParameter("note") ?: ""
        val txnRef = uri.getQueryParameter("tr") ?: uri.getQueryParameter("tid") ?: ""

        if (payeeVpa.isEmpty()) {
            Toast.makeText(this, "Invalid UPI ID in QR code", Toast.LENGTH_SHORT).show()
            return
        }

        pendingPayeeName = payeeName
        pendingPayeeVpa = payeeVpa
        pendingTxnRef = if (txnRef.isNotBlank()) txnRef else "TXN" + (System.currentTimeMillis() % 10000000)

        val isMerchantQr = !merchantCode.isNullOrBlank() && merchantCode != "0000"

        showAmountAndNoteDialog(
            payeeName = payeeName,
            payeeVpa = payeeVpa,
            prefilledAmount = prefilledAmount,
            prefilledNote = prefilledNote,
            isMerchantQr = isMerchantQr,
            originalUpiUriString = upiUriString
        )
    }

    private fun showAmountAndNoteDialog(
        payeeName: String,
        payeeVpa: String,
        prefilledAmount: String,
        prefilledNote: String,
        isMerchantQr: Boolean,
        originalUpiUriString: String
    ) {
        val dp = resources.displayMetrics.density
        fun dpToPx(dpVal: Float): Int = (dpVal * dp + 0.5f).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = dpToPx(20f)
            setPadding(p, dpToPx(12f), p, dpToPx(8f))
        }

        val detailsTv = TextView(this).apply {
            text = "Paying: $payeeName\nUPI ID: $payeeVpa"
            textSize = 14f
            setPadding(0, 0, 0, dpToPx(16f))
        }
        container.addView(detailsTv)

        val etAmount = EditText(this).apply {
            hint = "Amount (₹)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (prefilledAmount.isNotBlank()) {
                setText(prefilledAmount)
            }
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(12f)) }
        }
        container.addView(etAmount)

        val etNote = EditText(this).apply {
            hint = "Description / Note (e.g. Lunch, Rent)"
            inputType = InputType.TYPE_CLASS_TEXT
            if (prefilledNote.isNotBlank()) {
                setText(prefilledNote)
            }
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(etNote)

        AlertDialog.Builder(this)
            .setTitle("Payment Details")
            .setView(container)
            .setPositiveButton("Proceed to Pay") { _, _ ->
                val amountText = etAmount.text.toString().trim()
                val noteText = etNote.text.toString().trim()

                if (amountText.isEmpty()) {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val formattedAmount = try {
                    val doubleVal = amountText.toDouble()
                    if (doubleVal <= 0) {
                        Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    String.format(Locale.US, "%.2f", doubleVal)
                } catch (_: Exception) {
                    amountText
                }

                pendingAmount = "₹$formattedAmount"
                pendingDescription = noteText

                // Reconstruct final UPI URI with entered amount and note
                val baseUri = Uri.parse(originalUpiUriString)
                val builder = baseUri.buildUpon().clearQuery()

                for (paramName in baseUri.queryParameterNames) {
                    if (paramName != "am" && paramName != "tn" && paramName != "mam" && paramName != "note") {
                        for (value in baseUri.getQueryParameters(paramName)) {
                            builder.appendQueryParameter(paramName, value)
                        }
                    }
                }
                builder.appendQueryParameter("am", formattedAmount)
                if (noteText.isNotBlank()) {
                    builder.appendQueryParameter("tn", noteText)
                }

                val finalUpiUriString = builder.build().toString()

                if (isMerchantQr) {
                    launchDirectMerchantPayment(finalUpiUriString)
                } else {
                    handlePersonalP2PFallback(payeeName, payeeVpa)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchDirectMerchantPayment(upiUriString: String) {
        val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriString))
        val chooser = Intent.createChooser(upiIntent, "Pay Merchant with")

        try {
            upiPaymentLauncher.launch(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No UPI app installed", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleUpiPaymentResult(resultCode: Int, data: Intent?) {
        var status = ""
        var txnId = ""
        var responseCode = ""

        if (data != null) {
            val responseExtra = data.getStringExtra("response") ?: data.dataString ?: ""
            val params = parseQueryParams(responseExtra)
            status = params["status"] ?: params["Status"] ?: ""
            txnId = params["txnid"] ?: params["txnId"] ?: params["txnref"] ?: params["txnRef"] ?: params["approvalrefno"] ?: params["ApprovalRefNo"] ?: ""
            responseCode = params["responsecode"] ?: params["responseCode"] ?: ""
        }

        val isSuccess = status.equals("SUCCESS", ignoreCase = true) ||
                status.equals("SUBMITTED", ignoreCase = true) ||
                status.equals("SUCCESSFUL", ignoreCase = true) ||
                responseCode == "00" ||
                responseCode == "0" ||
                (resultCode == RESULT_OK && status.isNotBlank() && !status.equals("FAILURE", ignoreCase = true) && !status.equals("FAILED", ignoreCase = true) && !status.equals("CANCELLED", ignoreCase = true))

        if (isSuccess || resultCode == RESULT_OK) {
            val finalTxnId = if (txnId.isNotBlank()) txnId else if (pendingTxnRef.isNotBlank()) pendingTxnRef else "TXN" + (System.currentTimeMillis() % 10000000)
            val finalStatus = if (status.isNotBlank()) status.uppercase(Locale.getDefault()) else "SUCCESS"

            val transaction = UpiTransaction(
                payeeName = if (pendingPayeeName.isNotBlank()) pendingPayeeName else "Merchant Payment",
                payeeVpa = pendingPayeeVpa,
                amount = pendingAmount.ifBlank { "₹0.00" },
                description = pendingDescription,
                place = getCurrentTxPlace(),
                txnId = finalTxnId,
                status = finalStatus
            )

            recordSuccessfulTransaction(transaction)
            Toast.makeText(this, "Payment Successful! Ref: $finalTxnId", Toast.LENGTH_LONG).show()
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Payment Canceled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Payment Status: ${if (status.isNotBlank()) status else "Incomplete"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePersonalP2PFallback(payeeName: String, payeeVpa: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", payeeVpa))

        val snackbar = Snackbar.make(
            findViewById(R.id.main),
            "Because NPCI blocks direct intent links for personal QRs, we copied the UPI ID for you. Tap an app below to open, click on Pay UPI ID and paste:",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Done") {
            snackbar.dismiss()
        }
        snackbar.setTextMaxLines(5)
        snackbar.show()

        val upiApps = getInstalledUpiApps()

        if (upiApps.isEmpty()) {
            Toast.makeText(this, "UPI ID copied: $payeeVpa (No UPI app found)", Toast.LENGTH_LONG).show()
            return
        }

        val dp = resources.displayMetrics.density
        fun dpToPx(dpVal: Float): Int = (dpVal * dp + 0.5f).toInt()

        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pxPadding = dpToPx(20f)
            setPadding(pxPadding, dpToPx(8f), pxPadding, dpToPx(8f))
        }
        scrollView.addView(container)

        val noteInfo = if (pendingDescription.isNotBlank()) "\nNote: $pendingDescription" else ""
        val messageTv = TextView(this).apply {
            text = "Paying: $payeeName\n" +
                    "UPI ID: $payeeVpa\n" +
                    "Amount: $pendingAmount$noteInfo"
            textSize = 14f
            setPadding(0, 0, 0, dpToPx(16f))
        }
        container.addView(messageTv)

        val pm = packageManager
        var dialog: AlertDialog? = null

        for ((appName, pkg) in upiApps) {
            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val pVert = dpToPx(12f)
                val pHoriz = dpToPx(8f)
                setPadding(pHoriz, pVert, pHoriz, pVert)

                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)

                isClickable = true
                isFocusable = true
            }

            val iconIv = ImageView(this).apply {
                val iconSize = dpToPx(40f)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    setMargins(0, 0, dpToPx(16f), 0)
                }
                try {
                    setImageDrawable(pm.getApplicationIcon(pkg))
                } catch (_: Exception) {
                    setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
            itemView.addView(iconIv)

            val nameTv = TextView(this).apply {
                text = appName
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            itemView.addView(nameTv)

            itemView.setOnClickListener {
                dialog?.dismiss()
                showPersonalPaymentConfirmation(appName, pkg)
            }

            container.addView(itemView)
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("Personal UPI ID Copied")
            .setView(scrollView)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun getCurrentTxPlace(): String {
        return MainActivity.cityname.ifBlank {
            val userPrefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            userPrefs.getString("cityname", "") ?: ""
        }
    }

    private fun showPersonalPaymentConfirmation(appName: String, pkg: String) {
        openUpiAppHome(pkg)

        AlertDialog.Builder(this)
            .setTitle("Record Personal Payment")
            .setMessage("Did your payment of $pendingAmount via $appName to $pendingPayeeName ($pendingPayeeVpa) complete successfully?")
            .setPositiveButton("Yes, Success") { _, _ ->
                val transaction = UpiTransaction(
                    payeeName = pendingPayeeName,
                    payeeVpa = pendingPayeeVpa,
                    amount = pendingAmount.ifBlank { "₹0.00" },
                    description = pendingDescription,
                    place = getCurrentTxPlace(),
                    txnId = pendingTxnRef.ifBlank { "TXN" + (System.currentTimeMillis() % 10000000) },
                    status = "SUCCESS"
                )
                recordSuccessfulTransaction(transaction)
                Toast.makeText(this, "Transaction recorded successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun recordSuccessfulTransaction(transaction: UpiTransaction) {
        transactionList.add(0, transaction)
        saveTransactionsToPrefs()
        updateUiState()
        updateWidgetWithTransaction(transaction)
    }

    private fun updateWidgetWithTransaction(transaction: UpiTransaction) {
        val upiPrefs = getSharedPreferences("upi_prefs", Context.MODE_PRIVATE)
        upiPrefs.edit()
            .putString("last_txn_merchant", transaction.payeeName)
            .putString("last_txn_amount", transaction.amount)
            .putString("last_txn_note", transaction.description)
            .putString("last_txn_place", transaction.place)
            .apply()

        try {
            val intent = Intent(this, NewAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(this@ScanAndPayActivity)
                    .getAppWidgetIds(ComponentName(this@ScanAndPayActivity, NewAppWidget::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    private fun loadSavedTransactions() {
        transactionList.clear()
        val prefs = getSharedPreferences("upi_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions_list", null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<ArrayList<UpiTransaction>>() {}.type
                val savedList: ArrayList<UpiTransaction>? = Gson().fromJson(json, type)
                if (savedList != null) {
                    transactionList.addAll(savedList)
                }
            } catch (_: Exception) {
            }
        }
        updateUiState()
    }

    private fun saveTransactionsToPrefs() {
        val prefs = getSharedPreferences("upi_prefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(transactionList)
        prefs.edit().putString("transactions_list", json).apply()
    }

    private fun updateUiState() {
        adapter.notifyDataSetChanged()
        val count = transactionList.size
        tvTxnCount.text = if (count == 1) "1 payment" else "$count payments"
        if (transactionList.isEmpty()) {
            tvEmptyHistory.visibility = View.VISIBLE
            lvTransactions.visibility = View.GONE
        } else {
            tvEmptyHistory.visibility = View.GONE
            lvTransactions.visibility = View.VISIBLE
        }
    }

    private fun parseQueryParams(response: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (response.isBlank()) return map

        val queryString = if (response.contains("?")) response.substringAfter("?") else response
        val pairs = queryString.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size >= 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                map[key.lowercase(Locale.getDefault())] = value
                map[key] = value
            }
        }
        return map
    }

    private fun getInstalledUpiApps(): List<Pair<String, String>> {
        val apps = mutableListOf<Pair<String, String>>()
        val pm = packageManager
        val myPackageName = packageName

        val intentUris = listOf("upi://pay", "upi://pay?pa=test@upi", "upi://")
        for (uriString in intentUris) {
            val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    upiIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(upiIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg != myPackageName) {
                    val appName = info.loadLabel(pm).toString()
                    apps.add(Pair(appName, pkg))
                }
            }
        }

        val knownUpiPackages = listOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.phonepe.app",                        // PhonePe
            "net.one97.paytm",                        // Paytm
            "in.org.npci.upiapp",                      // BHIM
            "com.dreamplug.androidapp",               // Cred
            "com.naviapp",                            // Navi
            "com.amazon.mShop.android.shopping",       // Amazon Pay
            "com.whatsapp",                           // WhatsApp
            "com.freecharge.android",                 // Freecharge
            "com.mobikwik_new",                       // Mobikwik
            "com.mobikwik",                           // Mobikwik alt
            "com.enstage.wibmo.hdfc",                 // PayZapp
            "com.csam.icici.bank.imobile",             // iMobile
            "com.sbi.lotusintouch",                   // YONO SBI
            "money.jupiter",                          // Jupiter
            "com.epifi.gringotts",                    // Fi
            "org.altruist.slice",                     // Slice
            "com.kotak.mbanking",                     // Kotak 811
            "com.canarabank.mob",                     // Canara ai1
            "com.bankofbaroda.mconnect",               // BOB World
            "com.axis.mobile"                         // Axis Mobile
        )

        for (pkg in knownUpiPackages) {
            if (pkg == myPackageName) continue
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                if (appInfo.enabled) {
                    val appName = appInfo.loadLabel(pm).toString()
                    apps.add(Pair(appName, pkg))
                }
            } catch (_: Exception) {
            }
        }

        return apps.distinctBy { it.second }
    }

    private fun openUpiAppHome(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show()
        }
    }

    private class TransactionsAdapter(
        context: Context,
        private val items: List<UpiTransaction>
    ) : ArrayAdapter<UpiTransaction>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.item_upi_transaction,
                parent,
                false
            )

            val item = items[position]

            val tvPayeeName = view.findViewById<TextView>(R.id.tvItemPayeeName)
            val tvVpa = view.findViewById<TextView>(R.id.tvItemVpa)
            val tvAmount = view.findViewById<TextView>(R.id.tvItemAmount)
            val tvDesc = view.findViewById<TextView>(R.id.tvItemDesc)
            val tvTxnId = view.findViewById<TextView>(R.id.tvItemTxnId)
            val tvDate = view.findViewById<TextView>(R.id.tvItemDate)
            val tvStatus = view.findViewById<TextView>(R.id.tvItemStatus)

            tvPayeeName.text = item.payeeName
            tvVpa.text = item.payeeVpa
            tvAmount.text = item.amount
            if (item.description.isNotBlank()) {
                tvDesc.text = "Note: ${item.description}"
                tvDesc.visibility = View.VISIBLE
            } else {
                tvDesc.visibility = View.GONE
            }
            tvTxnId.text = "Txn ID: ${item.txnId}"
            val placeInfo = if (item.place.isNotBlank()) " • 📍 ${item.place}" else ""
            tvDate.text = "${item.date}$placeInfo"
            tvStatus.text = item.status

            return view
        }
    }
}
