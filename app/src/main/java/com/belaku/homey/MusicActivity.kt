package com.belaku.homey

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.Data
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MusicService.Companion.mediaPlayer
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.databinding.ActivityMusicBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

class MusicActivity : AppCompatActivity(), MusicAdapter.RecyclerViewEvent {

    private lateinit var player: MediaPlayer
    private var gotDuration: Boolean = false
    private lateinit var bitmapAlbum: Bitmap
    private lateinit var image: BitmapDrawable
    private lateinit var handlerSongInfo: Handler
    private lateinit var handlerSeekInfo: Handler
    private lateinit var playIntent: Intent
    private var songs: ArrayList<String> = ArrayList()
    private lateinit var query: String
    private lateinit var fabPlayPause: FloatingActionButton
    private lateinit var recyclerview: RecyclerView
    private lateinit var playerBg: RelativeLayout
    private lateinit var editTextQuery: EditText
    private var arraylistArtists = ArrayList<String>()
    private lateinit var handlerForBG: Handler

    companion object {
        lateinit var dataList: List<Data>

        fun isDataListInitialized(): Boolean {
            return ::dataList.isInitialized
        }
    }


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMusicBinding


    override
    fun onItemClick(position: Int) {
        makeToast("Yet2Impl")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //    makeToast(sharedPreferences.getInt("SIn", 99).toString())

        handlerForBG = Handler(Looper.getMainLooper())





        chipArtists()


        playerBg = findViewById<RelativeLayout>(R.id.player_bg)
        editTextQuery = findViewById<EditText>(R.id.edtx_query)
        recyclerview = findViewById<RecyclerView>(R.id.rv)
        fabPlayPause = findViewById<FloatingActionButton>(R.id.fab_play_pause)


        if (isMyMusicServiceRunning(MusicService::class.java))
            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        else fabPlayPause.setImageResource(android.R.drawable.ic_media_play)


        query = "Coldplay"
        editTextQuery.setText(query)
        Getdata()

        editTextQuery.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                Toast.makeText(this@MusicActivity, editTextQuery.getText(), Toast.LENGTH_SHORT)
                    .show()
                query = editTextQuery.getText().toString()
                Getdata()
                handled = true
            }
            handled
        })






        if (isMyMusicServiceRunning(MusicService::class.java)) {

            //    Toast.makeText(applicationContext, dataList[songIndex].title, Toast.LENGTH_LONG).show()
            //  recyclerview.scrollToPosition(songIndex)

        }



    }

    private fun chipArtists() {

        val chipGroup = findViewById<ChipGroup>(R.id.chip_group)
        val chipOptions = listOf("Coldplay", "Linkin Park", "The Fray", "XYZ")

        chipOptions.forEach { text ->
            val chip = Chip(this).apply {
                // Assign a unique ID to each chip, crucial for single selection to work correctly
                id = ViewCompat.generateViewId()
                this.text = text
                isSelected = false
                isCheckable = true // Makes the chip selectable/checkable
                // Use a choice or filter chip style for visual feedback when checked
                // style is important for visual consistency
            //    setChipBackgroundColorResource(android.R.color.darker_gray) // Use a color selector
            //    setTextAppearanceResource(android.R.style.TextAppearance_Holo) // Set a valid text appearance
            }
            chipGroup.addView(chip)
        }

        chipGroup.check(chipGroup.getChildAt(0).id)


        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // In single selection mode, checkedIds list will contain only one ID (or none if deselection is possible)
            val selectedChipId = checkedIds.firstOrNull()
            if (selectedChipId != null) {
                val selectedChip = findViewById<Chip>(selectedChipId)
                // Perform actions with the selected chip (e.g., fetch data, update UI)
             //   makeToast(selectedChip.text.toString())
                for (i in 0 until chipGroup.childCount) {
                    val chip = chipGroup.getChildAt(i) as Chip
                    val chipText = chip.text.toString()
                    // Do something with the chip or its text
                    if (chipText == selectedChip.text)
                        chip.isSelected = true
                    else chip.isSelected = false
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        if (MusicService.isMediaPlayerInitialized())
            try {
                if (!mediaPlayer.isPlaying) {
                    val myService = Intent(
                        this@MusicActivity,
                        MusicService::class.java
                    )
                    songIndex = 0
                    stopService(myService)
                }
            } catch (e: IllegalStateException) {
                // Handle the exception, potentially logging or resetting the player
                e.printStackTrace()
            }
    }


    override fun onResume() {
        super.onResume()

        playIntent = Intent(
            this,
            MusicService::class.java
        )
    }


    private fun Getdata() {
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)

        val retrofitData = retrofitBuilder.getDate(query)

        retrofitData.enqueue(object : Callback<MusicData?> {
            override fun onResponse(
                call: Call<MusicData?>,
                response: Response<MusicData?>
            ) {
                dataList = response.body()?.data!!

                appContx = applicationContext
                makeToast("MusicData ~ ${dataList.size}")

                if (dataList.size > 0) {
                    fabPlayPause.visibility = View.VISIBLE


                    fabPlayPause.setOnClickListener(View.OnClickListener {
                        if (!isMyMusicServiceRunning(MusicService::class.java)) {
                            var i: Int = 0;
                            for (item in songs) {
                                playIntent.putExtra(i.toString(), item)
                                i++
                            }

                            handlerSongInfo = @SuppressLint("HandlerLeak")
                            object : Handler(Looper.getMainLooper()) {
                                override fun handleMessage(msg: Message) {
                                    updateUI(msg.what)
                                }
                            }



                            playIntent.putExtra("songInfo", Messenger(handlerSongInfo));

                            //    updateUI(0)
                            startForegroundService(playIntent)
                            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)

                        } else {

                            if (MusicService.isMediaPlayerInitialized()) {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                    remoteViews?.setImageViewResource(
                                        com.belaku.homey.R.id.imgbtn_playpause,
                                        android.R.drawable.ic_media_play
                                    )
                                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                                    fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
                                } else {
                                    mediaPlayer.start()
                                    remoteViews?.setImageViewResource(
                                        com.belaku.homey.R.id.imgbtn_playpause,
                                        android.R.drawable.ic_media_pause
                                    )
                                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                                    fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)

                                }
                            }

                        }


                    })
                }


                songs.clear()
                for (item in dataList)
                    songs.add(item.preview)

                for (item in dataList)
                    Log.d("DATA7", "p - " + item.preview + "\n l - " + item.link)


                var rvAdapter = MusicAdapter(dataList, this@MusicActivity)
                recyclerview.adapter = rvAdapter
                //    recyclerview.layoutManager = LinearLayoutManager(this@MainActivity)
                recyclerview.setLayoutManager(
                    LinearLayoutManager(
                        this@MusicActivity,
                        LinearLayoutManager.HORIZONTAL, false
                    )
                )

           //     recyclerview.scrollToPosition(sharedPreferences.getInt("SIn", 0))
            }

            override fun onFailure(call: Call<MusicData?>, t: Throwable) {
                Toast.makeText(applicationContext, "not Found", Toast.LENGTH_LONG).show()
            }

        })

    }

    private fun updateSeek(what: Int) {

        if (!gotDuration) {
            gotDuration = true
            //   seekBar.max = what
        } else {
            //  seekBar.setProgress(what, true)
        }
    }

    private fun updateUI(what: Int) {


        Log.d("sInfomr", dataList[what].title + " ~ $what")
        recyclerview.scrollToPosition(what)
        //    findViewById<TextView>(R.id.tx_sname).text = MusicActivity.Companion.dataList[what].title
        val thread = Thread {
            try {
                // Your code goes here
                val url = URL(MusicActivity.Companion.dataList[what].album.cover)
                bitmapAlbum = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                image = BitmapDrawable(applicationContext.getResources(), bitmapAlbum)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.d("updateUI exception - ", e.toString())
            }
        }

        thread.start()


        if (::image.isInitialized)
            handlerForBG.postDelayed(Runnable { playerBg.background = image }, 1000)
    }

    private fun isMyMusicServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

