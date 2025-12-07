package com.belaku.homey

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences

class HabitsAdapter(context: Context, data: List<Habit>) :
    ArrayAdapter<Habit>(context, 0, data) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(
                R.layout.item_habit, parent, false
            )
        }

        val currentItem = getItem(position)

        val textView = listItemView!!.findViewById<TextView>(R.id.item_text_name)
        textView.text = "${(position + 1)}. ${currentItem?.name}"

        val checkBox = listItemView.findViewById<CheckBox>(R.id.item_checkbox)
        checkBox.isChecked = currentItem?.isChecked ?: false

        val textViewSu = listItemView.findViewById<TextView>(R.id.txSu)
        val textViewM = listItemView.findViewById<TextView>(R.id.txM)
        val textViewTu = listItemView.findViewById<TextView>(R.id.txT)
        val textViewW = listItemView.findViewById<TextView>(R.id.txW)
        val textViewTh = listItemView.findViewById<TextView>(R.id.txTh)
        val textViewF = listItemView.findViewById<TextView>(R.id.txF)
        val textViewS = listItemView.findViewById<TextView>(R.id.txS)



        val suState = if (sharedPreferences.getBoolean("${currentItem?.name}StateSu", false))
            "✓"
        else "Su"
        val mState = if (sharedPreferences.getBoolean("${currentItem?.name}StateM", false))
            "✓"
        else "M"
        val tuState = if (sharedPreferences.getBoolean("${currentItem?.name}StateTu", false))
            "✓"
        else "Tu"
        val wState = if (sharedPreferences.getBoolean("${currentItem?.name}StateW", false))
            "✓"
        else "W"
        val thState = if (sharedPreferences.getBoolean("${currentItem?.name}StateTh", false))
            "✓"
        else "Th"
        val fState = if (sharedPreferences.getBoolean("${currentItem?.name}StateF", false))
            "✓"
        else "F"
        val sState = if (sharedPreferences.getBoolean("${currentItem?.name}StateS", false))
            "✓"
        else "S"


        textViewSu.text = suState
        textViewM.text = mState
        textViewTu.text = tuState
        textViewW.text = wState
        textViewTh.text = thState
        textViewF.text = fState
        textViewS.text = sState

        when (suState) {
            "✓" -> textViewSu.setTextColor(Color.GREEN)
            "x" -> textViewSu.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }
        when (mState) {
            "✓" -> textViewM.setTextColor(Color.GREEN)
            "x" -> textViewM.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }
        when (tuState) {
            "✓" -> textViewTu.setTextColor(Color.GREEN)
            "x" -> textViewTu.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }
        when (wState) {
            "✓" -> textViewW.setTextColor(Color.GREEN)
            "x" -> textViewW.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }
        when (thState) {
            "✓" -> textViewTh.setTextColor(Color.GREEN)
            "x" -> textViewTh.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }
        when (fState) {
            "✓" -> textViewF.setTextColor(Color.GREEN)
            "x" -> textViewF.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }
        when (sState) {
            "✓" -> textViewS.setTextColor(Color.GREEN)
            "x" -> textViewS.setTextColor(Color.RED)
            else -> textViewSu.setTextColor(Color.BLACK)
        }


        return listItemView
    }
}