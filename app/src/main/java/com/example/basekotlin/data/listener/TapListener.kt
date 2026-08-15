package com.example.basekotlin.data.listener

import android.os.SystemClock
import android.view.View

abstract class TapListener : View.OnClickListener {

    companion object {
        private const val TIME_WAIT = 500L
    }

    private var lastClick: Long = 0
    private var now: Long = 0L

    override fun onClick(v: View?) {
        now = SystemClock.elapsedRealtime();
        if (now - lastClick > TIME_WAIT) {
            onTap(v)
        }
        lastClick = now
    }

    abstract fun onTap(v: View?)
}