package com.example.basekotlin.data.local.safebox

import java.security.cert.Extension
import java.util.Locale

enum class SafeBoxFileType {
    PICTURES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    OTHERS;

    companion object {
        fun fromExtension(extension: String): SafeBoxFileType{
            val ext = extension.lowercase(Locale.ROOT).trimStart('.')
            return when (ext) {
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> PICTURES
                "mp4", "mkv", "avi", "mov", "3gp", "webm", "flv" -> VIDEOS
                "mp3", "wav", "aac", "m4a", "flac", "ogg", "wma" -> AUDIO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt" -> DOCUMENTS
                else -> OTHERS
            }
        }
    }
}