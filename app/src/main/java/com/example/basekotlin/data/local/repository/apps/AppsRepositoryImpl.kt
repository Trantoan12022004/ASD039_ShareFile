package com.example.basekotlin.data.local.repository.apps

import android.content.Context
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.data.local.appstore.InstalledAppSource
import com.example.basekotlin.data.local.appstore.ReceivedAppSource
import com.example.basekotlin.model.AppInfo

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