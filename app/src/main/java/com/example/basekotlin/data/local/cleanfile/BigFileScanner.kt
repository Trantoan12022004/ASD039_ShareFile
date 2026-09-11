package com.example.basekotlin.data.local.cleanfile

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BigFileScanner {

    private const val TAG = "BigFileScanner"
    // Ngưỡng mặc định 50MB
    const val DEFAULT_MIN_SIZE_BYTES = 50L * 1024L * 1024L

    suspend fun scanBigFiles(
        context: Context,
        minSizeBytes: Long = DEFAULT_MIN_SIZE_BYTES
    ): List<CleanFileItem> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<CleanFileItem>()
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            val selection = "${MediaStore.Files.FileColumns.SIZE} >= ?"
            val selectionArgs = arrayOf(minSizeBytes.toString())
            val sortOrder = "${MediaStore.Files.FileColumns.SIZE} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameColumn = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dataColumn = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    val dateModifiedColumn = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val mimeColumn = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idColumn)
                        val size = c.getLong(sizeColumn)
                        val data = c.getString(dataColumn) ?: ""
                        val name = c.getString(nameColumn) ?: File(data).name
                        val dateModified = c.getLong(dateModifiedColumn) * 1000L
                        val mimeType = c.getString(mimeColumn)

                        val fileType = resolveFileType(name, mimeType)
                        val thumbnailUri = resolveThumbnailUri(id, fileType, data)
                        val folderName = File(data).parentFile?.name ?: ""

                        result.add(
                            CleanFileItem(
                                name = name,
                                path = data,
                                sizeBytes = size,
                                dateModifiedMillis = dateModified,
                                type = fileType,
                                thumbnailUri = thumbnailUri,
                                folderName = folderName,
                                mimeType = mimeType
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning big files: ${e.message}", e)
            }

            result
        }
    }

    fun resolveFileType(fileName: String, mimeType: String?): CleanFileType {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            mimeType?.startsWith("video/") == true || extension in listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv") ->
                CleanFileType.VIDEO
            mimeType?.startsWith("image/") == true || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic") ->
                CleanFileType.PHOTO
            mimeType?.startsWith("audio/") == true || extension in listOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma") ->
                CleanFileType.AUDIO
            extension == "apk" ->
                CleanFileType.APK
            extension in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") ->
                CleanFileType.DOCUMENT
            else ->
                CleanFileType.OTHER
        }
    }

    private fun resolveThumbnailUri(id: Long, type: CleanFileType, filePath: String): String? {
        return when (type) {
            CleanFileType.VIDEO ->
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
            CleanFileType.PHOTO ->
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
            else ->
                if (filePath.isNotEmpty()) "file://$filePath" else null
        }
    }
}
