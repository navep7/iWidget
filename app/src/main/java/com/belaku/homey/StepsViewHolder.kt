package com.belaku.homey

import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StepsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val txTitle: TextView = view.findViewById(R.id.tx_title)
    val txSteps: TextView = view.findViewById(R.id.txsteps)
    var progressSteps: ProgressBar = view.findViewById(R.id.progress_steps)
    val imgbtnTripInfo: ImageButton = view.findViewById(R.id.imgbtn_trip_info)
}