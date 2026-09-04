package com.example.basekotlin.ui.files.video

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.video.VideoRepository
import com.example.basekotlin.data.local.repository.video.VideoRepositoryImpl
import com.example.basekotlin.ui.files.video.model.VideoFolder
import com.example.basekotlin.ui.files.video.model.VideoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        val appContext = application.applicationContext
        repository = VideoRepositoryImpl(appContext)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.observeAllVideos().collect {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    // ================= TAB "All" =================
    // Toàn bộ video trong thiết bị
    val allVideosRepository: StateFlow<List<VideoInfo>> = repository.observeAllVideos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchedAllVideos: StateFlow<List<VideoInfo>> = combine(
        allVideosRepository,
        _searchQuery
    ) { videos, query ->
        filterVideosBySearchQuery(videos, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideosUi: StateFlow<List<VideoInfo>> = searchedAllVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ================= TAB "Folder" =================
    // Danh sách các folder chứa video
    val foldersRepository: StateFlow<List<VideoFolder>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchedFolders: StateFlow<List<VideoFolder>> = combine(
        foldersRepository,
        _searchQuery
    ) { folders, query ->
        filterFoldersBySearchQuery(folders, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersUi: StateFlow<List<VideoFolder>> = searchedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<VideoFolder>> get() = foldersUi

    // ================= TAB "Received" =================
    // Video trong thư mục ShareFile/Videos/Received/
    val receivedVideosRepository: StateFlow<List<VideoInfo>> = repository.observeReceivedVideos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val searchedReceivedVideos: StateFlow<List<VideoInfo>> = combine(
        receivedVideosRepository,
        _searchQuery
    ) { videos, query ->
        filterVideosBySearchQuery(videos, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivedVideosUi: StateFlow<List<VideoInfo>> = searchedReceivedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivedVideos: StateFlow<List<VideoInfo>> get() = receivedVideosUi

    // ================= FOLDER DETAIL =================
    // Dùng khi mở màn hình chi tiết xem các video bên trong 1 folder
    private val _currentFolderPath = MutableStateFlow("")
    val videosInCurrentFolderRepository: StateFlow<List<VideoInfo>> = _currentFolderPath
        .flatMapLatest { path ->
            if (path.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.observeVideosByFolder(path)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedVideosInCurrentFolder: StateFlow<List<VideoInfo>> = combine(
        videosInCurrentFolderRepository,
        _searchQuery
    ) { videos, query ->
        filterVideosBySearchQuery(videos, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosInCurrentFolderUi: StateFlow<List<VideoInfo>> = searchedVideosInCurrentFolder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosInCurrentFolder: StateFlow<List<VideoInfo>> get() = videosInCurrentFolderUi

    fun setCurrentFolder(path: String) {
        _currentFolderPath.value = path
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshAllVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshAllVideos()
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    // ================= SELECTION MODE =================
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    // Tập hợp đường dẫn file video đang được chọn
    private val _selectedVideoPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedVideoPaths: StateFlow<Set<String>> = _selectedVideoPaths

    // Tập hợp đường dẫn thư mục đang được chọn ở Tab Folder
    private val _selectedFolderPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolderPaths: StateFlow<Set<String>> = _selectedFolderPaths

    fun enterSelectionMode(filePath: String? = null) {
        _isSelectionMode.value = true
        if (filePath != null) {
            _selectedVideoPaths.value = setOf(filePath)
        } else {
            _selectedVideoPaths.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedVideoPaths.value = emptySet()
        _selectedFolderPaths.value = emptySet()
    }

    fun toggleVideoSelection(filePath: String) {
        val currentSet = _selectedVideoPaths.value.toMutableSet()
        if (currentSet.contains(filePath)) {
            currentSet.remove(filePath)
        } else {
            currentSet.add(filePath)
        }
        _selectedVideoPaths.value = currentSet
    }

    fun selectAllVideos(filePaths: List<String>) {
        _selectedVideoPaths.value = filePaths.toSet()
    }

    fun clearVideoSelection() {
        _selectedVideoPaths.value = emptySet()
    }

    // ================= FOLDER SELECTION HELPERS =================
    // Lấy toàn bộ danh sách VideoInfo thuộc về một folder cụ thể
    fun getVideosInFolder(folderPath: String): List<VideoInfo> {
        val allVideos = allVideosRepository.value
        val result = mutableListOf<VideoInfo>()
        for (video in allVideos) {
            if (video.relativeFolderPath == folderPath) {
                result.add(video)
            }
        }
        return result
    }

    // Kiểm tra xem toàn bộ các file video trong folder có đang được chọn hay không
    fun isFolderFullySelected(folderPath: String): Boolean {
        val videosInFolder = getVideosInFolder(folderPath)
        if (videosInFolder.isEmpty()) {
            return false
        }
        val selectedSet = _selectedVideoPaths.value
        for (video in videosInFolder) {
            val isContained = selectedSet.contains(video.filePath)
            if (!isContained) {
                return false
            }
        }
        return true
    }

    // Kích hoạt Selection Mode và chọn toàn bộ video trong folder ban đầu
    fun enterFolderSelectionMode(folderPath: String) {
        _isSelectionMode.value = true
        _selectedFolderPaths.value = setOf(folderPath)
        val videosInFolder = getVideosInFolder(folderPath)
        val selectedSet = mutableSetOf<String>()
        for (video in videosInFolder) {
            selectedSet.add(video.filePath)
        }
        _selectedVideoPaths.value = selectedSet
    }

    // Toggle chọn hoặc bỏ chọn toàn bộ video trong folder
    fun toggleFolderSelection(folderPath: String) {
        val videosInFolder = getVideosInFolder(folderPath)
        if (videosInFolder.isEmpty()) {
            return
        }
        val currentSet = _selectedVideoPaths.value.toMutableSet()
        val currentFolderSet = _selectedFolderPaths.value.toMutableSet()
        val isAllSelected = isFolderFullySelected(folderPath)
        if (isAllSelected) {
            // Đã chọn toàn bộ -> Bỏ chọn tất cả file trong folder
            currentFolderSet.remove(folderPath)
            for (video in videosInFolder) {
                currentSet.remove(video.filePath)
            }
        } else {
            // Chưa chọn đủ -> Thêm tất cả file trong folder vào danh sách chọn
            currentFolderSet.add(folderPath)
            for (video in videosInFolder) {
                currentSet.add(video.filePath)
            }
        }
        _selectedVideoPaths.value = currentSet
        _selectedFolderPaths.value = currentFolderSet
    }

    // Lấy danh sách các VideoFolder đang được chọn
    fun getSelectedFolders(): List<VideoFolder> {
        val selectedPaths = _selectedFolderPaths.value
        val allFolders = foldersUi.value
        return allFolders.filter { selectedPaths.contains(it.folderPath) }
    }

    // ================= FILTER HELPERS =================
    private fun filterVideosBySearchQuery(videos: List<VideoInfo>, query: String): List<VideoInfo> {
        if (query.isBlank()) {
            return videos
        }
        val filtered = mutableListOf<VideoInfo>()
        for (video in videos) {
            val nameMatch = video.displayName.contains(query, ignoreCase = true)
            val packageMatch = video.filePath.contains(query, ignoreCase = true)
            if (nameMatch || packageMatch) {
                filtered.add(video)
            }
        }
        return filtered
    }

    private fun filterFoldersBySearchQuery(folders: List<VideoFolder>, query: String): List<VideoFolder> {
        if (query.isBlank()) {
            return folders
        }
        val filtered = mutableListOf<VideoFolder>()
        for (folder in folders) {
            val nameMatch = folder.folderName.contains(query, ignoreCase = true)
            val pathMatch = folder.folderPath.contains(query, ignoreCase = true)
            if (nameMatch || pathMatch) {
                filtered.add(folder)
            }
        }
        return filtered
    }
}
