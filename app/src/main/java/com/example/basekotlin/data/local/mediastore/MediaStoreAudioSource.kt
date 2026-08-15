package com.example.basekotlin.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.basekotlin.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreAudioSource {

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.RELATIVE_PATH,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DISPLAY_NAME,
    )

    @OptIn(UnstableApi::class)
    suspend fun queryAllTracks(context: Context): List<MusicTrack> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<MusicTrack>()

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)


                while (cursor.moveToNext()) {
                    val songId = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        songId
                    )

                    var title = cursor.getString(titleColumn)
                    if (title == null) {
                        title = "Unknown"
                    } else {
                        // Bỏ phần mở rộng (.mp3, .flac...) cho giống cách hiển thị title thông thường
                        val dotIndex = title.lastIndexOf('.')
                        if (dotIndex > 0) {
                            title = title.substring(0, dotIndex)
                        }
                    }
                    var artist = cursor.getString(artistColumn)
                    if (artist == null) {
                        artist = "Unknown artist"
                    }
                    var album = cursor.getString(albumColumn)
                    if (album == null) {
                        album = "Unknown album"
                    }
                    var filePath = cursor.getString(dataColumn)
                    if (filePath == null) {
                        filePath = ""
                    }
                    var relativeFolderPath = cursor.getString(relativePathColumn)
                    if (relativeFolderPath == null) {
                        relativeFolderPath = ""
                    }

                    val albumId = cursor.getLong(albumIdColumn)
                    val durationMs = cursor.getLong(durationColumn)
                    val sizeBytes = cursor.getLong(sizeColumn)
                    val dateAddedSeconds = cursor.getLong(dateAddedColumn)
                    val dateModifiedSeconds = cursor.getLong(dateModifiedColumn)

                    val track = MusicTrack(
                        id = songId,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        sizeBytes = sizeBytes,
                        filePath = filePath,
                        relativeFolderPath = relativeFolderPath,
                        dateAddedSeconds = dateAddedSeconds,
                        dateModifiedSeconds = dateModifiedSeconds,
                        albumId = albumId,
                        contentUri = contentUri
                    )
                    result.add(track)
                }
                cursor.close()
            }

            result
        }
    }

    private const val MIN_DURATION_MS = 20_000L
}