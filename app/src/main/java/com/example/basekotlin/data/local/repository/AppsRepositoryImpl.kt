package com.example.basekotlin.data.local.repository

import android.content.Context
import android.util.Log
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.data.local.appstore.InstalledAppSource
import com.example.basekotlin.data.local.appstore.ReceivedAppSource
import com.example.basekotlin.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

class AppsRepositoryImpl(private val appContext: Context) : AppsRepository {

    override suspend fun fetchInstalledApps(): List<AppInfo> {
        return InstalledAppSource.queryInstalledApps(appContext)
    }

    override suspend fun fetchApkFiles(): List<AppInfo> {
        return ApkFileScanner.scanAllApkFiles(appContext)
    }

    override suspend fun fetchReceivedApps(): List<AppInfo> {
        return ReceivedAppSource.queryReceivedApks(appContext)
    }
}