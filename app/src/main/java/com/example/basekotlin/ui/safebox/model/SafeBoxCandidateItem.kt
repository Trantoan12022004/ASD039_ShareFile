package com.example.basekotlin.ui.safebox.model

import android.net.Uri
import com.example.basekotlin.data.local.safebox.SafeBoxFileType

data class SafeBoxCandidateItem(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: SafeBoxFileType,
    val contentUri: Uri? = null,
    var isSelected: Boolean = false
)
