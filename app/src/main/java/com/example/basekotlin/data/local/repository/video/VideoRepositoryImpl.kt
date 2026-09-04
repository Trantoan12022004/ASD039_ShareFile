package com.example.basekotlin.data.local.repository.video

import android.content.Context
import android.net.Uri
import com.example.basekotlin.data.local.photostore.MediaStorePhotoSource
import com.example.basekotlin.data.local.photostore.PhotoStoreObserver
import com.example.basekotlin.data.local.videostore.MediaStoreVideoSource
import com.example.basekotlin.data.local.videostore.VideoStoreObserver
import com.example.basekotlin.ui.files.video.model.VideoFolder
import com.example.basekotlin.ui.files.video.model.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import java.io.File

class VideoRepositoryImpl(private val appContext: Context) : VideoRepository {

    // Scope riêng của repository, sống cùng vòng đời app,
    // để nhiều ViewModel/Fragment (All, Folder, Received) chia sẻ chung 1 ContentObserver
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Trigger thủ công cho pull-to-refresh, gộp chung với tín hiệu từ ContentObserver
    private val manualRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // shareIn(replay = 1): chỉ đăng ký 1 ContentObserver duy nhất cho toàn app
    private val allVideosFlow: Flow<List<VideoInfo>> = merge(
        VideoStoreObserver.observeVideoStoreChanges(appContext),
        manualRefreshTrigger
    )
        .onStart { emit(Unit) }
        .map { MediaStoreVideoSource.queryAllVideos(appContext) }
        .flowOn(Dispatchers.IO)
        .shareIn(repositoryScope, SharingStarted.Eagerly, replay = 1)

    override fun observeAllVideos(): Flow<List<VideoInfo>> {
        return allVideosFlow
    }

    override fun observeFolders(): Flow<List<VideoFolder>> {
        return observeAllVideos().map { videos -> buildFolders(videos) }
    }

    override fun observeVideosByFolder(folderPath: String): Flow<List<VideoInfo>> {
        return observeAllVideos().map { videos ->
            val videosInFolder = mutableListOf<VideoInfo>()
            for (video in videos) {
                if (video.relativeFolderPath == folderPath) {
                    videosInFolder.add(video)
                }
            }
            videosInFolder
        }
    }

    override fun observeReceivedVideos(): Flow<List<VideoInfo>> {
        return observeAllVideos().map { videos ->
            val receivedVideos = mutableListOf<VideoInfo>()
            for (video in videos) {
                if (video.relativeFolderPath.startsWith(RECEIVED_RELATIVE_PATH)) {
                    receivedVideos.add(video)
                }
            }
            receivedVideos
        }
    }

    override suspend fun refreshAllVideos() {
        manualRefreshTrigger.emit(Unit)
    }

    // Nhóm ảnh theo relativeFolderPath, mỗi nhóm lấy ảnh đầu tiên làm ảnh đại diện (cover)
    private fun buildFolders(videos: List<VideoInfo>): List<VideoFolder> {
        val groupedByFolder = mutableMapOf<String, MutableList<VideoInfo>>()
        for (video in videos) {
            var folderList = groupedByFolder[video.relativeFolderPath]
            if (folderList == null) {
                folderList = mutableListOf()
                groupedByFolder[video.relativeFolderPath] = folderList
            }
            folderList.add(video)
        }

        val folders = mutableListOf<VideoFolder>()
        for (entry in groupedByFolder) {
            val path = entry.key
            val videosInFolder = entry.value
            val folderName = File(path).name

            var coverUri: Uri? = null
            if (videosInFolder.isNotEmpty()) {
                coverUri = videosInFolder[0].contentUri
            }

            val folder = VideoFolder(
                folderPath = path,
                folderName = folderName,
                videoCount = videosInFolder.size,
                coverVideoUri = coverUri
            )
            folders.add(folder)
        }
        return folders
    }

    companion object {
        // Thư mục quy ước cho tab Received, giống RECEIVED_RELATIVE_PATH của MusicRepositoryImpl
        const val RECEIVED_RELATIVE_PATH = "ShareFile/Videos/Received/"
    }
}