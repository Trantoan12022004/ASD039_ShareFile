package com.example.basekotlin.data.local.cleanfile

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object JunkFileScanner {

    private const val TAG = "JunkFileScanner"

    suspend fun scanJunkSize(context: Context): Long {
        return withContext(Dispatchers.IO) {
            var totalBytes = 0L

            // 1. App internal cache
            totalBytes += getDirSize(context.cacheDir)
            totalBytes += getDirSize(context.codeCacheDir)

            // 2. App external cache
            context.externalCacheDir?.let {
                totalBytes += getDirSize(it)
            }

            // 3. Quét temp files trong Download / Android cache nếu có
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null && downloadDir.exists()) {
                    val tempFiles = downloadDir.listFiles { file ->
                        file.isFile && (file.name.endsWith(".tmp", true) || file.name.endsWith(".log", true) || file.name.endsWith(".crdownload", true))
                    }
                    tempFiles?.forEach { totalBytes += it.length() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning temp files: ${e.message}", e)
            }

            totalBytes
        }
    }

    suspend fun cleanJunk(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            var allCleaned = true

            // Xóa internal cache
            context.cacheDir?.let {
                if (!deleteDirContent(it)) allCleaned = false
            }

            // Xóa external cache
            context.externalCacheDir?.let {
                if (!deleteDirContent(it)) allCleaned = false
            }

            // Xóa temp files trong Download
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null && downloadDir.exists()) {
                    val tempFiles = downloadDir.listFiles { file ->
                        file.isFile && (file.name.endsWith(".tmp", true) || file.name.endsWith(".log", true) || file.name.endsWith(".crdownload", true))
                    }
                    tempFiles?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning temp files: ${e.message}", e)
            }

            allCleaned
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            size += if (child.isDirectory) {
                getDirSize(child)
            } else {
                child.length()
            }
        }
        return size
    }

    private fun deleteDirContent(dir: File): Boolean {
        var success = true
        val children = dir.listFiles() ?: return true
        for (child in children) {
            val childSuccess = if (child.isDirectory) {
                deleteDirContent(child) && child.delete()
            } else {
                child.delete()
            }
            if (!childSuccess) success = false
        }
        return success
    }
}
