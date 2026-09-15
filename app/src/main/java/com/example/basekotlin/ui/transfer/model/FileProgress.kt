package com.example.basekotlin.ui.transfer.model

/**
 * Progress chi tiết cho 1 file đang truyền
 * UI dùng để update progress bar cho từng item trong RecyclerView
 */
data class FileProgress(
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
) {
    /** Phần trăm hoàn thành (0-100) */
    val percent: Int
        get() = if (totalBytes > 0) ((bytesTransferred * 100) / totalBytes).toInt() else 0
}
