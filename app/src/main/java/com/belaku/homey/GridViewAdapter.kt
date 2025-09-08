package com.belaku.homey


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class GridViewAdapter(
    context: Context,
    list: ArrayList<SelectedApp>
) : ArrayAdapter<SelectedApp?>(context, 0, list as List<SelectedApp?>) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {

        var itemView = view
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false)
        }

        val model: SelectedApp? = getItem(position)
        val textView = itemView!!.findViewById<TextView>(R.id.text_view)
        val imageView = itemView.findViewById<ImageView>(R.id.image_view)

        textView.text = model!!.name
        imageView.setImageBitmap(model.icon)
        return itemView
    }
}