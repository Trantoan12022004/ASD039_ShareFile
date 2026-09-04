package com.example.basekotlin.ui.storage

import android.app.Application
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.storage.StorageRepository
import com.example.basekotlin.data.local.repository.storage.StorageRepositoryImpl
import com.example.basekotlin.model.StorageItem
import com.example.basekotlin.model.StorageSortOption
import com.example.basekotlin.model.StorageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Stack

class StorageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StorageRepository

    init {
        val appContext = application.applicationContext
        repository = StorageRepositoryImpl(appContext)
    }

    // ======================================================
    // BACK STACK – Quản lý lịch sử điều hướng thư mục
    // ======================================================

    // Stack lưu lại các thư mục đã duyệt qua để hỗ trợ back navigation
    private val folderBackStack = Stack<File>()

    // Thư mục root (không thể back vượt qua root)
    val rootDirectory: File = repository.getRootDirectory()

    // ======================================================
    // CURRENT FOLDER & ITEMS
    // ======================================================

    private val _currentFolder = MutableStateFlow<File?>(null)
    val currentFolder: StateFlow<File?> = _currentFolder

    private val _currentItems = MutableStateFlow<List<StorageItem>>(emptyList())
    val currentItems: StateFlow<List<StorageItem>> = _currentItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ======================================================
    // STORAGE INFO – Thông tin dung lượng bộ nhớ
    // ======================================================

    private val _storageModel = MutableStateFlow(StorageModel())
    val storageModel: StateFlow<StorageModel> = _storageModel

    // ======================================================
    // SORT & DISPLAY MODE
    // ======================================================

    private val _sortOption = MutableStateFlow(StorageSortOption.NAME_A_Z)
    val sortOption: StateFlow<StorageSortOption> = _sortOption

    private val _isGridMode = MutableStateFlow(false)
    val isGridMode: StateFlow<Boolean> = _isGridMode

    // ======================================================
    // SEARCH
    // ======================================================

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Danh sách sau khi filter theo searchQuery
    private val _filteredItems = MutableStateFlow<List<StorageItem>>(emptyList())
    val filteredItems: StateFlow<List<StorageItem>> = _filteredItems

    // ======================================================
    // SELECTION MODE
    // ======================================================

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths

    // ======================================================
    // OPERATION RESULT – Kết quả thao tác CRUD
    // ======================================================

    // null = chưa có thao tác, true = thành công, false = thất bại
    private val _operationResult = MutableStateFlow<Boolean?>(null)
    val operationResult: StateFlow<Boolean?> = _operationResult

    // ======================================================
    // KHỞI TẠO – Load thư mục root khi ViewModel được tạo
    // ======================================================

    init {
        loadStorageInfo()
        loadFolder(rootDirectory)
    }

    // ======================================================
    // NAVIGATION – Điều hướng thư mục
    // ======================================================

    // Tải danh sách item của một thư mục
    // pushToStack = true khi người dùng đi vào thư mục con
    fun loadFolder(folder: File, pushToStack: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true

            // Nếu cần lưu thư mục hiện tại vào stack (khi đi vào con)
            if (pushToStack) {
                val current = _currentFolder.value
                if (current != null) {
                    folderBackStack.push(current)
                }
            }

            _currentFolder.value = folder

            try {
                val items = repository.fetchItemsInDirectory(folder, _sortOption.value)
                _currentItems.value = items
                // Cập nhật lại filteredItems theo searchQuery hiện tại
                applySearchFilter(items, _searchQuery.value)
            } catch (e: Exception) {
                e.printStackTrace()
                _currentItems.value = emptyList()
                _filteredItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Quay lại thư mục cha trong back stack
    // Trả về true nếu lùi thành công, false nếu đã ở root
    fun navigateUp(): Boolean {
        // Kiểm tra có thư mục trong stack không
        if (folderBackStack.isEmpty()) {
            return false
        }

        // Lấy thư mục trước đó từ stack (không push lại)
        val previousFolder = folderBackStack.pop()
        _currentFolder.value = previousFolder

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = repository.fetchItemsInDirectory(previousFolder, _sortOption.value)
                _currentItems.value = items
                applySearchFilter(items, _searchQuery.value)
            } catch (e: Exception) {
                e.printStackTrace()
                _currentItems.value = emptyList()
                _filteredItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }

        return true
    }

    // Nhảy thẳng đến một thư mục trong breadcrumb (pop stack đến đúng level)
    fun navigateTo(targetFolder: File) {
        while (folderBackStack.isNotEmpty()) {
            val top = folderBackStack.peek()
            if (top.absolutePath == targetFolder.absolutePath) {
                // Pop luôn targetFolder ra khỏi stack vì loadFolder sẽ set nó vào _currentFolder
                folderBackStack.pop()
                break
            }
            folderBackStack.pop()
        }

        if (targetFolder.absolutePath == rootDirectory.absolutePath) {
            folderBackStack.clear()
        }

        loadFolder(targetFolder, pushToStack = false)
    }


    // Làm mới thư mục đang xem hiện tại
    fun refreshCurrentFolder() {
        val folder = _currentFolder.value ?: rootDirectory
        loadFolder(folder, pushToStack = false)
    }

    // Lấy back stack hiện tại (dùng để render breadcrumb)
    fun getBreadcrumbList(): List<File> {
        val breadcrumb = mutableListOf<File>()
        // Thêm toàn bộ stack (từ root đến folder hiện tại)
        breadcrumb.addAll(folderBackStack)
        val current = _currentFolder.value
        if (current != null) {
            breadcrumb.add(current)
        }
        return breadcrumb
    }

    // ======================================================
    // STORAGE INFO
    // ======================================================

    fun loadStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = repository.getStorageInfo(rootDirectory)
                val context = getApplication<Application>().applicationContext

                val formattedUsed = Formatter.formatShortFileSize(context, info.usedBytes)
                val formattedTotal = Formatter.formatShortFileSize(context, info.totalBytes)

                _storageModel.value = StorageModel(
                    totalBytes = info.totalBytes,
                    usedBytes = info.usedBytes,
                    freeBytes = info.freeBytes,
                    usedPercentage = info.usedPercentage,
                    formattedUsed = formattedUsed,
                    formattedTotal = formattedTotal
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ======================================================
    // SORT & DISPLAY MODE
    // ======================================================

    // Đổi kiểu sắp xếp và reload thư mục hiện tại
    fun setSortOption(option: StorageSortOption) {
        _sortOption.value = option
        refreshCurrentFolder()
    }

    // Toggle giữa chế độ danh sách và lưới
    fun toggleGridMode() {
        _isGridMode.value = !_isGridMode.value
    }

    // ======================================================
    // SEARCH – Filter danh sách theo tên
    // ======================================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applySearchFilter(_currentItems.value, query)
    }

    // Lọc danh sách item theo từ khóa tìm kiếm (không phân biệt hoa thường)
    private fun applySearchFilter(items: List<StorageItem>, query: String) {
        if (query.isBlank()) {
            _filteredItems.value = items
        } else {
            val filtered = mutableListOf<StorageItem>()
            for (item in items) {
                if (item.name.contains(query, ignoreCase = true)) {
                    filtered.add(item)
                }
            }
            _filteredItems.value = filtered
        }
    }

    // ======================================================
    // SELECTION MODE
    // ======================================================

    fun enterSelectionMode(path: String? = null) {
        _isSelectionMode.value = true
        if (path != null) {
            _selectedPaths.value = setOf(path)
        } else {
            _selectedPaths.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPaths.value = emptySet()
    }

    fun toggleSelection(path: String) {
        val currentSet = _selectedPaths.value.toMutableSet()
        if (currentSet.contains(path)) {
            currentSet.remove(path)
        } else {
            currentSet.add(path)
        }
        _selectedPaths.value = currentSet
    }

    fun selectAll() {
        _selectedPaths.value = _filteredItems.value.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    // ======================================================
    // CRUD OPERATIONS
    // ======================================================

    // Tạo thư mục mới trong thư mục hiện tại
    fun createFolder(name: String) {
        val parent = _currentFolder.value ?: return
        viewModelScope.launch {
            val success = repository.createFolder(parent, name)
            _operationResult.value = success
            if (success) {
                // Reload để hiển thị thư mục mới
                refreshCurrentFolder()
                loadStorageInfo()
            }
        }
    }

    // Đổi tên file hoặc thư mục
    fun renameItem(target: File, newName: String) {
        viewModelScope.launch {
            val success = repository.rename(target, newName)
            _operationResult.value = success
            if (success) {
                refreshCurrentFolder()
                loadStorageInfo()
            }
        }
    }

    // Xóa một hoặc nhiều file/thư mục
    fun deleteItems(targets: List<File>) {
        viewModelScope.launch {
            var allSuccess = true
            for (target in targets) {
                val success = repository.delete(target)
                if (!success) {
                    allSuccess = false
                }
            }
            _operationResult.value = allSuccess
            // Thoát selection mode sau khi xóa
            exitSelectionMode()
            refreshCurrentFolder()
            loadStorageInfo()
        }
    }

    // Reset operationResult về null sau khi Activity đã xử lý
    fun clearOperationResult() {
        _operationResult.value = null
    }
}
