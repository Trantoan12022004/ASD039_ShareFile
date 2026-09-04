package com.example.basekotlin.model

// Domain model cho 1 file zip/archive trên thiết bị
data class ZipInfo(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val extension: String
)
