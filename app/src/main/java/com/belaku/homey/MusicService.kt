package com.belaku.homey

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MusicActivity.Companion.pDatalistSongs
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.MusicActivity.Companion.recyclerViewSongs
import com.belaku.homey.MusicActivity.Companion.txPlayingSong
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.isAppWidMInitialized
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.squareup.picasso.Picasso
import okio.IOException


class MusicService : Service() {

    private lateinit var audioManager: AudioManager
    private lateinit var handlerVolume: Handler
    private lateinit var runnableVolume: Runnable
    private lateinit var serviceNotification: Notification
    private lateinit var sendIntent: Intent


    companion object {

        private lateinit var sContext: MusicService


        var boolMusicServiceRunning: Boolean = false

        //  var songsUrlList: ArrayList<String> = ArrayList()
        var songIndex: Int = 0

        var mMediaPlayer: MediaPlayer? = null

        fun notifySong(sIndex: Int) {

            try {
                recyclerViewSongs.scrollToPosition(sIndex)
                txPlayingSong.setText(pDatalistSongs[sIndex].title)
            } catch (ex: Exception) {
                makeToast("EXP updating MusicActivity ~ ${ex.message}")
            }


            sharedPreferencesEditor.putInt("SIn", sIndex).apply()
            appWidM = AppWidgetManager.getInstance(sContext)
            remoteViews =
                RemoteViews(sContext.packageName, com.belaku.homey.R.layout.new_app_widget)
            newAppWidget = ComponentName(sContext, NewAppWidget::class.java)
            remoteViews?.setImageViewResource(
                R.id.imgbtn_playpause,
                R.drawable.pause_m
            )
            remoteViews?.setTextViewText(
                com.belaku.homey.R.id.tx_music_details,
                pDatalistSongs[sIndex].title + " | " + pDatalistSongs[sIndex].album.title + " | " + pDatalistSongs[sIndex].artist.name
            )

            if (isAppWidMInitialized())
                Picasso.get()
                    .load(pDatalistSongs[songIndex].album.cover)
                    .into(remoteViews!!, R.id.imgv_p_album, NewAppWidget.i_appWidgetIds)
            txPlayingSong.setText(pDatalistSongs[sIndex].title)



            appWidM.updateAppWidget(newAppWidget, remoteViews)
            //   serviceNotify(MainActivity.dataList[sIndex].title)
            val intent = Intent(
                sContext,
                MainActivity::class.java
            )
            val pendingIntent = PendingIntent.getActivity(
                sContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )


            val channelId = "some_channel_id"
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(sContext, channelId)
                    .setSilent(true)
                    .setSmallIcon(android.R.drawable.ic_media_play) //                        .setContentTitle(getString(R.string.app_name)
                    .setContentTitle(MusicActivity.pDatalistSongs[sIndex].title)
                    .setContentText(MusicActivity.pDatalistSongs[sIndex].album.title + " | \n" + MusicActivity.pDatalistSongs[sIndex].artist.name)
                    .setAutoCancel(true)
                    .setSound(null)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)

            val notificationManager =
                sContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager


            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_LOW
                )
                checkNotNull(notificationManager)
                notificationManager.createNotificationChannel(channel)
            }

            checkNotNull(notificationManager)
            notificationManager.notify(0, notificationBuilder.build())
        }
    }

    override fun onCreate() {
        super.onCreate()

        boolMusicServiceRunning = true

        if (MusicActivity.isDataListInitialized())
            serviceNotify(pDatalistSongs[songIndex].title)
        //    notifySong(0)
    }


    private fun serviceNotify(str: String) {

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            serviceNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(str)
                .setOngoing(true)
                .setContentText("").build()

            startForeground(1, serviceNotification)
        }
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        sContext = this;
        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        if (intent != null) {

            appWidM = AppWidgetManager.getInstance(sContext)
            remoteViews =
                RemoteViews(
                    applicationContext.packageName,
                    com.belaku.homey.R.layout.new_app_widget
                )
            newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)


            audioManager = sContext.getSystemService(AUDIO_SERVICE) as AudioManager
            //    songsUrlList = intent.getStringArrayListExtra("songsUrl")!!


            /*for (i in 0 until 30) {
                if (intent.extras?.get(i.toString()) != null)
                    songsUrlList.add(intent.extras?.get(i.toString()).toString())
                else break

            }*/


            if (isDataListInitialized()) {

                playSong(0)

            }


            sendIntent = intent


        }

        return START_STICKY
    }


    private fun playSong(index: Int) {
        if (index in 0 until pDatalistSongs.size) {
            songIndex = index
            val url = pDatalistSongs[songIndex].preview

            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setOnCompletionListener {
                        playNextSong()
                    }
                }
            } else {
                mMediaPlayer?.reset() // Reset to the idle state for a new data source
            }

            try {
                mMediaPlayer?.apply {
                    setDataSource(url)
                    prepareAsync()
                    setOnPreparedListener {
                        it.start() // Start playback when prepared
                        notifySong(songIndex)
                   //     trackSeek()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                makeToast("ECXP - ${e.message}")
                playNextSong() // Optionally skip on error
            }
        }
    }

    private fun playNextSong() {
        if (songIndex < pDatalistSongs.size - 1) {
            songIndex++
            playSong(songIndex)
        } else {
            // Handle end of playlist (e.g., stop playback or loop to the beginning)
            // For example, you can release the player and set it to null
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }


    private fun trackSeek() {

        //  makeToast("!trackSeek")

        //  makeToast("!increaseVol ~ ${mMediaPlayer!!.currentPosition}")
        increaseVol()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Code to run after the delay
                //   makeToast("50secs ~ ${mPlayer.currentPosition}")
                //   makeToast("!decreaseVol ~ ${mMediaPlayer!!.currentPosition}")
                reduceVolume()
            }


        }, (mMediaPlayer!!.duration - 5000).toLong())

    }

    private fun increaseVol() {
        val handler = Handler(Looper.getMainLooper())
        val steps = 5

        // Total maximum steps might vary, but this lowers 5 times
        for (i in 0 until steps) {
            handler.postDelayed({
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, // Or STREAM_RING, etc.
                    AudioManager.ADJUST_RAISE, // Decrease
                    0 // Show UI feedback
                )
            }, (i * 300).toLong()) // 300ms delay between steps
        }
    }

    private fun reduceVolume() {
        val handler = Handler(Looper.getMainLooper())
        val steps = 5

        // Total maximum steps might vary, but this lowers 5 times
        for (i in 0 until steps) {
            handler.postDelayed({
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, // Or STREAM_RING, etc.
                    AudioManager.ADJUST_LOWER, // Decrease
                    AudioManager.FLAG_SHOW_UI // Show UI feedback
                )
            }, (i * 300).toLong()) // 300ms delay between steps
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        if (mMediaPlayer != null) {
            mMediaPlayer!!.release(); // Release resources when done
            mMediaPlayer = null;
        }

        songIndex = 0
        sharedPreferencesEditor.putInt("SIn", 0).apply()


        Log.i("OnDestroyMS", "onDestroy: MS OnDestroy called");
    }


}