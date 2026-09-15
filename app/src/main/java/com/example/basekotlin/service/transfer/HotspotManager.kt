package com.example.basekotlin.service.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.util.transfer.NetworkUtils

/**
 * Quản lý LocalOnlyHotspot trên máy RECEIVER
 *
 * Flow:
 * 1. startHotspot() → hệ thống tạo hotspot với SSID/password ngẫu nhiên
 * 2. onStarted → lấy SSID, password, IP → callback cho UI
 * 3. UI generate QR code từ ConnectionInfo
 * 4. Khi transfer xong → stopHotspot()
 *
 * Lưu ý: LocalOnlyHotspot chỉ cho phép giao tiếp local (không có internet)
 * → phù hợp cho file transfer P2P
 */
class HotspotManager(private val context: Context) {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"
    }

    private var hotspotReservation: LocalOnlyHotspotReservation? = null
    private var isRunning = false

    /**
     * Callback khi trạng thái hotspot thay đổi
     */
    interface HotspotListener {
        fun onHotspotStarted(connectionInfo: ConnectionInfo)
        fun onHotspotFailed(errorCode: Int)
        fun onHotspotStopped()
    }

    /**
     * Bật LocalOnlyHotspot
     *
     * @param port port mà TCP server sẽ lắng nghe
     * @param listener callback nhận kết quả
     */
    fun startHotspot(port: Int, listener: HotspotListener) {
        if (isRunning) {
            Log.w(TAG, "[RECEIVER] Hotspot đang chạy rồi")
            return
        }

        Log.d(TAG, "[RECEIVER] Bắt đầu gọi WifiManager.startLocalOnlyHotspot()...")
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onStarted(reservation: LocalOnlyHotspotReservation?) {
                    hotspotReservation = reservation
                    isRunning = true

                    val ssid: String
                    val password: String

                    if (reservation?.wifiConfiguration != null) {
                        // API < 33
                        @Suppress("DEPRECATION")
                        ssid = reservation.wifiConfiguration?.SSID ?: "Unknown"
                        @Suppress("DEPRECATION")
                        password = reservation.wifiConfiguration?.preSharedKey ?: ""
                    } else {
                        // API 33+ dùng SoftApConfiguration
                        val softApConfig = reservation?.softApConfiguration
                        ssid = softApConfig?.ssid ?: "Unknown"
                        password = softApConfig?.passphrase ?: ""
                    }

                    // SỬA LỖI: Lấy IP chuẩn của Hotspot (192.168.49.1 / Hotspot interface), không bao giờ lấy IP 4G
                    val ip = NetworkUtils.getHotspotIpAddress()
                    val deviceName = NetworkUtils.getDeviceName()

                    val connectionInfo = ConnectionInfo(
                        ssid = ssid,
                        password = password,
                        ipAddress = ip,
                        port = port,
                        deviceName = deviceName
                    )

                    Log.d(TAG, "[RECEIVER] Hotspot bật THÀNH CÔNG -> SSID: $ssid | Pass: $password | IP: $ip | Port: $port")
                    listener.onHotspotStarted(connectionInfo)
                }

                override fun onStopped() {
                    isRunning = false
                    hotspotReservation = null
                    Log.d(TAG, "[RECEIVER] Hotspot đã tắt")
                    listener.onHotspotStopped()
                }

                override fun onFailed(reason: Int) {
                    isRunning = false
                    Log.e(TAG, "[RECEIVER] Hotspot bật thất bại, reason=$reason")
                    listener.onHotspotFailed(reason)
                }

            }, Handler(Looper.getMainLooper()))

        } catch (e: SecurityException) {
            Log.e(TAG, "[RECEIVER] Thiếu permission Location khi bật Hotspot", e)
            listener.onHotspotFailed(-1)
        } catch (e: Exception) {
            Log.e(TAG, "[RECEIVER] Lỗi không xác định khi bật hotspot", e)
            listener.onHotspotFailed(-2)
        }
    }

    /**
     * Tắt hotspot và giải phóng tài nguyên
     */
    fun stopHotspot() {
        try {
            hotspotReservation?.close()
        } catch (e: Exception) {
            Log.e(TAG, "[RECEIVER] Lỗi khi tắt hotspot", e)
        } finally {
            hotspotReservation = null
            isRunning = false
        }
    }

    /**
     * Kiểm tra hotspot có đang chạy không
     */
    fun isHotspotRunning(): Boolean = isRunning
}
