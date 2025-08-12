import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.InstalledApp
import com.belaku.homey.R
import org.w3c.dom.Text

class AppsAdapter(private val dataList: List<InstalledApp>) :
    RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    // ... ViewHolder class and implementation ...

    private lateinit var txAppname: TextView
    private lateinit var txAppIcon: ImageView

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

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: InstalledApp) {
            // Bind data to views in item_grid.xml
            itemView.findViewById<TextView>(R.id.tx_app_name).setText(item.name)
            itemView.findViewById<ImageView>(R.id.imgv_app_icon).setImageDrawable(item.icon)
        }
    }
}