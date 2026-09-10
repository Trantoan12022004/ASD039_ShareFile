package com.example.basekotlin.data.local.safebox.converter

import androidx.room.TypeConverter
import com.example.basekotlin.data.local.safebox.SafeBoxFileType

class SafeBoxTypeConverters {
    @TypeConverter
    fun fromFileType(fileType: SafeBoxFileType): String {
        return fileType.name
    }

    @TypeConverter
    fun toFileType(value: String): SafeBoxFileType {
        return runCatching { SafeBoxFileType.valueOf(value) }
            .getOrDefault(SafeBoxFileType.OTHERS)
    }
}
