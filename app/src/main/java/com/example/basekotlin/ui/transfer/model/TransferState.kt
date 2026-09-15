package com.example.basekotlin.ui.transfer.model

/**
 * Trạng thái tổng thể của phiên truyền file
 * TransferService expose StateFlow<TransferState> để UI observe
 */
sealed class TransferState {
    /** Chưa bắt đầu */
    data object Idle : TransferState()

    /** Đang kết nối đến thiết bị */
    data object Connecting : TransferState()

    /** Đã kết nối thành công */
    data class Connected(val device: DeviceInfo) : TransferState()

    /** Đang truyền file */
    data class Transferring(
        val currentFile: TransferFile,
        val currentIndex: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long
    ) : TransferState()

    /** Truyền xong */
    data class Complete(val summary: TransferSummary) : TransferState()

    /** Lỗi */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : TransferState()
}

/**
 * Tổng kết phiên truyền
 */
data class TransferSummary(
    val fileCount: Int,
    val totalBytes: Long,
    val durationMs: Long,
    val avgSpeedBytesPerSec: Long,
    val deviceName: String,
    val direction: TransferDirection
)

enum class TransferDirection {
    SENT,
    RECEIVED
}
