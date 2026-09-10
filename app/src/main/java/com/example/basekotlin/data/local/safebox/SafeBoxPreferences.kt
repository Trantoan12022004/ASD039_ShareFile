package com.example.basekotlin.data.local.safebox

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class SafeBoxPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "safebox_prefs"
        private const val KEY_PATTERN_HASH = "key_safebox_pattern_hash"
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val SALT = "ASD039_SAFEBOX_SECURITY_SALT_2026"

        @Volatile
        private var instance: SafeBoxPreferences? = null

        fun getInstance(context: Context): SafeBoxPreferences {
            val existing = instance
            if (existing != null) {
                return existing
            }
            return synchronized(this) {
                val current = instance
                if (current != null) {
                    current
                } else {
                    val created = SafeBoxPreferences(context.applicationContext)
                    instance = created
                    created
                }
            }
        }
    }

    // Kiểm tra xem user đã tạo pattern khóa hay chưa
    fun isPatternSet(): Boolean {
        val hash = prefs.getString(KEY_PATTERN_HASH, null)
        return !hash.isNullOrEmpty()
    }

    // Lưu pattern mới sau khi băm SHA-256 với Salt
    fun savePattern(pattern: String) {
        val hash = hashPattern(pattern)
        prefs.edit()
            .putString(KEY_PATTERN_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply()
    }

    // Kiểm tra pattern vẽ vào có khớp với pattern đã lưu không
    fun verifyPattern(pattern: String): Boolean {
        val savedHash = prefs.getString(KEY_PATTERN_HASH, null) ?: return false
        val inputHash = hashPattern(pattern)
        val isMatched = (savedHash == inputHash)
        if (isMatched) {
            resetFailedAttempts()
        } else {
            incrementFailedAttempts()
        }
        return isMatched
    }

    // Xóa mật khẩu (dùng khi reset hoặc debug)
    fun clearPattern() {
        prefs.edit()
            .remove(KEY_PATTERN_HASH)
            .remove(KEY_FAILED_ATTEMPTS)
            .apply()
    }

    fun getFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    private fun incrementFailedAttempts() {
        val current = getFailedAttempts()
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current + 1).apply()
    }

    // Hàm băm mật khẩu SHA-256 an toàn
    private fun hashPattern(pattern: String): String {
        val saltedInput = pattern + SALT
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(saltedInput.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
