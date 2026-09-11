package com.example.basekotlin.data.local.repository.cleanfile

import com.example.basekotlin.model.CleanFileGroup
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import com.example.basekotlin.model.MessengerCategory

interface CleanFileRepository {
    // Junk
    suspend fun scanJunkFiles(): Long
    suspend fun cleanJunkFiles(): Boolean


    // Big Files
    suspend fun fetchBigFiles(minSizeBytes: Long = 500L * 1024L * 1024L): List<CleanFileItem>

    // Media Cleanup (Video / Photo / Audio)
    suspend fun fetchMediaByFolder(type: CleanFileType): List<CleanFileGroup>

    // Duplicate
    suspend fun fetchDuplicateFiles(): List<CleanFileGroup>

    // Messenger
    fun isMessengerInstalled(messenger: String): Boolean
    suspend fun fetchMessengerCategories(messenger: String): List<MessengerCategory>
    suspend fun fetchMessengerFiles(messenger: String, category: String): List<CleanFileItem>

    // Delete
    suspend fun deleteFiles(paths: List<String>): Boolean
}
