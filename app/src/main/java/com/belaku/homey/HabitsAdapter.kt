package com.belaku.homey

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.belaku.homey.SetWallWorker.Companion.arrayListHabitStatuses
import com.belaku.homey.SetWallWorker.Companion.dayChange
import java.util.Calendar

class HabitsAdapter(context: Context, data: List<Habit>) :
    ArrayAdapter<Habit>(context, 0, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(
                R.layout.item_habit, parent, false
            )
        }

        val currentItem = getItem(position)

        val textView = listItemView!!.findViewById<TextView>(R.id.item_text_name)
        textView.text = currentItem?.name

        val checkBox = listItemView.findViewById<CheckBox>(R.id.item_checkbox)
        if (dayChange) {
            dayChange = false
            checkBox.isChecked = false
        }
        else checkBox.isChecked = currentItem?.isChecked ?: false

        val textViewSu = listItemView!!.findViewById<TextView>(R.id.txSu)
        val textViewM = listItemView!!.findViewById<TextView>(R.id.txM)
        val textViewTu = listItemView!!.findViewById<TextView>(R.id.txT)
        val textViewW = listItemView!!.findViewById<TextView>(R.id.txW)
        val textViewTh = listItemView!!.findViewById<TextView>(R.id.txTh)
        val textViewF = listItemView!!.findViewById<TextView>(R.id.txF)
        val textViewS = listItemView!!.findViewById<TextView>(R.id.txS)

        textViewSu.text = arrayListHabitStatuses[0]
        textViewM.text = arrayListHabitStatuses[1]
        textViewTu.text = arrayListHabitStatuses[2]
        textViewW.text = arrayListHabitStatuses[3]
        textViewTh.text = arrayListHabitStatuses[4]
        textViewF.text = arrayListHabitStatuses[5]
        textViewS.text = arrayListHabitStatuses[6]

        when(arrayListHabitStatuses.indexOf("•")) {
            1 -> textViewM.setTextColor(Color.GREEN)
            2 -> textViewTu.setTextColor(Color.GREEN)
            3 -> textViewW.setTextColor(Color.GREEN)
            4 -> textViewTh.setTextColor(Color.GREEN)
            5 -> textViewF.setTextColor(Color.GREEN)
            6 -> textViewS.setTextColor(Color.GREEN)
            0 -> textViewSu.setTextColor(Color.GREEN)
        }

        when(arrayListHabitStatuses.indexOf("✓")) {
            1 -> textViewM.setTextColor(Color.GREEN)
            2 -> textViewTu.setTextColor(Color.GREEN)
            3 -> textViewW.setTextColor(Color.GREEN)
            4 -> textViewTh.setTextColor(Color.GREEN)
            5 -> textViewF.setTextColor(Color.GREEN)
            6 -> textViewS.setTextColor(Color.GREEN)
            0 -> textViewSu.setTextColor(Color.GREEN)
        }

        when(arrayListHabitStatuses.indexOf("✕")) {
            1 -> textViewM.setTextColor(Color.RED)
            2 -> textViewTu.setTextColor(Color.RED)
            3 -> textViewW.setTextColor(Color.RED)
            4 -> textViewTh.setTextColor(Color.RED)
            5 -> textViewF.setTextColor(Color.RED)
            6 -> textViewS.setTextColor(Color.RED)
            0 -> textViewSu.setTextColor(Color.RED)
        }
   //     arrayListHabitStatuses[i] = "✓"




        return listItemView
    }
}