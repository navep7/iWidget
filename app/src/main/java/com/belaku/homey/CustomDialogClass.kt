package com.belaku.homey

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import com.belaku.homey.MainActivity.Companion.makeToast


class CustomDialogClass // TODO Auto-generated constructor stub
    (var c: Activity, var strHorR: String) : Dialog(c), View.OnClickListener {
    private lateinit var adapterHorRs: ArrayAdapter<String>
    private var arrayListHorRs: ArrayList<String> = ArrayList()
    private lateinit var editText: EditText
    var d: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_dialog)

        if (strHorR.equals("Habit")) {
            arrayListHorRs = RemindersActivity.arrayListHabits
            adapterHorRs = RemindersActivity.adapterHabits
        } else {
            arrayListHorRs = RemindersActivity.arrayListReminders
            adapterHorRs = RemindersActivity.adapterReminders
        }

        editText = findViewById<EditText>(R.id.edtx)
        editText.setHint("Add a $strHorR")
        findViewById<Button>(R.id.btn_okd).setOnClickListener(this)
        findViewById<Button>(R.id.btn_canceld).setOnClickListener(this)

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_okd -> {
                arrayListHorRs.add(editText.text.toString())
                adapterHorRs.notifyDataSetChanged()
                dismiss()
            }
            R.id.btn_canceld -> {
                dismiss()
            }
        }
        dismiss()
    }
}