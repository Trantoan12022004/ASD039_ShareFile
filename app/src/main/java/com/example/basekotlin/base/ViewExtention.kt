package com.example.basekotlin.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.basekotlin.data.listener.TapListener
import com.example.basekotlin.data.listener.TapNoHandleListener

@SuppressLint("ClickableViewAccessibility")
fun View.tap(action: (view: View) -> Unit) {

    setOnTouchListener { v, event ->

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                v.animate()
                    .scaleX(0.90f)
                    .scaleY(0.90f)
                    .alpha(0.7f)
                    .translationY(4f)
                    .setDuration(120)
                    .setInterpolator(
                        android.view.animation.DecelerateInterpolator()
                    )
                    .start()
            }


            MotionEvent.ACTION_UP -> {

                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .setInterpolator(
                        android.view.animation.OvershootInterpolator()
                    )
                    .start()

                action(v)
            }


            MotionEvent.ACTION_CANCEL -> {

                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .start()
            }
        }

        true
    }
}


fun View.tapNoHandle(action: (view: View?) -> Unit) {
    setOnClickListener(object : TapNoHandleListener() {
        override fun onTap(v: View?) {
            action(v)
        }
    })
}

@SuppressLint("ClickableViewAccessibility")
fun View.tapDouble(onDoubleClick: () -> Unit) {
    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return true
        }
    })

    setOnTouchListener { _, event ->
        gestureDetector.onTouchEvent(event)
    }
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.inVisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun setSrcImage(imageView: ImageView, drawable: Int) {
    imageView.setImageResource(drawable)
}

fun setTextBackground(context: Context, textView: TextView, drawable: Int) {
    textView.background =
        ContextCompat.getDrawable(context, drawable)
}

fun setTextColor(textView: TextView, color: String) {
    textView.setTextColor(Color.parseColor(color))
}

fun setTintBackgroundView(view: View, color: String) {
    view.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
}