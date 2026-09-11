package com.example.basekotlin.ui.cleanfile.apps

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.R
import com.example.basekotlin.data.local.repository.apps.AppsRepository
import com.example.basekotlin.data.local.repository.apps.AppsRepositoryImpl
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepository
import com.example.basekotlin.data.local.repository.cleanfile.CleanFileRepositoryImpl
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppsCleanupViewModel(application: Application) : AndroidViewModel(application) {

    private val appsRepository: AppsRepository = AppsRepositoryImpl(application.applicationContext)
    private val cleanRepository: CleanFileRepository = CleanFileRepositoryImpl(application.applicationContext)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _apkFiles = MutableStateFlow<List<AppInfo>>(emptyList())
    val apkFiles: StateFlow<List<AppInfo>> = _apkFiles.asStateFlow()

    private val _isLoadingInstalled = MutableStateFlow(false)
    val isLoadingInstalled: StateFlow<Boolean> = _isLoadingInstalled.asStateFlow()

    private val _isLoadingApk = MutableStateFlow(false)
    val isLoadingApk: StateFlow<Boolean> = _isLoadingApk.asStateFlow()

    init {
        loadInstalledApps()
        loadApkFiles()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingInstalled.value = true
            try {
                val apps = appsRepository.fetchInstalledApps()
                _installedApps.value = apps
            } finally {
                _isLoadingInstalled.value = false
            }
        }
    }

    fun loadApkFiles() {
        viewModelScope.launch {
            _isLoadingApk.value = true
            try {
                val apks = appsRepository.fetchApkFiles()
                _apkFiles.value = apks
            } finally {
                _isLoadingApk.value = false
            }
        }
    }

    fun deleteApks(paths: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = cleanRepository.deleteFiles(paths)
            if (success) {
                loadApkFiles()
            }
            onResult(success)
        }
    }

}
