package com.example.basekotlin.data.local.repository.safebox

import android.content.Context
import android.media.MediaScannerConnection
import com.example.basekotlin.data.local.safebox.SafeBoxDatabase
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class SafeBoxRepositoryImpl(
    private val appContext: Context,
    private val database: SafeBoxDatabase = SafeBoxDatabase.getInstance(appContext)
) : SafeBoxRepository {

    private val safeBoxDao = database.safeBoxDao()

    // Thư mục lưu trữ an toàn trong Internal Storage: files/safebox
    private val safeBoxDirectory: File by lazy {
        val dir = File(appContext.filesDir, "safebox")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // Tạo file .nomedia để MediaScanner bỏ qua thư mục này
        val noMediaFile = File(dir, ".nomedia")
        if (!noMediaFile.exists()) {
            runCatching { noMediaFile.createNewFile() }
        }
        dir
    }

    override fun observeAllFiles(): Flow<List<SafeBoxFile>> {
        return safeBoxDao.observeAllFiles()
    }

    override fun observeFilesByType(fileType: SafeBoxFileType): Flow<List<SafeBoxFile>> {
        return safeBoxDao.observeFilesByType(fileType)
    }

    override fun observeCountByType(fileType: SafeBoxFileType): Flow<Int> {
        return safeBoxDao.observeCountByType(fileType)
    }

    override suspend fun moveFileToSafeBox(
        originalPath: String,
        fileType: SafeBoxFileType
    ): Result<SafeBoxFile> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(originalPath)
            if (!sourceFile.exists() || !sourceFile.isFile) {
                throw IllegalArgumentException("File nguồn không tồn tại: $originalPath")
            }

            // Tạo file đích với tên ngẫu nhiên UUID để che giấu nội dung
            val fileExtension = sourceFile.extension
            val internalName = if (fileExtension.isNotEmpty()) {
                "${UUID.randomUUID()}.$fileExtension"
            } else {
                "${UUID.randomUUID()}.dat"
            }
            val destinationFile = File(safeBoxDirectory, internalName)

            // Copy file từ nguồn sang thư mục nội bộ an toàn
            copyFile(sourceFile, destinationFile)

            // Lưu record vào Room Database
            val entity = SafeBoxFile(
                originalPath = sourceFile.absolutePath,
                safeboxPath = destinationFile.absolutePath,
                fileType = fileType,
                fileName = sourceFile.name,
                fileSize = sourceFile.length(),
                dateAdded = System.currentTimeMillis()
            )
            val insertedId = safeBoxDao.insert(entity)

            // Xóa file nguồn bên ngoài bộ nhớ máy
            val deleted = sourceFile.delete()

            // Thông báo MediaScanner để cập nhật xóa file khỏi MediaStore ngay lập tức
            scanMediaStore(sourceFile.absolutePath)

            entity.copy(id = insertedId)
        }
    }

    override suspend fun restoreFile(file: SafeBoxFile): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val safeboxFile = File(file.safeboxPath)
            if (!safeboxFile.exists()) {
                // File vật lý không còn, xóa record trong DB
                safeBoxDao.deleteById(file.id)
                throw IllegalStateException("File trong SafeBox không tồn tại")
            }

            // Xác định đường dẫn khôi phục
            val originalTarget = File(file.originalPath)
            val parentDir = originalTarget.parentFile ?: appContext.filesDir
            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }

            // Tránh ghi đè nếu file cùng tên đã xuất hiện ở đường dẫn gốc
            val destinationFile = getUniqueRestoreFile(parentDir, file.fileName)

            // Copy file từ SafeBox trả ngược lại thư mục gốc
            copyFile(safeboxFile, destinationFile)

            // Xóa file trong SafeBox và xóa bản ghi DB
            safeboxFile.delete()
            safeBoxDao.deleteById(file.id)

            // Báo cho MediaScanner quét file vừa restore để hiện lại trong Gallery/Files
            scanMediaStore(destinationFile.absolutePath)

            true
        }
    }

    override suspend fun deletePermanently(file: SafeBoxFile): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val safeboxFile = File(file.safeboxPath)
            if (safeboxFile.exists()) {
                safeboxFile.delete()
            }
            safeBoxDao.deleteById(file.id)
            true
        }
    }

    // Hàm copy stream an toàn
    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    // Xử lý tạo tên mới nếu file gốc bị trùng tên khi khôi phục
    private fun getUniqueRestoreFile(parentDir: File, fileName: String): File {
        var file = File(parentDir, fileName)
        if (!file.exists()) return file

        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension
        var count = 1
        while (file.exists()) {
            val newName = if (ext.isNotEmpty()) {
                "${nameWithoutExt}_($count).$ext"
            } else {
                "${nameWithoutExt}_($count)"
            }
            file = File(parentDir, newName)
            count++
        }
        return file
    }

    // Thông báo cho hệ thống Android quét lại file qua MediaScannerConnection
    private fun scanMediaStore(filePath: String) {
        runCatching {
            MediaScannerConnection.scanFile(
                appContext,
                arrayOf(filePath),
                null,
                null
            )
        }
    }
}
