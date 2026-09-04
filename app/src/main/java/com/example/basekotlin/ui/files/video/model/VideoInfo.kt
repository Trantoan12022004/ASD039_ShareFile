package com.example.basekotlin.ui.files.video.model

import android.net.Uri

data class VideoInfo(
    val id: Long,
    val displayName: String,
    val filePath: String,
    val relativeFolderPath: String,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val widthPx: Int,
    val heightPx: Int,
    val durationMs: Long,
    val mimeType: String,
    val contentUri: Uri
)