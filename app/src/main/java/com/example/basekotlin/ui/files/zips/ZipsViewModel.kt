package com.example.basekotlin.ui.files.zips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.docs.DocumentsRepository
import com.example.basekotlin.data.local.repository.docs.DocumentsRepositoryImpl
import com.example.basekotlin.data.local.repository.zips.ZipsRepository
import com.example.basekotlin.data.local.repository.zips.ZipsRepositoryImpl
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.model.UnzippedItem
import com.example.basekotlin.model.ZipInfo
import com.example.basekotlin.util.ZipExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ZipsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ZipsRepository

    init {
        val appContext = application.applicationContext
        repository = ZipsRepositoryImpl(appContext)
    }

    private val _allZips = MutableStateFlow<List<ZipInfo>>(emptyList())
    val allZips: StateFlow<List<ZipInfo>> = _allZips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var loadDataFirst = false



    fun loadData() {
        if (loadDataFirst) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val zips = repository.fetchAllZipFiles()
                _allZips.value = zips
                loadDataFirst = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAllZips(){
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val zips = repository.fetchAllZipFiles()
                _allZips.value = zips
                loadDataFirst = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ================= SELECTION MODE =================
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedZipsPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedZipsPaths: StateFlow<Set<String>> = _selectedZipsPaths

    fun enterSelectionMode(filePath: String? = null) {
        _isSelectionMode.value = true
        if (filePath != null) {
            _selectedZipsPaths.value = setOf(filePath)
        } else {
            _selectedZipsPaths.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedZipsPaths.value = emptySet()
    }

    fun toggleZipSelection(filePath: String) {
        val currentSet = _selectedZipsPaths.value.toMutableSet()
        if (currentSet.contains(filePath)) {
            currentSet.remove(filePath)
        } else {
            currentSet.add(filePath)
        }
        _selectedZipsPaths.value = currentSet
    }

    fun selectAllZips() {
        _selectedZipsPaths.value = _allZips.value.map { it.filePath }.toSet()
    }

    fun clearAllZips() {
        _selectedZipsPaths.value = emptySet()
    }
//    Giải nén file
    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting

    fun extractZip(zipFilePath: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            _isExtracting.value = true
            val extractedFolder = withContext(Dispatchers.IO) {
                ZipExtractor.extractZipFile(getApplication(), zipFilePath)
            }
            _isExtracting.value = false
            onComplete(extractedFolder)
        }
    }

//    TAB UNZIPPED

    // Quản lý danh sách item trong thư mục đang xem
    private val _unzippedItems = MutableStateFlow<List<UnzippedItem>>(emptyList())
    val unzippedItems: StateFlow<List<UnzippedItem>> = _unzippedItems

    // Thư mục hiện tại đang đứng
    private val _currentFolder = MutableStateFlow<File?>(null)
    val currentFolder: StateFlow<File?> = _currentFolder

    val rootExtractFolder: File = repository.getExtractRootDir()

    // Tải dữ liệu của một thư mục
    fun loadFolder(folder: File = rootExtractFolder) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentFolder.value = folder
            try {
                val items = repository.fetchItemsInDirectory(folder)
                _unzippedItems.value = items
            } catch (e: Exception) {
                e.printStackTrace()
                _unzippedItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Làm mới thư mục hiện tại
    fun refreshCurrentFolder() {
        val folder = _currentFolder.value ?: rootExtractFolder
        loadFolder(folder)
    }

    // Lùi về thư mục cha (quay lại cấp trước)
    // Trả về true nếu lùi thành công, false nếu đã ở thư mục gốc ShareFile/Zip/Extract
    fun navigateUp(): Boolean {
        val current = _currentFolder.value
        if (current == null) {
            return false
        }
        // So sánh đường dẫn tuyệt đối với thư mục gốc
        if (current.absolutePath == rootExtractFolder.absolutePath) {
            return false
        }
        val parent = current.parentFile
        if (parent != null && parent.exists()) {
            loadFolder(parent)
            return true
        }
        return false
    }

}