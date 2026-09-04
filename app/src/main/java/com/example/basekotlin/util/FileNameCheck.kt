package com.example.basekotlin.util

import android.content.Context
import com.example.basekotlin.R
object FileNameCheck { // Danh sách các ký tự không hợp lệ trong hệ thống tệp tin
    private val INVALID_CHARACTERS: CharArray = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    /**
     * Kiểm tra chuỗi có chứa ký tự đặc biệt bị cấm hay không
     */
    fun containsInvalidCharacters(name: String): Boolean {
        for (char in name) {
            for (invalidChar in INVALID_CHARACTERS) {
                if (char == invalidChar) {
                    return true
                }
            }
        }
        return false
    }
    /**
     * Kiểm tra tính hợp lệ của tên Folder
     * @return Chuỗi thông báo lỗi, hoặc null nếu tên hợp lệ
     */
    fun validateFolderName(
        context: Context,
        name: String,
        customValidator: ((String) -> String?)? = null
    ): String? {
        val trimmedName: String = name.trim()
        // 1. Kiểm tra rỗng
        if (trimmedName.isEmpty()) {
            val emptyError: String = context.getString(R.string.folder_name_cannot_empty)
            return emptyError
        }
        // 2. Kiểm tra ký tự không hợp lệ
        val hasInvalidChar: Boolean = containsInvalidCharacters(trimmedName)
        if (hasInvalidChar) {
            val invalidCharError: String = context.getString(R.string.file_name_invalid_chars)
            return invalidCharError
        }
        // 3. Kiểm tra quy tắc tùy biến (ví dụ: trùng tên thư mục đã có)
        if (customValidator != null) {
            val customError: String? = customValidator.invoke(trimmedName)
            if (customError != null) {
                return customError
            }
        }
        return null
    }
    /**
     * Kiểm tra tính hợp lệ của tên File
     * @return Chuỗi thông báo lỗi, hoặc null nếu tên hợp lệ
     */
    fun validateFileName(
        context: Context,
        name: String,
        customValidator: ((String) -> String?)? = null
    ): String? {
        val trimmedName: String = name.trim()
        // 1. Kiểm tra rỗng
        if (trimmedName.isEmpty()) {
            val emptyError: String = context.getString(R.string.name_cannot_empty)
            return emptyError
        }
        // 2. Kiểm tra ký tự không hợp lệ
        val hasInvalidChar: Boolean = containsInvalidCharacters(trimmedName)
        if (hasInvalidChar) {
            val invalidCharError: String = context.getString(R.string.file_name_invalid_chars)
            return invalidCharError
        }
        // 3. Kiểm tra quy tắc tùy biến
        if (customValidator != null) {
            val customError: String? = customValidator.invoke(trimmedName)
            if (customError != null) {
                return customError
            }
        }
        return null
    }
}