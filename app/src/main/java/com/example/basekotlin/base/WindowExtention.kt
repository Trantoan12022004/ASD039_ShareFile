package com.example.basekotlin.base

import android.graphics.Rect
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Window.hideNavigation() {
    if (setFullScreenWallpaper()) return

    decorView.viewTreeObserver.addOnGlobalLayoutListener {
        val rect = Rect()
        val activityRoot = decorView
        activityRoot.getWindowVisibleDisplayFrame(rect)
        if (setFullScreenWallpaper()) return@addOnGlobalLayoutListener
    }
}

fun Window.hideStatusBar() {

    val windowInsetsController =
        WindowCompat.getInsetsController(
            this,
            decorView
        )

    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    windowInsetsController.hide(
        WindowInsetsCompat.Type.statusBars()
    )
}

fun Window.setupBottomInputDialog() {
    // Gỡ flag phá adjust resize/pan
    clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

    // Không ẩn navigation bar trên dialog input
    val controller = if (Build.VERSION.SDK_INT >= 30) {
        ViewCompat.getWindowInsetsController(decorView)
    } else {
        WindowInsetsControllerCompat(this, decorView)
    }
    controller?.show(WindowInsetsCompat.Type.navigationBars())

    // Pan đẩy dialog lên khi bàn phím mở — phù hợp bottom dialog
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
}

private fun Window.setFullScreenWallpaper(): Boolean {
    val windowInsetsController: WindowInsetsControllerCompat? = if (Build.VERSION.SDK_INT >= 30) {
        ViewCompat.getWindowInsetsController(decorView)
    } else {
        WindowInsetsControllerCompat(this, decorView)
    }

    if (windowInsetsController == null) {
        return true
    }
    setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
    )
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    windowInsetsController.hide(WindowInsetsCompat.Type.systemGestures())
    return false
}
