package com.belaku.homey


import AppsAdapter
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SetWallWorker.Companion.dayIndex
import com.belaku.homey.SetWallWorker.Companion.rActOpenedFirst
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.SetWallWorker.Companion.wallBitmap
import com.belaku.homey.databinding.ActivityRemindersBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable
import com.belaku.homey.MusicActivity.Companion.pDatalistSongs
import com.belaku.homey.NewAppWidget.Companion.blurWallBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class RemindersActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var binding: ActivityRemindersBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRemindersBinding.inflate(layoutInflater)

        setContentView(binding.root)


        val rootLayout = findViewById<RelativeLayout>(R.id.reminders_layout)



        binding.txAddHabits.setOnClickListener(View.OnClickListener {
            val cdd = CustomDialogClass(this@RemindersActivity, "Habit")
            cdd.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            cdd.show()
        })

        binding.txAddReminders.setOnClickListener(View.OnClickListener {
            val cdd = CustomDialogClass(this@RemindersActivity, "Reminder")
            cdd.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            cdd.show()
        })


        val listViewHabits = findViewById<ListView>(R.id.rv_habits)
        adapterHabits = HabitsAdapter(this, arrayListHabits)
        listViewHabits.adapter = adapterHabits
        dayIndex =  Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        loadHabits()
        checkHabitStreaks()

        listViewHabits.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            val habit = parent.getItemAtPosition(position) as Habit
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            val yesterday = getYesterdayDateString()

            if (habit.isChecked) {
                habit.isChecked = false
                updateHabitDayState(habit.name, false)
            } else {
                habit.isChecked = true
                updateHabitDayState(habit.name, true)

                // Streak Logic
                if (habit.lastUpdatedDate == yesterday) {
                    habit.streak++
                } else if (habit.lastUpdatedDate != today) {
                    habit.streak = 1
                }
                habit.lastUpdatedDate = today
                // Save specific streak to SharedPreferences for immediate persistence/UI consistency
                sharedPreferencesEditor.putInt("${habit.name}_streak", habit.streak).apply()
            }
            saveHabits()
            adapterHabits.notifyDataSetChanged()
        })

        listViewHabits.setOnItemLongClickListener { adapterView, view, i, l ->
            arrayListHabits.removeAt(i)
            saveHabits()
            adapterHabits.notifyDataSetChanged()
            true
        }



        var listviewReminders = findViewById<ListView>(R.id.rv_reminders)
        adapterReminders = RemindersAdapter(
            this,
            arrayListReminders
        )
        listviewReminders.setAdapter(adapterReminders)

        listviewReminders.setOnItemLongClickListener(AdapterView.OnItemLongClickListener { parent, view, position, id -> // Remove the item from the data source

            cancelReminder()

            arrayListReminders.removeAt(position)
            // Notify the adapter that the data has changed
            adapterReminders.notifyDataSetChanged()
            true
        })

        try {
            rootLayout.setBackgroundDrawable(
                BitmapDrawable(
                    getResources(),
                    blurWallBitmap
                )
            )
        } catch (ex: Exception) {

        }


    }

    private fun getYesterdayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(cal.time)
    }

    private fun updateHabitDayState(name: String, value: Boolean) {
        val keySuffix = when(dayIndex) {
            1 -> "StateSu"
            2 -> "StateM"
            3 -> "StateTu"
            4 -> "StateW"
            5 -> "StateTh"
            6 -> "StateF"
            7 -> "StateS"
            else -> ""
        }
        if (keySuffix.isNotEmpty()) {
            sharedPreferencesEditor.putBoolean("$name$keySuffix", value).apply()
        }
    }

    private fun checkHabitStreaks() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val yesterday = getYesterdayDateString()
        var changed = false

        for (habit in arrayListHabits) {
            // If missed yesterday AND not done today, reset streak to 0
            if (habit.lastUpdatedDate != today && habit.lastUpdatedDate != yesterday) {
                if (habit.streak != 0) {
                    habit.streak = 0
                    sharedPreferencesEditor.putInt("${habit.name}_streak", 0)
                    changed = true
                }
            }
            // Sync isChecked with the daily state in SharedPreferences
            val keySuffix = when(dayIndex) {
                1 -> "StateSu"
                2 -> "StateM"
                3 -> "StateTu"
                4 -> "StateW"
                5 -> "StateTh"
                6 -> "StateF"
                7 -> "StateS"
                else -> ""
            }
            habit.isChecked = sharedPreferences.getBoolean("${habit.name}$keySuffix", false)
        }
        if (changed) {
            saveHabits()
        }
        adapterHabits.notifyDataSetChanged()
    }

    fun cancelReminder() {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

// 1. Create an Intent identical to the one used to set the alarm
        val intent = Intent(applicationContext, AlarmBroadcastReceiver::class.java)

// 2. Create the identical PendingIntent (Match the requestCode and Flags)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0, // Must match the code used when setting the alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

// 3. Cancel the alarm
        alarmManager.cancel(pendingIntent)

// 4. (Optional) Remove the PendingIntent from the system's tracking
        pendingIntent.cancel()

    }


    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(apps[pos].pName)
        startActivity(launchIntent)
    }

    companion object {

        lateinit var previewSelectedTimeTextView: TextView
        val timePickerDialogListener: TimePickerDialog.OnTimeSetListener =
            object : TimePickerDialog.OnTimeSetListener {
                override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {

                    // makeToast("onTimeSet - $hourOfDay")
                    // logic to properly handle
                    // the picked timings by user
                    val formattedTime = "$hourOfDay:$minute"

                    previewSelectedTimeTextView.setText(formattedTime)
                }
            }
        lateinit var adapterHabits: HabitsAdapter

        fun isadapterHabitsInitialized(): Boolean {
            return ::adapterHabits.isInitialized
        }

        lateinit var adapterReminders: RemindersAdapter
        var arrayListHabits: ArrayList<Habit> = ArrayList()
        var arrayListReminders: ArrayList<Reminder> = ArrayList()

        fun saveHabits() {
            val gson = Gson()
            val json = gson.toJson(arrayListHabits)
            sharedPreferencesEditor.putString("habits_list", json).apply()
        }

        fun loadHabits() {
            val json = sharedPreferences.getString("habits_list", null)
            if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<ArrayList<Habit>>() {}.type
                val habits: ArrayList<Habit> = gson.fromJson(json, type)
                arrayListHabits.clear()
                arrayListHabits.addAll(habits)
            }
            if (isadapterHabitsInitialized())
                adapterHabits.notifyDataSetChanged()
        }
    }

    fun timePicker(view: View) {
        val timePicker: TimePickerDialog = TimePickerDialog(
            // pass the Context
            this,
            // listener to perform task
            // when time is picked
            timePickerDialogListener,

            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            // default minute when the time picker
            // dialog is opened
            Calendar.getInstance().get(Calendar.MINUTE),
            // 24 hours time picker is
            // false (varies according to the region)
            false
        )

        // then after building the timepicker
        // dialog show the dialog to user
        timePicker.show()
    }

}
