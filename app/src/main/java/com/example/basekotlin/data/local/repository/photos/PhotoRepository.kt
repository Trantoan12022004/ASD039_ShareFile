package com.example.basekotlin.data.local.repository.photos

import com.example.basekotlin.model.PhotoFolder
import com.example.basekotlin.model.PhotoInfo
import kotlinx.coroutines.flow.Flow

// Repository — nơi ghép MediaStore.Images thành dữ liệu cho 3 tab All / Folder / Received
interface PhotoRepository {
    fun observeAllPhotos(): Flow<List<PhotoInfo>>
    fun observeFolders(): Flow<List<PhotoFolder>>
    fun observePhotosByFolder(folderPath: String): Flow<List<PhotoInfo>>
    fun observeReceivedPhotos(): Flow<List<PhotoInfo>>

    suspend fun refreshAllPhotos()
}