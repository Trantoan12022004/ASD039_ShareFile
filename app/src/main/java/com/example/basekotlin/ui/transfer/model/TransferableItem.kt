package com.example.basekotlin.ui.transfer.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class FileCategory {
    RECENT_SENT, RECENT_RECEIVED, CONTACTS, FILES, VIDEO, APPS, MUSIC, PHOTO
}

enum class RecentSubTab { SEND, RECEIVED }
enum class VideoSubTab { RECENT, FOLDERS }
enum class PhotoSubTab { RECENT, FOLDERS }
enum class AppsSubTab { INSTALLED, NOT_INSTALLED }

/**
 * Interface duy nhất đại diện cho tất cả các đối tượng có thể chuyển gửi
 */
interface TransferableItem {
    val id: String               // Key duy nhất đối chiếu Set Selection O(1)
    val displayName: String      // Tên file / Contact name / App name
    val subInfo: String          // "23 MB - Oct 03, 2022" hoặc SĐT "0912 345 678"
    val sizeBytes: Long          // Dung lượng byte (Contact = 0)
    val dateModifiedMillis: Long // Thời gian tạo / sửa đổi
    val mimeType: String         // Mime type
    val uri: Uri                 // Content Uri truyền file
    val category: FileCategory   // Phân loại tab
    val thumbnailUri: Uri?       // Uri ảnh đại diện
    val fallbackLetter: String?  // Ký tự chữ cái (Contact/Music khi không có ảnh)
    val groupKey: String         // Khóa gom nhóm (Thư mục, Ngày tháng, Chữ cái, Loại file)
}

data class BaseFileItem(
    override val id: String,
    override val displayName: String,
    override val subInfo: String,
    override val sizeBytes: Long,
    override val dateModifiedMillis: Long,
    override val mimeType: String,
    override val uri: Uri,
    override val category: FileCategory,
    override val thumbnailUri: Uri? = null,
    override val fallbackLetter: String? = null,
    override val groupKey: String = ""
) : TransferableItem

data class ItemGroup<T : TransferableItem>(
    val title: String,          // Tiêu đề nhóm: "Jul 16, 2025" / "Camera" / "A" / "PDF"
    val count: Int,             // Số lượng item trong nhóm: 3
    val items: List<T>          // Danh sách các item
)
