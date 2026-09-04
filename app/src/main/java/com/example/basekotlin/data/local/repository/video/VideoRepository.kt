package com.example.basekotlin.data.local.repository.video

import com.example.basekotlin.model.PhotoFolder
import com.example.basekotlin.model.VideoInfo
import com.example.basekotlin.ui.files.video.model.VideoFolder
import com.example.basekotlin.ui.files.video.model.VideoInfo
import kotlinx.coroutines.flow.Flow

// Repository — nơi ghép MediaStore.Images thành dữ liệu cho 3 tab All / Folder / Received
interface VideoRepository {
    fun observeAllVideos(): Flow<List<VideoInfo>>
    fun observeFolders(): Flow<List<VideoFolder>>
    fun observeVideosByFolder(folderPath: String): Flow<List<VideoInfo>>
    fun observeReceivedVideos(): Flow<List<VideoInfo>>

    suspend fun refreshAllVideos()
}