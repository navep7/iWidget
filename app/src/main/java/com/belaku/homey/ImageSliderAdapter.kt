package com.belaku.homey

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.SetWallWorker.Companion.urls
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import retrofit2.http.Url
import java.security.MessageDigest
import kotlin.random.Random


class ImageSliderAdapter(private val imageList: List<String>, private val context: Context) :
    RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.slider_item, parent, false)
     //   // makeToast("SZ - ${imageList.size}")
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var imgUrl = imageList[Random.Default.nextInt(imageList.size)]
        imgUrl = imgUrl.split("+ ")[1]

     //   if (position != 0)
        Glide.with(context)
            .asBitmap()
            .load(imgUrl)
            .override(holder.imageView.width, holder.imageView.height)
            .transform(ThinFilmTransformation(NewAppWidget.primaryColor, 100))
            .thumbnail(Glide.with(context).asBitmap().load(R.drawable.loading_gif))
            .error(R.drawable.transparent_bg)
            .into(holder.imageView)
        
        
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById<ImageView>(R.id.image_view_bg)
    }

    class ThinFilmTransformation(private val filmColor: Int, private val filmAlpha: Int) : BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(("ThinFilmTransformation_" + filmColor + "_" + filmAlpha).toByteArray(Charsets.UTF_8))
        }

        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
            val resultBitmap = pool.get(toTransform.width, toTransform.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(toTransform, 0f, 0f, null)

            val paint = Paint()
            paint.color = filmColor
            paint.alpha = filmAlpha

            canvas.drawRect(
                0f,
                0f,
                toTransform.width.toFloat(),
                toTransform.height.toFloat(),
                paint
            )

            return resultBitmap
        }
    }
}