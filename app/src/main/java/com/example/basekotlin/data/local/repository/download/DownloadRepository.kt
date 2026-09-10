package com.example.basekotlin.data.local.repository.download

import com.example.basekotlin.ui.download.model.DownloadItem
import com.example.basekotlin.ui.download.model.DownloadScanResult
import com.example.basekotlin.ui.download.model.DownloadType

interface DownloadRepository {
    /**
     * Quét và lấy toàn bộ danh sách file tải về (bao gồm cả 4 thư mục Videos, Photos, Music, Apps)
     */
    suspend fun fetchAllDownloads(): DownloadScanResult

    /**
     * Lấy danh sách file theo phân loại cụ thể
     */
    suspend fun fetchDownloadsByType(type: DownloadType): List<DownloadItem>
}
