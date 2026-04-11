package com.belaku.homey;


import static com.belaku.homey.SetWallWorker.sharedPreferences;
import static com.belaku.homey.SetWallWorker.sharedPreferencesEditor;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class DayChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
            SetWallWorker.stepsToday = 0;
            sharedPreferencesEditor.putInt("breatheCount", 0).apply();
            sharedPreferencesEditor.putInt("drinkCount", 0).apply();

            AppWidgetManager appWidM = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
            ComponentName newAppWidget = new ComponentName(context, NewAppWidget.class);

            appWidM.updateAppWidget(newAppWidget, remoteViews);

        }
    }
}
