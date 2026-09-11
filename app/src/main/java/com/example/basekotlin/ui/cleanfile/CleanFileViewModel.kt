package com.example.basekotlin.ui.cleanfile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.R
import com.example.basekotlin.data.local.cleanfile.BigFileScanner
import com.example.basekotlin.data.local.cleanfile.MessengerMediaScanner
import com.example.basekotlin.data.local.repository.apps.AppsRepository
import com.example.basekotlin.data.local.repository.apps.AppsRepositoryImpl
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.CleanFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CleanFileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)
    private val appsRepository: AppsRepository = AppsRepositoryImpl(application.applicationContext)

    // Junk
    private val _junkSizeBytes = MutableStateFlow(0L)
    val junkSizeBytes: StateFlow<Long> = _junkSizeBytes.asStateFlow()

    private val _isCleaningJunk = MutableStateFlow(false)
    val isCleaningJunk: StateFlow<Boolean> = _isCleaningJunk.asStateFlow()

    // 6 System Cleaners Subtitle Info: "Size/Files"
    private val _appsCleanupInfo = MutableStateFlow("0 B/0 files")
    val appsCleanupInfo: StateFlow<String> = _appsCleanupInfo.asStateFlow()

    private val _bigFilesInfo = MutableStateFlow("0 B/0 files")
    val bigFilesInfo: StateFlow<String> = _bigFilesInfo.asStateFlow()

    private val _videoCleanInfo = MutableStateFlow("0 B/0 files")
    val videoCleanInfo: StateFlow<String> = _videoCleanInfo.asStateFlow()

    private val _photoCleanInfo = MutableStateFlow("0 B/0 files")
    val photoCleanInfo: StateFlow<String> = _photoCleanInfo.asStateFlow()

    private val _audioCleanInfo = MutableStateFlow("0 B/0 files")
    val audioCleanInfo: StateFlow<String> = _audioCleanInfo.asStateFlow()

    private val _duplicateInfo = MutableStateFlow("0 B/0 files")
    val duplicateInfo: StateFlow<String> = _duplicateInfo.asStateFlow()

    // Messengers
    private val _telegramInstalled = MutableStateFlow(false)
    val telegramInstalled: StateFlow<Boolean> = _telegramInstalled.asStateFlow()

    private val _telegramSizeBytes = MutableStateFlow(0L)
    val telegramSizeBytes: StateFlow<Long> = _telegramSizeBytes.asStateFlow()

    private val _whatsappInstalled = MutableStateFlow(false)
    val whatsappInstalled: StateFlow<Boolean> = _whatsappInstalled.asStateFlow()

    private val _whatsappSizeBytes = MutableStateFlow(0L)
    val whatsappSizeBytes: StateFlow<Long> = _whatsappSizeBytes.asStateFlow()

    fun loadData() {
        // 1. Quét Junk
        viewModelScope.launch(Dispatchers.IO) {
            _junkSizeBytes.value = repository.scanJunkFiles()
        }

        // 2. Quét Apps Cleanup (Installed Apps + APK)
        viewModelScope.launch(Dispatchers.IO) {
            val installed = appsRepository.fetchInstalledApps()
            val apks = appsRepository.fetchApkFiles()
            val totalSize = installed.sumOf { it.sizeBytes } + apks.sumOf { it.sizeBytes }
            val totalFiles = installed.size + apks.size
            _appsCleanupInfo.value = formatInfoString(totalSize, totalFiles)
        }

        // 3. Quét Big Files (> 500MB)
        viewModelScope.launch(Dispatchers.IO) {
            val bigFiles = repository.fetchBigFiles(BigFileScanner.DEFAULT_MIN_SIZE_BYTES)
            val totalSize = bigFiles.sumOf { it.sizeBytes }
            _bigFilesInfo.value = formatInfoString(totalSize, bigFiles.size)
        }

        // 4. Quét Video
        viewModelScope.launch(Dispatchers.IO) {
            val videoGroups = repository.fetchMediaByFolder(CleanFileType.VIDEO)
            val totalSize = videoGroups.sumOf { it.totalSizeBytes }
            val totalFiles = videoGroups.sumOf { it.items.size }
            _videoCleanInfo.value = formatInfoString(totalSize, totalFiles)
        }

        // 5. Quét Photo
        viewModelScope.launch(Dispatchers.IO) {
            val photoGroups = repository.fetchMediaByFolder(CleanFileType.PHOTO)
            val totalSize = photoGroups.sumOf { it.totalSizeBytes }
            val totalFiles = photoGroups.sumOf { it.items.size }
            _photoCleanInfo.value = formatInfoString(totalSize, totalFiles)
        }

        // 6. Quét Audio
        viewModelScope.launch(Dispatchers.IO) {
            val audioGroups = repository.fetchMediaByFolder(CleanFileType.AUDIO)
            val totalSize = audioGroups.sumOf { it.totalSizeBytes }
            val totalFiles = audioGroups.sumOf { it.items.size }
            _audioCleanInfo.value = formatInfoString(totalSize, totalFiles)
        }

        // 7. Quét Duplicate
        viewModelScope.launch(Dispatchers.IO) {
            val dupGroups = repository.fetchDuplicateFiles()
            val totalSize = dupGroups.sumOf { it.totalSizeBytes }
            val totalFiles = dupGroups.sumOf { it.items.size }
            _duplicateInfo.value = formatInfoString(totalSize, totalFiles)
        }

        // 8. Messengers (Telegram / WhatsApp)
        viewModelScope.launch(Dispatchers.IO) {
            val tgInstalled = repository.isMessengerInstalled(MessengerMediaScanner.MESSENGER_TELEGRAM)
            _telegramInstalled.value = tgInstalled
            if (tgInstalled) {
                val categories = repository.fetchMessengerCategories(MessengerMediaScanner.MESSENGER_TELEGRAM)
                _telegramSizeBytes.value = categories.sumOf { it.sizeBytes }
            }

            val waInstalled = repository.isMessengerInstalled(MessengerMediaScanner.MESSENGER_WHATSAPP)
            _whatsappInstalled.value = waInstalled
            if (waInstalled) {
                val categories = repository.fetchMessengerCategories(MessengerMediaScanner.MESSENGER_WHATSAPP)
                _whatsappSizeBytes.value = categories.sumOf { it.sizeBytes }
            }
        }
    }

    private fun formatInfoString(sizeBytes: Long, fileCount: Int): String {
        val sizeText = CleanFileUtils.formatFileSize(sizeBytes)
        val filesText = if (fileCount == 1) {
            getApplication<Application>().getString(R.string.category_file_single_count)
        } else {
            getApplication<Application>().getString(R.string.category_files_count, fileCount)
        }
        return "$sizeText/$filesText"
    }

    fun cleanJunk(onComplete: (Boolean) -> Unit) {
        if (_isCleaningJunk.value) return
        _isCleaningJunk.value = true
        viewModelScope.launch {
            val success = repository.cleanJunkFiles()
            _junkSizeBytes.value = 0L
            _isCleaningJunk.value = false
            onComplete(success)
        }
    }
}
