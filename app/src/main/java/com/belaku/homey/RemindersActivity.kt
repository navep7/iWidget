package com.belaku.homey


import AppsAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.sharedPreferences
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
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
        rootLayout.setBackgroundDrawable(
            BitmapDrawable(
                getResources(),
                blur(applicationContext, SetWallWorker.wallBitmap)
            )
        )


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
        lateinit var adapterHabits: ArrayAdapter<String>
        lateinit var adapterReminders: ArrayAdapter<String>
        var arrayListHabits: ArrayList<String> = ArrayList()
        var arrayListReminders: ArrayList<String> = ArrayList()
    }

}