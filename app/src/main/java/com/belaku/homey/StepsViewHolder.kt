package com.belaku.homey

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StepsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val txSteps: TextView = view.findViewById(R.id.txsteps)
    var progressSteps: ProgressBar = view.findViewById(R.id.progress_steps)
}