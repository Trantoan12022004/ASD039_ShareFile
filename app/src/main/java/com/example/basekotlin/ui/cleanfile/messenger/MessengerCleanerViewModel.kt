package com.example.basekotlin.ui.cleanfile.messenger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.cleanfile.MessengerMediaScanner
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.MessengerCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessengerCleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)

    private val _categories = MutableStateFlow<List<MessengerCategory>>(emptyList())
    val categories: StateFlow<List<MessengerCategory>> = _categories.asStateFlow()

    private val _totalSizeBytes = MutableStateFlow(0L)
    val totalSizeBytes: StateFlow<Long> = _totalSizeBytes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentMessenger = MessengerMediaScanner.MESSENGER_TELEGRAM

    fun loadCategories(messenger: String) {
        currentMessenger = messenger
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cats = repository.fetchMessengerCategories(messenger)
                _categories.value = cats
                _totalSizeBytes.value = cats.sumOf { it.sizeBytes }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cleanJunk(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val junkFiles = repository.fetchMessengerFiles(currentMessenger, MessengerMediaScanner.CAT_JUNK)
            val paths = junkFiles.map { it.path }
            val success = repository.deleteFiles(paths)
            loadCategories(currentMessenger)
            onResult(success)
        }
    }
}
