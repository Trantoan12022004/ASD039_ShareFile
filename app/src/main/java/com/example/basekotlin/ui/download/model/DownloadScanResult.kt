package com.example.basekotlin.ui.download.model

data class DownloadScanResult(
    val allFiles: List<DownloadItem> = emptyList(),
    val videoFiles: List<DownloadItem> = emptyList(),
    val photoFiles: List<DownloadItem> = emptyList(),
    val musicFiles: List<DownloadItem> = emptyList(),
    val appFiles: List<DownloadItem> = emptyList()
) {
    /**
     * Lấy danh sách file theo phân loại
     */
    fun getFilesByType(type: DownloadType): List<DownloadItem> {
        return when (type) {
            DownloadType.ALL -> allFiles
            DownloadType.VIDEOS -> videoFiles
            DownloadType.PHOTOS -> photoFiles
            DownloadType.MUSIC -> musicFiles
            DownloadType.APPS -> appFiles
        }
    }
}