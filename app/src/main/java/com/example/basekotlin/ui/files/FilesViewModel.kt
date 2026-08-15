package com.example.basekotlin.ui.files

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.model.StorageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class FilesViewModel : ViewModel() {

    // Quản lý riêng trạng thái Loading
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageModel())
    val storageInfo: StateFlow<StorageModel> = _storageInfo.asStateFlow()

    init {
        loadStorageInfo()
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            _isLoading.value = true

            val info = withContext(Dispatchers.IO) {
                getInternalStorageInfo()
            }

            _storageInfo.value = info
            _isLoading.value = false
        }
    }

    private fun getInternalStorageInfo(): StorageModel {
        return try {
            val path: File = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)

            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val percentage = if (totalBytes > 0) {
                ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
            } else 0

            val totalGB = totalBytes.toDouble() / (1024 * 1024 * 1024)
            val usedGB = usedBytes.toDouble() / (1024 * 1024 * 1024)

            StorageModel(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                usedPercentage = percentage,
                formattedUsed = String.format(Locale.getDefault(), "%.2f GB", usedGB),
                formattedTotal = String.format(Locale.getDefault(), "%.2f GB", totalGB)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            StorageModel()
        }
    }
}