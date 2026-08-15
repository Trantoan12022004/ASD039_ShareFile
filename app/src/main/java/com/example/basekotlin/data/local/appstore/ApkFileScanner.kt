package com.example.basekotlin.data.local.appstore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.model.AppSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ApkFileScanner {

    suspend fun scanAllApkFiles(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            Log.d("APP_LOAD", "scanAllApkFiles START")
            val result = mutableListOf<AppInfo>()
            val rootDir = Environment.getExternalStorageDirectory()

            val foundFiles = mutableListOf<File>()
            collectApkFilesRecursively(rootDir, foundFiles, depth = 0)

            val packageManager = context.packageManager
            val installedPackageNames = getInstalledPackageNameSet(context)
            Log.d("APP_LOAD", "collectApkFilesRecursively found=${foundFiles.size} elapsedMs=${System.currentTimeMillis() - startTime}")
            for (apkFile in foundFiles) {
                val appInfo = readApkInfo(packageManager, apkFile, installedPackageNames)
                if (appInfo != null) {
                    result.add(appInfo)
                }
            }
            Log.d("APP_LOAD", "scanAllApkFiles DONE total=${result.size} elapsedMs=${System.currentTimeMillis() - startTime}")
            result
        }
    }

    // Đệ quy có giới hạn độ sâu + bỏ qua thư mục nặng để tránh quét chậm/lỗi quyền
    private fun collectApkFilesRecursively(dir: File, output: MutableList<File>, depth: Int) {
        if (depth > MAX_SCAN_DEPTH) {
            return
        }
        val children = dir.listFiles()
        if (children == null) {
            return
        }
        for (child in children) {
            if (child.isDirectory) {
                if (SKIP_DIR_NAMES.contains(child.name) == false) {
                    collectApkFilesRecursively(child, output, depth + 1)
                }
            } else {
                if (child.name.endsWith(".apk", ignoreCase = true)) {
                    output.add(child)
                }
            }
        }
    }

    private fun readApkInfo(
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

    private fun getInstalledPackageNameSet(context: Context): Set<String> {
        val names = mutableSetOf<String>()
        val installedPackages = context.packageManager.getInstalledPackages(0)
        for (packageInfo in installedPackages) {
            names.add(packageInfo.packageName)
        }
        return names
    }

    // Icon từ file apk chưa cài — load lazy tương tự InstalledAppSource
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

    private const val MAX_SCAN_DEPTH = 12
    private val SKIP_DIR_NAMES = setOf("Android", ".thumbnails", ".trashed")
}