package com.belaku.homey

import android.content.Intent
import android.widget.RemoteViewsService


class RemoteViewsAppsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RemoteViewsAppsFactory(this.applicationContext, intent)
    }
}