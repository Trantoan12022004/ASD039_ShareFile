package com.example.basekotlin.util

import android.content.Context
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepositoryImpl
import com.example.basekotlin.data.local.safebox.SafeBoxFileType

object SafeBoxHelper {

    // Di chuyển 1 file vào SafeBox
    suspend fun moveToSafeBox(
        context: Context,
        filePath: String,
        fileType: SafeBoxFileType
    ): Boolean {
        val repository = SafeBoxRepositoryImpl(context)
        val result = repository.moveFileToSafeBox(filePath, fileType)
        return result.isSuccess
    }

    // Di chuyển nhiều file vào SafeBox hàng loạt
    suspend fun moveMultipleToSafeBox(
        context: Context,
        filePaths: List<String>,
        fileType: SafeBoxFileType
    ): Int {
        val repository = SafeBoxRepositoryImpl(context)
        var count = 0
        for (path in filePaths) {
            val result = repository.moveFileToSafeBox(path, fileType)
            if (result.isSuccess) count++
        }
        return count
    }
}
