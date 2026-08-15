package com.example.basekotlin.model

// Phân biệt nguồn gốc để biết cách xử lý hành động (mở app / cài đặt / xóa file)
enum class AppSourceType {
    INSTALLED,
    APK_FILE,
    RECEIVED
}

// domain model cho 1 app hoặc 1 file apk
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkFilePath: String,
    val sizeBytes: Long,
    val isSystemApp: Boolean,
    val isCurrentlyInstalled: Boolean,
    val firstInstallTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val sourceType: AppSourceType
)