package com.belaku.homey

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class PaymentHistoryAdapter(context: Context, data: List<PaymentHistory>) :
    ArrayAdapter<PaymentHistory>(context, 0, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_payment_history, parent, false
        )

        val item = getItem(position)

        val tvAmount = view.findViewById<TextView>(R.id.tv_amount)
        val tvNote = view.findViewById<TextView>(R.id.tv_note)
        val tvTimestamp = view.findViewById<TextView>(R.id.tv_timestamp)

        tvAmount.text = "₹${item?.amount}"
        tvNote.text = item?.note
        tvTimestamp.text = item?.timestamp

        return view
    }
}