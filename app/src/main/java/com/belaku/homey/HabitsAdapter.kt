package com.belaku.homey

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.RemindersActivity.Companion.adapterHabits
import com.belaku.homey.RemindersActivity.Companion.arrayListHabits
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import java.util.Calendar

class HabitsAdapter(context: Context, private val habits: List<Habit>) :
    ArrayAdapter<Habit>(context, 0, habits) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItem = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_habit, parent, false)

        val habit = habits[position]

        val hName = listItem.findViewById<CheckedTextView>(R.id.tx_habit)
        hName.text = habits[position].name
        hName.isChecked = sharedPreferences.getBoolean("cB${habit.name}", false)

        val hStateM = listItem.findViewById<TextView>(R.id.tx_m)
        hStateM.text = habits[position].habitStatuses[0]

        val hStateTu = listItem.findViewById<TextView>(R.id.tx_tu)
        hStateTu.text = habits[position].habitStatuses[1]

        val hStateW = listItem.findViewById<TextView>(R.id.tx_w)
        hStateW.text = habits[position].habitStatuses[2]

        val hStateTh = listItem.findViewById<TextView>(R.id.tx_th)
        hStateTh.text = habits[position].habitStatuses[3]

        val hStateF = listItem.findViewById<TextView>(R.id.tx_f)
        hStateF.text = habits[position].habitStatuses[4]

        val hStateS = listItem.findViewById<TextView>(R.id.tx_s)
        hStateS.text = habits[position].habitStatuses[5]

        val hStateSu = listItem.findViewById<TextView>(R.id.tx_su)
        hStateSu.text = habits[position].habitStatuses[6]

        listItem.setOnClickListener(View.OnClickListener {
            var cTx = listItem.findViewById<CheckedTextView>(R.id.tx_habit)
            makeToast(cTx.text.toString())
            if (cTx.isChecked) {
                cTx.isChecked = false
                sharedPreferencesEditor.putBoolean("cB${cTx.text}", false).apply()
            //    habits[position].habitStatuses[Calendar.getInstance().get(Calendar.DAY_OF_WEEK + 1)] = "✗"
            } else {
                makeToast(Calendar.getInstance().get(Calendar.DAY_OF_WEEK).toString())
                cTx.isChecked = true
                sharedPreferencesEditor.putBoolean("cB${cTx.text}", true).apply()
                habits[position].habitStatuses[Calendar.getInstance().get(Calendar.DAY_OF_WEEK + 1)] = "✓"
            }

        })

        listItem.setOnLongClickListener {

            habits.toMutableList().removeAt(position)
            adapterHabits.notifyDataSetChanged()

            true
            }



        return listItem
    }
}