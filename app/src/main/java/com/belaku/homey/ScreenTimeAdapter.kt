package com.belaku.homey

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator

class ScreenTimeAdapter(private val appUsageList: List<AppUsage>, private val maxUsage: Int) :
    RecyclerView.Adapter<ScreenTimeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAppIcon: ImageView = view.findViewById(R.id.img_app_icon)
        val txAppName: TextView = view.findViewById(R.id.tx_app_name)
        val txUsageTime: TextView = view.findViewById(R.id.tx_usage_time)
        val progressUsage: LinearProgressIndicator = view.findViewById(R.id.progress_usage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screen_time, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appUsageList[position]
        holder.txAppName.text = item.appName
        
        val timeParts = item.usageTime.split(":")
        val mins = timeParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val secs = timeParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        
        holder.txUsageTime.text = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

        holder.progressUsage.progress = if (maxUsage > 0) (mins * 100) / maxUsage else 0
        
        // Reset to default icon first to handle view recycling and provide a fallback
        holder.imgAppIcon.setImageResource(R.drawable.launch)


            holder.txAppName.text = item.appName


            // 1. Reset the icon to a placeholder to handle recycling
            holder.imgAppIcon.setImageResource(R.drawable.launch)

            val pkg = item.packageName?.trim()
            if (!pkg.isNullOrEmpty()) {

                item.packageName?.let { pkg ->

                    holder.imgAppIcon.post {
                        try {
                            val icon = holder.itemView.context.packageManager.getApplicationIcon(pkg)
                            holder.imgAppIcon.setImageDrawable(icon)
                        } catch (e: Exception) { }
                    }
                }
            }

    }

    override fun getItemCount() = appUsageList.size
}
