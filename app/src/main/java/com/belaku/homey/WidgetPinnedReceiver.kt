package com.belaku.homey

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.belaku.homey.MainActivity.Companion.makeToast

class MyWidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.firstOrNull()
            ?: intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // The widget was pinned successfully; widgetId is available here
            context.startActivity(Intent(context, DialogActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("DialogIntent", "Permissions"))
            makeToast(context, "Allow permissions by clicking on yellow triangles to see the Infos")
        }
    }
}
