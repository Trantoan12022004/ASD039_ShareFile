package com.example.basekotlin.util.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.basekotlin.R
import com.example.basekotlin.ui.main.MainActivity

/**
 * Quản lý notification cho quá trình truyền file
 * - Tạo notification channel
 * - Hiển thị notification với progress
 * - Cập nhật progress realtime
 * - Hiển thị kết quả (thành công/thất bại)
 */
object TransferNotificationHelper {

    private const val CHANNEL_ID = "file_transfer_channel"
    const val NOTIFICATION_ID = 2001

    /**
     * Tạo notification channel (gọi 1 lần khi app khởi tạo)
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.transfer_notification_channel),
            NotificationManager.IMPORTANCE_LOW // LOW để không phát âm thanh
        ).apply {
            description = "File transfer progress"
            setShowBadge(false)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Tạo notification builder cho transfer đang chạy
     * Dùng cho startForeground()
     */
    fun createTransferNotification(
        context: Context,
        title: String,
        text: String,
        progress: Int = 0,
        maxProgress: Int = 100,
        indeterminate: Boolean = false
    ): NotificationCompat.Builder {
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(maxProgress, progress, indeterminate)
            .setOngoing(true)       // không thể vuốt dismiss
            .setAutoCancel(false)
            .setOnlyAlertOnce(true) // không phát sound mỗi lần update
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    /**
     * Cập nhật notification với progress mới
     */
    fun updateProgress(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        maxProgress: Int = 100
    ) {
        val notification = createTransferNotification(
            context, title, text, progress, maxProgress
        ).build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Hiển thị notification hoàn thành
     */
    fun showCompleteNotification(context: Context, isSender: Boolean, fileCount: Int) {
        val text = if (isSender) {
            context.getString(R.string.transfer_complete_files_sent, fileCount)
        } else {
            context.getString(R.string.transfer_complete_files_received, fileCount)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(context.getString(R.string.transfer_complete_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Xóa notification
     */
    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
