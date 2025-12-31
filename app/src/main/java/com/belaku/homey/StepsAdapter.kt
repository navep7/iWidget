package com.belaku.homey

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.DialogActivity.Companion.isStepsMapsInitialized
import com.belaku.homey.DialogActivity.Companion.stepsMaps
import com.belaku.homey.MainActivity.Companion.makeToast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class StepsAdapter(
    private val stepsData: ArrayList<String>,
    private val stepsLocInfo: ArrayList<LatLng>
) : RecyclerView.Adapter<StepsViewHolder>() {
    private var km: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        return StepsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.steps_list_item, parent, false),
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {


        if (isStepsMapsInitialized())
            when (position) {
                0 -> {
                    stepsMaps.clear()
                    if (stepsData[position].toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Monday \n ${stepsData[position]} steps... \n ~ $km km!"

                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

                1 -> {
                    stepsMaps.clear()
                    if (stepsData[position].toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Tuesday \n ${stepsData[position]} steps... \n ~ $km km!"
                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

                2 -> {
                    stepsMaps.clear()
                    if (stepsData[position].toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Wednesday \n ${stepsData[position]} steps... \n ~ $km km!"
                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

                3 -> {
                    stepsMaps.clear()
                    if (stepsData[position].toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Thursday \n ${stepsData[position]} steps... \n ~ $km km!"
                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

                4 -> {
                    stepsMaps.clear()
                    if (stepsData[position].toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Friday \n ${stepsData[position]} steps... \n ~ $km km!"
                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

                5 -> {
                    stepsMaps.clear()
                    if (stepsData[position].toInt() != 0)
                        km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Saturday \n ${stepsData[position]} steps... \n ~ $km km!"
                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

                6 -> {
                    stepsMaps.clear()
                    if (Integer.parseInt(stepsData[position]) != 0)
                     km = String.format("%.1f",  (Integer.parseInt(stepsData[position]) * 74f) / 100000f)
                    else km = "0"
                    holder.txSteps.text = "Sunday \n ${stepsData[position]} steps... \n ~ $km km!"
                    if (stepsData[position].toInt() > 1000)
                        holder.progressSteps.progress = stepsData[position].toInt() / 100
                    else holder.progressSteps.progress = stepsData[position].toInt() / 10
                }

            }

    }



    override fun getItemCount() = stepsData.size
}