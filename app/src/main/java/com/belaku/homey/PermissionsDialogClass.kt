package com.belaku.homey

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button


class PermissionsDialogClass // TODO Auto-generated constructor stub
    (var c: Activity) : Dialog(c) {
    var d: Dialog? = null
    var yes: Button? = null
    var no: Button? = null

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.permissions_dialog)

    }


}