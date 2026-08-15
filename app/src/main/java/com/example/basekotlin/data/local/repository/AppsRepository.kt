package com.example.basekotlin.data.local.repository

import com.example.basekotlin.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppsRepository {
    suspend fun fetchInstalledApps(): List<AppInfo>
    suspend fun fetchApkFiles(): List<AppInfo>
    suspend fun fetchReceivedApps(): List<AppInfo>
}