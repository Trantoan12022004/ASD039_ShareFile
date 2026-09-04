package com.example.basekotlin.ui.files.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.photos.PhotoRepository
import com.example.basekotlin.data.local.repository.photos.PhotoRepositoryImpl
import com.example.basekotlin.model.PhotoFolder
import com.example.basekotlin.model.PhotoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        val appContext = application.applicationContext
        repository = PhotoRepositoryImpl(appContext)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.observeAllPhotos().collect {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    // ================= TAB "All" =================
    // Toàn bộ ảnh trong thiết bị
    val allPhotosRepository: StateFlow<List<PhotoInfo>> = repository.observeAllPhotos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchedAllPhotos: StateFlow<List<PhotoInfo>> = combine(
        allPhotosRepository,
        _searchQuery
    ) { photos, query ->
        filterPhotosBySearchQuery(photos, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPhotosUi: StateFlow<List<PhotoInfo>> = searchedAllPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ================= TAB "Folder" =================
    // Danh sách các folder ảnh
    val foldersRepository: StateFlow<List<PhotoFolder>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchedFolders: StateFlow<List<PhotoFolder>> = combine(
        foldersRepository,
        _searchQuery
    ) { folders, query ->
        filterFoldersBySearchQuery(folders, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersUi: StateFlow<List<PhotoFolder>> = searchedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<PhotoFolder>> get() = foldersUi

    // ================= TAB "Received" =================
    // Ảnh trong ShareFile/Photos/Received
    val receivedPhotosRepository: StateFlow<List<PhotoInfo>> = repository.observeReceivedPhotos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchedReceivedPhotos: StateFlow<List<PhotoInfo>> = combine(
        receivedPhotosRepository,
        _searchQuery
    ) { photos, query ->
        filterPhotosBySearchQuery(photos, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivedPhotosUi: StateFlow<List<PhotoInfo>> = searchedReceivedPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivedPhotos: StateFlow<List<PhotoInfo>> get() = receivedPhotosUi

    // ================= FOLDER DETAIL =================
    // Dùng khi mở màn hình chi tiết 1 folder (bấm vào item ở tab Folder)
    private val _currentFolderPath = MutableStateFlow("")
    val photosInCurrentFolderRepository: StateFlow<List<PhotoInfo>> = _currentFolderPath
        .flatMapLatest { path ->
            if (path.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.observePhotosByFolder(path)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedPhotosInCurrentFolder: StateFlow<List<PhotoInfo>> = combine(
        photosInCurrentFolderRepository,
        _searchQuery
    ) { photos, query ->
        filterPhotosBySearchQuery(photos, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photosInCurrentFolderUi: StateFlow<List<PhotoInfo>> = searchedPhotosInCurrentFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photosInCurrentFolder: StateFlow<List<PhotoInfo>> get() = photosInCurrentFolderUi

    fun setCurrentFolder(path: String) {
        _currentFolderPath.value = path
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshAllPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshAllPhotos()
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    // ================= SELECTION MODE =================
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedPhotoPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotoPaths: StateFlow<Set<String>> = _selectedPhotoPaths

    fun enterSelectionMode(filePath: String? = null) {
        _isSelectionMode.value = true
        if (filePath != null) {
            _selectedPhotoPaths.value = setOf(filePath)
        } else {
            _selectedPhotoPaths.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPhotoPaths.value = emptySet()
    }

    fun togglePhotoSelection(filePath: String) {
        val currentSet = _selectedPhotoPaths.value.toMutableSet()
        if (currentSet.contains(filePath)) {
            currentSet.remove(filePath)
        } else {
            currentSet.add(filePath)
        }
        _selectedPhotoPaths.value = currentSet
    }

    fun selectAllPhotos(filePaths: List<String>) {
        _selectedPhotoPaths.value = filePaths.toSet()
    }

    fun clearPhotoSelection() {
        _selectedPhotoPaths.value = emptySet()
    }

    // ================= FILTER HELPERS =================
    private fun filterPhotosBySearchQuery(photos: List<PhotoInfo>, query: String): List<PhotoInfo> {
        if (query.isBlank()) {
            return photos
        }
        val filtered = mutableListOf<PhotoInfo>()
        for (photo in photos) {
            val nameMatch = photo.displayName.contains(query, ignoreCase = true)
            val packageMatch = photo.filePath.contains(query, ignoreCase = true)
            if (nameMatch || packageMatch) {
                filtered.add(photo)
            }
        }
        return filtered
    }

    private fun filterFoldersBySearchQuery(folders: List<PhotoFolder>, query: String): List<PhotoFolder> {
        if (query.isBlank()) {
            return folders
        }
        val filtered = mutableListOf<PhotoFolder>()
        for (folder in folders) {
            val nameMatch = folder.folderName.contains(query, ignoreCase = true)
            val pathMatch = folder.folderPath.contains(query, ignoreCase = true)
            if (nameMatch || pathMatch) {
                filtered.add(folder)
            }
        }
        return filtered
    }

    // Lấy toàn bộ danh sách PhotoInfo thuộc về một folder cụ thể

    fun getPhotosInFolder(folderPath: String): List<PhotoInfo> {
        val allPhotos = allPhotosRepository.value
        val result = mutableListOf<PhotoInfo>()
        for (photo in allPhotos) {
            if (photo.relativeFolderPath == folderPath)
                result.add(photo)
        }
        return result
    }

    // Kiểm tra xem toàn bộ các file ảnh trong folder có đang được chọn hay không
    fun isFolderFullySelected(folderPath: String): Boolean {
        val photosInFolder = getPhotosInFolder(folderPath)
        if (photosInFolder.isEmpty()) {
            return false
        }
        val selectedSet = _selectedPhotoPaths.value
        for (photo in photosInFolder) {
            val isContained = selectedSet.contains(photo.filePath)
            if (!isContained) {
                return false
            }
        }
        return true
    }

    // Kích hoạt Selection Mode và chọn toàn bộ file trong folder ban đầu
    fun enterFolderSelectionMode(folderPath: String) {
        _isSelectionMode.value = true
        val photosInFolder = getPhotosInFolder(folderPath)
        val selectedSet = mutableSetOf<String>()
        for (photo in photosInFolder) {
            selectedSet.add(photo.filePath)
        }
        _selectedPhotoPaths.value = selectedSet
    }

    // Toggle chọn hoặc bỏ chọn toàn bộ file trong folder
    fun toggleFolderSelection(folderPath: String) {
        val photosInFolder = getPhotosInFolder(folderPath)
        if (photosInFolder.isEmpty()) {
            return
        }
        val currentSet = _selectedPhotoPaths.value.toMutableSet()
        val isAllSelected = isFolderFullySelected(folderPath)
        if (isAllSelected) {
            // Đã chọn toàn bộ -> Bỏ chọn tất cả file trong folder
            for (photo in photosInFolder) {
                currentSet.remove(photo.filePath)
            }
        } else {
            // Chưa chọn đủ -> Thêm tất cả file trong folder vào danh sách chọn
            for (photo in photosInFolder) {
                currentSet.add(photo.filePath)
            }
        }
        _selectedPhotoPaths.value = currentSet
    }

}