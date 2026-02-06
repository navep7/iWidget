package com.belaku.homey

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.belaku.homey.MusicService.Companion.appContx
import com.belaku.homey.MainActivity.Companion.makeToast
import com.belaku.homey.RemindersActivity.Companion.adapterReminders
import com.belaku.homey.RemindersActivity.Companion.arrayListReminders


class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        notifyAlarm(intent.getStringExtra("alertSub"))
        // You can perform other actions here, like starting a service or showing a notification
    }

    private fun notifyAlarm(rSubject: String?) {

        val CHANNEL_ID = "my_channel_id"
        val name: CharSequence = "My Channel Name"
        val description = "Channel for important notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH // or IMPORTANCE_DEFAULT for sound

        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description


        // Set default sound
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)


        // Or set a custom sound from your raw resources
        // Uri customSoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/my_custom_sound");
        // channel.setSound(customSoundUri, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        val notificationManager: NotificationManager =
            appContx.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)


        val builder: NotificationCompat.Builder = NotificationCompat.Builder(appContx, CHANNEL_ID)
            .setSmallIcon(R.drawable.walp_icon)
            .setContentTitle("Reminding you to..")
            .setContentText(rSubject)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Match channel importance

        val notificationManagerCompat = NotificationManagerCompat.from(appContx)
        if (ActivityCompat.checkSelfPermission(
                appContx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManagerCompat.notify(1, builder.build())
        for (i in 0 until arrayListReminders.size)
            try {
                if (arrayListReminders[i].rType == "One time")
                if (arrayListReminders[i].name == rSubject) {
                    arrayListReminders.removeAt(i)
                    adapterReminders.notifyDataSetChanged()
                }
            } catch (ex: Exception) {

            }

    }
}
