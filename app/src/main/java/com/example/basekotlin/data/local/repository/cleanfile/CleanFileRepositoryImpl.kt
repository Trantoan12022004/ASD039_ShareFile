package com.example.basekotlin.data.local.repository.cleanfile

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.basekotlin.data.local.cleanfile.BigFileScanner
import com.example.basekotlin.data.local.cleanfile.DuplicateFileScanner
import com.example.basekotlin.data.local.cleanfile.JunkFileScanner
import com.example.basekotlin.data.local.cleanfile.MediaCleanupScanner
import com.example.basekotlin.data.local.cleanfile.MessengerMediaScanner
import com.example.basekotlin.model.CleanFileGroup
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import com.example.basekotlin.model.MessengerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CleanFileRepositoryImpl(private val appContext: Context) : CleanFileRepository {

    companion object {
        private const val TAG = "CleanFileRepo"
    }

    override suspend fun scanJunkFiles(): Long {
        return JunkFileScanner.scanJunkSize(appContext)
    }

    override suspend fun cleanJunkFiles(): Boolean {
        return JunkFileScanner.cleanJunk(appContext)
    }


    override suspend fun fetchBigFiles(minSizeBytes: Long): List<CleanFileItem> {
        return BigFileScanner.scanBigFiles(appContext, minSizeBytes)
    }

    override suspend fun fetchMediaByFolder(type: CleanFileType): List<CleanFileGroup> {
        return when (type) {
            CleanFileType.VIDEO -> MediaCleanupScanner.scanVideos(appContext)
            CleanFileType.PHOTO -> MediaCleanupScanner.scanPhotos(appContext)
            CleanFileType.AUDIO -> MediaCleanupScanner.scanAudios(appContext)
            else -> emptyList()
        }
    }

    override suspend fun fetchDuplicateFiles(): List<CleanFileGroup> {
        return DuplicateFileScanner.scanDuplicateFiles(appContext)
    }

    override fun isMessengerInstalled(messenger: String): Boolean {
        return MessengerMediaScanner.isMessengerInstalled(appContext, messenger)
    }

    override suspend fun fetchMessengerCategories(messenger: String): List<MessengerCategory> {
        return MessengerMediaScanner.scanCategories(appContext, messenger)
    }

    override suspend fun fetchMessengerFiles(
        messenger: String,
        category: String
    ): List<CleanFileItem> {
        return MessengerMediaScanner.scanCategoryFiles(appContext, messenger, category)
    }

    override suspend fun deleteFiles(paths: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            var allSuccess = true
            for (path in paths) {
                try {
                    val file = File(path)
                    val deleted = if (file.exists()) file.delete() else true

                    // Đồng bộ xóa MediaStore
                    try {
                        appContext.contentResolver.delete(
                            MediaStore.Files.getContentUri("external"),
                            "${MediaStore.Files.FileColumns.DATA} = ?",
                            arrayOf(path)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete from MediaStore: $path, ${e.message}")
                    }

                    if (!deleted) {
                        allSuccess = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file: $path, ${e.message}", e)
                    allSuccess = false
                }
            }
            allSuccess
        }
    }
}
