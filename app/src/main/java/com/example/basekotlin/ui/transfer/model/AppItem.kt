package com.example.basekotlin.ui.transfer.model

data class AppItem(
    val packageName: String,
    val appName: String,
    val apkPath: String,
    val size: Long,
    val icon: android.graphics.drawable.Drawable?,
)

enum class AppSubTab {
    INSTALLED,
    NOT_INSTALLED
}