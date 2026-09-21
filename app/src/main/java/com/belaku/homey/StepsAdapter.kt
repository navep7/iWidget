package com.belaku.homey

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.Constants.Companion.stepsToday
import com.belaku.homey.DialogActivity.Companion.dialogIntentStr
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.StepsService.Companion.presentActivityState
import java.time.LocalDate

class StepsAdapter(
    strType: String,
    private val stepsData: ArrayList<String>,
) : RecyclerView.Adapter<StepsViewHolder>() {

    var strT = strType
    lateinit var contx: Context
    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    // Using a large number to simulate infinite scrolling
    private val MAX_COUNT = 7000 

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        contx = parent.context
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
        
        val dayKey = days[realPosition].uppercase()
        var durationMillis = sharedPreferences.getLong(dayKey + "_walk_duration", 0L)

        if (realPosition == currentDayIndex) {
            stepsToday = sharedPreferences.getInt(LocalDate.now().dayOfWeek.name, 0)
            remoteViews?.setTextViewText(R.id.tx_act_count, "$stepsToday")
            appWidM.updateAppWidget(newAppWidget, remoteViews)
            
            if (presentActivityState == "WALKING") {
                val baseTime = sharedPreferences.getLong("walkChr", 0L)
                if (baseTime != 0L) {
                    // Show sum of all walking streaks in a day by adding current streak to the stored total
                    durationMillis += (SystemClock.elapsedRealtime() - baseTime)
                }
            }
        }

        val km = if (steps != 0) String.format("%.1f", (steps * 74f) / 100000f) else "0"
        val kCal = (steps * 0.04 * (80 / 70)).toInt()
        val durationStr = formatDuration(durationMillis)

        holder.txTitle.text = days[realPosition]
       // makeToast(contx, "StA ~ " + strT)
        if (strT == "walk")
        holder.txSteps.text = "$steps steps\n~ $km km\n~ $kCal kCal\nActive: $durationStr"
        else holder.txSteps.text = "Max ~ ${stepsData[realPosition]} KmpH"

        // Material-like progress
        holder.progressSteps.max = 10000 // Standard daily goal
        holder.progressSteps.progress = steps
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun getItemCount(): Int = if (stepsData.isEmpty()) 0 else MAX_COUNT
}
