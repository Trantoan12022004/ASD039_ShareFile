package com.example.basekotlin.util.transfer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.basekotlin.ui.transfer.model.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Quản lý tìm kiếm và phát hiện thiết bị trên mạng LAN (Wi-Fi)
 * Kết hợp 2 cơ chế:
 * 1. UDP Broadcast (Port 8889) để phản hồi siêu tốc (<100ms)
 * 2. Android NSD (Network Service Discovery / mDNS) làm kênh dự phòng chuẩn Android
 */
class LanDiscoveryHelper(private val context: Context) {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"
        private const val DISCOVERY_PORT = 8889
        private const val PREFIX_BEACON = "ASD_SHARE_DEVICE"
        private const val PING_QUERY = "ASD_PING_QUERY"
        private const val NSD_SERVICE_TYPE = "_asdtx._tcp"

        /**
         * Chuẩn hóa địa chỉ IP: loại bỏ dấu "/" và scope interface IPv6
         */
        fun normalizeIp(rawIp: String): String {
            return rawIp.trim().removePrefix("/").split("%")[0]
        }
    }

    private var broadcastJob: Job? = null
    private var discoveryJob: Job? = null
    private var pingJob: Job? = null
    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    // Tập hợp khóa chống trùng lặp thiết bị giữa UDP và NSD
    private val discoveredKeys = ConcurrentHashMap.newKeySet<String>()

    private val nsdManager: NsdManager? by lazy {
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null

    init {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("ASD_LAN_DISCOVERY")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "[LanDiscovery] Không thể acquire multicastLock", e)
        }
    }

    /**
     * PHÍA MÁY NHẬN: Phát beacon định kỳ qua UDP & đăng ký NSD
     */
    fun startBroadcasting(deviceName: String, ipAddress: String, port: Int) {
        stopBroadcasting()
        val cleanIp = normalizeIp(ipAddress)

        broadcastJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }

                val beaconMessage = "$PREFIX_BEACON|$deviceName|$cleanIp|$port"
                val sendData = beaconMessage.toByteArray(Charsets.UTF_8)
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val broadcastPacket = DatagramPacket(sendData, sendData.size, broadcastAddress, DISCOVERY_PORT)

                Log.d(TAG, "[LAN_DISCOVERY] Máy nhận bắt đầu phát beacon: $beaconMessage")

                val loopJob = launch {
                    while (isActive) {
                        try {
                            socket?.send(broadcastPacket)
                        } catch (e: Exception) {
                            Log.e(TAG, "[LAN_DISCOVERY] Lỗi gửi beacon UDP", e)
                        }
                        delay(1200)
                    }
                }

                val buffer = ByteArray(1024)
                while (isActive) {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    try {
                        socket?.receive(receivePacket)
                        val msg = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                        if (msg == PING_QUERY) {
                            val replyPacket = DatagramPacket(
                                sendData,
                                sendData.size,
                                receivePacket.address,
                                receivePacket.port
                            )
                            socket?.send(replyPacket)
                        }
                    } catch (_: Exception) {
                        if (!isActive) break
                    }
                }
                loopJob.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "[LAN_DISCOVERY] Lỗi socket phát beacon", e)
            }
        }

        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "ASD_${deviceName}"
                serviceType = NSD_SERVICE_TYPE
                this.port = port
            }
            nsdRegistrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(service: NsdServiceInfo?) {}
                override fun onRegistrationFailed(service: NsdServiceInfo?, errorCode: Int) {}
                override fun onServiceUnregistered(service: NsdServiceInfo?) {}
                override fun onUnregistrationFailed(service: NsdServiceInfo?, errorCode: Int) {}
            }
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "[LAN_DISCOVERY] Lỗi đăng ký NSD", e)
        }
    }

    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        try { nsdRegistrationListener?.let { nsdManager?.unregisterService(it) } } catch (_: Exception) {}
        nsdRegistrationListener = null
    }

    /**
     * PHÍA MÁY GỬI: Lắng nghe tìm kiếm thiết bị (Khử trùng lặp tuyệt đối)
     */
    fun startDiscovery(onDeviceFound: (DeviceInfo) -> Unit) {
        stopDiscovery()
        discoveredKeys.clear()

        // 1. Lắng nghe UDP
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(0))
                }

                val myIp = normalizeIp(NetworkUtils.getWifiIpAddress())

                pingJob = launch {
                    val pingBytes = PING_QUERY.toByteArray(Charsets.UTF_8)
                    val broadcastAddress = InetAddress.getByName("255.255.255.255")
                    val pingPacket = DatagramPacket(pingBytes, pingBytes.size, broadcastAddress, DISCOVERY_PORT)
                    while (isActive) {
                        try {
                            socket?.send(pingPacket)
                        } catch (_: Exception) {}
                        delay(1500)
                    }
                }

                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket?.receive(packet)
                        val message = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                        if (message.startsWith(PREFIX_BEACON)) {
                            val parts = message.split("|")
                            if (parts.size >= 4) {
                                val name = parts[1].trim()
                                val cleanIp = normalizeIp(parts[2])
                                val port = parts[3].toIntOrNull() ?: 8888

                                if (cleanIp != myIp && cleanIp.isNotEmpty() && !cleanIp.contains(":")) {
                                    val key = "${name.lowercase()}_${cleanIp}"
                                    if (discoveredKeys.add(key)) {
                                        Log.d(TAG, "[LAN_DISCOVERY] Tìm thấy thiết bị qua UDP: $name ($cleanIp:$port)")
                                        onDeviceFound(DeviceInfo(name = name, ipAddress = cleanIp, port = port))
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[LAN_DISCOVERY] Lỗi discovery socket", e)
            }
        }

        // 2. Lắng nghe NSD (mDNS)
        try {
            nsdDiscoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String?) {}
                override fun onDiscoveryStopped(serviceType: String?) {}
                override fun onServiceFound(service: NsdServiceInfo?) {
                    if (service?.serviceType == NSD_SERVICE_TYPE || service?.serviceType == "$NSD_SERVICE_TYPE.") {
                        try {
                            nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                                override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                                    val rawIp = serviceInfo?.host?.hostAddress ?: return
                                    val cleanIp = normalizeIp(rawIp)
                                    // Bỏ qua IPv6
                                    if (cleanIp.contains(":") || cleanIp.isEmpty()) return

                                    val port = serviceInfo.port
                                    val rawName = serviceInfo.serviceName ?: "Unknown Device"
                                    val name = if (rawName.startsWith("ASD_")) rawName.substring(4) else rawName
                                    val myIp = normalizeIp(NetworkUtils.getWifiIpAddress())

                                    if (cleanIp != myIp) {
                                        val key = "${name.lowercase()}_${cleanIp}"
                                        if (discoveredKeys.add(key)) {
                                            Log.d(TAG, "[LAN_DISCOVERY] Tìm thấy thiết bị qua NSD: $name ($cleanIp:$port)")
                                            onDeviceFound(DeviceInfo(name = name, ipAddress = cleanIp, port = port))
                                        }
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "[LAN_DISCOVERY] Lỗi resolveService", e)
                        }
                    }
                }
                override fun onServiceLost(service: NsdServiceInfo?) {}
            }
            nsdManager?.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "[LAN_DISCOVERY] Lỗi NSD discoverServices", e)
        }
    }

    fun stopDiscovery() {
        pingJob?.cancel()
        pingJob = null
        discoveryJob?.cancel()
        discoveryJob = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        try { nsdDiscoveryListener?.let { nsdManager?.stopServiceDiscovery(it) } } catch (_: Exception) {}
        nsdDiscoveryListener = null
        discoveredKeys.clear()
    }

    fun release() {
        stopBroadcasting()
        stopDiscovery()
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }
}
