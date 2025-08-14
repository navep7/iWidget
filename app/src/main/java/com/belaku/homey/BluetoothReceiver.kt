package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.belaku.homey.MainActivity.Companion.makeSnack
import com.belaku.homey.MainActivity.Companion.makeToast


class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
        makeSnack("onReceive BLT - ${state.toString()}")

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context!!, NewAppWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        val remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)



        when (state) {

            BluetoothAdapter.STATE_CONNECTED ->
                makeToast("STATE_CONNECTED")

            BluetoothAdapter.STATE_DISCONNECTED ->
                makeToast("STATE_DISCONNECTED")


            BluetoothAdapter.STATE_OFF -> {
                makeToast("STATE_OFF")
                try {
                    MainActivity.notifyBluetoothState(false)
                } catch (ex: Exception) {
                    makeToast("EXXP - ${ex.message}")
                }
            }

            BluetoothAdapter.STATE_ON ->
            {
                makeToast("STATE_ON")
                try {
                    MainActivity.notifyBluetoothState(true)
                } catch (ex: Exception) {
                    makeToast("EXXP - ${ex.message}")
                }
            }

        }

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }


    }
}