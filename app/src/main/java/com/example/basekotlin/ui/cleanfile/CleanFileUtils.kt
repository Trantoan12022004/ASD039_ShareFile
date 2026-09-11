package com.example.basekotlin.ui.cleanfile

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CleanFileUtils {

    fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            sizeBytes >= gb -> String.format(Locale.US, "%.1f GB", sizeBytes / gb)
            sizeBytes >= mb -> String.format(Locale.US, "%.1f MB", sizeBytes / mb)
            sizeBytes >= kb -> String.format(Locale.US, "%.1f KB", sizeBytes / kb)
            sizeBytes > 0 -> "$sizeBytes B"
            else -> "0 B"
        }
    }

    fun formatDate(timeMillis: Long): String {
        if (timeMillis <= 0) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }
}
