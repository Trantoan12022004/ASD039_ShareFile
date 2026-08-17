package com.example.basekotlin.model

import android.net.Uri

// domain model 1 thư mục ảnh, dùng cho tab Folder
data class PhotoFolder(
    val folderPath: String,
    val folderName: String,
    val photoCount: Int,
    val coverPhotoUri: Uri?
)