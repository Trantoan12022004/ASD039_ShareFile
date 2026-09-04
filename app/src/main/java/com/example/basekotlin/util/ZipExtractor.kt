package com.example.basekotlin.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipExtractor {

    private const val BUFFER_SIZE = 8192

    /**
     * Giải nén file zip vào thư mục ShareFile/Zip/Extract/<Tên_File_Zip>
     *
     * @param context Context ứng dụng
     * @param zipFilePath Đường dẫn tuyệt đối đến file zip nguồn
     * @return File đại diện cho thư mục đã giải nén thành công, hoặc null nếu xảy ra lỗi
     */
    fun extractZipFile(context: Context, zipFilePath: String): File? {
        val sourceFile = File(zipFilePath)
        // Kiểm tra file nguồn có tồn tại và hợp lệ không
        if (!sourceFile.exists() || !sourceFile.canRead()) {
            return null
        }

        // 1. Tạo thư mục cha cơ sở: ShareFile/Zip/Extract
        val rootDir = Environment.getExternalStorageDirectory()
        val extractBaseDir = File(rootDir, "ShareFile/Zip/Extract")
        if (!extractBaseDir.exists()) {
            extractBaseDir.mkdirs()
        }

        // 2. Tạo thư mục riêng cho file zip này dựa trên tên file (không tính đuôi mở rộng)
        val baseFolderName = sourceFile.nameWithoutExtension
        var targetFolder = File(extractBaseDir, baseFolderName)
        var duplicateIndex = 1

        // Nếu thư mục đã tồn tại thì thêm hậu tố (1), (2),... để không bị đè dữ liệu cũ
        while (targetFolder.exists()) {
            val newFolderName = "$baseFolderName ($duplicateIndex)"
            targetFolder = File(extractBaseDir, newFolderName)
            duplicateIndex = duplicateIndex + 1
        }

        val folderCreated = targetFolder.mkdirs()
        if (!folderCreated && !targetFolder.exists()) {
            return null
        }

        val extractedFiles = mutableListOf<String>()
        var isSuccess = false

        var fileInputStream: FileInputStream? = null
        var bufferedInputStream: BufferedInputStream? = null
        var zipInputStream: ZipInputStream? = null

        try {
            fileInputStream = FileInputStream(sourceFile)
            bufferedInputStream = BufferedInputStream(fileInputStream)
            zipInputStream = ZipInputStream(bufferedInputStream)

            val canonicalDestDirPath = targetFolder.canonicalPath
            var entry: ZipEntry? = zipInputStream.nextEntry

            // 3. Lặp qua từng entry trong file zip
            while (entry != null) {
                val entryFile = File(targetFolder, entry.name)
                val canonicalEntryPath = entryFile.canonicalPath

                // Kiểm tra bảo mật Zip Slip Vulnerability
                if (!canonicalEntryPath.startsWith(canonicalDestDirPath + File.separator)) {
                    throw SecurityException("Zip Slip detected: ${entry.name}")
                }

                if (entry.isDirectory) {
                    // Nếu là thư mục thì tạo thư mục
                    if (!entryFile.exists()) {
                        entryFile.mkdirs()
                    }
                } else {
                    // Nếu là file, đảm bảo thư mục cha của file đã được tạo
                    val parentFolder = entryFile.parentFile
                    if (parentFolder != null && !parentFolder.exists()) {
                        parentFolder.mkdirs()
                    }

                    // Ghi nội dung file ra bộ nhớ
                    val fileOutputStream = FileOutputStream(entryFile)
                    val bufferedOutputStream = BufferedOutputStream(fileOutputStream, BUFFER_SIZE)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead = zipInputStream.read(buffer)

                    while (bytesRead != -1) {
                        bufferedOutputStream.write(buffer, 0, bytesRead)
                        bytesRead = zipInputStream.read(buffer)
                    }

                    bufferedOutputStream.flush()
                    bufferedOutputStream.close()
                    fileOutputStream.close()

                    extractedFiles.add(entryFile.absolutePath)
                }

                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }

            isSuccess = true
        } catch (e: Exception) {
            e.printStackTrace()
            isSuccess = false
        } finally {
            try {
                zipInputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                bufferedInputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                fileInputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Quét MediaScanner để hệ thống nhận diện file đã giải nén
        if (extractedFiles.isNotEmpty()) {
            val pathArray = extractedFiles.toTypedArray()
            MediaScannerConnection.scanFile(
                context.applicationContext,
                pathArray,
                null,
                null
            )
        }

        if (isSuccess) {
            return targetFolder
        } else {
            // Nếu giải nén thất bại, dọn dẹp thư mục lỗi
            if (targetFolder.exists()) {
                targetFolder.deleteRecursively()
            }
            return null
        }
    }
}
