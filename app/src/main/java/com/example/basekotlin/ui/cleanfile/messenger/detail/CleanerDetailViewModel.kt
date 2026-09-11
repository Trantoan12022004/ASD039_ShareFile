package com.example.basekotlin.ui.cleanfile.messenger.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.CleanFileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CleanerDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)

    private val _files = MutableStateFlow<List<CleanFileItem>>(emptyList())
    val files: StateFlow<List<CleanFileItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentMessenger = ""
    private var currentCategory = ""

    fun loadFiles(messenger: String, category: String) {
        currentMessenger = messenger
        currentCategory = category
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.fetchMessengerFiles(messenger, category)
                _files.value = list
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFiles(paths: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteFiles(paths)
            if (success) {
                loadFiles(currentMessenger, currentCategory)
            }
            onResult(success)
        }
    }
}
