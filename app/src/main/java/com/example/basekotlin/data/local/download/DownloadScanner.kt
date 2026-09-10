package com.example.basekotlin.data.local.download

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import com.example.basekotlin.ui.download.model.DownloadItem
import com.example.basekotlin.ui.download.model.DownloadScanResult
import com.example.basekotlin.ui.download.model.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Phân loại các mục trong mục Download
 */


/**
 * Model đại diện cho một file đã tải về trong ShareFile/Download
 */


/**
 * Kết quả quét tổng hợp theo từng loại
 */


/**
 * Quét các file trong thư mục ShareFile/Download theo 4 folder: Videos, Photos, Music, Apps
 */
object DownloadScanner {

    private const val ROOT_DOWNLOAD_PATH = "ShareFile/Download"
    private const val FOLDER_VIDEOS = "Videos"
    private const val FOLDER_PHOTOS = "Photos"
    private const val FOLDER_MUSIC = "Music"
    private const val FOLDER_APPS = "Apps"

    /**
     * Lấy thư mục gốc ShareFile/Download
     */
    fun getRootDownloadDir(): File {
        val root = Environment.getExternalStorageDirectory()
        return File(root, ROOT_DOWNLOAD_PATH)
    }

    /**
     * Lấy File đại diện cho từng thư mục con
     */
    fun getVideosDir(): File = File(getRootDownloadDir(), FOLDER_VIDEOS)
    fun getPhotosDir(): File = File(getRootDownloadDir(), FOLDER_PHOTOS)
    fun getMusicDir(): File = File(getRootDownloadDir(), FOLDER_MUSIC)
    fun getAppsDir(): File = File(getRootDownloadDir(), FOLDER_APPS)

    /**
     * Quét tất cả các file từ 4 thư mục và phân loại
     * Chạy bất đồng bộ trên IO Dispatcher
     */
    suspend fun scanAllDownloads(context: Context? = null): DownloadScanResult {
        return withContext(Dispatchers.IO) {
            val videoFiles = scanFolder(getVideosDir(), DownloadType.VIDEOS, context)
            val photoFiles = scanFolder(getPhotosDir(), DownloadType.PHOTOS, context)
            val musicFiles = scanFolder(getMusicDir(), DownloadType.MUSIC, context)
            val appFiles = scanFolder(getAppsDir(), DownloadType.APPS, context)

            // Gộp tất cả và sắp xếp theo ngày sửa đổi mới nhất
            val allFiles = (videoFiles + photoFiles + musicFiles + appFiles)
                .sortedByDescending { it.dateModifiedMillis }

            DownloadScanResult(
                allFiles = allFiles,
                videoFiles = videoFiles,
                photoFiles = photoFiles,
                musicFiles = musicFiles,
                appFiles = appFiles
            )
        }
    }

    /**
     * Quét các file trong một thư mục cụ thể và ánh xạ sang DownloadItem
     */
    suspend fun scanByType(type: DownloadType, context: Context? = null): List<DownloadItem> {
        return withContext(Dispatchers.IO) {
            when (type) {
                DownloadType.ALL -> scanAllDownloads(context).allFiles
                DownloadType.VIDEOS -> scanFolder(getVideosDir(), DownloadType.VIDEOS, context)
                DownloadType.PHOTOS -> scanFolder(getPhotosDir(), DownloadType.PHOTOS, context)
                DownloadType.MUSIC -> scanFolder(getMusicDir(), DownloadType.MUSIC, context)
                DownloadType.APPS -> scanFolder(getAppsDir(), DownloadType.APPS, context)
            }
        }
    }

    private fun scanFolder(
        folder: File,
        type: DownloadType,
        context: Context?
    ): List<DownloadItem> {
        if (!folder.exists() || !folder.isDirectory) {
            return emptyList()
        }

        val fileList = mutableListOf<File>()
        collectFilesRecursively(folder, fileList)

        val items = mutableListOf<DownloadItem>()
        for (file in fileList) {
            val item = parseFileToDownloadItem(file, type, context)
            items.add(item)
        }

        // Sắp xếp file mới nhất lên đầu
        return items.sortedByDescending { it.dateModifiedMillis }
    }

    /**
     * Đệ quy thu thập tất cả các file bên trong thư mục (bỏ qua file/folder ẩn bắt đầu bằng dấu chấm)
     */
    private fun collectFilesRecursively(dir: File, result: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.name.startsWith(".")) {
                continue
            }
            if (file.isDirectory) {
                collectFilesRecursively(file, result)
            } else if (file.isFile) {
                result.add(file)
            }
        }
    }

    /**
     * Chuyển đổi từ File sang DownloadItem
     */
    private fun parseFileToDownloadItem(
        file: File,
        type: DownloadType,
        context: Context?
    ): DownloadItem {
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""

        var appName: String? = null
        var packageName: String? = null

        // Nếu là file APK và có context, phân tích đọc tên app và packageName
        if (type == DownloadType.APPS && extension == "apk" && context != null) {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            if (packageInfo != null && packageInfo.applicationInfo != null) {
                packageInfo.applicationInfo!!.sourceDir = file.absolutePath
                packageInfo.applicationInfo!!.publicSourceDir = file.absolutePath
                appName = packageManager.getApplicationLabel(packageInfo.applicationInfo!!).toString()
                packageName = packageInfo.packageName
            }
        }

        return DownloadItem(
            id = file.absolutePath,
            name = file.name,
            path = file.absolutePath,
            sizeBytes = file.length(),
            dateModifiedMillis = file.lastModified(),
            extension = extension,
            type = type,
            mimeType = mimeType,
            appName = appName,
            packageName = packageName
        )
    }
}
