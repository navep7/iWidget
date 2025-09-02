package com.belaku.homey

import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.RemindersActivity.Companion.previewSelectedTimeTextView


class CustomDialogClass // TODO Auto-generated constructor stub
    (var c: Activity, var strHorR: String) : Dialog(c), View.OnClickListener {
    private lateinit var adapterHorRs: ArrayAdapter<String>
    private var arrayListHorRs: ArrayList<String> = ArrayList()
    private lateinit var editText: EditText
    var d: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)


        if (strHorR.equals("Habit")) {
            arrayListHorRs = RemindersActivity.arrayListHabits
            adapterHorRs = RemindersActivity.adapterHabits
            setContentView(R.layout.habit_dialog)

            editText = findViewById<EditText>(R.id.edtx)
            editText.setHint("Add a $strHorR")
            findViewById<Button>(R.id.btn_okd).setOnClickListener(this)
            findViewById<Button>(R.id.btn_canceld).setOnClickListener(this)
        } else {
            arrayListHorRs = RemindersActivity.arrayListReminders
            adapterHorRs = RemindersActivity.adapterReminders
            setContentView(R.layout.reminder_dialog)

            setContentView(R.layout.reminder_dialog)

            previewSelectedTimeTextView = findViewById<TextView>(R.id.tx_time)
            findViewById<Spinner>(R.id.remindertype).adapter = ArrayAdapter(context,android.R.layout.simple_list_item_1,context.resources.getStringArray(R.array.ReminderTypes))

            findViewById<Button>(R.id.remind_button).setOnClickListener(this)
            findViewById<Button>(R.id.cancel_button).setOnClickListener(this)
        }



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
            R.id.cancel_button -> {
                dismiss()
            }
            R.id.remind_button -> {
                arrayListHorRs.add(findViewById<EditText>(R.id.etTitle).text.toString() + "\t@\t" + previewSelectedTimeTextView.text + "\t\t\t:\t\t\t" + findViewById<Spinner>(R.id.remindertype).selectedItem)
                adapterHorRs.notifyDataSetChanged()
            }
        }
        dismiss()
    }
}