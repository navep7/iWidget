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

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    // Using a large number to simulate infinite scrolling
    private val MAX_COUNT = 7000 

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        return StepsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.steps_list_item, parent, false),
        )
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {
        if (stepsData.isEmpty()) return

        val realPosition = position % stepsData.size
        val steps = stepsData[realPosition].trim().toIntOrNull() ?: 0

        // Update widget if it's today
        val currentDayIndex = (LocalDate.now().dayOfWeek.value + 6) % 7 // Monday = 0
        if (realPosition == currentDayIndex) {
            stepsToday = sharedPreferences.getInt(LocalDate.now().dayOfWeek.name, 0)
            remoteViews?.setTextViewText(R.id.tx_steps, "$stepsToday")
            appWidM.updateAppWidget(newAppWidget, remoteViews)
        }

        val km = if (steps != 0) String.format("%.1f", (steps * 74f) / 100000f) else "0"
        val kCal = (steps * 0.04 * (80 / 70)).toInt()

        holder.txTitle.text = days[realPosition]
        holder.txSteps.text = "$steps steps\n~ $km km\n~ $kCal kCal"

        // Material-like progress
        holder.progressSteps.max = 10000 // Standard daily goal
        holder.progressSteps.progress = steps
    }

    override fun getItemCount(): Int = if (stepsData.isEmpty()) 0 else MAX_COUNT
}