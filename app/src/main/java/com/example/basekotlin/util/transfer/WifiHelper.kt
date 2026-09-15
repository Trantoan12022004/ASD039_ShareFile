package com.example.basekotlin.util.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.net.Inet4Address
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper để SENDER kết nối vào WiFi Hotspot của RECEIVER
 *
 * Sử dụng WifiNetworkSpecifier (API 29+) để connect programmatically
 * KHÔNG cần user tự vào Settings → WiFi → chọn network
 *
 * Flow:
 * 1. Sender scan QR → lấy SSID + password
 * 2. connectToWifi(ssid, password) → hệ thống hiện dialog confirm
 * 3. User chấp nhận → onAvailable() → connected
 * 4. Lấy network object → bind socket vào network này
 */
class WifiHelper(private val context: Context) {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"

        /**
         * Lưu Network của Hotspot vừa kết nối thành công để Socket trong FileSenderService
         * có thể ép buộc kết nối qua đúng interface Wi-Fi (bỏ qua mạng 4G)
         */
        var activeWifiNetwork: Network? = null
        private var activeInstance: WifiHelper? = null

        /**
         * Gateway IP thực tế phát hiện từ DHCP sau khi kết nối Hotspot.
         * Dùng thay thế IP từ QR code vì một số thiết bị (Xiaomi, Samsung)
         * dùng subnet khác chuẩn 192.168.49.x cho LocalOnlyHotspot.
         */
        var activeGatewayIp: String? = null

        /**
         * Hủy kết nối Hotspot và unbind process khỏi network khi kết thúc truyền file
         */
        fun disconnectActiveWifi(context: Context) {
            try {
                activeInstance?.disconnectWifi()
            } catch (e: Exception) {
                Log.e(TAG, "[WifiHelper] Lỗi khi disconnect activeInstance", e)
            }
            try {
                val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                cm.bindProcessToNetwork(null)
            } catch (e: Exception) {
                Log.e(TAG, "[WifiHelper] Lỗi khi unbind process", e)
            }
            activeWifiNetwork = null
            activeGatewayIp = null
            activeInstance = null
        }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectedNetwork: Network? = null
    // Guard: đảm bảo onConnected chỉ được gọi đúng 1 lần dù system fire onAvailable nhiều lần
    private val hasCallbackFired = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dhcpTimeoutRunnable: Runnable? = null

    /**
     * Callback khi kết nối WiFi
     */
    interface WifiConnectionListener {
        fun onConnected(network: Network)
        fun onDisconnected()
        fun onFailed()
    }

    /**
     * Kết nối vào WiFi hotspot của receiver
     *
     * @param ssid tên mạng WiFi (từ QR code)
     * @param password mật khẩu (từ QR code)
     * @param listener callback kết quả
     */
    fun connectToWifi(ssid: String, password: String, listener: WifiConnectionListener) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        if (wifiManager?.isWifiEnabled == false) {
            Log.w(TAG, "[SENDER] [WifiHelper] Cảnh báo: Wi-Fi của thiết bị đang TẮT! Cần bật Wi-Fi để kết nối tới Hotspot.")
        }

        Log.d(TAG, "[SENDER] [WifiHelper] Bắt đầu yêu cầu kết nối tới Hotspot -> SSID: $ssid")

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        // SỬA LỖI: Bắt buộc loại bỏ NET_CAPABILITY_INTERNET vì Hotspot cục bộ không có internet
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Hủy callback cũ nếu có
        disconnectWifi()
        activeInstance = this
        hasCallbackFired.set(false)

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Lưu network và bind process, nhưng CHƯA gọi listener
                // Đợi onLinkPropertiesChanged xác nhận DHCP đã cấp IP xong
                connectedNetwork = network
                activeWifiNetwork = network
                connectivityManager.bindProcessToNetwork(network)
                Log.d(TAG, "[SENDER] [WifiHelper] onAvailable: Wi-Fi L2 connected, đợi DHCP cấp IP...")

