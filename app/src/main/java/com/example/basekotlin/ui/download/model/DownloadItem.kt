package com.example.basekotlin.ui.download.model

data class DownloadItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val extension: String,
    val type: DownloadType,
    val mimeType: String = "",
    val appName: String? = null,
    val packageName: String? = null
)