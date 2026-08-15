package com.example.basekotlin.data.listener

import android.view.View

abstract class TapNoHandleListener : View.OnClickListener {

    override fun onClick(v: View?) {
        onTap(v)
    }

    abstract fun onTap(v: View?)
}