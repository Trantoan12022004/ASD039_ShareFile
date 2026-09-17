package com.example.basekotlin.util.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Locale

/**
 * Utility cho network operations
 * - Lấy IP local, tìm port trống
 * - Format tốc độ, dung lượng
 * - Check trạng thái WiFi
 */
object NetworkUtils {

    private const val TAG = "DEBUG_SEND_FILE"

    /**
     * Lấy địa chỉ IP khi Receiver bật LocalOnlyHotspot.
     * Pass 1: Ưu tiên tìm interface ap/swlan/softap/wlan1 hoặc dải hotspot.
     * Pass 2: Tìm IP dải 192.168.49.x / 192.168.43.x.
     * Pass 3: Quét tất cả interface, loại trừ cellular/VPN/wlan0-client.
     * Fallback: Mặc định 192.168.49.1.
     */
    fun getHotspotIpAddress(): String {
        try {
            val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces())

            // 1. Tìm interface đặc trưng của Hotspot / SoftAP
            for (ni in interfaces) {
                if (!ni.isUp || ni.isLoopback) continue
                val name = ni.name.lowercase()
                if (name.contains("ap") || name.contains("swlan") || name.contains("softap") || name.contains("wlan1") || name.contains("p2p")) {
                    for (addr in java.util.Collections.list(ni.inetAddresses)) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress ?: continue
                            if (host != "0.0.0.0") {
                                android.util.Log.d(TAG, "[NetworkUtils] Tìm thấy Hotspot IP từ interface ${ni.name}: $host")
                                return host
                            }
                        }
                    }
                }
            }

            // 2. Tìm interface dải 192.168.49.x hoặc 192.168.43.x
            for (ni in interfaces) {
                if (!ni.isUp || ni.isLoopback) continue
                for (addr in java.util.Collections.list(ni.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.49.") || host.startsWith("192.168.43.")) {
                            android.util.Log.d(TAG, "[NetworkUtils] Tìm thấy Hotspot IP theo subnet: $host")
                            return host
                        }
                    }
                }
            }

            // 3. Quét tất cả interface, loại trừ cellular/VPN/wlan0-client
            // Phục vụ các thiết bị Xiaomi/Samsung dùng interface name và subnet không chuẩn
            for (ni in interfaces) {
                if (!ni.isUp || ni.isLoopback) continue
                val name = ni.name.lowercase()
                // Bỏ qua cellular (rmnet), VPN (tun/tap), dummy, loopback, wlan0 (WiFi client)
                if (name.startsWith("rmnet") || name.startsWith("dummy") ||
                    name.startsWith("tun") || name.startsWith("tap") ||
                    name.startsWith("lo") || name == "wlan0") continue

                for (addr in java.util.Collections.list(ni.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (host != "0.0.0.0") {
                            android.util.Log.d(TAG, "[NetworkUtils] Tìm thấy Hotspot IP (pass 3) từ interface ${ni.name}: $host")
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[NetworkUtils] Lỗi khi lấy Hotspot IP", e)
        }

        // Fallback chuẩn của Android LocalOnlyHotspot
        android.util.Log.d(TAG, "[NetworkUtils] Sử dụng Hotspot IP mặc định: 192.168.49.1")
        return "192.168.49.1"
    }

    /**
     * Lấy IP khi 2 máy kết nối chung mạng Wi-Fi ngoài (wlan0)
     */
    fun getWifiIpAddress(): String {
        try {
            val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            for (ni in interfaces) {
                if (!ni.isUp || ni.isLoopback) continue
                if (ni.name.startsWith("wlan", ignoreCase = true) && !ni.name.contains("wlan1")) {
                    for (addr in java.util.Collections.list(ni.inetAddresses)) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress ?: continue
                            if (host != "0.0.0.0") return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[NetworkUtils] Lỗi khi lấy Wi-Fi IP", e)
        }
        return "0.0.0.0"
    }

    /**
     * Lấy địa chỉ IPv4 local của thiết bị (backward compatibility)
     */
    fun getLocalIpAddress(): String = getWifiIpAddress()

    /**
     * Tìm 1 port TCP trống, trả về port number
     * Dùng ServerSocket(0) để OS tự chọn port available
     */
    fun getAvailablePort(): Int {
        return try {
            ServerSocket(0).use { it.localPort }
        } catch (e: Exception) {
            // Fallback port nếu không tìm được
            8888
        }
    }

    /**
     * Kiểm tra WiFi có đang kết nối không (hỗ trợ cả khi 4G đang là activeNetwork)
     */
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = cm.activeNetwork
        if (activeNet != null) {
            val caps = cm.getNetworkCapabilities(activeNet)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) return true
        }
        return cm.allNetworks.any { net ->
            val caps = cm.getNetworkCapabilities(net)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    /**
     * Lấy IP Gateway của mạng Wi-Fi/Hotspot đang kết nối từ DHCP
     */
    fun getWifiGatewayIp(context: Context): String? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiNet = cm.allNetworks.firstOrNull { net ->
                cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } ?: return null
            val lp = cm.getLinkProperties(wifiNet) ?: return null
            val gw = lp.routes.mapNotNull { route ->
                val g = route.gateway
                if (g is Inet4Address && g.hostAddress != "0.0.0.0") g.hostAddress else null
            }.firstOrNull()
            if (gw != null) return gw

            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val dhcp = wm?.dhcpInfo
            if (dhcp != null && dhcp.gateway != 0) {
                val ip = dhcp.gateway
                return String.format(Locale.US, "%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Lấy tên WiFi đang kết nối (hỗ trợ tốt từ Android 9 đến 14+)
     */
    fun getConnectedWifiName(context: Context): String {
        try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            var ssid = wifiInfo?.ssid?.replace("\"", "") ?: ""

            // Nếu trả về unknown, thử lấy qua ConnectivityManager (chuẩn Android 10+)
            if (ssid.isEmpty() || ssid == "<unknown ssid>") {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(network)
                val transportInfo = capabilities?.transportInfo
                if (transportInfo is android.net.wifi.WifiInfo) {
                    ssid = transportInfo.ssid.replace("\"", "")
                }
            }

            return if (ssid.isNotEmpty() && ssid != "<unknown ssid>") ssid else "Connected WiFi"
        } catch (e: Exception) {
            return "Connected WiFi"
        }
    }


    /**
     * Format bytes thành chuỗi dễ đọc
     * Ví dụ: 1536 → "1.5 KB", 1073741824 → "1.0 GB"
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(
                Locale.US,
                "%.1f MB",
                bytes / (1024.0 * 1024)
            )
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * Format tốc độ truyền (bytes/sec → "23.5 MB")
     * Lưu ý: chỉ format phần số, "/s" sẽ thêm từ string resource
     */
    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B"
            bytesPerSec < 1024 * 1024 -> String.format(
                Locale.US,
                "%.1f KB",
                bytesPerSec / 1024.0
            )
            else -> String.format(Locale.US, "%.1f MB", bytesPerSec / (1024.0 * 1024))
        }
    }

    /**
     * Format thời gian (milliseconds → "3m 24s")
     */
    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return when {
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Lấy tên thiết bị
     */
    fun getDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar {
            it.uppercaseChar()
        }
        val model = android.os.Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}
