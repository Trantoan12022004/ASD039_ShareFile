package com.example.basekotlin.data.local.cleanfile

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.basekotlin.model.CleanFileGroup
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object DuplicateFileScanner {

    private const val TAG = "DuplicateFileScanner"

    suspend fun scanDuplicateFiles(context: Context): List<CleanFileGroup> {
        return withContext(Dispatchers.IO) {
            val allFiles = mutableListOf<CleanFileItem>()
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            // Lọc các file có kích thước > 1KB để tránh file rỗng
            val selection = "${MediaStore.Files.FileColumns.SIZE} > 1024"
            val sortOrder = "${MediaStore.Files.FileColumns.SIZE} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    val dateModCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val size = c.getLong(sizeCol)
                        val data = c.getString(dataCol) ?: ""
                        val name = c.getString(nameCol) ?: File(data).name
                        val dateMod = c.getLong(dateModCol) * 1000L
                        val mime = c.getString(mimeCol)
                        val type = BigFileScanner.resolveFileType(name, mime)
                        val folderName = File(data).parentFile?.name ?: ""

                        val thumbUri = when (type) {
                            CleanFileType.VIDEO ->
                                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                            CleanFileType.PHOTO ->
                                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                            else -> if (data.isNotEmpty()) "file://$data" else null
                        }

                        allFiles.add(
                            CleanFileItem(
                                name = name,
                                path = data,
                                sizeBytes = size,
                                dateModifiedMillis = dateMod,
                                type = type,
                                thumbnailUri = thumbUri,
                                folderName = folderName,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning files for duplicates: ${e.message}", e)
            }

            // Bước 1: Nhóm sơ bộ theo size
            val sizeGroups = allFiles.groupBy { it.sizeBytes }.filter { it.value.size > 1 }

            // Bước 2: Kiểm tra trùng lặp qua partial MD5 hoặc (name + size)
            val resultGroups = mutableListOf<CleanFileGroup>()
            var groupIndex = 1

            for ((_, candidates) in sizeGroups) {
                // Nhóm sâu hơn theo partial MD5 nếu file tồn tại, hoặc theo name nếu không đọc được file
                val subGroups = candidates.groupBy { item ->
                    val file = File(item.path)
                    if (file.exists() && file.canRead()) {
                        getQuickHash(file)
                    } else {
                        item.name
                    }
                }.filter { it.value.size > 1 }

                for ((_, duplicateItems) in subGroups) {
                    val firstItemName = duplicateItems.first().name
                    resultGroups.add(
                        CleanFileGroup(
                            groupName = "Group $groupIndex ($firstItemName)",
                            totalSizeBytes = duplicateItems.sumOf { it.sizeBytes },
                            items = duplicateItems.toMutableList(),
                            isExpanded = true
                        )
                    )
                    groupIndex++
                }
            }

            resultGroups.sortedByDescending { it.totalSizeBytes }
        }
    }

    // Đọc tối đa 8KB đầu file để băm nhanh, tránh đọc toàn bộ file nặng
    private fun getQuickHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                val read = fis.read(buffer)
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            // Thêm độ dài file vào hash để tăng tính chính xác
            digest.digest().joinToString("") { "%02x".format(it) } + "_${file.length()}"
        } catch (e: Exception) {
            file.name + "_" + file.length()
        }
    }
}
