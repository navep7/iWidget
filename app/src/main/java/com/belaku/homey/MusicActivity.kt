package com.belaku.homey

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.Data
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MainActivity.Companion.parentLayout
import com.belaku.homey.MusicService.Companion.boolMusicServiceRunning
import com.belaku.homey.MusicService.Companion.mMediaPlayer
import com.belaku.homey.MusicService.Companion.songIndex
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.belaku.homey.databinding.ActivityMusicBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import androidx.core.view.isEmpty

class MusicActivity : AppCompatActivity(), MusicAdapter.RecyclerViewEvent {

    private lateinit var musicActivityContext: Context
    private lateinit var json: String
    private lateinit var gson: Gson

    private lateinit var chipGroup: ChipGroup
    private var gotDuration: Boolean = false
    private lateinit var bitmapAlbum: Bitmap
    private lateinit var image: BitmapDrawable

    private lateinit var playIntent: Intent
    private lateinit var fabPlayPause: FloatingActionButton
    private lateinit var playerBg: RelativeLayout
    private lateinit var editTextQuery: EditText
    private lateinit var handlerForBG: Handler

    private lateinit var imgbtnPlayAlbum: ImageButton
    private lateinit var imgbtnFavAlbum: ImageButton


    companion object {
        lateinit var recyclerViewSongs: RecyclerView
        var favAlbums: ArrayList<String> = ArrayList()
        lateinit var dataListSongs: List<Data>
        lateinit var pDatalistSongs: List<Data>
        var searchQuery: String = ""
        var playingAlbum: String = ""
        lateinit var txPlayingSong: TextView

        fun isDataListInitialized(): Boolean {
            return ::dataListSongs.isInitialized
        }
    }

    private lateinit var binding: ActivityMusicBinding


