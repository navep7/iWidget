import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.ColorUtil
import com.belaku.homey.InstalledApp
import com.belaku.homey.NewAppWidget
import com.belaku.homey.NewAppWidget.Companion.primaryColor
import com.belaku.homey.NewAppWidget.Companion.tertianaryColor
import com.belaku.homey.R
import okio.blackholeSink
import org.w3c.dom.Text

class AppsAdapter(private val dataList: List<InstalledApp>,
                    private val listener: RvEvent) :
    RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    // ... ViewHolder class and implementation ...

    private lateinit var contx: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        contx = parent.context
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

            try {
                if (!ColorUtil().isColorDark(primaryColor))
                itemView.findViewById<TextView>(R.id.tx_app_name)
                    .setTextColor(contx.getColor(R.color.black))
                else itemView.findViewById<TextView>(R.id.tx_app_name)
                    .setTextColor(contx.getColor(R.color.white))

            } catch (ex: Exception) {

            }
        //    itemView.findViewById<TextView>(R.id.tx_app_name).setTextColor(tertianaryColor)
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