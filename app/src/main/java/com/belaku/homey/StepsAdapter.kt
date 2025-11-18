package com.belaku.homey

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.DialogActivity.Companion.isStepsMapsInitialized
import com.belaku.homey.DialogActivity.Companion.stepsMaps
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class StepsAdapter(
    private val stepsData: ArrayList<String>,
    private val stepsLocInfo: ArrayList<LatLng>
) : RecyclerView.Adapter<StepsViewHolder>() {
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
                addMarker(stepsLocInfo[position], "Monday", "Sydney")
                holder.txSteps.text = "Monday \n ${stepsData[position]} steps..."

                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

            1 -> {
                stepsMaps.clear()
                addMarker(stepsLocInfo[position], "Tuesday", "Tokyo")
                holder.txSteps.text = "Tuesday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

            2 -> {
                stepsMaps.clear()
                addMarker(stepsLocInfo[position], "Wednesday", "Mumbai")
                holder.txSteps.text = "Wednesday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

            3 -> {
                stepsMaps.clear()
                addMarker(stepsLocInfo[position], "Thursday", "Mexico")
                holder.txSteps.text = "Thursday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

            4 -> {
                stepsMaps.clear()
                addMarker(stepsLocInfo[position], "Friday", "Berlin")
                holder.txSteps.text = "Friday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

            5 -> {
                stepsMaps.clear()
                addMarker(stepsLocInfo[position], "Saturday", "Brazil")
                holder.txSteps.text = "Saturday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

            6 -> {
                stepsMaps.clear()
                addMarker(stepsLocInfo[position], "Sunday", "NewYork")
                holder.txSteps.text = "Sunday \n ${stepsData[position]} steps..."
                if (stepsData[position].toInt() > 1000)
                    holder.progressSteps.progress = stepsData[position].toInt() / 100
                else holder.progressSteps.progress = stepsData[position].toInt() / 10
            }

        }

    }

    private fun addMarker(markerLocationn: LatLng, mTitle: String, mDesc: String) {

        val markerLocation = LatLng(37.7749, -122.4194)
        val marker = stepsMaps.addMarker(
            MarkerOptions()
                .position(markerLocation)
                .title(mTitle)
                .snippet(mDesc)
        )

        // Show the info window for the marker immediately
        marker?.showInfoWindow()

        stepsMaps.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                markerLocation,
                19f
            )
        )
    }

    override fun getItemCount() = stepsData.size
}