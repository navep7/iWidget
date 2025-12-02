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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.RemindersActivity.Companion.previewSelectedTimeTextView
import com.belaku.homey.SetWallWorker.Companion.arrayListHabitStatuses
import java.util.Calendar


class CustomDialogClass // TODO Auto-generated constructor stub
    (var c: Activity, var strHorR: String) : Dialog(c), View.OnClickListener {

    private lateinit var adapterHorRs: ArrayAdapter<String>
    private lateinit var adapterHabits: ArrayAdapter<Habit>
    private var arrayListHabits: ArrayList<Habit> = ArrayList()
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
        } /*else {
            arrayListHorRs = RemindersActivity.arrayListReminders
            adapterHorRs = RemindersActivity.adapterReminders
            setContentView(R.layout.reminder_dialog)

            setContentView(R.layout.reminder_dialog)

            previewSelectedTimeTextView = findViewById<TextView>(R.id.tx_time)
            findViewById<Spinner>(R.id.remindertype).adapter = ArrayAdapter(context,android.R.layout.simple_list_item_1,context.resources.getStringArray(R.array.ReminderTypes))

            findViewById<Button>(R.id.remind_button).setOnClickListener(this)
            findViewById<Button>(R.id.cancel_button).setOnClickListener(this)
        }*/



    }



    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_okd -> {

                arrayListHabitStatuses.add("Su")
                arrayListHabitStatuses.add("M")
                arrayListHabitStatuses.add("Tu")
                arrayListHabitStatuses.add("W")
                arrayListHabitStatuses.add("Th")
                arrayListHabitStatuses.add("F")
                arrayListHabitStatuses.add("S")

                val dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                makeToast("dayIndex : " + dayIndex)

                when(dayIndex) {
                    0 -> arrayListHabitStatuses[6] = "•"
                    1 -> arrayListHabitStatuses[0] = "•"
                    2 -> arrayListHabitStatuses[1] = "•"
                    3 -> arrayListHabitStatuses[2] = "•"
                    4 -> arrayListHabitStatuses[3] = "•"
                    5 -> arrayListHabitStatuses[4] = "•"
                    6 -> arrayListHabitStatuses[5] = "•"
                }





                arrayListHabits.add(Habit(editText.text.toString(), false, arrayListHabitStatuses))
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
                var rSubject = findViewById<EditText>(R.id.etTitle).text.toString()
                var rTime = previewSelectedTimeTextView.text
                var rTimeSplits = rTime.split(":")
                var h = rTimeSplits[0]
                var m = rTimeSplits[1].substring(0, 2)
                var rType = findViewById<Spinner>(R.id.remindertype).selectedItem
              //  arrayListHorRs.add(rSubject + "\t@\t" + rTime + "\t\t\t:\t\t\t" + rType)
                adapterHorRs.notifyDataSetChanged()
                addAlarm(rSubject, h.toInt(), m.toInt(), rType.toString())
            }
        }
        dismiss()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun addAlarm(rSubject: String, hr: Int, mn: Int,  rType: String) {

   //     makeToast("addAlarm - $rSubject @ $hr:$mn -  ${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:${Calendar.getInstance().get(Calendar.MINUTE) + 3} ,  $rType" )

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


    }


}