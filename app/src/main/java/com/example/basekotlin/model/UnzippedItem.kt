package com.example.basekotlin.model

data class UnzippedItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val itemCount: Int = 0,
    val extension: String = ""
)
