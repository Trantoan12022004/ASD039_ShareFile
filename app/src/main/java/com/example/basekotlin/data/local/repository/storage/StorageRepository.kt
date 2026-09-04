package com.example.basekotlin.data.local.repository.storage

import com.example.basekotlin.data.local.storage.StorageScanner
import com.example.basekotlin.model.StorageItem
import com.example.basekotlin.model.StorageSortOption
import java.io.File

interface StorageRepository {

    // Lấy thư mục root của bộ nhớ ngoài
    fun getRootDirectory(): File

    // Lấy danh sách item trong một thư mục
    suspend fun fetchItemsInDirectory(
        dir: File,
        sortOption: StorageSortOption
    ): List<StorageItem>

    // Tạo thư mục mới
    suspend fun createFolder(parent: File, name: String): Boolean

    // Đổi tên file hoặc thư mục
    suspend fun rename(target: File, newName: String): Boolean

    // Xóa file hoặc thư mục
    suspend fun delete(target: File): Boolean

    // Đọc thông tin dung lượng bộ nhớ
    fun getStorageInfo(rootDir: File): StorageScanner.StorageInfo
}
