package com.belaku.homey

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class StepsAdapter(private val stepsData: ArrayList<String>) : RecyclerView.Adapter<StepsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        return StepsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.steps_list_item, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {
        when(position) {
            0 -> holder.cheeseName.text = "Monday \n ${stepsData[position]}"
            1 -> holder.cheeseName.text = "Tuesday \n ${stepsData[position]}"
            2 -> holder.cheeseName.text = "Wednesday \n ${stepsData[position]}"
            3 -> holder.cheeseName.text = "Thursday \n ${stepsData[position]}"
            4 -> holder.cheeseName.text = "Friday \n ${stepsData[position]}"
            5 -> holder.cheeseName.text = "Saturday \n ${stepsData[position]}"
            6 -> holder.cheeseName.text = "Sunday \n ${stepsData[position]}"
        }

    }

    override fun getItemCount() = stepsData.size
}