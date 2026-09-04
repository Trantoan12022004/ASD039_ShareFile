package com.example.basekotlin.data.local.zipstore

import android.os.Environment
import com.example.basekotlin.model.UnzippedItem
import com.example.basekotlin.model.ZipInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ZipFileScanner {

    // Danh sách các đuôi file nén được hỗ trợ
    private val ZIP_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    private const val MAX_SCAN_DEPTH = 12
    private val SKIP_DIR_NAMES = setOf("Android", ".thumbnails", ".trashed")

    // Quét toàn bộ file nén trong bộ nhớ ngoài
    suspend fun scanAllZipFiles(): List<ZipInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<ZipInfo>()
            val rootDir = Environment.getExternalStorageDirectory()

            val foundFiles = mutableListOf<File>()
            collectZipFilesRecursively(rootDir, foundFiles, depth = 0)

            for (file in foundFiles) {
                val zipInfo = buildZipInfo(file)
                result.add(zipInfo)
            }
            result
        }
    }

    // Đệ quy quét thư mục
    private fun collectZipFilesRecursively(dir: File, output: MutableList<File>, depth: Int) {
        if (depth > MAX_SCAN_DEPTH) {
            return
        }
        val children = dir.listFiles()
        if (children == null) {
            return
        }
        for (child in children) {
            if (child.isDirectory) {
                if (SKIP_DIR_NAMES.contains(child.name) == false) {
                    collectZipFilesRecursively(child, output, depth + 1)
                }
            } else {
                val extension = child.extension.lowercase()
                if (ZIP_EXTENSIONS.contains(extension)) {
                    output.add(child)
                }
            }
        }
    }

    // Chuyển đổi File sang ZipInfo model
    fun buildZipInfo(file: File): ZipInfo {
        val extension = file.extension.lowercase()
        return ZipInfo(
            fileName = file.name,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            dateModifiedMillis = file.lastModified(),
            extension = extension
        )
    }

    // Lấy danh sách item (folder & file) trong một thư mục cụ thể
    fun getItemsInDirectory(directory: File): List<UnzippedItem> {
        val result = mutableListOf<UnzippedItem>()
        if (!directory.exists() || !directory.isDirectory) {
            return result
        }
        val files = directory.listFiles()
        if (files == null) {
            return result
        }
        val folderList = mutableListOf<UnzippedItem>()
        val fileList = mutableListOf<UnzippedItem>()
        for (file in files) {
            val isDir = file.isDirectory
            if (isDir) {
                // Đếm số lượng item con bên trong thư mục
                val childFiles = file.listFiles()
                var count = 0
                if (childFiles != null) {
                    count = childFiles.size
                }
                val item = UnzippedItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = true,
                    sizeBytes = 0L,
                    dateModifiedMillis = file.lastModified(),
                    itemCount = count,
                    extension = ""
                )
                folderList.add(item)
            } else {
                val item = UnzippedItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    sizeBytes = file.length(),
                    dateModifiedMillis = file.lastModified(),
                    itemCount = 0,
                    extension = file.extension.lowercase()
                )
                fileList.add(item)
            }
        }
        // Sắp xếp: Thư mục xếp theo tên A-Z, File xếp theo tên A-Z
        folderList.sortBy { it.name.lowercase() }
        fileList.sortBy { it.name.lowercase() }
        // Gộp thư mục lên trước, file theo sau
        result.addAll(folderList)
        result.addAll(fileList)
        return result
    }
}
