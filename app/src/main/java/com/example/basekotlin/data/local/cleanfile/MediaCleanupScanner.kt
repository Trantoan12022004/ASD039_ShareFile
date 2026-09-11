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

object MediaCleanupScanner {

    private const val TAG = "MediaCleanupScanner"

    suspend fun scanVideos(context: Context): List<CleanFileGroup> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<CleanFileItem>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.MIME_TYPE
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val dateModCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val size = c.getLong(sizeCol)
                        val data = c.getString(dataCol) ?: ""
                        val name = c.getString(nameCol) ?: File(data).name
                        val dateMod = c.getLong(dateModCol) * 1000L
                        val mime = c.getString(mimeCol)
                        val folderName = File(data).parentFile?.name ?: "Other"
                        val thumbUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                        items.add(
                            CleanFileItem(
                                name = name,
                                path = data,
                                sizeBytes = size,
                                dateModifiedMillis = dateMod,
                                type = CleanFileType.VIDEO,
                                thumbnailUri = thumbUri,
                                folderName = folderName,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning videos: ${e.message}", e)
            }

            groupItemsByFolder(items)
        }
    }

    suspend fun scanPhotos(context: Context): List<CleanFileGroup> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<CleanFileItem>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.MIME_TYPE
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val dateModCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                    val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val size = c.getLong(sizeCol)
                        val data = c.getString(dataCol) ?: ""
                        val name = c.getString(nameCol) ?: File(data).name
                        val dateMod = c.getLong(dateModCol) * 1000L
                        val mime = c.getString(mimeCol)
                        val folderName = File(data).parentFile?.name ?: "Other"
                        val thumbUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()

                        items.add(
                            CleanFileItem(
                                name = name,
                                path = data,
                                sizeBytes = size,
                                dateModifiedMillis = dateMod,
                                type = CleanFileType.PHOTO,
                                thumbnailUri = thumbUri,
                                folderName = folderName,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning photos: ${e.message}", e)
            }

            groupItemsByFolder(items)
        }
    }

    suspend fun scanAudios(context: Context): List<CleanFileGroup> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<CleanFileItem>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val dateModCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val size = c.getLong(sizeCol)
                        val data = c.getString(dataCol) ?: ""
                        val name = c.getString(nameCol) ?: File(data).name
                        val dateMod = c.getLong(dateModCol) * 1000L
                        val mime = c.getString(mimeCol)
                        val folderName = File(data).parentFile?.name ?: "Other"
                        val thumbUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                        items.add(
                            CleanFileItem(
                                name = name,
                                path = data,
                                sizeBytes = size,
                                dateModifiedMillis = dateMod,
                                type = CleanFileType.AUDIO,
                                thumbnailUri = thumbUri,
                                folderName = folderName,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning audios: ${e.message}", e)
            }

            groupItemsByFolder(items)
        }
    }

    private fun groupItemsByFolder(items: List<CleanFileItem>): List<CleanFileGroup> {
        val grouped = items.groupBy { it.folderName }
        return grouped.map { (folder, fileList) ->
            CleanFileGroup(
                groupName = folder,
                totalSizeBytes = fileList.sumOf { it.sizeBytes },
                items = fileList.toMutableList(),
                isExpanded = true
            )
        }.sortedByDescending { it.totalSizeBytes }
    }
}
