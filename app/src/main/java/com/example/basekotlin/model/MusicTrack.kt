package com.example.basekotlin.model
// domain model 1 bài hát
data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val filePath: String,
    val relativeFolderPath: String,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val albumId: Long,
    val contentUri: android.net.Uri,
    val isFavorite: Boolean = false
)
//Trường isFavorite không lấy từ MediaStore, mà được Repository gán thêm sau khi join với Room — giữ model domain gọn, không lẫn logic nguồn dữ liệu