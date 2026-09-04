package com.example.basekotlin.data.local.repository.storage

import android.content.Context
import android.os.Environment
import com.example.basekotlin.data.local.storage.StorageScanner
import com.example.basekotlin.model.StorageItem
import com.example.basekotlin.model.StorageSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StorageRepositoryImpl(private val context: Context) : StorageRepository {

    // Trả về thư mục root bộ nhớ ngoài (ví dụ: /storage/emulated/0)
    override fun getRootDirectory(): File {
        return Environment.getExternalStorageDirectory()
    }

    // Gọi Scanner để lấy danh sách item, chạy trên IO thread
    override suspend fun fetchItemsInDirectory(
        dir: File,
        sortOption: StorageSortOption
    ): List<StorageItem> {
        return StorageScanner.getItemsInDirectory(dir, sortOption)
    }

    // Tạo thư mục mới, chạy trên IO thread
    override suspend fun createFolder(parent: File, name: String): Boolean {
        return withContext(Dispatchers.IO) {
            StorageScanner.createFolder(parent, name)
        }
    }

    // Đổi tên, chạy trên IO thread
    override suspend fun rename(target: File, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            StorageScanner.rename(target, newName)
        }
    }

    // Xóa file hoặc thư mục, chạy trên IO thread
    override suspend fun delete(target: File): Boolean {
        return withContext(Dispatchers.IO) {
            StorageScanner.delete(target)
        }
    }

    // Đọc thông tin dung lượng bộ nhớ (đồng bộ, nhẹ)
    override fun getStorageInfo(rootDir: File): StorageScanner.StorageInfo {
        return StorageScanner.getStorageInfo(rootDir)
    }
}
