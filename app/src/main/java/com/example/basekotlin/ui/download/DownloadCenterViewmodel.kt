package com.example.basekotlin.ui.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.media.MediaScannerConnection
import com.example.basekotlin.data.local.repository.download.DownloadRepository
import com.example.basekotlin.data.local.repository.download.DownloadRepositoryImpl
import com.example.basekotlin.ui.download.model.DownloadItem
import com.example.basekotlin.ui.download.model.DownloadScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadCenterViewmodel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository = DownloadRepositoryImpl(application.applicationContext)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _scanResult = MutableStateFlow(DownloadScanResult())

    // 5 StateFlow phục vụ trực tiếp cho 5 tab mà không cần search logic
    val allDownloads: StateFlow<List<DownloadItem>> = _scanResult
        .map { it.allFiles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoDownloads: StateFlow<List<DownloadItem>> = _scanResult
        .map { it.videoFiles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photoDownloads: StateFlow<List<DownloadItem>> = _scanResult
        .map { it.photoFiles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val musicDownloads: StateFlow<List<DownloadItem>> = _scanResult
        .map { it.musicFiles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appDownloads: StateFlow<List<DownloadItem>> = _scanResult
        .map { it.appFiles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _scanResult.value = repository.fetchAllDownloads()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun enterSelectionMode(path: String? = null) {
        _isSelectionMode.value = true
        _selectedPaths.value = if (path != null) setOf(path) else emptySet()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPaths.value = emptySet()
    }

    fun toggleItemSelection(path: String) {
        val currentSet = _selectedPaths.value.toMutableSet()
        if (currentSet.contains(path)) {
            currentSet.remove(path)
        } else {
            currentSet.add(path)
        }
        _selectedPaths.value = currentSet
    }

    fun selectAll(paths: List<String>) {
        _selectedPaths.value = paths.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun deleteSelectedFiles(onResult: (Boolean) -> Unit) {
        val paths = _selectedPaths.value.toList()
        if (paths.isEmpty()) {
            onResult(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var allSuccess = true
            val deletedPaths = mutableListOf<String>()
            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    if (file.delete()) {
                        deletedPaths.add(path)
                    } else {
                        allSuccess = false
                    }
                }
            }

            if (deletedPaths.isNotEmpty()) {
                MediaScannerConnection.scanFile(
                    getApplication<Application>().applicationContext,
                    deletedPaths.toTypedArray(),
                    null,
                    null
                )
            }

            withContext(Dispatchers.Main) {
                exitSelectionMode()
                loadDownloads()
                onResult(allSuccess)
            }
        }
    }

    fun deleteFile(item: DownloadItem, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(item.path)
            val deleted = if (file.exists()) file.delete() else false
            if (deleted) {
                MediaScannerConnection.scanFile(
                    getApplication<Application>().applicationContext,
                    arrayOf(item.path),
                    null,
                    null
                )
            }
            withContext(Dispatchers.Main) {
                if (deleted) {
                    loadDownloads()
                }
                onResult(deleted)
            }
        }
    }

    fun renameFile(item: DownloadItem, newBaseName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFile = File(item.path)
            val extension = currentFile.extension
            val newFileName = if (extension.isNotEmpty()) "$newBaseName.$extension" else newBaseName
            val targetFile = File(currentFile.parentFile, newFileName)
            val success = currentFile.renameTo(targetFile)
            if (success) {
                MediaScannerConnection.scanFile(
                    getApplication<Application>().applicationContext,
                    arrayOf(currentFile.absolutePath, targetFile.absolutePath),
                    null,
                    null
                )
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    loadDownloads()
                }
                onResult(success)
            }
        }
    }
}