    override
    fun onItemClick(position: Int) {
        makeToast("Yet2Impl")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gson = Gson()

        findViewByIds()


        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        musicActivityContext = applicationContext

        val set = sharedPreferences.getStringSet("favAlbums", null)

        if (set != null) {
            favAlbums = ArrayList(set)

            //  makeToast("onCreate ~ ${favAlbums.size}")
            for (i in favAlbums) {
                addAlbumChip(i)
            }
            if (playingAlbum.isEmpty())
                if (favAlbums.isNotEmpty()) {
                    Getdata(favAlbums[0])
                    chipGroup.getChildAt(0).isSelected = true
                }
        }

        //    makeToast(sharedPreferences.getInt("SIn", 99).toString())

        handlerForBG = Handler(Looper.getMainLooper())
        SetWallWorker.mAct = this


        //    chipArtists()


        parentLayout = findViewById(android.R.id.content);

        if (chipGroup.isEmpty())
            Snackbar.make(parentLayout, "Search for something to PLAY...", Snackbar.LENGTH_LONG)
                .show()




        editTextQuery.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                Toast.makeText(this@MusicActivity, editTextQuery.getText(), Toast.LENGTH_SHORT)
                    .show()
                searchQuery = editTextQuery.getText().toString()
                Getdata(searchQuery)
                handled = true
            }
            handled
        })


        makeToast("boolMusicServiceRunning ~ $boolMusicServiceRunning : $playingAlbum")
        if (boolMusicServiceRunning) {
            //    makeToast(dataList[songIndex].title + " ~ " + query)

            if (isDataListInitialized())
                txPlayingSong.text = pDatalistSongs[songIndex].title

            try {

                if (mMediaPlayer != null)
                    if (mMediaPlayer!!.isPlaying)
                        fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    else fabPlayPause.setImageResource(android.R.drawable.ic_media_play)

            } catch (ex: Exception) {

            }


            for (i in 0 until chipGroup.childCount) {
                var ch = chipGroup.getChildAt(i) as Chip
                if (playingAlbum == ch.text)
                    ch.isSelected = true
                else ch.isSelected = false
            }
            Getdata(playingAlbum)

            fabPlayPause.visibility = View.VISIBLE

        }


    }

    private fun addAlbumChip(i: String) {
        var chip = Chip(this@MusicActivity)
        chip.text = i
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { view ->
            // Remove the chip from the ChipGroup when the close icon is clicked
            chipGroup.removeView(view)
            favAlbums.remove(i)
            saveFavAlbums(favAlbums)

        }
        chip.setOnClickListener {
            for (i in 0 until chipGroup.childCount) {
                var ch = chipGroup.getChildAt(i) as Chip
                if ((it as Chip).text == ch.text)
                    ch.isSelected = true
                else ch.isSelected = false
            }
            Getdata((it as Chip).text.toString())
        }
        chipGroup.addView(chip)
    }

    private fun findViewByIds() {

        chipGroup = findViewById<ChipGroup>(R.id.chip_group)
        imgbtnPlayAlbum = findViewById(R.id.imgbtn_play_album)
        imgbtnFavAlbum = findViewById(R.id.imgbtn_fav_album)
        playerBg = findViewById<RelativeLayout>(R.id.player_bg)
        editTextQuery = findViewById<EditText>(R.id.edtx_query)
        recyclerViewSongs = findViewById<RecyclerView>(R.id.rv)
        fabPlayPause = findViewById<FloatingActionButton>(R.id.fab_play_pause)
        txPlayingSong = findViewById<TextView>(R.id.tx_psong_name)

    }

    private fun chipArtists() {


        val chipOptions = listOf("Coldplay", "Linkin Park", "The Fray")

        chipOptions.forEach { text ->
            val chip = Chip(this).apply {
                // Assign a unique ID to each chip, crucial for single selection to work correctly
                id = ViewCompat.generateViewId()
                this.text = text
                isSelected = false
                isCheckable = true // Makes the chip selectable/checkable

// Make the close icon visible
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    chipGroup.removeView(it)
                }
                // Use a choice or filter chip style for visual feedback when checked
                // style is important for visual consistency
                //    setChipBackgroundColorResource(android.R.color.darker_gray) // Use a color selector
                //    setTextAppearanceResource(android.R.style.TextAppearance_Holo) // Set a valid text appearance
            }
            chipGroup.addView(chip)
        }

        if (!boolMusicServiceRunning)
            chipGroup.check(chipGroup.getChildAt(0).id)


    }


    override fun onDestroy() {
        super.onDestroy()

    }


    fun saveFavAlbums(list: List<String>) {
        val set = list.toHashSet()
        sharedPreferencesEditor.putStringSet("favAlbums", set).apply()
        //     makeToast("saveFavAlbums ~ ${list.size}")
    }


    override fun onResume() {
        super.onResume()

        playIntent = Intent(
            this,
            MusicService::class.java
        )

        val intentIndex = intent.getStringExtra("songIndex")
        if (intentIndex != null) {
            Toast.makeText(applicationContext, "iIn ${intentIndex.toString()}", Toast.LENGTH_LONG).show()
        }

    }


    private fun Getdata(query: String) {

        if (query.isNotEmpty()) {

            if (favAlbums.contains(query.trim()))
                imgbtnFavAlbum.setImageResource(android.R.drawable.star_on)
            else imgbtnFavAlbum.setImageResource(android.R.drawable.star_off)


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

                    if (response.body() != null)
                        if (response.body()?.data != null) {
                            dataListSongs = response.body()?.data!!
                            if (query == playingAlbum)
                                pDatalistSongs = dataListSongs

                            musicActivityContext = applicationContext
                            //     makeToast("MusicData ~ ${dataList.size}")

                            if (dataListSongs.size > 0) {

                                imgbtnPlayAlbum.visibility = View.VISIBLE
                                imgbtnFavAlbum.visibility = View.VISIBLE

                                imgbtnPlayAlbum.setOnClickListener {

                                    playingAlbum = query
                                    pDatalistSongs = dataListSongs

                                    makeToast(query + " ~ " + dataListSongs[0].title)
                                    if (boolMusicServiceRunning) {
                                        stopService(
                                            Intent(
                                                musicActivityContext,
                                                MusicService::class.java
                                            )
                                        )
                                    }

                                    var i = 0
                                    playIntent = Intent(
                                        musicActivityContext,
                                        MusicService::class.java
                                    )
                                    for (item in dataListSongs) {
                                        playIntent.putExtra(i.toString(), item.preview)
                                        i++
                                    }

                                    txPlayingSong.text = dataListSongs[0].title

                                    songIndex = 0

                                    startForegroundService(playIntent)

                                    imgbtnPlayAlbum.setImageResource(android.R.drawable.ic_media_pause)
                                    fabPlayPause.visibility = View.VISIBLE
                                    fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)

                                }

                                imgbtnFavAlbum.setOnClickListener {
                                    if (!favAlbums.contains(query.trim())) {
                                        imgbtnFavAlbum.setImageResource(android.R.drawable.star_on)
                                        favAlbums.add(query.trim())
                                        saveFavAlbums(favAlbums)
                                        addAlbumChip(query.trim())
                                    } else {
                                        imgbtnFavAlbum.setImageResource(android.R.drawable.star_off)
                                        favAlbums.remove(query.trim())
                                        for (i in 0 until chipGroup.childCount) {
                                            var ch = chipGroup.getChildAt(i) as Chip
                                            if (query.trim() == ch.text)
                                                chipGroup.removeView(ch)
                                        }
                                    }
                                }


                                fabPlayPause.setOnClickListener(View.OnClickListener {

                                    if (mMediaPlayer != null)
                                        if (mMediaPlayer!!.isPlaying) {
                                            mMediaPlayer!!.pause()
                                            remoteViews?.setImageViewResource(
                                                com.belaku.homey.R.id.imgbtn_playpause,
                                                R.drawable.play_m
                                            )
                                            appWidM.updateAppWidget(newAppWidget, remoteViews)
                                            fabPlayPause.setImageResource(android.R.drawable.ic_media_play)
                                        } else {
                                            mMediaPlayer!!.play()
                                            remoteViews?.setImageViewResource(
                                                com.belaku.homey.R.id.imgbtn_playpause,
                                                R.drawable.pause_m
                                            )
                                            appWidM.updateAppWidget(newAppWidget, remoteViews)
                                            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause)

                                        }


                                })
                            }




                            for (item in dataListSongs)
                                Log.d("DATA7", "p - " + item.preview + "\n l - " + item.link)


                            var rvAdapter = MusicAdapter(dataListSongs, this@MusicActivity)
                            recyclerViewSongs.adapter = rvAdapter
                            //    recyclerview.layoutManager = LinearLayoutManager(this@MainActivity)
                            recyclerViewSongs.setLayoutManager(
                                LinearLayoutManager(
                                    this@MusicActivity,
                                    LinearLayoutManager.HORIZONTAL, false
                                )
                            )
                            recyclerViewSongs.scrollToPosition(songIndex)

                            //     recyclerview.scrollToPosition(sharedPreferences.getInt("SIn", 0))
                        } else Toast.makeText(
                            applicationContext,
                            "DeeZerDOWN, maybe!",
                            Toast.LENGTH_LONG
                        ).show()
                    else Toast.makeText(applicationContext, "DeeZerDOWN, maybe!", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onFailure(call: Call<MusicData?>, t: Throwable) {
                    Toast.makeText(applicationContext, "not Found", Toast.LENGTH_LONG).show()
                }

            })

        }
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


        Log.d("sInfomr", dataListSongs[what].title + " ~ $what")
        recyclerViewSongs.scrollToPosition(what)
        //    findViewById<TextView>(R.id.tx_sname).text = MusicActivity.Companion.dataList[what].title
        val thread = Thread {
            try {
                // Your code goes here
                val url = URL(MusicActivity.Companion.dataListSongs[what].album.cover)
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


}

