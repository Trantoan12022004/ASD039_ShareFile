package com.example.basekotlin.model

import android.net.Uri

// domain model 1 tấm ảnh trên máy
data class PhotoInfo(
    val id: Long,
    val displayName: String,
    val filePath: String,
    val relativeFolderPath: String,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val widthPx: Int,
    val heightPx: Int,
    val mimeType: String,
    val contentUri: Uri
)

