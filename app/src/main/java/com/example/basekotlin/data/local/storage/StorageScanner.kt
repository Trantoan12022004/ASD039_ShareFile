package com.example.basekotlin.data.local.storage

import android.os.StatFs
import com.example.basekotlin.model.StorageItem
import com.example.basekotlin.model.StorageSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageScanner {

    // Giới hạn độ sâu khi quét đệ quy
    private const val MAX_SCAN_DEPTH = 12

    // Các thư mục hệ thống cần bỏ qua
    private val SKIP_DIR_NAMES = setOf("Android", ".thumbnails", ".trashed")

    // ======================================================
    // SCAN – Lấy danh sách item trong một thư mục cụ thể
    // ======================================================

    suspend fun getItemsInDirectory(
        directory: File,
        sortOption: StorageSortOption = StorageSortOption.NAME_A_Z
    ): List<StorageItem> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<StorageItem>()

            // Kiểm tra thư mục hợp lệ
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext result
            }

            val children = directory.listFiles()
            if (children == null) {
                return@withContext result
            }

            val folderList = mutableListOf<StorageItem>()
            val fileList = mutableListOf<StorageItem>()

            for (child in children) {
                // Bỏ qua thư mục hệ thống và file ẩn
                if (SKIP_DIR_NAMES.contains(child.name)) {
                    continue
                }

                if (child.isDirectory) {
                    // Đếm số item con trực tiếp bên trong thư mục
                    val childFiles = child.listFiles()
                    var count = 0
                    if (childFiles != null) {
                        count = childFiles.size
                    }

                    val item = StorageItem(
                        name = child.name,
                        path = child.absolutePath,
                        isDirectory = true,
                        sizeBytes = 0L,
                        dateModifiedMillis = child.lastModified(),
                        itemCount = count,
                        extension = ""
                    )
                    folderList.add(item)
                } else {
                    val item = StorageItem(
                        name = child.name,
                        path = child.absolutePath,
                        isDirectory = false,
                        sizeBytes = child.length(),
                        dateModifiedMillis = child.lastModified(),
                        itemCount = 0,
                        extension = child.extension.lowercase()
                    )
                    fileList.add(item)
                }
            }

            // Sắp xếp theo sortOption
            val sortedFolders = applySortToFolders(folderList, sortOption)
            val sortedFiles = applySortToFiles(fileList, sortOption)

            // Thư mục luôn đứng trên, file theo sau
            result.addAll(sortedFolders)
            result.addAll(sortedFiles)

            result
        }
    }

    // Sắp xếp danh sách thư mục theo sortOption
    private fun applySortToFolders(
        list: MutableList<StorageItem>,
        sortOption: StorageSortOption
    ): List<StorageItem> {
        return when (sortOption) {
            StorageSortOption.NAME_A_Z -> list.sortedBy { it.name.lowercase() }
            StorageSortOption.NAME_Z_A -> list.sortedByDescending { it.name.lowercase() }
            StorageSortOption.DATE_NEWEST -> list.sortedByDescending { it.dateModifiedMillis }
            StorageSortOption.DATE_OLDEST -> list.sortedBy { it.dateModifiedMillis }
            // Thư mục không có size thực → sắp xếp theo tên khi chọn size
            StorageSortOption.SIZE_BIG_SMALL -> list.sortedBy { it.name.lowercase() }
            StorageSortOption.SIZE_SMALL_BIG -> list.sortedBy { it.name.lowercase() }
        }
    }

    // Sắp xếp danh sách file theo sortOption
    private fun applySortToFiles(
        list: MutableList<StorageItem>,
        sortOption: StorageSortOption
    ): List<StorageItem> {
        return when (sortOption) {
            StorageSortOption.NAME_A_Z -> list.sortedBy { it.name.lowercase() }
            StorageSortOption.NAME_Z_A -> list.sortedByDescending { it.name.lowercase() }
            StorageSortOption.DATE_NEWEST -> list.sortedByDescending { it.dateModifiedMillis }
            StorageSortOption.DATE_OLDEST -> list.sortedBy { it.dateModifiedMillis }
            StorageSortOption.SIZE_BIG_SMALL -> list.sortedByDescending { it.sizeBytes }
            StorageSortOption.SIZE_SMALL_BIG -> list.sortedBy { it.sizeBytes }
        }
    }

    // ======================================================
    // CRUD – Tạo / Đổi tên / Xóa
    // ======================================================

    // Tạo thư mục mới bên trong thư mục cha
    // Trả về true nếu tạo thành công
    fun createFolder(parent: File, name: String): Boolean {
        // Kiểm tra tên không rỗng
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return false
        }

        val newFolder = File(parent, trimmedName)

        // Kiểm tra nếu đã tồn tại thì không tạo
        if (newFolder.exists()) {
            return false
        }

        return newFolder.mkdirs()
    }

    // Đổi tên file hoặc thư mục
    // Trả về true nếu đổi tên thành công
    fun rename(target: File, newName: String): Boolean {
        // Kiểm tra tên không rỗng
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) {
            return false
        }

        // Kiểm tra target có tồn tại không
        if (!target.exists()) {
            return false
        }

        val parent = target.parentFile
        if (parent == null) {
            return false
        }

        val destination = File(parent, trimmedName)

        // Kiểm tra file đích đã tồn tại chưa (tránh ghi đè)
        if (destination.exists()) {
            return false
        }

        return target.renameTo(destination)
    }

    // Xóa file hoặc thư mục (xóa đệ quy nếu là thư mục)
    // Trả về true nếu xóa hoàn toàn thành công
    fun delete(target: File): Boolean {
        if (!target.exists()) {
            return false
        }

        if (target.isDirectory) {
            // Xóa đệ quy toàn bộ nội dung bên trong
            return deleteRecursively(target)
        }

        return target.delete()
    }

    // Đệ quy xóa toàn bộ thư mục và nội dung
    private fun deleteRecursively(dir: File): Boolean {
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                if (child.isDirectory) {
                    val success = deleteRecursively(child)
                    if (!success) {
                        return false
                    }
                } else {
                    val deleted = child.delete()
                    if (!deleted) {
                        return false
                    }
                }
            }
        }
        // Xóa thư mục cha sau khi đã xóa sạch nội dung
        return dir.delete()
    }

    // ======================================================
    // STORAGE INFO – Thông tin dung lượng bộ nhớ
    // ======================================================

    // Đọc thông tin dung lượng từ một thư mục root (dùng StatFs)
    fun getStorageInfo(rootDir: File): StorageInfo {
        val stat = StatFs(rootDir.absolutePath)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes

        // Tính phần trăm đã dùng
        val usedPercentage = if (totalBytes > 0) {
            ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
        } else {
            0
        }

        return StorageInfo(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            usedPercentage = usedPercentage
        )
    }

    // Tính tổng dung lượng của một thư mục (đệ quy)
    fun calculateFolderSize(folder: File): Long {
        var totalSize = 0L
        if (!folder.exists()) {
            return totalSize
        }

        val children = folder.listFiles()
        if (children == null) {
            return totalSize
        }

        for (child in children) {
            if (child.isDirectory) {
                totalSize += calculateFolderSize(child)
            } else {
                totalSize += child.length()
            }
        }

        return totalSize
    }

    // Data class phụ trợ – chứa thông tin dung lượng thô
    // (dùng nội bộ trong Scanner, ViewModel sẽ format lại)
    data class StorageInfo(
        val totalBytes: Long,
        val usedBytes: Long,
        val freeBytes: Long,
        val usedPercentage: Int
    )
}
