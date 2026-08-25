package com.belaku.homey

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import java.time.LocalDate


class StepsAdapter(
    private val stepsData: ArrayList<String>,
) : RecyclerView.Adapter<StepsViewHolder>() {
    private var km: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        return StepsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.steps_list_item, parent, false),
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {

        stepsToday = sharedPreferences.getInt(LocalDate.now().dayOfWeek.name, 0)
        remoteViews?.setTextViewText(R.id.txsteps, "$stepsToday Steps")
        appWidM.updateAppWidget(newAppWidget, remoteViews)


            when (position) {


                0 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"

                    holder.txTitle.text = "Monday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

                1 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txTitle.text = "Tuesday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

                2 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txTitle.text = "Wednesday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

                3 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txTitle.text = "Thursday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

                4 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txTitle.text = "Friday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

                5 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txTitle.text = "Saturday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

                6 -> {

                    if (stepsData[position].trim().toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txTitle.text = "Sunday!"
                    holder.txSteps.text = "${stepsData[position]} steps... \n ~ $km km! \n ~ " + (stepsData[position].toInt() * 0.04 * (80 / 70)).toInt().toString() + " kCal"
                    if (stepsData[position].trim().toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].trim().toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].trim().toInt() / 10
                }

            }

    }



    override fun getItemCount() = stepsData.size
}