package com.example.basekotlin.model
// domain model 1 thư mục nhạc
data class MusicFolder(
    val folderPath: String,
    val folderName: String,
    val trackCount: Int,
    val totalDurationMs: Long
)