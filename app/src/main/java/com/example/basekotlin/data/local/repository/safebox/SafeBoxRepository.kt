package com.example.basekotlin.data.local.repository.safebox

import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import kotlinx.coroutines.flow.Flow

interface SafeBoxRepository {

    fun observeAllFiles(): Flow<List<SafeBoxFile>>
    // Luồng dữ liệu danh sách file theo từng Category
    fun observeFilesByType(fileType: SafeBoxFileType): Flow<List<SafeBoxFile>>

    // Đếm số lượng file theo loại cho Màn hình Home
    fun observeCountByType(fileType: SafeBoxFileType): Flow<Int>

    // Di chuyển file vào SafeBox
    suspend fun moveFileToSafeBox(originalPath: String, fileType: SafeBoxFileType): Result<SafeBoxFile>

    // Khôi phục file ra khỏi SafeBox
    suspend fun restoreFile(file: SafeBoxFile): Result<Boolean>

    // Xóa vĩnh viễn file
    suspend fun deletePermanently(file: SafeBoxFile): Result<Boolean>
}
