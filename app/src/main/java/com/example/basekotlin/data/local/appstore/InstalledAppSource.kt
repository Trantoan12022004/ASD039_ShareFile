package com.example.basekotlin.data.local.appstore

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.model.AppSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InstalledAppSource {

    suspend fun queryInstalledApps(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            Log.d("APP_LOAD", "queryInstalledApps START on thread=${Thread.currentThread().name}")
            val result = mutableListOf<AppInfo>()
            val packageManager = context.packageManager

            // GET_META_DATA không bắt buộc, nhưng để mở rộng sau này (đọc icon, permission...)
            val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

            for (packageInfo in installedPackages) {
                val applicationInfo = packageInfo.applicationInfo
                if (applicationInfo == null) {
                    continue
                }

                // Lấy tên hiển thị (label), fallback về packageName nếu không đọc được
                var appLabel = packageManager.getApplicationLabel(applicationInfo).toString()
                if (appLabel.isEmpty()) {
                    appLabel = packageInfo.packageName
                }

                // Cờ FLAG_SYSTEM để phân biệt app hệ thống, dùng để lọc/ẩn nếu cần
                val isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                var versionCode = packageInfo.versionCode.toLong()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    versionCode = packageInfo.longVersionCode
                }

                var apkSizeBytes = 0L
                val apkFile = java.io.File(applicationInfo.sourceDir)
                if (apkFile.exists()) {
                    apkSizeBytes = apkFile.length()
                }

                val appInfo = AppInfo(
                    packageName = packageInfo.packageName,
                    appName = appLabel,
                    versionName = packageInfo.versionName ?: "",
                    versionCode = versionCode,
                    apkFilePath = applicationInfo.sourceDir,
                    sizeBytes = apkSizeBytes,
                    isSystemApp = isSystemApp,
                    isCurrentlyInstalled = true,
                    firstInstallTimeMillis = packageInfo.firstInstallTime,
                    lastUpdateTimeMillis = packageInfo.lastUpdateTime,
                    sourceType = AppSourceType.INSTALLED
                )
                result.add(appInfo)
            }
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("APP_LOAD", "queryInstalledApps DONE count=${result.size} elapsedMs=$elapsed")

            result
        }
    }

    // Icon load lazy, gọi trong Adapter khi bind view (nên có LruCache bọc ngoài)
    fun loadIcon(context: Context, packageName: String): android.graphics.drawable.Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (error: PackageManager.NameNotFoundException) {
            null
        }
    }
}