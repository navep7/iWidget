import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.InstalledApp
import com.belaku.homey.R
import org.w3c.dom.Text

class AppsAdapter(private val dataList: List<InstalledApp>,
                    private val listener: RvEvent) :
    RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    // ... ViewHolder class and implementation ...

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = dataList.size

    interface RvEvent {
        fun onItemClick(pos: Int)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        fun bind(item: InstalledApp) {
            // Bind data to views in item_grid.xml
            itemView.findViewById<TextView>(R.id.tx_app_name).setText(item.name)
            itemView.findViewById<ImageView>(R.id.imgv_app_icon).setImageDrawable(item.icon)
        }

        init {
            itemView.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }
}