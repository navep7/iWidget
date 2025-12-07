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
import com.belaku.homey.SetWallWorker.Companion.dayChange
import com.belaku.homey.SetWallWorker.Companion.dayIndex
import com.belaku.homey.SetWallWorker.Companion.rActOpenedFirst
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import java.util.Calendar

class RemindersAdapter(context: Context, data: List<Reminder>) :
    ArrayAdapter<Reminder>(context, 0, data) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(
                R.layout.item_reminder, parent, false
            )
        }

        val currentItem = getItem(position)

        val textViewRname = listItemView!!.findViewById<TextView>(R.id.item_text_rname)
        textViewRname.text = "${(position + 1)}. ${currentItem?.name}"
     //   textViewRname.setTextColor(NewAppWidget.primaryColor)

        val textViewRtime = listItemView!!.findViewById<TextView>(R.id.item_text_rtime)
        textViewRtime.text = currentItem?.rTime
    //    textViewRtime.setTextColor(NewAppWidget.tertianaryColor)



        return listItemView
    }
}