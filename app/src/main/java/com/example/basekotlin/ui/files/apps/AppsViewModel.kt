package com.example.basekotlin.ui.files.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.AppsRepository
import com.example.basekotlin.data.local.repository.AppsRepositoryImpl
import com.example.basekotlin.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppsRepository

    init {
        val appContext = application.applicationContext
        repository = AppsRepositoryImpl(appContext)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // ================= INSTALLED TAB =================
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingInstalled = MutableStateFlow(false)
    val isLoadingInstalled: StateFlow<Boolean> = _isLoadingInstalled
    private var installedLoadedOnce = false

    private val searchedInstalledApps: StateFlow<List<AppInfo>> = combine(
        _installedApps,
        _searchQuery
    ) { apps, query ->
        filterApkBySearchQuery(apps, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedAppsSearch: StateFlow<List<AppInfo>> = searchedInstalledApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chỉ fetch nếu tab Installed chưa từng được load, tránh gọi lại mỗi lần onResume
    fun loadInstalledAppsIfNeeded() {
        if (installedLoadedOnce) {
            return
        }
        viewModelScope.launch {
            _isLoadingInstalled.value = true
            try {
                val apps = repository.fetchInstalledApps()
                _installedApps.value = apps
                installedLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingInstalled.value = false
            }
        }
    }

    // Dùng cho pull-to-refresh hoặc sau khi xoá/cài app, luôn fetch lại bất kể đã load hay chưa
    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingInstalled.value = true
            try {
                val apps = repository.fetchInstalledApps()
                _installedApps.value = apps
                installedLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingInstalled.value = false
            }
        }
    }

    // ================= APK TAB (Not Installed + Deletable) =================
    private val _apkFiles = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingApk = MutableStateFlow(false)
    val isLoadingApk: StateFlow<Boolean> = _isLoadingApk
    private var apkLoadedOnce = false

    private val searchedApkFiles: StateFlow<List<AppInfo>> = combine(
        _apkFiles,
        _searchQuery
    ) { apps, query ->
        filterApkBySearchQuery(apps, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notInstalledApkFiles: StateFlow<List<AppInfo>> = searchedApkFiles
        .map { apkList -> filterApkByInstalledState(apkList, wantInstalled = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletableApkFiles: StateFlow<List<AppInfo>> = searchedApkFiles
        .map { apkList -> filterApkByInstalledState(apkList, wantInstalled = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadApkFilesIfNeeded() {
        if (apkLoadedOnce) {
            return
        }
        viewModelScope.launch {
            _isLoadingApk.value = true
            try {
                val apks = repository.fetchApkFiles()
                _apkFiles.value = apks
                apkLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingApk.value = false
            }
        }
    }

    fun refreshApkFiles() {
        viewModelScope.launch {
            _isLoadingApk.value = true
            try {
                val apks = repository.fetchApkFiles()
                _apkFiles.value = apks
                apkLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingApk.value = false
            }
        }
    }

    // ================= RECEIVED TAB =================
    private val _receivedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingReceived = MutableStateFlow(false)
    val isLoadingReceived: StateFlow<Boolean> = _isLoadingReceived
    private var receivedLoadedOnce = false

    private val searchedReceiveApps: StateFlow<List<AppInfo>> = combine(
        _receivedApps,
        _searchQuery
    ) { apps, query ->
        filterApkBySearchQuery(apps, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivedAppsSearch: StateFlow<List<AppInfo>> = searchedReceiveApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadReceivedAppsIfNeeded() {
        if (receivedLoadedOnce) {
            return
        }
        viewModelScope.launch {
            _isLoadingReceived.value = true
            try {
                val apps = repository.fetchReceivedApps()
                _receivedApps.value = apps
                receivedLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingReceived.value = false
            }
        }
    }

    fun refreshReceivedApps() {
        viewModelScope.launch {
            _isLoadingReceived.value = true
            try {
                val apps = repository.fetchReceivedApps()
                _receivedApps.value = apps
                receivedLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingReceived.value = false
            }
        }
    }

    // ================= SELECTION MODE (dùng chung cho tab APK) =================
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedApkFilePaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedApkFilePaths: StateFlow<Set<String>> = _selectedApkFilePaths

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun enterApkSelectionMode(apkFilePath: String? = null) {
        _isSelectionMode.value = true
        if (apkFilePath != null) {
            _selectedApkFilePaths.value = setOf(apkFilePath)
        } else {
            _selectedApkFilePaths.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedApkFilePaths.value = emptySet()
    }

    fun toggleApkSelection(apkFilePath: String) {
        val currentSet = _selectedApkFilePaths.value.toMutableSet()
        if (currentSet.contains(apkFilePath)) {
            currentSet.remove(apkFilePath)
        } else {
            currentSet.add(apkFilePath)
        }
        _selectedApkFilePaths.value = currentSet
    }

    fun selectAllApks(apkFilePath: List<String>) {
        _selectedApkFilePaths.value = apkFilePath.toSet()
    }

    fun clearApkSelection() {
        _selectedApkFilePaths.value = emptySet()
    }

    // ================= FILTER HELPERS =================
    private fun filterApkByInstalledState(
        apkList: List<AppInfo>,
        wantInstalled: Boolean
    ): List<AppInfo> {
        val result = mutableListOf<AppInfo>()
        for (apk in apkList) {
            if (apk.isCurrentlyInstalled == wantInstalled) {
                result.add(apk)
            }
        }
        return result
    }

    private fun filterApkBySearchQuery(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) {
            return apps
        }
        val filtered = mutableListOf<AppInfo>()
        for (app in apps) {
            val nameMatch = app.appName.contains(query, ignoreCase = true)
            val packageMatch = app.packageName.contains(query, ignoreCase = true)
            if (nameMatch || packageMatch) {
                filtered.add(app)
            }
        }
        return filtered
    }
}