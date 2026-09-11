package com.example.basekotlin.ui.cleanfile.audiocleanup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.CleanFileGroup
import com.example.basekotlin.model.CleanFileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioCleanupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)

    private val _allGroups = MutableStateFlow<List<CleanFileGroup>>(emptyList())
    private val _filteredGroups = MutableStateFlow<List<CleanFileGroup>>(emptyList())
    val filteredGroups: StateFlow<List<CleanFileGroup>> = _filteredGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentQuery = ""

    fun loadAudios() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groups = repository.fetchMediaByFolder(CleanFileType.AUDIO)
                _allGroups.value = groups
                applySearch(currentQuery)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        currentQuery = query.trim()
        applySearch(currentQuery)
    }

    private fun applySearch(query: String) {
        val all = _allGroups.value
        if (query.isEmpty()) {
            _filteredGroups.value = all
            return
        }

        val filtered = all.mapNotNull { group ->
            val matchingItems = group.items.filter { it.name.contains(query, ignoreCase = true) }
            if (matchingItems.isNotEmpty()) {
                CleanFileGroup(
                    groupName = group.groupName,
                    totalSizeBytes = matchingItems.sumOf { it.sizeBytes },
                    items = matchingItems.toMutableList(),
                    isExpanded = true
                )
            } else {
                null
            }
        }
        _filteredGroups.value = filtered
    }

    fun deleteFiles(paths: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteFiles(paths)
            if (success) {
                loadAudios()
            }
            onResult(success)
        }
    }
}
