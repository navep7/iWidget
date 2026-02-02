package com.belaku.homey

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.belaku.homey.MainActivity.Companion.appContx
import com.belaku.homey.MusicActivity.Companion.dataList
import com.belaku.homey.NewAppWidget.Companion.appWidM
import com.belaku.homey.NewAppWidget.Companion.newAppWidget
import com.belaku.homey.NewAppWidget.Companion.remoteViews
import com.belaku.homey.SetWallWorker.Companion.sharedPreferencesEditor
import java.util.Timer
import java.util.TimerTask
import androidx.core.net.toUri
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.MusicActivity.Companion.isDataListInitialized
import com.belaku.homey.SetWallWorker.Companion.sharedPreferences


class MusicService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private lateinit var serviceNotification: Notification
    private lateinit var sendIntent: Intent
    private lateinit var scontext: MusicService



    companion object {

        var songsUrlList: ArrayList<String> = ArrayList()
        var songIndex: Int = 0

        lateinit var mPlayer: MediaPlayer

        fun isMediaPlayerInitialized(): Boolean {
            return ::mPlayer.isInitialized
        }

        fun notifySong(sIndex: Int) {

            sharedPreferencesEditor.putInt("SIn", sIndex).apply()
            appWidM = AppWidgetManager.getInstance(appContx)
            remoteViews =
                RemoteViews(appContx.packageName, com.belaku.homey.R.layout.new_app_widget)
            newAppWidget = ComponentName(appContx, NewAppWidget::class.java)
            remoteViews?.setTextViewText(com.belaku.homey.R.id.tx_music_details, dataList[sIndex].title + " | " + dataList[sIndex].album.title + " | " + dataList[sIndex].artist.name)


            appWidM.updateAppWidget(newAppWidget, remoteViews)
            //   serviceNotify(MainActivity.dataList[sIndex].title)
            val intent = Intent(
                appContx,
                MainActivity::class.java
            )
            val pendingIntent = PendingIntent.getActivity(
                appContx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )


            val channelId = "some_channel_id"
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(appContx, channelId)
                    .setSilent(true)
                    .setSmallIcon(R.drawable.ic_media_play) //                        .setContentTitle(getString(R.string.app_name)
                    .setContentTitle(MusicActivity.dataList[sIndex].title)
                    .setContentText(MusicActivity.dataList[sIndex].album.title + " | \n" + MusicActivity.dataList[sIndex].artist.name)
                    .setAutoCancel(true)
                    .setSound(null)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)

            val notificationManager =
                appContx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager


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

        if (MusicActivity.isDataListInitialized())
        serviceNotify(dataList[songIndex].title)
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

        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        if (intent != null) {

            appWidM = AppWidgetManager.getInstance(appContx)
            remoteViews =
                RemoteViews(
                    applicationContext.packageName,
                    com.belaku.homey.R.layout.new_app_widget
                )
            newAppWidget = ComponentName(applicationContext, NewAppWidget::class.java)


            scontext = this;
            //    songsUrlList = intent.getStringArrayListExtra("songsUrl")!!


            for (i in 0 until 30) {
                if (intent.extras?.get(i.toString()) != null)
                    songsUrlList.add(intent.extras?.get(i.toString()).toString())
                else break

            }

            println("S21 - rSize" + songsUrlList.size)
            for (item in songsUrlList)
                println("S21 - received" + item)

            if (songsUrlList.isNotEmpty() && isDataListInitialized()) {

                notifySong(0)

                try {
                    val uri = songsUrlList[songIndex].toUri()

                    mPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )

                        makeToast("URI ~ $uri")
                        setDataSource(applicationContext, uri)
                        prepare() // might take long! (for buffering, etc)
                        start()
                    }
                    remoteViews?.setImageViewResource(
                        com.belaku.homey.R.id.imgbtn_playpause,
                        android.R.drawable.ic_media_pause
                    )
                    appWidM.updateAppWidget(newAppWidget, remoteViews)
                    mPlayer.setOnCompletionListener(this)
                    mPlayer.setOnErrorListener(this)
                } catch (e: Exception) {
                    println(e.toString())
                    //    Toast.makeText(applicationContext, "P ex - " + e, Toast.LENGTH_LONG).show()
                }


            }


                sendIntent = intent


            updateActivity()
        }

        return START_STICKY
    }

    private fun updateActivity() {

        val messengerSongInfo = sendIntent.getParcelableExtra("songInfo") as Messenger?
        val messageSongInfo: Message = Message.obtain(null, songIndex)

        try {
            messengerSongInfo!!.send(messageSongInfo)
        } catch (exception: RemoteException) {
            exception.printStackTrace()
        }


    }


    override fun onCompletion(p0: MediaPlayer?) {

        songIndex++


        notifySong(songIndex)

        if (songIndex < songsUrlList.size) {
            val uri = Uri.parse(songsUrlList[songIndex])
            mPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare() // might take long! (for buffering, etc)
                start()
            }

            mPlayer.setOnCompletionListener(this)
            mPlayer.setOnErrorListener(this)
        }

        updateActivity()

    }


    override fun onDestroy() {
        super.onDestroy()
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        mPlayer.release();
        songIndex = 0
        sharedPreferencesEditor.putInt("SIn", 0).apply()
        remoteViews?.setTextViewText(com.belaku.homey.R.id.tx_music_details, dataList[0].title + " | " + dataList[0].album.title + " | " + dataList[0].artist.name)
        appWidM.updateAppWidget(newAppWidget, remoteViews)

        Log.i("OnDestroyMS", "onDestroy: MS OnDestroy called");
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("Not yet implemented")
        Toast.makeText(applicationContext, "Err - " + p1.toString(), Toast.LENGTH_LONG).show()
        Log.d("onErrorMusService", p1.toString())
    }


}