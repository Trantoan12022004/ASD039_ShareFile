package com.example.basekotlin.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object RingtoneHelper {

    fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Uri? {
        val resolver = context.contentResolver

        val values = ContentValues()
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "${displayName}_ringtone.mp3")
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true)
        values.put(MediaStore.Audio.Media.IS_MUSIC, false)
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
        values.put(MediaStore.Audio.Media.IS_ALARM, false)
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, "ShareFile/Music/Ringtones/")
        values.put(MediaStore.Audio.Media.IS_PENDING, 1)

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            return null
        }

        val inputStream = sourceFile.inputStream()
        val outputStream = resolver.openOutputStream(uri)
        if (outputStream == null) {
            resolver.delete(uri, null, null)
            inputStream.close()
            return null
        }

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        val publishValues = ContentValues()
        publishValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, publishValues, null, null)

        return uri
    }
}