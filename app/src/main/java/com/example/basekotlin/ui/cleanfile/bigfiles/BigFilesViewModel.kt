package com.example.basekotlin.ui.cleanfile.bigfiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.cleanfile.BigFileScanner
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BigFilesViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val FILTER_ALL = 0
        const val FILTER_PHOTO = 1
        const val FILTER_VIDEO = 2
        const val FILTER_MUSIC = 3
        const val FILTER_OTHER = 4
    }

    private val repository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)

    private val _allFiles = MutableStateFlow<List<CleanFileItem>>(emptyList())
    private val _filteredFiles = MutableStateFlow<List<CleanFileItem>>(emptyList())
    val filteredFiles: StateFlow<List<CleanFileItem>> = _filteredFiles.asStateFlow()

    private val _currentFilter = MutableStateFlow(FILTER_ALL)
    val currentFilter: StateFlow<Int> = _currentFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadBigFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val files = repository.fetchBigFiles(BigFileScanner.DEFAULT_MIN_SIZE_BYTES)
                _allFiles.value = files
                applyFilter(_currentFilter.value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: Int) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: Int) {
        val all = _allFiles.value
        _filteredFiles.value = when (filter) {
            FILTER_PHOTO -> all.filter { it.type == CleanFileType.PHOTO }
            FILTER_VIDEO -> all.filter { it.type == CleanFileType.VIDEO }
            FILTER_MUSIC -> all.filter { it.type == CleanFileType.AUDIO }
            FILTER_OTHER -> all.filter { it.type != CleanFileType.PHOTO && it.type != CleanFileType.VIDEO && it.type != CleanFileType.AUDIO }
            else -> all
        }
    }

    fun deleteFiles(paths: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteFiles(paths)
            if (success) {
                loadBigFiles()
            }
            onResult(success)
        }
    }
}
