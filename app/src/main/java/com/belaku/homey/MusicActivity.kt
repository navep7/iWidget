package com.belaku.homey

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.Data
import com.belaku.homey.databinding.ActivityMusicBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

class MusicActivity : AppCompatActivity(), MusicAdapter.RecyclerViewEvent {

    private var songIndex: Int = 0
    private lateinit var player: MediaPlayer
    private var gotDuration: Boolean = false
    private lateinit var bitmapAlbum: Bitmap
    private lateinit var image: BitmapDrawable
    private lateinit var handlerSongInfo: Handler
    private lateinit var handlerSeekInfo: Handler
    private lateinit var playIntent: Intent
    private var songs: ArrayList<String> = ArrayList()
    private lateinit var query: String
    private lateinit var btnPlayPause: ImageButton
    private lateinit var imgbtnPlay: ImageButton
    private lateinit var recyclerview: RecyclerView
    private lateinit var playerBg: RelativeLayout
    private lateinit var editTextQuery: EditText
    private lateinit var seekBar: SeekBar
    private var arraylistArtists = ArrayList<String>()
    private lateinit var handlerForBG: Handler

    companion object {
        lateinit var dataList: List<Data>
    }


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMusicBinding


    override
    fun onItemClick(position: Int) {
        val sData = MusicActivity.Companion.dataList[position]

        try {
            findViewById<TextView>(R.id.tx_songname).text = MusicActivity.Companion.dataList[songIndex].title
            val uri = Uri.parse(MusicActivity.Companion.dataList.get(songIndex++).preview)
            player = MediaPlayer()
            player.setAudioStreamType(AudioManager.STREAM_MUSIC)
            player.setDataSource(this, uri)
            player.prepare()
            player.start()
        } catch (e: Exception) {
            println(e.toString())
            Toast.makeText(applicationContext, "P ex - " + e, Toast.LENGTH_LONG).show()
        }

        //  player.setOnCompletionListener(this)

        val thread = Thread {
            try {
                // Your code goes here
                val url = URL(sData.album.cover)
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                image = BitmapDrawable(applicationContext.getResources(), bitmap)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
            }
        }

        thread.start()


        handlerForBG.postDelayed(Runnable {  playerBg.background = image }, 1000)


        Toast.makeText(
            this,
            sData.title,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)


        arraylistArtists.add("Linkin Park")
        arraylistArtists.add("Coldplay")
        arraylistArtists.add("Sanjith Hegde")

        handlerForBG = Handler()

        seekBar = findViewById(R.id.seekbar)
        playerBg = findViewById<RelativeLayout>(R.id.player_bg)
        editTextQuery = findViewById<EditText>(R.id.edtx_query)
        editTextQuery.setInputType(InputType.TYPE_CLASS_TEXT)
        recyclerview = findViewById<RecyclerView>(R.id.rv)
        imgbtnPlay = findViewById<ImageButton>(R.id.imgbtn_play)
        btnPlayPause = findViewById<ImageButton>(R.id.imgbtn_playpause)

        if (isMyMusicServiceRunning(MusicService::class.java)) {
            imgbtnPlay.setImageResource(android.R.drawable.ic_media_pause)
        }


        query = "Coldplay"
        editTextQuery.setText(query)
        Getdata()

        editTextQuery.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                Toast.makeText(this@MusicActivity, editTextQuery.getText(), Toast.LENGTH_SHORT).show()
                query = editTextQuery.getText().toString()
                Getdata()
                handled = true
            }
            handled
        })



        btnPlayPause.setOnClickListener(View.OnClickListener {

        })


        imgbtnPlay.setOnClickListener(View.OnClickListener {

            if(!isMyMusicServiceRunning(MusicService::class.java)) {
                imgbtnPlay.setImageResource(android.R.drawable.ic_media_pause)
                seekBar.thumb = resources.getDrawable(android.R.drawable.ic_media_pause)

                var i: Int = 0;
                for (item in songs) {
                    playIntent.putExtra(i.toString(), item)
                    i++
                }

                handlerSongInfo = object : Handler() {
                    override fun handleMessage(msg: Message) {
                        updateUI(msg.what)
                    }
                }

                handlerSeekInfo = object : Handler() {
                    override fun handleMessage(msg: Message) {
                        updateSeek(msg.what)
                    }
                }

                playIntent.putExtra("songInfo", Messenger(handlerSongInfo));
                playIntent.putExtra("seekInfo", Messenger(handlerSeekInfo))

                //    updateUI(0)
                startForegroundService(playIntent)

            } else {
                imgbtnPlay.setImageResource(android.R.drawable.ic_media_play)
                seekBar.thumb = resources.getDrawable(android.R.drawable.ic_media_play)
                val myService = Intent(
                    this@MusicActivity,
                    MusicService::class.java
                )
                stopService(myService)
            }

        })




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
                        LinearLayoutManager.HORIZONTAL,false
                    )
                )

            }

            override fun onFailure(call: Call<MusicData?>, t: Throwable) {
                Toast.makeText(applicationContext, "not Found", Toast.LENGTH_LONG).show()
            }

        })

    }

    private fun updateSeek(what: Int) {

        if (!gotDuration) {
            gotDuration = true
            seekBar.max = what
        } else {
            seekBar.setProgress(what, true)
        }
    }

    private fun updateUI(what: Int) {

        recyclerview.scrollToPosition(what)
        findViewById<TextView>(R.id.tx_songname).text = MusicActivity.Companion.dataList[what].title
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


        handlerForBG.postDelayed(Runnable {  playerBg.background = image }, 1000)
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

