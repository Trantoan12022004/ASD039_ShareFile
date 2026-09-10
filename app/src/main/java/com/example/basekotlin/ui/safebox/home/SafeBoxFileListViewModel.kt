package com.example.basekotlin.ui.safebox.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.data.local.documentstore.DocumentFileScanner
import com.example.basekotlin.data.local.mediastore.MediaStoreAudioSource
import com.example.basekotlin.data.local.photostore.MediaStorePhotoSource
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepository
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepositoryImpl
import com.example.basekotlin.data.local.repository.zips.ZipsRepositoryImpl
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import com.example.basekotlin.data.local.videostore.MediaStoreVideoSource
import com.example.basekotlin.ui.safebox.model.SafeBoxCandidateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SafeBoxFileListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SafeBoxRepository = SafeBoxRepositoryImpl(application)

    private val _currentType = MutableStateFlow(SafeBoxFileType.PICTURES)
    val currentType: StateFlow<SafeBoxFileType> = _currentType.asStateFlow()

    // Danh sách file theo category từ Room DB
    @OptIn(ExperimentalCoroutinesApi::class)
    val files: StateFlow<List<SafeBoxFile>> = _currentType
        .flatMapLatest { type ->
            repository.observeFilesByType(type)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun initType(type: SafeBoxFileType) {
        _currentType.value = type
    }

    // Lấy danh sách file ứng viên trên thiết bị tương ứng với từng loại
    suspend fun getCandidateFiles(type: SafeBoxFileType): List<SafeBoxCandidateItem> = withContext(
        Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        val list = when (type) {
            SafeBoxFileType.PICTURES -> {
                MediaStorePhotoSource.queryAllPhotos(context).map { photo ->
                    SafeBoxCandidateItem(
                        filePath = photo.filePath,
                        fileName = photo.displayName,
                        fileSize = photo.sizeBytes,
                        fileType = SafeBoxFileType.PICTURES,
                        contentUri = photo.contentUri
                    )
                }
            }
            SafeBoxFileType.VIDEOS -> {
                MediaStoreVideoSource.queryAllVideos(context).map { video ->
                    SafeBoxCandidateItem(
                        filePath = video.filePath,
                        fileName = video.displayName,
                        fileSize = video.sizeBytes,
                        fileType = SafeBoxFileType.VIDEOS,
                        contentUri = video.contentUri
                    )
                }
            }
            SafeBoxFileType.AUDIO -> {
                MediaStoreAudioSource.queryAllTracks(context).map { track ->
                    SafeBoxCandidateItem(
                        filePath = track.filePath,
                        fileName = track.title,
                        fileSize = track.sizeBytes,
                        fileType = SafeBoxFileType.AUDIO,
                        contentUri = track.contentUri
                    )
                }
            }
            SafeBoxFileType.DOCUMENTS -> {
                DocumentFileScanner.scanAllDocuments().map { doc ->
                    SafeBoxCandidateItem(
                        filePath = doc.filePath,
                        fileName = doc.fileName,
                        fileSize = doc.sizeBytes,
                        fileType = SafeBoxFileType.DOCUMENTS
                    )
                }
            }
            SafeBoxFileType.OTHERS -> {
                val apks = ApkFileScanner.scanAllApkFiles(context).map { apk ->
                    SafeBoxCandidateItem(
                        filePath = apk.apkFilePath,
                        fileName = apk.appName.ifEmpty { File(apk.apkFilePath).name },
                        fileSize = apk.sizeBytes,
                        fileType = SafeBoxFileType.OTHERS
                    )
                }
                val zips = ZipsRepositoryImpl(context).fetchAllZipFiles().map { zip ->
                    SafeBoxCandidateItem(
                        filePath = zip.filePath,
                        fileName = zip.fileName,
                        fileSize = zip.sizeBytes,
                        fileType = SafeBoxFileType.OTHERS
                    )
                }
                apks + zips
            }
        }
        list.filter { File(it.filePath).exists() }
    }
    // Di chuyển các file được chọn vào SafeBox
    fun addSelectedFilesToSafeBox(
        selectedItems: List<SafeBoxCandidateItem>,
        type: SafeBoxFileType,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            var successCount = 0
            withContext(Dispatchers.IO) {
                for (item in selectedItems) {
                    val result = repository.moveFileToSafeBox(item.filePath, type)
                    if (result.isSuccess) successCount++
                }
            }
            onComplete(successCount)
        }
    }



    fun restoreFile(file: SafeBoxFile, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreFile(file)
            onResult(result.isSuccess)
        }
    }

    fun deletePermanently(file: SafeBoxFile, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deletePermanently(file)
            onResult(result.isSuccess)
        }
    }
}
