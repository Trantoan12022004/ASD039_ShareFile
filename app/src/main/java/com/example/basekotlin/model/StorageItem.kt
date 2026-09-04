package com.example.basekotlin.model

data class StorageItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val itemCount: Int = 0,   // số item con nếu là thư mục
    val extension: String = ""
)