package com.belaku.homey


import AppsAdapter
import android.app.TimePickerDialog
import android.content.Context
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
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

import androidx.navigation.ui.AppBarConfiguration
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.SetWallWorker.Companion.wallBitmap
import com.belaku.homey.databinding.ActivityRemindersBinding


class RemindersActivity : AppCompatActivity(), AppsAdapter.RvEvent {



    private lateinit var prompt: String
    private lateinit var txAi: TextView
    private lateinit var edtxAi: EditText
    private lateinit var appBarConfiguration: AppBarConfiguration
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

            wallBitmap = BitmapFactory.decodeResource(resources, R.drawable.gradient_glass)

            rootLayout.setBackgroundDrawable(
                BitmapDrawable(
                    getResources(),
                    blur(applicationContext, wallBitmap)
                )
            )
        }


        binding.txAddHabits.setOnClickListener(View.OnClickListener {
            val cdd = CustomDialogClass(this@RemindersActivity, "Habit")
            cdd.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            cdd.show()
        })

        binding.txAddReminders.setOnClickListener(View.OnClickListener {
            val cdd = CustomDialogClass(this@RemindersActivity, "Reminder")
            cdd.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            cdd.show()
        })

        var listViewHabits = findViewById<ListView>(R.id.rv_habits)
        listViewHabits.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE)

        adapterHabits = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice,  // Default layout for a single text item
            arrayListHabits
        )
        listViewHabits.setAdapter(adapterHabits)

        for (i in 0 until arrayListHabits.size) {
            if (sharedPreferences.getBoolean("cB$i", false))
                listViewHabits.setItemChecked(i, true)
            else listViewHabits.setItemChecked(i, false)
        }

        listViewHabits.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            if ((view as CheckedTextView).isChecked)
                sharedPreferencesEditor.putBoolean("cB$position", true)
            else sharedPreferencesEditor.putBoolean("cB$position", false)
            sharedPreferencesEditor.apply()
        }

        adapterReminders = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,  // Default layout for a single text item
            arrayListReminders
        )
        findViewById<ListView>(R.id.rv_reminders).setAdapter(adapterReminders)


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

                    // logic to properly handle
                    // the picked timings by user
                    val formattedTime: String = when {
                        hourOfDay == 0 -> {
                            if (minute < 10) {
                                "${hourOfDay + 12}:0${minute} am"
                            } else {
                                "${hourOfDay + 12}:${minute} am"
                            }
                        }
                        hourOfDay > 12 -> {
                            if (minute < 10) {
                                "${hourOfDay - 12}:0${minute} pm"
                            } else {
                                "${hourOfDay - 12}:${minute} pm"
                            }
                        }
                        hourOfDay == 12 -> {
                            if (minute < 10) {
                                "${hourOfDay}:0${minute} pm"
                            } else {
                                "${hourOfDay}:${minute} pm"
                            }
                        }
                        else -> {
                            if (minute < 10) {
                                "${hourOfDay}:${minute} am"
                            } else {
                                "${hourOfDay}:${minute} am"
                            }
                        }
                    }

                    previewSelectedTimeTextView.setText(formattedTime)
                }
            }
        lateinit var adapterHabits: ArrayAdapter<String>
        lateinit var adapterReminders: ArrayAdapter<String>
        var arrayListHabits: ArrayList<String> = ArrayList()
        var arrayListReminders: ArrayList<String> = ArrayList()
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