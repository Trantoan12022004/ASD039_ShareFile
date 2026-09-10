package com.example.basekotlin.data.local.repository.download


import android.content.Context
import com.example.basekotlin.data.local.download.DownloadScanner
import com.example.basekotlin.ui.download.model.DownloadItem
import com.example.basekotlin.ui.download.model.DownloadScanResult
import com.example.basekotlin.ui.download.model.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepositoryImpl(private val context: Context) : DownloadRepository {

    override suspend fun fetchAllDownloads(): DownloadScanResult {
        return DownloadScanner.scanAllDownloads(context)
    }

    override suspend fun fetchDownloadsByType(type: DownloadType): List<DownloadItem> {
        return DownloadScanner.scanByType(type, context)
    }
}
