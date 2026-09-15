package com.example.basekotlin.ui.transfer.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Đại diện cho 1 file trong quá trình truyền
 * Dùng chung cho cả sender và receiver
 */
@Parcelize
data class TransferFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val status: TransferStatus = TransferStatus.PENDING
) : Parcelable

/**
 * Trạng thái truyền của từng file
 */
enum class TransferStatus {
    PENDING,       // chưa bắt đầu
    TRANSFERRING,  // đang truyền
    COMPLETED,     // truyền xong
    FAILED         // truyền lỗi
}
