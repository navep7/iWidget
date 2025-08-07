package com.belaku.homey;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DayChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
            SetWallWorker.steps = 0;
        }
    }
}
