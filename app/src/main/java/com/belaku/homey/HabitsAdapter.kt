package com.belaku.homey

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView
import com.belaku.homey.SetWallWorker.Companion.dayChange
import com.belaku.homey.SetWallWorker.Companion.dayIndex
import com.belaku.homey.SetWallWorker.Companion.rActOpenedFirst
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
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
        checkBox.isChecked = currentItem?.isChecked ?: false

        val textViewSu = listItemView!!.findViewById<TextView>(R.id.txSu)
        val textViewM = listItemView!!.findViewById<TextView>(R.id.txM)
        val textViewTu = listItemView!!.findViewById<TextView>(R.id.txT)
        val textViewW = listItemView!!.findViewById<TextView>(R.id.txW)
        val textViewTh = listItemView!!.findViewById<TextView>(R.id.txTh)
        val textViewF = listItemView!!.findViewById<TextView>(R.id.txF)
        val textViewS = listItemView!!.findViewById<TextView>(R.id.txS)

        var suState: String
        var mState: String
        var tuState: String
        var wState: String
        var thState: String
        var fState: String
        var sState: String

        suState = if (sharedPreferences.getBoolean("${currentItem?.name}StateSu", false))
            "✓"
        else "Su"
        mState = if (sharedPreferences.getBoolean("${currentItem?.name}StateM", false))
            "✓"
        else "M"
        tuState = if (sharedPreferences.getBoolean("${currentItem?.name}StateTu", false))
            "✓"
        else "Tu"
        wState = if (sharedPreferences.getBoolean("${currentItem?.name}StateW", false))
            "✓"
        else "W"
        thState = if (sharedPreferences.getBoolean("${currentItem?.name}StateTh", false))
            "✓"
        else "Th"
        fState = if (sharedPreferences.getBoolean("${currentItem?.name}StateF", false))
            "✓"
        else "F"
        sState = if (sharedPreferences.getBoolean("${currentItem?.name}StateS", false))
            "✓"
        else "S"




        textViewSu.text = suState
        textViewM.text = mState
        textViewTu.text = tuState
        textViewW.text = wState
        textViewTh.text = thState
        textViewF.text = fState
        textViewS.text = sState

        return listItemView
    }
}