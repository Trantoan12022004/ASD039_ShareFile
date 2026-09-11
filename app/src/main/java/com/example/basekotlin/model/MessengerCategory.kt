package com.example.basekotlin.model

// Model đại diện cho một danh mục trong Telegram / WhatsApp Cleaner
data class MessengerCategory(
    val id: String,                  // "photos", "videos", "audios", "files", "junk"
    val name: String,                // Tên hiển thị (Photos, Videos, Audios, Files, Junk Files)
    val description: String,         // Mô tả ("Clean up photos in chats", v.v.)
    val iconRes: Int,                // Icon hiển thị
    var sizeBytes: Long = 0L,        // Tổng dung lượng
    var fileCount: Int = 0,          // Số lượng file
    val isJunk: Boolean = false,     // Có phải danh mục Junk Files không
    var hasJunk: Boolean = false     // Có rác để dọn không
)
