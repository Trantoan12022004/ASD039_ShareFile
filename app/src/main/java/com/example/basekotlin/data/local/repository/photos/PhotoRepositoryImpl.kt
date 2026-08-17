package com.example.basekotlin.data.local.repository.photos

import android.content.Context
import android.net.Uri
import com.example.basekotlin.data.local.photostore.MediaStorePhotoSource
import com.example.basekotlin.data.local.photostore.PhotoStoreObserver
import com.example.basekotlin.model.PhotoFolder
import com.example.basekotlin.model.PhotoInfo
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

class PhotoRepositoryImpl(private val appContext: Context) : PhotoRepository {

    // Scope riêng của repository, sống cùng vòng đời app,
    // để nhiều ViewModel/Fragment (All, Folder, Received) chia sẻ chung 1 ContentObserver
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Trigger thủ công cho pull-to-refresh, gộp chung với tín hiệu từ ContentObserver
    private val manualRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // shareIn(replay = 1): chỉ đăng ký 1 ContentObserver duy nhất cho toàn app
    private val allPhotosFlow: Flow<List<PhotoInfo>> = merge(
        PhotoStoreObserver.observePhotoStoreChanges(appContext),
        manualRefreshTrigger
    )
        .onStart { emit(Unit) }
        .map { MediaStorePhotoSource.queryAllPhotos(appContext) }
        .flowOn(Dispatchers.IO)
        .shareIn(repositoryScope, SharingStarted.Eagerly, replay = 1)

    override fun observeAllPhotos(): Flow<List<PhotoInfo>> {
        return allPhotosFlow
    }

    override fun observeFolders(): Flow<List<PhotoFolder>> {
        return observeAllPhotos().map { photos -> buildFolders(photos) }
    }

    override fun observePhotosByFolder(folderPath: String): Flow<List<PhotoInfo>> {
        return observeAllPhotos().map { photos ->
            val photosInFolder = mutableListOf<PhotoInfo>()
            for (photo in photos) {
                if (photo.relativeFolderPath == folderPath) {
                    photosInFolder.add(photo)
                }
            }
            photosInFolder
        }
    }

    override fun observeReceivedPhotos(): Flow<List<PhotoInfo>> {
        return observeAllPhotos().map { photos ->
            val receivedPhotos = mutableListOf<PhotoInfo>()
            for (photo in photos) {
                if (photo.relativeFolderPath.startsWith(RECEIVED_RELATIVE_PATH)) {
                    receivedPhotos.add(photo)
                }
            }
            receivedPhotos
        }
    }

    override suspend fun refreshAllPhotos() {
        manualRefreshTrigger.emit(Unit)
    }

    // Nhóm ảnh theo relativeFolderPath, mỗi nhóm lấy ảnh đầu tiên làm ảnh đại diện (cover)
    private fun buildFolders(photos: List<PhotoInfo>): List<PhotoFolder> {
        val groupedByFolder = mutableMapOf<String, MutableList<PhotoInfo>>()
        for (photo in photos) {
            var folderList = groupedByFolder[photo.relativeFolderPath]
            if (folderList == null) {
                folderList = mutableListOf()
                groupedByFolder[photo.relativeFolderPath] = folderList
            }
            folderList.add(photo)
        }

        val folders = mutableListOf<PhotoFolder>()
        for (entry in groupedByFolder) {
            val path = entry.key
            val photosInFolder = entry.value
            val folderName = File(path).name

            var coverUri: Uri? = null
            if (photosInFolder.isNotEmpty()) {
                coverUri = photosInFolder[0].contentUri
            }

            val folder = PhotoFolder(
                folderPath = path,
                folderName = folderName,
                photoCount = photosInFolder.size,
                coverPhotoUri = coverUri
            )
            folders.add(folder)
        }
        return folders
    }

    companion object {
        // Thư mục quy ước cho tab Received, giống RECEIVED_RELATIVE_PATH của MusicRepositoryImpl
        const val RECEIVED_RELATIVE_PATH = "ShareFile/Photos/Received/"
    }
}