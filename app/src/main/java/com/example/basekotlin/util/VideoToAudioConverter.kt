package com.example.basekotlin.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object VideoToAudioConverter {

    suspend fun convertVideoToMp3(
        context: Context,
        videoPath: String,
        outputFileName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val finalName = if (outputFileName.endsWith(".mp3", ignoreCase = true)) {
                outputFileName
            } else {
                "$outputFileName.mp3"
            }

            // Lưu file mp3 vào thư mục Music/ShareFile/Converted/
            val musicDir = Environment.getExternalStorageDirectory()
            val targetDir = File(musicDir, "ShareFile/Videos/Converted")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val outputFile = File(targetDir, finalName)

            // Lệnh FFmpeg: trích xuất audio stream, mã hóa mp3 bitrate 192k
            val command = String.format(
                Locale.US,
                "-i \"%s\" -vn -acodec libmp3lame -ab 192k -ar 44100 -y \"%s\"",
                videoPath,
                outputFile.absolutePath
            )

            val session = FFmpegKit.execute(command)
            if (ReturnCode.isSuccess(session.returnCode) && outputFile.exists() && outputFile.length() > 0) {
                // Quét MediaScanner để hiển thị ngay trong hệ thống
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("audio/mpeg"),
                    null
                )
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
