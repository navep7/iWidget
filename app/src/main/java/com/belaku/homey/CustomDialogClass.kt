package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.RemindersActivity.Companion.previewSelectedTimeTextView
import com.belaku.homey.SetWallWorker.Companion.dayIndex
import java.util.Calendar


class CustomDialogClass // TODO Auto-generated constructor stub
    (var c: Activity, var strHorR: String) : Dialog(c), View.OnClickListener {

    private var boolRepeating: Boolean = false
    private var spinReminderTypes: ArrayList<String> = ArrayList()
    private lateinit var adapterHabits: ArrayAdapter<Habit>
    private lateinit var adapterReminders: ArrayAdapter<Reminder>
    private var arrayListHabits: ArrayList<Habit> = ArrayList()
    private var arrayListReminders: ArrayList<Reminder> = ArrayList()
    private lateinit var editText: EditText
    var d: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)


        if (strHorR.equals("Habit")) {
            arrayListHabits = RemindersActivity.arrayListHabits
            adapterHabits = RemindersActivity.adapterHabits
            setContentView(R.layout.habit_dialog)

            editText = findViewById<EditText>(R.id.edtx)
            editText.setHint("Add a $strHorR")
            findViewById<Button>(R.id.btn_okd).setOnClickListener(this)
            findViewById<Button>(R.id.btn_canceld).setOnClickListener(this)
        } else {
         //   arrayListHorRs = RemindersActivity.arrayListReminders
            arrayListReminders = RemindersActivity.arrayListReminders
            adapterReminders = RemindersActivity.adapterReminders
            setContentView(R.layout.reminder_dialog)

            setContentView(R.layout.reminder_dialog)

            previewSelectedTimeTextView = findViewById<TextView>(R.id.tx_time)
       //     findViewById<Spinner>(R.id.remindertype).adapter = ArrayAdapter(context,android.R.layout.simple_list_item_1,context.resources.getStringArray(R.array.ReminderTypes))

            findViewById<Button>(R.id.remind_button).setOnClickListener(this)
            findViewById<Button>(R.id.cancel_button).setOnClickListener(this)

            spinReminderTypes.add("One time")
            spinReminderTypes.add("Repeating")

            // Source - https://stackoverflow.com/a
// Posted by Kailash Dabhi
// Retrieved 2025-12-26, License - CC BY-SA 3.0
            val spinReminder =
                findViewById<View>(R.id.spinner_reminder) as Spinner //fetch the spinner from layout file
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_item, spinReminderTypes
            ) //setting the country_array to spinner
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinReminder.adapter = adapter

//if you want to set any action you can do in this listener
            spinReminder.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    arg0: AdapterView<*>?, arg1: View?,
                    position: Int, id: Long
                ) {
                    makeToast(spinReminderTypes[position])
                    if (spinReminderTypes[position] == "Repeating")
                        boolRepeating = true
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {
                }
            })

        }



    }



    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_okd -> {


                makeToast("dayIndex : " + dayIndex)

                arrayListHabits.add(Habit(editText.text.toString(), false))
                adapterHabits.notifyDataSetChanged()
                dismiss()
            }
            R.id.btn_canceld -> {
                dismiss()
            }
            R.id.cancel_button -> {
                dismiss()
            }
            R.id.remind_button -> {
                val rSubject = findViewById<EditText>(R.id.etTitle).text.toString()
                val rTime = previewSelectedTimeTextView.text.toString()
                if (rSubject.isNotEmpty() && rTime.isNotEmpty()) {
                    val rTimeSplits = rTime.split(":")
                    val h = rTimeSplits[0]
                    val m = rTimeSplits[1].split(" ")[0]
                    //    var rType = findViewById<Spinner>(R.id.remindertype).selectedItem
                    //  arrayListHorRs.add(rSubject + "\t@\t" + rTime + "\t\t\t:\t\t\t" + rType)
                    var rType: String
                    if (boolRepeating)
                        rType = "Repeating"
                    else rType = "One time"
                    arrayListReminders.add(Reminder(rSubject, "$h:$m", rType))
                    adapterReminders.notifyDataSetChanged()

                    addAlarm(rSubject, h.toInt(), m.toInt())

                } else makeToast("Add a subject and time!")
            }
        }
        dismiss()
    }



    @SuppressLint("ScheduleExactAlarm")
    private fun addAlarm(rSubject: String, hr: Int, mn: Int) {


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent: Intent = Intent(
            context,
            AlarmBroadcastReceiver::class.java
        ).putExtra("alertSub", rSubject)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE
        )


        // Calculate the trigger time in milliseconds (e.g., for 7:30 AM tomorrow)
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(System.currentTimeMillis())

        calendar.set(Calendar.HOUR_OF_DAY, hr)
        calendar.set(Calendar.MINUTE, mn)
        calendar.set(Calendar.SECOND, Calendar.getInstance().get(Calendar.SECOND))
        calendar.set(Calendar.MILLISECOND, 0)


        // If the target time is in the past for the current day, set it for the next day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }


        if (!boolRepeating)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,  // Wakes up the device to fire the alarm
                calendar.timeInMillis,
                pendingIntent
            )
            makeToast("AlarmSET @ ${calendar.time}")
        } catch (ex: Exception) {
            makeToast("AlarmEx - ${ex.message}")
        }
        else
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,          // Wakes the device up
                    calendar.timeInMillis,            // First trigger time
                    AlarmManager.INTERVAL_DAY,        // Repeat interval (built-in constant for efficiency)
                    pendingIntent
                )
                makeToast("AlarmSET @ ${calendar.time}, everyday!")
            } catch (ex: Exception) {
                makeToast("AlarmEx - ${ex.message}")
            }



    }


}