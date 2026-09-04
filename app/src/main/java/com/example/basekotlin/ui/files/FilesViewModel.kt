package com.example.basekotlin.ui.files

import android.app.Application
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.storage.StorageRepository
import com.example.basekotlin.data.local.repository.storage.StorageRepositoryImpl
import com.example.basekotlin.model.StorageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StorageRepository

    // Quản lý riêng trạng thái Loading
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageModel())
    val storageInfo: StateFlow<StorageModel> = _storageInfo.asStateFlow()

    init {
        val appContext = application.applicationContext
        repository = StorageRepositoryImpl(appContext)
        loadStorageInfo()
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            _isLoading.value = true

            val info = withContext(Dispatchers.IO) {
                try {
                    val rootDirectory = repository.getRootDirectory()
                    val storageScannerInfo = repository.getStorageInfo(rootDirectory)
                    val context = getApplication<Application>().applicationContext

                    val formattedUsed = Formatter.formatShortFileSize(context, storageScannerInfo.usedBytes)
                    val formattedTotal = Formatter.formatShortFileSize(context, storageScannerInfo.totalBytes)

                    StorageModel(
                        totalBytes = storageScannerInfo.totalBytes,
                        usedBytes = storageScannerInfo.usedBytes,
                        freeBytes = storageScannerInfo.freeBytes,
                        usedPercentage = storageScannerInfo.usedPercentage,
                        formattedUsed = formattedUsed,
                        formattedTotal = formattedTotal
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    StorageModel()
                }
            }

            _storageInfo.value = info
            _isLoading.value = false
        }
    }
}