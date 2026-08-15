package com.example.basekotlin.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    /**
     * Kiểm tra một quyền đã được cấp chưa.
     */
    fun Context.isGranted(permission: String): Boolean {
        val result = ContextCompat.checkSelfPermission(this, permission)
        val granted = result == PackageManager.PERMISSION_GRANTED
        return granted
    }

    /**
     * Kiểm tra quyền Thông báo.
     */
    fun checkNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.isGranted(Manifest.permission.POST_NOTIFICATIONS)
            return granted
        } else {
            val notificationManager = NotificationManagerCompat.from(context)
            val enabled = notificationManager.areNotificationsEnabled()
            return enabled
        }
    }

    /**
     * Kiểm tra quyền Đọc bộ nhớ / Nhạc (Read Storage / READ_MEDIA_AUDIO).
     */
    fun checkReadPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.isGranted(Manifest.permission.READ_MEDIA_AUDIO)
            return granted
        } else {
            val granted = context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
            return granted
        }
    }

    /**
     * Kiểm tra quyền Ghi cài đặt hệ thống (WRITE_SETTINGS) — dùng để đặt nhạc chuông.
     */
    fun checkWritePermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }
}
