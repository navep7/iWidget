package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.lapCount
import com.belaku.homey.SetWallWorker.Companion.steps


class DialogWidgetStepsActivity : Activity() {


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog_widget_steps)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var txSteps: TextView = findViewById(R.id.tx_t_steps)
        var btnNewLap: Button = findViewById(R.id.btn_newlap)
        var cv1: CardView = findViewById(R.id.cv1)
        var cv2: CardView = findViewById(R.id.cv2)
        var cv3: CardView = findViewById(R.id.cv3)

        txSteps.text = "Total Steps ~ $steps"

        appContx = applicationContext
        makeToast("lapCount - " + lapCount)

        if (lapCount == 1)
            cv1.visibility = View.VISIBLE
        else if (lapCount == 2) {
            cv1.visibility = View.VISIBLE
            cv2.visibility = View.VISIBLE
        } else if (lapCount == 3) {
            cv1.visibility = View.VISIBLE
            cv2.visibility = View.VISIBLE
            cv3.visibility = View.VISIBLE
        }
    }
}