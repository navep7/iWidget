package com.belaku.homey

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText


class CustomDialogClass // TODO Auto-generated constructor stub
    (var c: Activity, var strHorR: String) : Dialog(c), View.OnClickListener {
    private var arrayListHOrRs: ArrayList<String> = ArrayList()
    private lateinit var editText: EditText
    var d: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.custom_dialog)

        if (strHorR.equals("Habit"))
            arrayListHOrRs = RemindersActivity.arrayListHabits
        else arrayListHOrRs = RemindersActivity.arrayListReminders

        editText = findViewById<EditText>(R.id.edtx)
        editText.setHint("Add a $strHorR")

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_ok -> {
                arrayListHOrRs.add(editText.text.toString())
                dismiss()
            }
            R.id.btn_cancel -> {

                dismiss()
            }
        }
        dismiss()
    }
}