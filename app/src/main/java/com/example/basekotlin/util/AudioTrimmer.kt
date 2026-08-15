package com.example.basekotlin.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object AudioTrimmer {

    private const val TAG = "DEBUG_RINGTONE"

    fun trim(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): Boolean {
        var inputTempFile: File? = null

        try {
            // Bước 1: FFmpeg cần path file, không đọc content:// trực tiếp ổn định
            inputTempFile = copyUriToTempFile(context, inputUri)
            if (inputTempFile == null) {
                Log.e(TAG, "copyUriToTempFile failed")
                return false
            }

            // Bước 2: đổi ms → giây
            val startSec = startMs / 1000.0
            val endSec = endMs / 1000.0

            // Bước 3: xóa output cũ nếu có
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Bước 4: ghép lệnh FFmpeg
            val command = buildTrimCommand(
                inputPath = inputTempFile.absolutePath,
                outputPath = outputFile.absolutePath,
                startSec = startSec,
                endSec = endSec
            )
            Log.d(TAG, "ffmpeg command=$command")

            // Bước 5: chạy FFmpeg (blocking — gọi từ Dispatchers.IO)
            val session = FFmpegKit.execute(command)
            val returnCode = session.returnCode
            val failStackTrace = session.failStackTrace

            Log.d(TAG, "ffmpeg returnCode=$returnCode")
            if (failStackTrace != null) {
                Log.e(TAG, "ffmpeg failStackTrace=$failStackTrace")
            }

            if (!ReturnCode.isSuccess(returnCode)) {
                Log.e(TAG, "ffmpeg failed, output=${session.output}")
                return false
            }

            if (!outputFile.exists()) {
                Log.e(TAG, "output file not created")
                return false
            }

            if (outputFile.length() == 0L) {
                Log.e(TAG, "output file is empty")
                return false
            }

            Log.d(TAG, "trim success, size=${outputFile.length()}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrimmer error", e)
            return false
        } finally {
            if (inputTempFile != null) {
                if (inputTempFile.exists()) {
                    inputTempFile.delete()
                }
            }
        }
    }

    private fun buildTrimCommand(
        inputPath: String,
        outputPath: String,
        startSec: Double,
        endSec: Double
    ): String {
        // -ss/-to sau -i: cắt chính xác hơn (phù hợp ringtone)
        // -map 0:a: chỉ lấy audio stream, bỏ qua video/cover art (attached pic)
        // nhiều file MP3 nhúng sẵn ảnh bìa dưới dạng 1 video stream, nếu không loại bỏ
        // FFmpeg sẽ cố mux luôn stream đó vào container m4a và báo "Conversion failed!"
        // -c:a aac: MP3 → AAC trong container m4a
        // -b:a 128k: bitrate nhạc chuông
        // -y: ghi đè nếu file output đã tồn tại
        return String.format(
            Locale.US,
            "-i \"%s\" -ss %.3f -to %.3f -map 0:a -c:a aac -b:a 128k -y \"%s\"",
            inputPath,
            startSec,
            endSec,
            outputPath
        )
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        var tempFile: File? = null
        var outputStream: FileOutputStream? = null

        try {
            tempFile = File(
                context.cacheDir,
                "ffmpeg_input_${System.currentTimeMillis()}.tmp"
            )

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return null
            }

            outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(8192)
            var readCount = 0

            while (true) {
                readCount = inputStream.read(buffer)
                if (readCount <= 0) {
                    break
                }
                outputStream.write(buffer, 0, readCount)
            }

            inputStream.close()
            outputStream.close()

            if (tempFile.length() == 0L) {
                return null
            }

            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "copyUriToTempFile error", e)
            if (tempFile != null) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            return null
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }
}