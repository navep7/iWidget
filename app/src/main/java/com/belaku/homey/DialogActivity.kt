package com.belaku.homey

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.belaku.homey.NewAppWidget.Companion.tW

class DialogActivity : AppCompatActivity() {

    private lateinit var txTitle: TextView
    private lateinit var txContent: TextView

    private lateinit var edtxDialog: EditText

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    private lateinit var imgbtnShare: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var bluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    // Bluetooth enabled by user
                } else {
                    // Bluetooth not enabled by user
                }
            }

        txTitle = findViewById<TextView>(R.id.tx_dialog_title)
        txContent = findViewById<TextView>(R.id.tx_dialog_content)
        edtxDialog = findViewById<EditText>(R.id.edtx_dialog)
        btnOk = findViewById<Button>(R.id.btn_dialog_ok)
        btnCancel = findViewById<Button>(R.id.btn_dialog_cancel)
        imgbtnShare = findViewById<ImageButton>(R.id.imgbtn_dialog_share)


        var dialogIntentStr = intent.getStringExtra("DialogIntent")

        if (dialogIntentStr != null) {
            if (dialogIntentStr == "StT") {
                edtxDialog.visibility = View.INVISIBLE
                btnOk.setText("Speak")
                txTitle.setText("Speech to Text")
                txContent.setText("Speak")
            }
            else  if (dialogIntentStr == "ST") {
                edtxDialog.visibility = View.INVISIBLE
                btnOk.visibility = View.INVISIBLE
                btnCancel.visibility = View.INVISIBLE
                txTitle.setText("Tweet")
                txContent.setText(tW)
            } else  if (dialogIntentStr == "STH") {
                txTitle.setText("Twitter")
                txContent.visibility = View.INVISIBLE
                btnOk.setText("Set")
            } else if (dialogIntentStr == "BLUEEnable") {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothLauncher.launch(enableBtIntent)
            } else if (dialogIntentStr == "BLUEDisable") {
                val disableintent = Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
                bluetoothLauncher.launch(disableintent)
            }

        }
    }


}