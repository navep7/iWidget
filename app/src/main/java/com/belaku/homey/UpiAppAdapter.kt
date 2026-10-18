package com.belaku.homey

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class UpiAppAdapter(
    context: Context,
    private val apps: List<UpiApp>
) : ArrayAdapter<UpiApp>(context, 0, apps) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_upi_app, parent, false)
        val app = getItem(position)!!

        val imgIcon = view.findViewById<ImageView>(R.id.img_upi_icon)
        val tvName = view.findViewById<TextView>(R.id.tv_upi_name)
        val tvOffers = view.findViewById<TextView>(R.id.tv_upi_offers)

        imgIcon.setImageDrawable(app.icon)
        tvName.text = app.name
        tvOffers.text = "${app.offersCount} Offers available"

        return view
    }
}
