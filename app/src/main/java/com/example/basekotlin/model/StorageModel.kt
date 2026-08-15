package com.example.basekotlin.model

data class StorageModel(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercentage: Int = 0,
    val formattedUsed: String = "0 GB",
    val formattedTotal: String = "0 GB"
) {
    val formattedDisplay: String
        get() = "$formattedUsed/$formattedTotal"
}