package com.example.basekotlin.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Model đại diện cho 1 file trong các tính năng Clean File
@Parcelize
data class CleanFileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val type: CleanFileType,
    val thumbnailUri: String? = null,
    val folderName: String = "",
    var isSelected: Boolean = false,
    val mimeType: String? = null
) : Parcelable
