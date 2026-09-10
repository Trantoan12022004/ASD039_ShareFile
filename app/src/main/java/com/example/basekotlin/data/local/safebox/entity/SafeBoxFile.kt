package com.example.basekotlin.data.local.safebox.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.basekotlin.data.local.safebox.SafeBoxFileType

@Entity(tableName = "safebox_files")
data class SafeBoxFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,    // Đường dẫn gốc ngoài bộ nhớ máy để phục hồi
    val safeboxPath: String,     // Đường dẫn lưu file an toàn trong app filesDir
    val fileType: SafeBoxFileType, // Loại file: PICTURES, VIDEOS, AUDIO...
    val fileName: String,        // Tên file hiển thị (vd: my_photo.jpg)
    val fileSize: Long,          // Kích thước file theo bytes
    val dateAdded: Long = System.currentTimeMillis() // Thời gian thêm vào SafeBox
)
