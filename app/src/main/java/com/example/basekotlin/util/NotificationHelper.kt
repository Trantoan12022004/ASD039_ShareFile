package com.example.basekotlin.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.basekotlin.R
import com.example.basekotlin.ui.splash.SplashActivity
import kotlin.random.Random

object NotificationHelper {
    // Đổi ID để hệ thống tạo Channel mới (vì Channel cũ đã cố định mức DEFAULT)
    private const val CHANNEL_ID = "app_notification_channel_v2"
    private const val CHANNEL_NAME = "App Updates"

    fun showRandomNotification(context: Context) {
        if (!PermissionManager.checkNotificationPermission(context)) return

        val messages = context.resources.getStringArray(R.array.random_notifications)
        val randomMessage = messages[Random.nextInt(messages.size)]

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // QUAN TRỌNG: Mức HIGH để hiện Heads-up
            ).apply {
                description = "Channel for high priority notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 1. Tạo Intent để mở SplashActivity khi người dùng nhấn vào thông báo
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // QUAN TRỌNG: Ưu tiên cao
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Bật âm thanh/rung để kích hoạt Heads-up
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }
}