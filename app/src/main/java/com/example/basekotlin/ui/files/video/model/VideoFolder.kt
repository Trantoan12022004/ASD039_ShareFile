package com.example.basekotlin.ui.files.video.model

import android.net.Uri

data class VideoFolder(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int,
    val coverVideoUri: Uri?
)