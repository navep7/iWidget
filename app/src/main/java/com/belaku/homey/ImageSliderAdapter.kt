package com.belaku.homey

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SetWallWorker.Companion.urls
import com.bumptech.glide.Glide
import retrofit2.http.Url
import kotlin.random.Random


class ImageSliderAdapter(private val imageList: List<String>, private val context: Context) :
    RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.slider_item, parent, false)
     //   makeToast("SZ - ${imageList.size}")
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var imgUrl = imageList[Random.Default.nextInt(imageList.size)]
        imgUrl = imgUrl.split("+ ")[1]

        Glide.with(context)
            .load(imgUrl)
            .override(holder.imageView.width, holder.imageView.height)
            .error(R.drawable.transparent_bg)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById<ImageView>(R.id.image_view_bg)
    }
}