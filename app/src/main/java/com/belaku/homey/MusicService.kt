package com.belaku.homey

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.belaku.homey.MusicActivity.Companion.dataListSongs
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.MusicActivity.Companion.pDatalistSongs
import com.belaku.homey.MusicActivity.Companion.recyclerViewSongs
import com.belaku.homey.MusicActivity.Companion.txPlayingSong
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.isAppWidMInitialized
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import com.squareup.picasso.Picasso


class MusicService : Service() {

    private var mediaItems: ArrayList<MediaItem> = ArrayList()
    private lateinit var audioManager: AudioManager
    private lateinit var handlerVolume: Handler
    private lateinit var runnableVolume: Runnable
    private lateinit var serviceNotification: Notification
    private lateinit var sendIntent: Intent
    private var volumeAnimator: ValueAnimator? = null


    companion object {

        private var sIndex: Int = 0
        private lateinit var sContext: MusicService


        var boolMusicServiceRunning: Boolean = false

        //  var songsUrlList: ArrayList<String> = ArrayList()
        var songIndex: Int = 0

        var mMediaPlayer: ExoPlayer? = null

        fun notifySong(mediaItem: MediaItem) {

            var mediaMetadata = mediaItem.mediaMetadata

            try {
                for (i in 0 until pDatalistSongs.size)
                    if (pDatalistSongs[i].title == mediaMetadata.title) {
                        sIndex = i
                        recyclerViewSongs.scrollToPosition(i)
                    }
                txPlayingSong.text = mediaMetadata.title
            } catch (ex: Exception) {
                // makeToast("EXP updating MusicActivity ~ ${ex.message}")
            }


            //     sharedPreferencesEditor.putInt("SIn", sIndex).apply()
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
                mediaMetadata.title.toString() + " | " + mediaMetadata.albumTitle + " | " + mediaMetadata.artist
            )

        //    if (isAppWidMInitialized() && mediaMetadata.artworkUri != null)
                Picasso.get()
                    .load(dataListSongs[songIndex].album.cover)
                    .into(remoteViews!!, R.id.imgbtn_albumcover, NewAppWidget.i_appWidgetIds)
            txPlayingSong.text = mediaMetadata.title



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
                    .setContentTitle(mediaMetadata.title)
                    .setContentText(mediaMetadata.albumTitle.toString() + " | \n" + mediaMetadata.artist)
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
        handlerVolume = Handler(Looper.getMainLooper())
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
        return null
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

            if (isDataListInitialized()) {
                playSong(0)
            }

            sendIntent = intent
        }

        return START_STICKY
    }


    private fun playSong(index: Int, isCrossfade: Boolean = false) {
        if (index in 0 until pDatalistSongs.size) {
            val oldPlayer = mMediaPlayer

            if (!isCrossfade) {
                volumeAnimator?.cancel()
                handlerVolume.removeCallbacksAndMessages(null)
                oldPlayer?.stop()
                oldPlayer?.release()
                mMediaPlayer = null
            }

            val newPlayer = ExoPlayer.Builder(applicationContext).build()
            newPlayer.volume = 0f

            val song = pDatalistSongs[index]
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtworkUri(song.album.cover.toUri())
                .setAlbumTitle(song.album.title)
                .setArtist(song.artist.name)
                .build()

            val mItem = MediaItem.Builder()
                .setUri(song.preview)
                .setMediaMetadata(metadata)
                .build()

            newPlayer.setMediaItem(mItem)

            newPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        if (newPlayer == mMediaPlayer) trackSeek()
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        if (newPlayer == mMediaPlayer && index == pDatalistSongs.size - 1) {
                             remoteViews?.setImageViewResource(R.id.imgbtn_playpause, R.drawable.play_m)
                             remoteViews?.setTextViewText(R.id.tx_music_details, "End of Playback")
                             appWidM.updateAppWidget(newAppWidget, remoteViews)
                        }
                        newPlayer.release()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (newPlayer == mMediaPlayer) {
                        if (isPlaying) trackSeek()
                        else handlerVolume.removeCallbacksAndMessages(null)
                    }
                }
            })

            newPlayer.prepare()
            newPlayer.play()

            mMediaPlayer = newPlayer
            songIndex = index
            notifySong(mItem)

            if (isCrossfade && oldPlayer != null) {
                volumeAnimator?.cancel()
                volumeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 5000
                    addUpdateListener { animator ->
                        val v = animator.animatedValue as Float
                        newPlayer.volume = v
                        try {
                            if (oldPlayer.isPlaying) oldPlayer.volume = 1f - v
                        } catch (e: Exception) {}
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (isCrossfade) {
                                oldPlayer?.stop()
                                oldPlayer?.release()
                            }
                        }
                    })
                    start()
                }
            } else {
                increaseVol()
            }
        }
    }

    private fun playNextSong() {
        if (songIndex < pDatalistSongs.size - 1) {
            playSong(songIndex + 1)
        } else {
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }


    private fun trackSeek() {
        val player = mMediaPlayer ?: return
        val duration = player.duration
        val position = player.currentPosition

        if (duration <= 0) return

        handlerVolume.removeCallbacksAndMessages(null)

        val timeToCrossfade = duration - 5000L
        val delay = timeToCrossfade - position

        if (delay > 0) {
            handlerVolume.postDelayed({
                if (mMediaPlayer == player && player.isPlaying) {
                    if (songIndex < pDatalistSongs.size - 1) {
                        playSong(songIndex + 1, isCrossfade = true)
                    } else {
                        reduceVolume()
                    }
                }
            }, delay)
        } else if (position < duration) {
            // Already within the last 5 seconds, should crossfade now if not end of playlist
             if (songIndex < pDatalistSongs.size - 1) {
                 playSong(songIndex + 1, isCrossfade = true)
             } else {
                 reduceVolume()
             }
        }
    }

    private fun increaseVol() {
        volumeAnimator?.cancel()
        volumeAnimator = ValueAnimator.ofFloat(mMediaPlayer?.volume ?: 0f, 1f).apply {
            duration = 5000
            addUpdateListener { animator ->
                mMediaPlayer?.volume = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun reduceVolume() {
        volumeAnimator?.cancel()
        volumeAnimator = ValueAnimator.ofFloat(mMediaPlayer?.volume ?: 1f, 0f).apply {
            duration = 5000
            addUpdateListener { animator ->
                mMediaPlayer?.volume = animator.animatedValue as Float
            }
            start()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        volumeAnimator?.cancel()
        handlerVolume.removeCallbacksAndMessages(null)

        if (mMediaPlayer != null) {
            mMediaPlayer!!.release(); // Release resources when done
            mMediaPlayer = null;
        }

        songIndex = 0
        sharedPreferencesEditor.putInt("SIn", 0).apply()


        Log.i("OnDestroyMS", "onDestroy: MS OnDestroy called");
    }


}