                // Safety timeout: nếu DHCP không hoàn tất trong 8 giây, connect với network hiện tại
                dhcpTimeoutRunnable = Runnable {
                    if (hasCallbackFired.compareAndSet(false, true)) {
                        Log.w(TAG, "[SENDER] [WifiHelper] DHCP timeout 8s, tiếp tục connect với network hiện tại: $ssid")
                        listener.onConnected(network)
                    }
                }
                mainHandler.postDelayed(dhcpTimeoutRunnable!!, 8000)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                // Kiểm tra DHCP đã cấp IPv4 address chưa
                val clientLink = linkProperties.linkAddresses
                    .firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
                val ipv4Address = clientLink?.address?.hostAddress

                if (ipv4Address != null && hasCallbackFired.compareAndSet(false, true)) {
                    // Hủy safety timeout vì DHCP đã hoàn tất
                    dhcpTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

                    // Phát hiện gateway IP thực từ DHCP routes
                    // Cần thiết vì một số thiết bị (Xiaomi, Samsung) dùng subnet
                    // khác chuẩn 192.168.49.x cho LocalOnlyHotspot
                    val gatewayFromRoutes = linkProperties.routes
                        .mapNotNull { route ->
                            val gw = route.gateway
                            if (gw is Inet4Address && gw.hostAddress != "0.0.0.0") gw.hostAddress else null
                        }
                        .firstOrNull()

                    // Fallback: tính gateway từ client IP (giả định gateway = .1 trên cùng subnet)
                    val gatewayIp = gatewayFromRoutes ?: run {
                        try {
                            val bytes = (clientLink.address as Inet4Address).address.copyOf()
                            bytes[3] = 1
                            Inet4Address.getByAddress(bytes).hostAddress
                        } catch (e: Exception) {
                            null
                        }
                    }

                    activeGatewayIp = gatewayIp
                    Log.d(TAG, "[SENDER] [WifiHelper] DHCP hoàn tất, Client IP: $ipv4Address, Gateway IP: $gatewayIp -> Kết nối Hotspot THÀNH CÔNG: $ssid")
                    listener.onConnected(network)
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                dhcpTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                connectedNetwork = null
                activeWifiNetwork = null
                connectivityManager.bindProcessToNetwork(null)
                Log.d(TAG, "[SENDER] [WifiHelper] Mất kết nối Hotspot: $ssid")
                listener.onDisconnected()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                dhcpTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                Log.e(TAG, "[SENDER] [WifiHelper] Không thể kết nối tới Hotspot (onUnavailable): $ssid")
                listener.onFailed()
            }
        }

        try {
            connectivityManager.requestNetwork(request, networkCallback!!)
            Log.d(TAG, "[SENDER] [WifiHelper] Đã gửi requestNetwork tới hệ thống cho Hotspot: $ssid")
        } catch (e: Exception) {
            Log.e(TAG, "[SENDER] [WifiHelper] Lỗi khi request network", e)
            listener.onFailed()
        }
    }

    /**
     * Ngắt kết nối WiFi và giải phóng tài nguyên
     */
    fun disconnectWifi() {
        // Hủy DHCP timeout nếu đang chờ
        dhcpTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        dhcpTimeoutRunnable = null
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                Log.d(TAG, "[SENDER] [WifiHelper] Đã hủy đăng ký NetworkCallback")
            } catch (e: Exception) {
                Log.e(TAG, "[SENDER] [WifiHelper] Lỗi khi unregister network callback", e)
            }
        }
        connectivityManager.bindProcessToNetwork(null)
        networkCallback = null
        connectedNetwork = null
        activeWifiNetwork = null
        activeGatewayIp = null
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    /**
     * Lấy network object đã kết nối (dùng để bind socket)
     */
    fun getConnectedNetwork(): Network? = connectedNetwork
}
