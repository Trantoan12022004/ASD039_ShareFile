package com.example.basekotlin.ui.transfer.model

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

/**
 * Thông tin kết nối được encode vào QR code
 * Receiver tạo → encode JSON → generate QR
 * Sender scan QR → parse JSON → lấy info để connect
 */
@Parcelize
data class ConnectionInfo(
    val ssid: String,
    val password: String,
    val ipAddress: String,
    val port: Int,
    val deviceName: String
) : Parcelable {

    /**
     * Serialize thành JSON string để nhúng vào QR code
     */
    fun toJson(): String = Gson().toJson(this)

    companion object {
        /**
         * Parse từ JSON string (từ QR code scan)
         * Trả về null nếu JSON không hợp lệ
         */
        fun fromJson(json: String): ConnectionInfo? {
            return try {
                Gson().fromJson(json, ConnectionInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
