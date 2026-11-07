package com.belaku.homey

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class UpiPaymentManager(private val context: Context) {

    /**
     * Triggers the Google Code Scanner. Does not require manual Camera permissions.
     */
    fun startQrScanAndPay() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(context, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null && rawValue.startsWith("upi://pay", ignoreCase = true)) {
                    openUpiAppsChooser(rawValue)
                } else {
                    Toast.makeText(context, "Invalid UPI QR Code", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Scanning failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Passes the scanned UPI URI into an explicit package-targeted Intent using UpiAppAdapter.
     */
    private fun openUpiAppsChooser(upiUriString: String) {
        val upiUri = Uri.parse(upiUriString)

        val builder = upiUri.buildUpon()
        if (upiUri.getQueryParameter("cu") == null) {
            builder.appendQueryParameter("cu", "INR")
        }
        if (upiUri.getQueryParameter("am") == null) {
            builder.appendQueryParameter("am", "1")
        }
        val finalUri = builder.build()

        val upiApps = getInstalledUpiApps(context)
        if (upiApps.isEmpty()) {
            Toast.makeText(context, "No UPI applications found installed on this device.", Toast.LENGTH_LONG).show()
            return
        }

        val builderDialog = AlertDialog.Builder(context)
        builderDialog.setTitle("Choose UPI App to pay")

        val adapter = UpiAppAdapter(context, upiApps)
        builderDialog.setAdapter(adapter) { _, which ->
            val selectedApp = upiApps[which]
            val upiIntent = Intent(Intent.ACTION_VIEW, finalUri).apply {
                setPackage(selectedApp.packageName)
            }
            val chooserIntent = Intent.createChooser(upiIntent, "Pay using:").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error launching payment app", Toast.LENGTH_SHORT).show()
            }
        }

        builderDialog.show()
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
}
