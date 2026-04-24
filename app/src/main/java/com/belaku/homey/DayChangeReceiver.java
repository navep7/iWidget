package com.belaku.homey;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class DayChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {

            Log.d("ACTION_DATE_CHANGED", "Y3S");

            AppWidgetManager appWidM = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
            ComponentName newAppWidget = new ComponentName(context, NewAppWidget.class);

            appWidM.updateAppWidget(newAppWidget, remoteViews);

        }
    }
}
