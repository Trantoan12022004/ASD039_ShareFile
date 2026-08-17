package com.example.basekotlin.data.local.photostore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.basekotlin.model.PhotoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStorePhotoSource {

    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.RELATIVE_PATH,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
    )

    suspend fun queryAllPhotos(context: Context): List<PhotoInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<PhotoInfo>()
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            if (cursor != null) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val photoId = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        photoId
                    )

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

                    val photo = PhotoInfo(
                        id = photoId,
                        displayName = displayName,
                        filePath = filePath,
                        relativeFolderPath = relativeFolderPath,
                        sizeBytes = sizeBytes,
                        dateAddedSeconds = dateAddedSeconds,
                        dateModifiedSeconds = dateModifiedSeconds,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        mimeType = mimeType,
                        contentUri = contentUri
                    )
                    result.add(photo)
                }
                cursor.close()
            }
            result
        }
    }
}