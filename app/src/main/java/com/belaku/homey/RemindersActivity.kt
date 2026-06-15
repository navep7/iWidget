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


class RemindersActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var binding: ActivityRemindersBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRemindersBinding.inflate(layoutInflater)

        setContentView(binding.root)


        val rootLayout = findViewById<RelativeLayout>(R.id.reminders_layout)
        try {
            rootLayout.setBackgroundDrawable(
                BitmapDrawable(
                    getResources(),
                    blur(applicationContext, wallBitmap)
                )
            )
        } catch (ex: Exception) {

            wallBitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.gradient_glass)

            rootLayout.setBackgroundDrawable(
                blur(applicationContext, wallBitmap).toDrawable(getResources())
            )
        }


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

        listViewHabits.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            if ((parent.getItemAtPosition(position) as Habit).isChecked) {
                (parent.getItemAtPosition(position) as Habit).isChecked = false
                when(dayIndex) {
                    1 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateSu", false).apply()
                    2 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateM", false).apply()
                    3 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateTu", false).apply()
                    4 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateW", false).apply()
                    5 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateTh", false).apply()
                    6 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateF", false).apply()
                    7 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateS", false).apply()
                }
            } else {
                (parent.getItemAtPosition(position) as Habit).isChecked = true
                when(dayIndex) {
                    1 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateSu", true).apply()
                    2 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateM", true).apply()
                    3 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateTu", true).apply()
                    4 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateW", true).apply()
                    5 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateTh", true).apply()
                    6 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateF", true).apply()
                    7 -> sharedPreferencesEditor.putBoolean("${(parent.getItemAtPosition(position) as Habit).name}StateS", true).apply()
                }
            }
            adapterHabits.notifyDataSetChanged()
        })

        listViewHabits.setOnItemLongClickListener { adapterView, view, i, l ->
            arrayListHabits.removeAt(i)
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

    fun blur(context: Context?, image: Bitmap): Bitmap {

        var BITMAP_SCALE = 0.4f; // Scale down bitmap for performance
        var BLUR_RADIUS = 25f; // Adjust blur intensity

        val width = Math.round(image.width * BITMAP_SCALE).toInt()
        val height = Math.round(image.height * BITMAP_SCALE).toInt()

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)

        return outputBitmap
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