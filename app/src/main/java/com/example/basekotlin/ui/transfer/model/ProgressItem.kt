package com.example.basekotlin.ui.transfer.model


enum class TransferStatus1 {
    QUEUED,        // Đang xếp hàng
    TRANSFERRING,  // Đang truyền
    COMPLETED,     // Đã gửi/nhận xong
    CANCELED,      // Đã dừng
    ERROR,         // Lỗi trong quá trình gửi/nhận
    DECLINED       // Bị từ chối
}

/**
 * Sealed class đại diện cho các item trên timeline chat
 */
sealed class ProgressItem(open val id: String, open val timestamp: Long) {

    // Tin nhắn text
    data class TextMessage(
        override val id: String,
        val text: String,
        val isMe: Boolean,
        val timeFormatted: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProgressItem(id, timestamp)

    // File truyền nhận
    data class FileTransfer(
        override val id: String,
        val file: TransferFile,
        val isMe: Boolean,
        var status: TransferStatus1 = TransferStatus1.QUEUED,
        var bytesTransferred: Long = 0L,
        var totalBytes: Long = file.size,
        var progressPercent: Int = 0,
        var savedPath: String? = null,
        var errorMessage: String? = null,
        var thumbnailBase64: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProgressItem(id, timestamp)

    // Thông báo sự kiện hệ thống (Online/Offline, Kết nối)
    data class SystemEvent(
        override val id: String,
        val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ProgressItem(id, timestamp)
}

/**
 * Thống kê tổng tiến trình hiển thị trên Toolbar
 */
data class OverallStats(
    val totalTransferredBytes: Long = 0L,
    val secondsLeft: Long = 0L,
    val elapsedSeconds: Long = 0L
)
