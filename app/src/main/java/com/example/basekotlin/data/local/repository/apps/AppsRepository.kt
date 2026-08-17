package com.example.basekotlin.data.local.repository.apps

import com.example.basekotlin.model.AppInfo

interface AppsRepository {
    suspend fun fetchInstalledApps(): List<AppInfo>
    suspend fun fetchApkFiles(): List<AppInfo>
    suspend fun fetchReceivedApps(): List<AppInfo>
}