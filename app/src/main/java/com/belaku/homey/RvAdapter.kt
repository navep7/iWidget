package com.belaku.homey

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy


class RvAdapter internal constructor(
    private val context: Context?,
    private val mUrls: List<String>,
    private val mDescs: List<String>
) :
    RecyclerView.Adapter<RvAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.rv_row, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    //    val desc = mDescs[position]
        var url = mUrls[position]
    //    holder.ryTextView.text = desc
        url = url.split("+ ")[1]

        Log.d("SSver7", url)

        holder.rvImgv.setImageURI(Uri.parse(url))

        Glide.with(context!!)
            .load(url)
            .into(holder.rvImgv)

    }

    // total number of rows
    override fun getItemCount(): Int {
        return mUrls.size
    }


    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var rvImgv: ImageView = itemView.findViewById(R.id.rv_imgv)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
            MainActivity.showSelected(adapterPosition)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): String {
        return mDescs[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        this.mClickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }
}
