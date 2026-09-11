package com.example.basekotlin.ui.cleanfile.duplicate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.CleanFileGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuplicateFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)

    private val _duplicateGroups = MutableStateFlow<List<CleanFileGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<CleanFileGroup>> = _duplicateGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadDuplicateFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groups = repository.fetchDuplicateFiles()
                _duplicateGroups.value = groups
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFiles(paths: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteFiles(paths)
            if (success) {
                loadDuplicateFiles()
            }
            onResult(success)
        }
    }
}
