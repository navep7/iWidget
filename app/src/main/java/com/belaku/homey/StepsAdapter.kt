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
            0 -> {
                holder.txSteps.text = "Monday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
            1 -> {
                holder.txSteps.text = "Tuesday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
            2 -> {
                holder.txSteps.text = "Wednesday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
            3 -> {
                holder.txSteps.text = "Thursday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
            4 -> {
                holder.txSteps.text = "Friday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
            5 -> {
                holder.txSteps.text = "Saturday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
            6 -> {
                holder.txSteps.text = "Sunday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else  holder.progressSteps.progress = stepsData[position].toInt() / 10
            }
        }

    }

    override fun getItemCount() = stepsData.size
}