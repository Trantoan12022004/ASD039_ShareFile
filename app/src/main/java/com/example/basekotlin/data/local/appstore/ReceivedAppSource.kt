package com.example.basekotlin.data.local.appstore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.model.AppSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ReceivedAppSource {

    // Cùng quy ước thư mục với Music: ShareFile/Music/Received/ -> ShareFile/Apps/Received/
    const val RECEIVED_RELATIVE_PATH = "ShareFile/Apps/Received/"

    suspend fun queryReceivedApks(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<AppInfo>()
            val receivedDir = File(Environment.getExternalStorageDirectory(), RECEIVED_RELATIVE_PATH)

            val files = receivedDir.listFiles()
            if (files != null) {
                val packageManager = context.packageManager
                val installedPackageNames = mutableSetOf<String>()
                val installedPackages = packageManager.getInstalledPackages(0)
                for (packageInfo in installedPackages) {
                    installedPackageNames.add(packageInfo.packageName)
                }

                for (file in files) {
                    if (file.isFile && file.name.endsWith(".apk", ignoreCase = true)) {
                        val appInfo = ApkFileInfoReader(packageManager, file, installedPackageNames)
                        if (appInfo != null) {
                            result.add(appInfo)
                        }
                    }
                }
            }
            result
        }
    }

    fun loadIcon(context: Context, apkFilePath: String): android.graphics.drawable.Drawable? {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(apkFilePath, 0)
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return null
        }
        packageInfo.applicationInfo!!.sourceDir = apkFilePath
        packageInfo.applicationInfo!!.publicSourceDir = apkFilePath
        return packageManager.getApplicationIcon(packageInfo.applicationInfo!!)
    }

    private fun ApkFileInfoReader(
        packageManager: PackageManager,
        apkFile: File,
        installedPackageNames: Set<String>
    ): AppInfo? {
        val packageInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        if (packageInfo == null) {
            return null
        }

        val applicationInfo = packageInfo.applicationInfo
        if (applicationInfo == null) {
            return null
        }

        // Bắt buộc gán lại 2 field này thì mới getApplicationLabel/loadIcon đọc được từ file archive
        applicationInfo.sourceDir = apkFile.absolutePath
        applicationInfo.publicSourceDir = apkFile.absolutePath

        var appLabel = packageManager.getApplicationLabel(applicationInfo).toString()
        if (appLabel.isEmpty()) {
            appLabel = apkFile.nameWithoutExtension
        }

        var versionCode = packageInfo.versionCode.toLong()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            versionCode = packageInfo.longVersionCode
        }

        val isInstalled = installedPackageNames.contains(packageInfo.packageName)

        return AppInfo(
            packageName = packageInfo.packageName,
            appName = appLabel,
            versionName = packageInfo.versionName ?: "",
            versionCode = versionCode,
            apkFilePath = apkFile.absolutePath,
            sizeBytes = apkFile.length(),
            isSystemApp = false,
            isCurrentlyInstalled = isInstalled,
            firstInstallTimeMillis = apkFile.lastModified(),
            lastUpdateTimeMillis = apkFile.lastModified(),
            sourceType = AppSourceType.APK_FILE
        )
    }
}