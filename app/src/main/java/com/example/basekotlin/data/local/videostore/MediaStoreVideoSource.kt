package com.example.basekotlin.data.local.videostore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.basekotlin.model.PhotoInfo
import com.example.basekotlin.ui.files.video.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreVideoSource {

    private val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.RELATIVE_PATH,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.MIME_TYPE,
    )


        suspend fun queryAllVideos(context: Context): List<VideoInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<VideoInfo>()
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            if (cursor != null) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val videoId = cursor.getLong(idColumn)
                    var displayName = cursor.getString(nameColumn)
                    if (displayName == null) {
                        displayName = "Unknown"
                    }
                    var filePath = cursor.getString(dataColumn)
                    if (filePath == null) {
                        filePath = ""
                    }

                    var relativeFolderPath = cursor.getString(relativePathColumn)
                    if (relativeFolderPath == null) {
                        relativeFolderPath = ""
                    }

                    var mimeType = cursor.getString(mimeColumn)
                    if (mimeType == null) {
                        mimeType = ""
                    }

                    val sizeBytes = cursor.getLong(sizeColumn)
                    val dateAddedSeconds = cursor.getLong(dateAddedColumn)
                    val dateModifiedSeconds = cursor.getLong(dateModifiedColumn)
                    val widthPx = cursor.getInt(widthColumn)
                    val heightPx = cursor.getInt(heightColumn)
                    val durationMs = cursor.getLong(durationColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        videoId
                    )
    
                    val video = VideoInfo(
                        id = videoId,
                        displayName = displayName,
                        filePath = filePath,
                        relativeFolderPath = relativeFolderPath,
                        sizeBytes = sizeBytes,
                        dateAddedSeconds = dateAddedSeconds,
                        dateModifiedSeconds = dateModifiedSeconds,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        mimeType = mimeType,
                        contentUri = contentUri,
                        durationMs = durationMs
                    )
                    result.add(video)
                }
                cursor.close()
            }
            result
        }
    }
}