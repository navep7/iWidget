package com.belaku.homey

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView

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




        return listItemView
    }
}