package com.belaku.homey

import android.content.Intent
import android.widget.RemoteViewsService


class RemoteViewsContactsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RemoteViewsContactsFactory(this.applicationContext)
    }
}