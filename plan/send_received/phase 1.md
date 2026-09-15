# PHASE 1: Core Infrastructure — Hướng Dẫn Code Chi Tiết

> **Mục tiêu**: Xây dựng toàn bộ nền tảng kết nối WiFi Hotspot và truyền file TCP Socket giữa 2 thiết bị Android.
> 
> **Kết quả Phase 1**: 2 thiết bị có thể kết nối qua Hotspot → truyền file thành công qua TCP Socket.

---

## Bước 1.1: Setup Project

### 1.1.1 Thêm Permissions vào AndroidManifest.xml

Thêm sau block permission hiện có (dòng 39 trong `AndroidManifest.xml`):

```xml
<!-- ===== TRANSFER FEATURE: WiFi & Hotspot ===== -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

<!-- Location (bắt buộc cho WiFi scan) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Nearby devices (Android 12+) -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="s" />

<!-- Camera cho QR Scanner -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Foreground Service cho transfer -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
```

Thêm khai báo `TransferService` trong block `<application>` (sau `MusicService`):

```xml
<service
    android:name=".service.transfer.TransferService"
    android:exported="false"
    android:foregroundServiceType="connectedDevice" />
```

### 1.1.2 Thêm Dependencies vào build.gradle (app)

Thêm vào block `dependencies`:

```groovy
// ===== TRANSFER FEATURE =====
// QR Code generate
implementation 'com.google.zxing:core:3.5.3'

// CameraX (cho QR scanner)
implementation 'androidx.camera:camera-camera2:1.5.0'
implementation 'androidx.camera:camera-lifecycle:1.5.0'
implementation 'androidx.camera:camera-view:1.5.0'

// ML Kit Barcode scanning
implementation 'com.google.mlkit:barcode-scanning:17.3.0'

// Lottie animation (radar, loading)
implementation 'com.airbnb.android:lottie:6.4.0'
```

### 1.1.3 Tạo Package Structure

Tạo các thư mục sau:

```
app/src/main/java/com/example/basekotlin/
├── service/transfer/          # TransferService, FileServer, FileSender, HotspotManager
├── ui/transfer/
│   ├── model/                 # Data models
│   ├── send/                  # Send flow
│   │   ├── adapter/
│   │   └── fragment/
│   ├── receive/               # Receive flow
│   ├── scanner/               # QR scanner
│   ├── progress/              # Transfer progress
│   ├── complete/              # Transfer complete
│   ├── history/               # Transfer history
│   │   └── adapter/
│   ├── received/              # Received files manager
│   └── dialog/                # Dialogs
├── data/local/transfer/       # Room DB
│   ├── dao/
│   └── entity/
└── util/transfer/             # Utilities
```

### 1.1.4 Thêm String Resources

Thêm vào `res/values/strings.xml`:

```xml
<!-- ===== TRANSFER FEATURE ===== -->
<!-- Chung -->
<string name="transfer_send">Send</string>
<string name="transfer_receive">Receive</string>
<string name="transfer_cancel">Cancel</string>
<string name="transfer_retry">Retry</string>
<string name="transfer_done">Done</string>

<!-- Send Files -->
<string name="send_files_title">Send Files</string>
<string name="send_tab_recent">Recent</string>
<string name="send_tab_installed">Installed</string>
<string name="send_tab_file">File</string>
<string name="send_tab_photo">Photo</string>
<string name="send_tab_video">Video</string>
<string name="send_tab_music">Music</string>
<string name="send_files_selected">%d files selected</string>
<string name="send_no_files">No files found</string>

<!-- QR Scanner -->
<string name="qr_scanner_title">Scan QR Code</string>
<string name="qr_scanner_hint">Point camera at QR code from receiver</string>
<string name="qr_scan_success">QR code scanned successfully</string>
<string name="qr_scan_invalid">Invalid QR code</string>

<!-- Receive -->
<string name="receive_title">Receive</string>
<string name="receive_scan_hint">Scan code to send files</string>
<string name="receive_ready">Ready to Receive</string>
<string name="receive_description">Other devices can find you in the same WiFi</string>
<string name="receive_device_name">Device\'s Name</string>
<string name="receive_wifi_network">WiFi Network</string>
<string name="receive_ip_address">IP Address</string>

<!-- Connection -->
<string name="connection_title">Connecting</string>
<string name="connection_searching">Searching for device…</string>
<string name="connection_connecting">Connecting to %s…</string>
<string name="connection_connected">Connected to %s</string>
<string name="connection_failed">Connection Failed</string>
<string name="connection_failed_message">Could not connect to the device. Please try again.</string>
<string name="connection_speed">%s/s</string>

<!-- Transfer Progress -->
<string name="transfer_progress_title">Transferring</string>
<string name="transfer_progress_to">To: %s</string>
<string name="transfer_progress_from">From: %s</string>
<string name="transfer_progress_files">%1$d/%2$d files</string>
<string name="transfer_progress_size">%s transferred</string>
<string name="transfer_cancel_confirm">Cancel transfer?</string>
<string name="transfer_cancel_message">The current transfer will be stopped. Files already transferred will be kept.</string>

<!-- Transfer Complete -->
<string name="transfer_complete_title">Transfer Complete!</string>
<string name="transfer_complete_files_sent">%d files sent</string>
<string name="transfer_complete_files_received">%d files received</string>
<string name="transfer_complete_total">Total: %s</string>
<string name="transfer_complete_time">Time: %s</string>
<string name="transfer_complete_speed">Avg Speed: %s/s</string>
<string name="transfer_complete_view_files">View Files</string>
<string name="transfer_complete_send_more">Send More</string>

<!-- Incoming Transfer Dialog -->
<string name="incoming_transfer_title">Incoming Transfer</string>
<string name="incoming_transfer_from">From: %s</string>
<string name="incoming_transfer_info">%1$d files (%2$s)</string>
<string name="incoming_transfer_accept">Accept</string>
<string name="incoming_transfer_reject">Reject</string>

<!-- Notification -->
<string name="transfer_notification_channel">File Transfer</string>
<string name="transfer_notification_sending">Sending files…</string>
<string name="transfer_notification_receiving">Receiving files…</string>
<string name="transfer_notification_complete">Transfer complete</string>
<string name="transfer_notification_failed">Transfer failed</string>

<!-- Hotspot -->
<string name="hotspot_starting">Starting hotspot…</string>
<string name="hotspot_active">Hotspot active</string>
<string name="hotspot_failed">Failed to start hotspot</string>
<string name="hotspot_ssid">SSID: %s</string>
<string name="hotspot_password">Password: %s</string>

<!-- History -->
<string name="history_title">Transfer History</string>
<string name="history_tab_sent">Sent</string>
<string name="history_tab_received">Received</string>
<string name="history_tab_all">All</string>
<string name="history_empty">No transfer history</string>
<string name="history_today">Today</string>
<string name="history_yesterday">Yesterday</string>

<!-- Errors -->
<string name="error_wifi_not_available">WiFi is not available</string>
<string name="error_hotspot_failed">Failed to create hotspot</string>
<string name="error_connection_timeout">Connection timed out</string>
<string name="error_transfer_failed">File transfer failed</string>
<string name="error_storage_full">Not enough storage space</string>
<string name="error_permission_required">Permission required</string>
```

---

## Bước 1.2: Data Models

### File: `ui/transfer/model/ConnectionInfo.kt`

```kotlin
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
```

### File: `ui/transfer/model/DeviceInfo.kt`

```kotlin
package com.example.basekotlin.ui.transfer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Thông tin thiết bị (hiển thị trong UI device discovery, history...)
 */
@Parcelize
data class DeviceInfo(
    val name: String,
    val ipAddress: String,
    val port: Int = 0
) : Parcelable
```

### File: `ui/transfer/model/TransferFile.kt`

```kotlin
package com.example.basekotlin.ui.transfer.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Đại diện cho 1 file trong quá trình truyền
 * Dùng chung cho cả sender và receiver
 */
@Parcelize
data class TransferFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val status: TransferStatus = TransferStatus.PENDING
) : Parcelable

/**
 * Trạng thái truyền của từng file
 */
enum class TransferStatus {
    PENDING,       // chưa bắt đầu
    TRANSFERRING,  // đang truyền
    COMPLETED,     // truyền xong
    FAILED         // truyền lỗi
}
```

### File: `ui/transfer/model/TransferState.kt`

```kotlin
package com.example.basekotlin.ui.transfer.model

/**
 * Trạng thái tổng thể của phiên truyền file
 * TransferService expose StateFlow<TransferState> để UI observe
 */
sealed class TransferState {
    /** Chưa bắt đầu */
    data object Idle : TransferState()

    /** Đang kết nối đến thiết bị */
    data object Connecting : TransferState()

    /** Đã kết nối thành công */
    data class Connected(val device: DeviceInfo) : TransferState()

    /** Đang truyền file */
    data class Transferring(
        val currentFile: TransferFile,
        val currentIndex: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long
    ) : TransferState()

    /** Truyền xong */
    data class Complete(val summary: TransferSummary) : TransferState()

    /** Lỗi */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : TransferState()
}

/**
 * Tổng kết phiên truyền
 */
data class TransferSummary(
    val fileCount: Int,
    val totalBytes: Long,
    val durationMs: Long,
    val avgSpeedBytesPerSec: Long,
    val deviceName: String,
    val direction: TransferDirection
)

enum class TransferDirection {
    SENT,
    RECEIVED
}
```

### File: `ui/transfer/model/FileProgress.kt`

```kotlin
package com.example.basekotlin.ui.transfer.model

/**
 * Progress chi tiết cho 1 file đang truyền
 * UI dùng để update progress bar cho từng item trong RecyclerView
 */
data class FileProgress(
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
) {
    /** Phần trăm hoàn thành (0-100) */
    val percent: Int
        get() = if (totalBytes > 0) ((bytesTransferred * 100) / totalBytes).toInt() else 0
}
```

---

## Bước 1.3: NetworkUtils

### File: `util/transfer/NetworkUtils.kt`

```kotlin
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

    /**
     * Lấy địa chỉ IPv4 local của thiết bị
     * Ưu tiên interface wlan0 (WiFi)
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // Ưu tiên wlan (WiFi interface)
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }

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
     * Kiểm tra WiFi có đang kết nối không
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Lấy tên WiFi đang kết nối
     */
    fun getConnectedWifiName(context: Context): String {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid
        // SSID trả về có dấu ngoặc kép bao quanh
        return ssid?.replace("\"", "") ?: "Unknown"
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
```

---

## Bước 1.4: QrCodeHelper

### File: `util/transfer/QrCodeHelper.kt`

```kotlin
package com.example.basekotlin.util.transfer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Helper tạo và parse QR code cho kết nối transfer
 *
 * QR code chứa JSON: {"ssid":"xxx","password":"xxx","ip":"192.168.x.x","port":8888,"deviceName":"Phone"}
 * Receiver generate QR → Sender scan → parse → kết nối
 */
object QrCodeHelper {

    /**
     * Tạo QR code bitmap từ ConnectionInfo
     *
     * @param info thông tin kết nối cần encode
     * @param size kích thước bitmap (pixel), mặc định 512
     * @return Bitmap chứa QR code
     */
    fun generateQrBitmap(info: ConnectionInfo, size: Int = 512): Bitmap {
        val content = info.toJson()

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1 // margin nhỏ để QR to hơn
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Parse nội dung QR code thành ConnectionInfo
     *
     * @param qrContent chuỗi JSON từ QR scan
     * @return ConnectionInfo nếu parse thành công, null nếu không hợp lệ
     */
    fun parseQrContent(qrContent: String): ConnectionInfo? {
        return ConnectionInfo.fromJson(qrContent)
    }
}
```

---

## Bước 1.5: HotspotManager

### File: `service/transfer/HotspotManager.kt`

```kotlin
package com.example.basekotlin.service.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        private const val TAG = "HotspotManager"
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
            Log.w(TAG, "Hotspot đang chạy rồi")
            return
        }

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {

                override fun onStarted(reservation: LocalOnlyHotspotReservation?) {
                    hotspotReservation = reservation
                    isRunning = true

                    val config = reservation?.wifiConfiguration
                        ?: reservation?.softApConfiguration

                    // Lấy SSID và password từ config
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

                    val ip = NetworkUtils.getLocalIpAddress()
                    val deviceName = NetworkUtils.getDeviceName()

                    val connectionInfo = ConnectionInfo(
                        ssid = ssid,
                        password = password,
                        ipAddress = ip,
                        port = port,
                        deviceName = deviceName
                    )

                    Log.d(TAG, "Hotspot bật OK: SSID=$ssid, IP=$ip, Port=$port")
                    listener.onHotspotStarted(connectionInfo)
                }

                override fun onStopped() {
                    isRunning = false
                    hotspotReservation = null
                    Log.d(TAG, "Hotspot đã tắt")
                    listener.onHotspotStopped()
                }

                override fun onFailed(reason: Int) {
                    isRunning = false
                    Log.e(TAG, "Hotspot bật thất bại, reason=$reason")
                    listener.onHotspotFailed(reason)
                }

            }, Handler(Looper.getMainLooper()))

        } catch (e: SecurityException) {
            Log.e(TAG, "Thiếu permission Location", e)
            listener.onHotspotFailed(-1)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi không xác định khi bật hotspot", e)
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
            Log.e(TAG, "Lỗi khi tắt hotspot", e)
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
```

---

## Bước 1.6: WifiHelper

### File: `util/transfer/WifiHelper.kt`

```kotlin
package com.example.basekotlin.util.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.util.Log

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
        private const val TAG = "WifiHelper"
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectedNetwork: Network? = null

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
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Hủy callback cũ nếu có
        disconnectWifi()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectedNetwork = network
                // Bind process vào network này để socket dùng đúng interface
                connectivityManager.bindProcessToNetwork(network)
                Log.d(TAG, "Đã kết nối WiFi: $ssid")
                listener.onConnected(network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                connectedNetwork = null
                connectivityManager.bindProcessToNetwork(null)
                Log.d(TAG, "Mất kết nối WiFi: $ssid")
                listener.onDisconnected()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.e(TAG, "Không thể kết nối WiFi: $ssid")
                listener.onFailed()
            }
        }

        try {
            connectivityManager.requestNetwork(request, networkCallback!!)
            Log.d(TAG, "Đang yêu cầu kết nối WiFi: $ssid")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi request network", e)
            listener.onFailed()
        }
    }

    /**
     * Ngắt kết nối WiFi và giải phóng tài nguyên
     */
    fun disconnectWifi() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi unregister network callback", e)
            }
        }
        connectivityManager.bindProcessToNetwork(null)
        networkCallback = null
        connectedNetwork = null
    }

    /**
     * Lấy network object đã kết nối (dùng để bind socket)
     */
    fun getConnectedNetwork(): Network? = connectedNetwork
}
```

---

## Bước 1.7: FileTransferProtocol

### File: `util/transfer/FileTransferProtocol.kt`

```kotlin
package com.example.basekotlin.util.transfer

import com.google.gson.Gson
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Định nghĩa protocol truyền file qua TCP Socket
 *
 * Format message: [TYPE:4bytes][LENGTH:4bytes][PAYLOAD:variable]
 * - TYPE: loại message (HANDSHAKE, FILE_META, FILE_DATA, ACK...)
 * - LENGTH: độ dài payload (bytes)
 * - PAYLOAD: nội dung JSON hoặc binary data
 */
object FileTransferProtocol {

    // Kích thước buffer cho chunked transfer (64KB cho tốc độ tối ưu)
    const val BUFFER_SIZE = 64 * 1024

    // Timeout kết nối (giây)
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 30_000

    // Các loại message
    const val MSG_HANDSHAKE = 1         // Sender → Receiver: thông tin phiên
    const val MSG_ACK_HANDSHAKE = 2     // Receiver → Sender: chấp nhận
    const val MSG_REJECT = 3            // Receiver → Sender: từ chối
    const val MSG_FILE_META = 4         // Sender → Receiver: metadata file
    const val MSG_ACK_META = 5          // Receiver → Sender: sẵn sàng nhận file
    const val MSG_FILE_DATA = 6         // Sender → Receiver: dữ liệu file (chunked)
    const val MSG_ACK_FILE = 7          // Receiver → Sender: nhận xong 1 file
    const val MSG_TRANSFER_COMPLETE = 8 // Sender → Receiver: truyền xong tất cả
    const val MSG_ACK_COMPLETE = 9      // Receiver → Sender: xác nhận xong
    const val MSG_CANCEL = 10           // Cả 2 chiều: hủy transfer
    const val MSG_ERROR = 11            // Cả 2 chiều: báo lỗi

    /**
     * Thông tin handshake ban đầu
     */
    data class HandshakeData(
        val deviceName: String,
        val fileCount: Int,
        val totalSize: Long
    )

    /**
     * Metadata của 1 file trước khi gửi data
     */
    data class FileMetaData(
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val index: Int  // thứ tự file (0-based)
    )

    /**
     * Xác nhận đã nhận xong 1 file
     */
    data class FileAck(
        val fileName: String,
        val receivedSize: Long,
        val success: Boolean
    )

    // ===== SEND/RECEIVE METHODS =====

    /**
     * Gửi 1 message qua DataOutputStream
     * Format: [type:Int][length:Int][payload:ByteArray]
     */
    fun sendMessage(output: DataOutputStream, type: Int, payload: String) {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        output.writeInt(type)
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    /**
     * Đọc 1 message từ DataInputStream
     * @return Pair(type, payload_string)
     */
    fun readMessage(input: DataInputStream): Pair<Int, String> {
        val type = input.readInt()
        val length = input.readInt()
        val buffer = ByteArray(length)
        input.readFully(buffer)
        return Pair(type, String(buffer, Charsets.UTF_8))
    }

    /**
     * Gửi handshake (sender → receiver)
     */
    fun sendHandshake(output: DataOutputStream, data: HandshakeData) {
        sendMessage(output, MSG_HANDSHAKE, Gson().toJson(data))
    }

    /**
     * Gửi file metadata (sender → receiver, trước mỗi file)
     */
    fun sendFileMeta(output: DataOutputStream, meta: FileMetaData) {
        sendMessage(output, MSG_FILE_META, Gson().toJson(meta))
    }

    /**
     * Gửi ACK (dùng chung cho nhiều loại ACK)
     */
    fun sendAck(output: DataOutputStream, type: Int) {
        sendMessage(output, type, "{}")
    }

    /**
     * Gửi file ACK (receiver → sender, sau khi nhận xong 1 file)
     */
    fun sendFileAck(output: DataOutputStream, ack: FileAck) {
        sendMessage(output, MSG_ACK_FILE, Gson().toJson(ack))
    }

    /**
     * Parse HandshakeData từ JSON
     */
    fun parseHandshake(json: String): HandshakeData {
        return Gson().fromJson(json, HandshakeData::class.java)
    }

    /**
     * Parse FileMetaData từ JSON
     */
    fun parseFileMeta(json: String): FileMetaData {
        return Gson().fromJson(json, FileMetaData::class.java)
    }

    /**
     * Parse FileAck từ JSON
     */
    fun parseFileAck(json: String): FileAck {
        return Gson().fromJson(json, FileAck::class.java)
    }
}
```

---

## Bước 1.8: FileServerService (TCP Server — Receiver Side)

### File: `service/transfer/FileServerService.kt`

```kotlin
package com.example.basekotlin.service.transfer

import android.os.Environment
import android.util.Log
import com.example.basekotlin.ui.transfer.model.FileProgress
import com.example.basekotlin.util.transfer.FileTransferProtocol
import com.example.basekotlin.util.transfer.FileTransferProtocol.BUFFER_SIZE
import com.example.basekotlin.util.transfer.FileTransferProtocol.CONNECT_TIMEOUT_MS
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * TCP Server chạy trên RECEIVER
 *
 * Flow:
 * 1. startServer(port) → ServerSocket lắng nghe
 * 2. acceptConnection() → chờ sender kết nối
 * 3. receiveHandshake() → nhận thông tin phiên (device name, file count)
 * 4. Loop: receiveFileMeta() → receiveFileData() → sendAck()
 * 5. Nhận TRANSFER_COMPLETE → kết thúc
 *
 * Tất cả methods chạy trên Coroutine (Dispatchers.IO)
 */
class FileServerService {

    companion object {
        private const val TAG = "FileServerService"
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var dataInput: DataInputStream? = null
    private var dataOutput: DataOutputStream? = null

    // Thư mục lưu file nhận được
    private val receiveDir: File by lazy {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ShareFile"
        )
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * Callback cho các sự kiện server
     */
    interface ServerListener {
        fun onClientConnected(handshake: FileTransferProtocol.HandshakeData)
        fun onFileReceiveStarted(meta: FileTransferProtocol.FileMetaData)
        fun onFileProgress(progress: FileProgress)
        fun onFileReceived(meta: FileTransferProtocol.FileMetaData, savedPath: String)
        fun onTransferComplete()
        fun onError(message: String)
    }

    /**
     * Khởi tạo server socket
     */
    fun startServer(port: Int) {
        serverSocket = ServerSocket(port).apply {
            soTimeout = 0 // chờ vô hạn cho client kết nối
            reuseAddress = true
        }
        Log.d(TAG, "Server bắt đầu lắng nghe trên port $port")
    }

    /**
     * Chờ client kết nối (blocking)
     * Gọi trên Dispatchers.IO
     */
    fun acceptConnection(): Socket {
        val socket = serverSocket?.accept()
            ?: throw IllegalStateException("ServerSocket chưa khởi tạo")

        socket.soTimeout = FileTransferProtocol.READ_TIMEOUT_MS

        clientSocket = socket
        dataInput = DataInputStream(socket.getInputStream().buffered())
        dataOutput = DataOutputStream(socket.getOutputStream().buffered())

        Log.d(TAG, "Client đã kết nối: ${socket.inetAddress.hostAddress}")
        return socket
    }

    /**
     * Nhận toàn bộ phiên transfer
     * Gọi sau acceptConnection(), chạy trên Dispatchers.IO
     */
    fun receiveSession(listener: ServerListener) {
        val input = dataInput ?: throw IllegalStateException("Chưa có kết nối")
        val output = dataOutput ?: throw IllegalStateException("Chưa có kết nối")

        try {
            // Bước 1: Nhận handshake
            val (type, payload) = FileTransferProtocol.readMessage(input)
            if (type != FileTransferProtocol.MSG_HANDSHAKE) {
                listener.onError("Không nhận được handshake")
                return
            }
            val handshake = FileTransferProtocol.parseHandshake(payload)
            listener.onClientConnected(handshake)

            // Gửi ACK handshake (chấp nhận)
            FileTransferProtocol.sendAck(output, FileTransferProtocol.MSG_ACK_HANDSHAKE)

            // Bước 2: Nhận từng file
            var fileIndex = 0
            while (fileIndex < handshake.fileCount) {
                // Đọc metadata file
                val (metaType, metaPayload) = FileTransferProtocol.readMessage(input)
                when (metaType) {
                    FileTransferProtocol.MSG_FILE_META -> {
                        val meta = FileTransferProtocol.parseFileMeta(metaPayload)
                        listener.onFileReceiveStarted(meta)

                        // Gửi ACK meta (sẵn sàng nhận)
                        FileTransferProtocol.sendAck(output, FileTransferProtocol.MSG_ACK_META)

                        // Nhận file data
                        val savedPath = receiveFileData(input, meta, listener)

                        // Gửi file ACK
                        val ack = FileTransferProtocol.FileAck(
                            fileName = meta.fileName,
                            receivedSize = meta.fileSize,
                            success = true
                        )
                        FileTransferProtocol.sendFileAck(output, ack)

                        listener.onFileReceived(meta, savedPath)
                        fileIndex++
                    }

                    FileTransferProtocol.MSG_CANCEL -> {
                        listener.onError("Sender đã hủy transfer")
                        return
                    }

                    else -> {
                        listener.onError("Message không mong đợi: type=$metaType")
                        return
                    }
                }
            }

            // Bước 3: Nhận TRANSFER_COMPLETE
            val (completeType, _) = FileTransferProtocol.readMessage(input)
            if (completeType == FileTransferProtocol.MSG_TRANSFER_COMPLETE) {
                FileTransferProtocol.sendAck(output, FileTransferProtocol.MSG_ACK_COMPLETE)
                listener.onTransferComplete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi nhận file", e)
            listener.onError("Lỗi: ${e.message}")
        }
    }

    /**
     * Nhận dữ liệu 1 file (chunked)
     * Đọc trực tiếp bytes theo kích thước đã biết từ metadata
     *
     * @return đường dẫn file đã lưu
     */
    private fun receiveFileData(
        input: DataInputStream,
        meta: FileTransferProtocol.FileMetaData,
        listener: ServerListener
    ): String {
        // Tạo file đích, xử lý trùng tên
        val targetFile = getUniqueFile(meta.fileName)
        val buffer = ByteArray(BUFFER_SIZE)
        var totalReceived = 0L
        var lastProgressTime = System.currentTimeMillis()
        var bytesInLastInterval = 0L

        BufferedOutputStream(FileOutputStream(targetFile)).use { fileOutput ->
            while (totalReceived < meta.fileSize) {
                val remaining = meta.fileSize - totalReceived
                val toRead = minOf(remaining, BUFFER_SIZE.toLong()).toInt()
                val bytesRead = input.read(buffer, 0, toRead)

                if (bytesRead == -1) break

                fileOutput.write(buffer, 0, bytesRead)
                totalReceived += bytesRead
                bytesInLastInterval += bytesRead

                // Cập nhật progress mỗi 200ms để tránh spam
                val now = System.currentTimeMillis()
                if (now - lastProgressTime >= 200) {
                    val elapsedSec = (now - lastProgressTime) / 1000.0
                    val speed = if (elapsedSec > 0) {
                        (bytesInLastInterval / elapsedSec).toLong()
                    } else 0L

                    listener.onFileProgress(
                        FileProgress(
                            fileName = meta.fileName,
                            bytesTransferred = totalReceived,
                            totalBytes = meta.fileSize,
                            speedBytesPerSec = speed
                        )
                    )

                    lastProgressTime = now
                    bytesInLastInterval = 0
                }
            }
            fileOutput.flush()
        }

        Log.d(TAG, "Nhận xong: ${meta.fileName} (${totalReceived} bytes) → ${targetFile.absolutePath}")
        return targetFile.absolutePath
    }

    /**
     * Tạo file không trùng tên trong thư mục nhận
     * Nếu file.jpg đã tồn tại → file(1).jpg → file(2).jpg...
     */
    private fun getUniqueFile(fileName: String): File {
        var file = File(receiveDir, fileName)
        if (!file.exists()) return file

        val nameWithoutExt = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        var counter = 1

        while (file.exists()) {
            val newName = if (ext.isNotEmpty()) {
                "${nameWithoutExt}($counter).$ext"
            } else {
                "${nameWithoutExt}($counter)"
            }
            file = File(receiveDir, newName)
            counter++
        }
        return file
    }

    /**
     * Dừng server và giải phóng tài nguyên
     */
    fun stopServer() {
        try {
            dataInput?.close()
            dataOutput?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dừng server", e)
        } finally {
            dataInput = null
            dataOutput = null
            clientSocket = null
            serverSocket = null
        }
        Log.d(TAG, "Server đã dừng")
    }
}
```

---

## Bước 1.9: FileSenderService (TCP Client — Sender Side)

### File: `service/transfer/FileSenderService.kt`

```kotlin
package com.example.basekotlin.service.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.basekotlin.ui.transfer.model.FileProgress
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.util.transfer.FileTransferProtocol
import com.example.basekotlin.util.transfer.FileTransferProtocol.BUFFER_SIZE
import com.example.basekotlin.util.transfer.NetworkUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * TCP Client chạy trên SENDER
 *
 * Flow:
 * 1. connect(ip, port) → kết nối đến server receiver
 * 2. sendHandshake() → gửi thông tin phiên
 * 3. Loop: sendFileMeta() → sendFileData() → đợi ACK
 * 4. sendTransferComplete()
 *
 * Tất cả methods chạy trên Coroutine (Dispatchers.IO)
 */
class FileSenderService(private val context: Context) {

    companion object {
        private const val TAG = "FileSenderService"
    }

    private var socket: Socket? = null
    private var dataInput: DataInputStream? = null
    private var dataOutput: DataOutputStream? = null

    /**
     * Callback cho các sự kiện sender
     */
    interface SenderListener {
        fun onConnected()
        fun onHandshakeAccepted()
        fun onHandshakeRejected()
        fun onFileSendStarted(file: TransferFile, index: Int)
        fun onFileProgress(progress: FileProgress)
        fun onFileSent(file: TransferFile, index: Int)
        fun onTransferComplete()
        fun onError(message: String)
    }

    /**
     * Kết nối đến server (receiver)
     */
    fun connect(ip: String, port: Int) {
        socket = Socket().apply {
            soTimeout = FileTransferProtocol.READ_TIMEOUT_MS
            connect(
                InetSocketAddress(ip, port),
                FileTransferProtocol.CONNECT_TIMEOUT_MS
            )
        }
        dataInput = DataInputStream(socket!!.getInputStream().buffered())
        dataOutput = DataOutputStream(socket!!.getOutputStream().buffered())
        Log.d(TAG, "Đã kết nối đến server: $ip:$port")
    }

    /**
     * Gửi toàn bộ danh sách file
     */
    fun sendAllFiles(files: List<TransferFile>, listener: SenderListener) {
        val output = dataOutput ?: throw IllegalStateException("Chưa kết nối")
        val input = dataInput ?: throw IllegalStateException("Chưa kết nối")

        try {
            listener.onConnected()

            // Bước 1: Gửi handshake
            val totalSize = files.sumOf { it.size }
            val handshake = FileTransferProtocol.HandshakeData(
                deviceName = NetworkUtils.getDeviceName(),
                fileCount = files.size,
                totalSize = totalSize
            )
            FileTransferProtocol.sendHandshake(output, handshake)

            // Đợi ACK handshake
            val (ackType, _) = FileTransferProtocol.readMessage(input)
            when (ackType) {
                FileTransferProtocol.MSG_ACK_HANDSHAKE -> {
                    listener.onHandshakeAccepted()
                }
                FileTransferProtocol.MSG_REJECT -> {
                    listener.onHandshakeRejected()
                    return
                }
                else -> {
                    listener.onError("Phản hồi handshake không hợp lệ")
                    return
                }
            }

            // Bước 2: Gửi từng file
            files.forEachIndexed { index, file ->
                listener.onFileSendStarted(file, index)
                sendSingleFile(file, index, listener)
                listener.onFileSent(file, index)
            }

            // Bước 3: Gửi TRANSFER_COMPLETE
            FileTransferProtocol.sendMessage(
                output,
                FileTransferProtocol.MSG_TRANSFER_COMPLETE,
                "{}"
            )

            // Đợi ACK complete
            val (completeAckType, _) = FileTransferProtocol.readMessage(input)
            if (completeAckType == FileTransferProtocol.MSG_ACK_COMPLETE) {
                listener.onTransferComplete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi file", e)
            listener.onError("Lỗi: ${e.message}")
        }
    }

    /**
     * Gửi 1 file: metadata → data stream → đợi ACK
     */
    private fun sendSingleFile(
        file: TransferFile,
        index: Int,
        listener: SenderListener
    ) {
        val output = dataOutput!!
        val input = dataInput!!

        // Gửi metadata
        val meta = FileTransferProtocol.FileMetaData(
            fileName = file.name,
            fileSize = file.size,
            mimeType = file.mimeType,
            index = index
        )
        FileTransferProtocol.sendFileMeta(output, meta)

        // Đợi ACK meta
        val (ackMetaType, _) = FileTransferProtocol.readMessage(input)
        if (ackMetaType != FileTransferProtocol.MSG_ACK_META) {
            throw Exception("Receiver từ chối file: ${file.name}")
        }

        // Gửi file data (chunked)
        val buffer = ByteArray(BUFFER_SIZE)
        var totalSent = 0L
        var lastProgressTime = System.currentTimeMillis()
        var bytesInLastInterval = 0L

        context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
            while (totalSent < file.size) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                output.write(buffer, 0, bytesRead)
                totalSent += bytesRead
                bytesInLastInterval += bytesRead

                // Cập nhật progress mỗi 200ms
                val now = System.currentTimeMillis()
                if (now - lastProgressTime >= 200) {
                    output.flush()

                    val elapsedSec = (now - lastProgressTime) / 1000.0
                    val speed = if (elapsedSec > 0) {
                        (bytesInLastInterval / elapsedSec).toLong()
                    } else 0L

                    listener.onFileProgress(
                        FileProgress(
                            fileName = file.name,
                            bytesTransferred = totalSent,
                            totalBytes = file.size,
                            speedBytesPerSec = speed
                        )
                    )

                    lastProgressTime = now
                    bytesInLastInterval = 0
                }
            }
            output.flush()
        } ?: throw Exception("Không mở được file: ${file.uri}")

        // Đợi file ACK từ receiver
        val (ackFileType, ackPayload) = FileTransferProtocol.readMessage(input)
        if (ackFileType == FileTransferProtocol.MSG_ACK_FILE) {
            val ack = FileTransferProtocol.parseFileAck(ackPayload)
            if (!ack.success) {
                throw Exception("Receiver báo lỗi nhận file: ${file.name}")
            }
        }

        Log.d(TAG, "Gửi xong: ${file.name} ($totalSent bytes)")
    }

    /**
     * Hủy transfer
     */
    fun cancelTransfer() {
        try {
            dataOutput?.let {
                FileTransferProtocol.sendMessage(it, FileTransferProtocol.MSG_CANCEL, "{}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi cancel", e)
        }
    }

    /**
     * Ngắt kết nối và giải phóng tài nguyên
     */
    fun disconnect() {
        try {
            dataInput?.close()
            dataOutput?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ngắt kết nối", e)
        } finally {
            dataInput = null
            dataOutput = null
            socket = null
        }
        Log.d(TAG, "Đã ngắt kết nối")
    }
}
```

---

## Bước 1.10: TransferNotificationHelper

### File: `util/transfer/TransferNotificationHelper.kt`

```kotlin
package com.example.basekotlin.util.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.basekotlin.R
import com.example.basekotlin.ui.main.MainActivity

/**
 * Quản lý notification cho quá trình truyền file
 * - Tạo notification channel
 * - Hiển thị notification với progress
 * - Cập nhật progress realtime
 * - Hiển thị kết quả (thành công/thất bại)
 */
object TransferNotificationHelper {

    private const val CHANNEL_ID = "file_transfer_channel"
    const val NOTIFICATION_ID = 2001

    /**
     * Tạo notification channel (gọi 1 lần khi app khởi tạo)
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.transfer_notification_channel),
            NotificationManager.IMPORTANCE_LOW // LOW để không phát âm thanh
        ).apply {
            description = "File transfer progress"
            setShowBadge(false)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Tạo notification builder cho transfer đang chạy
     * Dùng cho startForeground()
     */
    fun createTransferNotification(
        context: Context,
        title: String,
        text: String,
        progress: Int = 0,
        maxProgress: Int = 100,
        indeterminate: Boolean = false
    ): NotificationCompat.Builder {
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(maxProgress, progress, indeterminate)
            .setOngoing(true)       // không thể vuốt dismiss
            .setAutoCancel(false)
            .setOnlyAlertOnce(true) // không phát sound mỗi lần update
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    /**
     * Cập nhật notification với progress mới
     */
    fun updateProgress(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        maxProgress: Int = 100
    ) {
        val notification = createTransferNotification(
            context, title, text, progress, maxProgress
        ).build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Hiển thị notification hoàn thành
     */
    fun showCompleteNotification(context: Context, isSender: Boolean, fileCount: Int) {
        val text = if (isSender) {
            context.getString(R.string.transfer_complete_files_sent, fileCount)
        } else {
            context.getString(R.string.transfer_complete_files_received, fileCount)
        }

        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(context.getString(R.string.transfer_complete_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Xóa notification
     */
    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
```

---

## Bước 1.11: TransferService (Foreground Service Chính)

### File: `service/transfer/TransferService.kt`

```kotlin
package com.example.basekotlin.service.transfer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.basekotlin.R
import com.example.basekotlin.ui.transfer.model.DeviceInfo
import com.example.basekotlin.ui.transfer.model.FileProgress
import com.example.basekotlin.ui.transfer.model.TransferDirection
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.model.TransferState
import com.example.basekotlin.ui.transfer.model.TransferSummary
import com.example.basekotlin.util.transfer.FileTransferProtocol
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.TransferNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service quản lý toàn bộ quá trình truyền file
 *
 * Chức năng:
 * - Duy trì kết nối khi app ở background
 * - Hiển thị notification với progress
 * - Broadcast trạng thái qua StateFlow cho UI observe
 * - Hỗ trợ cả Send và Receive mode
 *
 * UI bind tới service qua TransferBinder để lấy StateFlow
 */
class TransferService : Service() {

    companion object {
        private const val TAG = "TransferService"
        private const val ACTION_SEND = "ACTION_SEND"
        private const val ACTION_RECEIVE = "ACTION_RECEIVE"
        private const val ACTION_CANCEL = "ACTION_CANCEL"
        private const val EXTRA_IP = "EXTRA_IP"
        private const val EXTRA_PORT = "EXTRA_PORT"
        private const val EXTRA_FILES = "EXTRA_FILES"

        /**
         * Start service ở chế độ SEND
         */
        fun startSend(
            context: Context,
            ip: String,
            port: Int,
            files: ArrayList<TransferFile>
        ) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_SEND
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_PORT, port)
                putParcelableArrayListExtra(EXTRA_FILES, files)
            }
            context.startForegroundService(intent)
        }

        /**
         * Start service ở chế độ RECEIVE
         */
        fun startReceive(context: Context, port: Int) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_RECEIVE
                putExtra(EXTRA_PORT, port)
            }
            context.startForegroundService(intent)
        }

        /**
         * Hủy transfer đang chạy
         */
        fun cancel(context: Context) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    // Coroutine scope cho service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var transferJob: Job? = null

    // Trạng thái transfer — UI observe qua binder
    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    // Progress file đang truyền — UI observe qua binder
    private val _fileProgress = MutableStateFlow<FileProgress?>(null)
    val fileProgress: StateFlow<FileProgress?> = _fileProgress.asStateFlow()

    // Sender / Receiver service
    private var fileSenderService: FileSenderService? = null
    private var fileServerService: FileServerService? = null

    // Tracking thời gian để tính tốc độ trung bình
    private var transferStartTime = 0L

    // ========== BINDER ==========

    inner class TransferBinder : Binder() {
        fun getService(): TransferService = this@TransferService
    }

    private val binder = TransferBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    // ========== LIFECYCLE ==========

    override fun onCreate() {
        super.onCreate()
        TransferNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND -> {
                val ip = intent.getStringExtra(EXTRA_IP) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                val files = intent.getParcelableArrayListExtra<TransferFile>(EXTRA_FILES)
                    ?: return START_NOT_STICKY

                startForegroundNotification(
                    getString(R.string.transfer_notification_sending)
                )
                startSending(ip, port, files)
            }

            ACTION_RECEIVE -> {
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                startForegroundNotification(
                    getString(R.string.transfer_notification_receiving)
                )
                startReceiving(port)
            }

            ACTION_CANCEL -> {
                cancelTransfer()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        transferJob?.cancel()
        serviceScope.cancel()
        fileSenderService?.disconnect()
        fileServerService?.stopServer()
        super.onDestroy()
    }

    // ========== FOREGROUND NOTIFICATION ==========

    private fun startForegroundNotification(text: String) {
        val notification = TransferNotificationHelper.createTransferNotification(
            context = this,
            title = getString(R.string.transfer_progress_title),
            text = text,
            indeterminate = true
        ).build()

        startForeground(TransferNotificationHelper.NOTIFICATION_ID, notification)
    }

    // ========== SEND MODE ==========

    private fun startSending(ip: String, port: Int, files: List<TransferFile>) {
        transferJob?.cancel()
        transferJob = serviceScope.launch {
            _transferState.value = TransferState.Connecting
            transferStartTime = System.currentTimeMillis()

            val sender = FileSenderService(applicationContext)
            fileSenderService = sender

            try {
                // Kết nối đến receiver
                sender.connect(ip, port)

                // Gửi file
                sender.sendAllFiles(files, object : FileSenderService.SenderListener {
                    override fun onConnected() {
                        _transferState.value = TransferState.Connected(
                            DeviceInfo(name = "Receiver", ipAddress = ip, port = port)
                        )
                    }

                    override fun onHandshakeAccepted() {
                        Log.d(TAG, "Handshake được chấp nhận")
                    }

                    override fun onHandshakeRejected() {
                        _transferState.value = TransferState.Error(
                            message = getString(R.string.incoming_transfer_reject),
                            canRetry = false
                        )
                    }

                    override fun onFileSendStarted(file: TransferFile, index: Int) {
                        _transferState.value = TransferState.Transferring(
                            currentFile = file,
                            currentIndex = index,
                            totalFiles = files.size,
                            bytesTransferred = 0,
                            totalBytes = files.sumOf { it.size },
                            speedBytesPerSec = 0
                        )
                    }

                    override fun onFileProgress(progress: FileProgress) {
                        _fileProgress.value = progress

                        // Cập nhật notification
                        TransferNotificationHelper.updateProgress(
                            context = applicationContext,
                            title = getString(R.string.transfer_notification_sending),
                            text = "${progress.fileName} - ${progress.percent}%",
                            progress = progress.percent
                        )
                    }

                    override fun onFileSent(file: TransferFile, index: Int) {
                        Log.d(TAG, "Gửi xong file ${index + 1}/${files.size}: ${file.name}")
                    }

                    override fun onTransferComplete() {
                        val duration = System.currentTimeMillis() - transferStartTime
                        val totalBytes = files.sumOf { it.size }
                        val avgSpeed = if (duration > 0) {
                            (totalBytes * 1000 / duration)
                        } else 0L

                        _transferState.value = TransferState.Complete(
                            TransferSummary(
                                fileCount = files.size,
                                totalBytes = totalBytes,
                                durationMs = duration,
                                avgSpeedBytesPerSec = avgSpeed,
                                deviceName = "Receiver",
                                direction = TransferDirection.SENT
                            )
                        )

                        TransferNotificationHelper.showCompleteNotification(
                            applicationContext, isSender = true, fileCount = files.size
                        )

                        stopSelf()
                    }

                    override fun onError(message: String) {
                        _transferState.value = TransferState.Error(message)
                        stopSelf()
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi send", e)
                _transferState.value = TransferState.Error(
                    message = e.message ?: getString(R.string.error_transfer_failed)
                )
                stopSelf()
            } finally {
                sender.disconnect()
            }
        }
    }

    // ========== RECEIVE MODE ==========

    private fun startReceiving(port: Int) {
        transferJob?.cancel()
        transferJob = serviceScope.launch {
            _transferState.value = TransferState.Connecting
            transferStartTime = System.currentTimeMillis()

            val server = FileServerService()
            fileServerService = server

            try {
                // Khởi tạo server
                server.startServer(port)

                // Chờ client kết nối
                server.acceptConnection()

                // Nhận file
                server.receiveSession(object : FileServerService.ServerListener {
                    override fun onClientConnected(
                        handshake: FileTransferProtocol.HandshakeData
                    ) {
                        _transferState.value = TransferState.Connected(
                            DeviceInfo(
                                name = handshake.deviceName,
                                ipAddress = ""
                            )
                        )
                    }

                    override fun onFileReceiveStarted(
                        meta: FileTransferProtocol.FileMetaData
                    ) {
                        Log.d(TAG, "Bắt đầu nhận: ${meta.fileName}")
                    }

                    override fun onFileProgress(progress: FileProgress) {
                        _fileProgress.value = progress

                        TransferNotificationHelper.updateProgress(
                            context = applicationContext,
                            title = getString(R.string.transfer_notification_receiving),
                            text = "${progress.fileName} - ${progress.percent}%",
                            progress = progress.percent
                        )
                    }

                    override fun onFileReceived(
                        meta: FileTransferProtocol.FileMetaData,
                        savedPath: String
                    ) {
                        Log.d(TAG, "Nhận xong: ${meta.fileName} → $savedPath")
                    }

                    override fun onTransferComplete() {
                        val duration = System.currentTimeMillis() - transferStartTime

                        _transferState.value = TransferState.Complete(
                            TransferSummary(
                                fileCount = 0, // sẽ cập nhật từ handshake
                                totalBytes = 0,
                                durationMs = duration,
                                avgSpeedBytesPerSec = 0,
                                deviceName = "Sender",
                                direction = TransferDirection.RECEIVED
                            )
                        )

                        TransferNotificationHelper.showCompleteNotification(
                            applicationContext, isSender = false, fileCount = 0
                        )

                        stopSelf()
                    }

                    override fun onError(message: String) {
                        _transferState.value = TransferState.Error(message)
                        stopSelf()
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi receive", e)
                _transferState.value = TransferState.Error(
                    message = e.message ?: getString(R.string.error_transfer_failed)
                )
                stopSelf()
            } finally {
                server.stopServer()
            }
        }
    }

    // ========== CANCEL ==========

    private fun cancelTransfer() {
        fileSenderService?.cancelTransfer()
        transferJob?.cancel()
        fileSenderService?.disconnect()
        fileServerService?.stopServer()
        _transferState.value = TransferState.Idle
        TransferNotificationHelper.cancelNotification(this)
        stopSelf()
    }
}
```

---

## Tóm Tắt Files Phase 1

| # | File | Package | Chức năng |
|---|------|---------|-----------|
| 1 | `ConnectionInfo.kt` | `ui/transfer/model/` | Data class thông tin QR |
| 2 | `DeviceInfo.kt` | `ui/transfer/model/` | Data class thiết bị |
| 3 | `TransferFile.kt` | `ui/transfer/model/` | Data class file truyền |
| 4 | `TransferState.kt` | `ui/transfer/model/` | Sealed class trạng thái |
| 5 | `FileProgress.kt` | `ui/transfer/model/` | Progress từng file |
| 6 | `NetworkUtils.kt` | `util/transfer/` | IP, port, format utils |
| 7 | `QrCodeHelper.kt` | `util/transfer/` | Generate/parse QR |
| 8 | `HotspotManager.kt` | `service/transfer/` | Quản lý Hotspot |
| 9 | `WifiHelper.kt` | `util/transfer/` | Kết nối WiFi |
| 10 | `FileTransferProtocol.kt` | `util/transfer/` | Protocol definitions |
| 11 | `FileServerService.kt` | `service/transfer/` | TCP Server (receiver) |
| 12 | `FileSenderService.kt` | `service/transfer/` | TCP Client (sender) |
| 13 | `TransferNotificationHelper.kt` | `util/transfer/` | Notification |
| 14 | `TransferService.kt` | `service/transfer/` | Foreground Service chính |

**Tổng: 14 files Kotlin mới + sửa `AndroidManifest.xml` + sửa `build.gradle` + sửa `strings.xml`**

---

## Cách Test Phase 1

### Test 1: QR Code
```
1. Gọi QrCodeHelper.generateQrBitmap(ConnectionInfo(...)) 
2. Hiển thị bitmap → scan bằng app QR reader
3. Verify JSON content đúng
```

### Test 2: Hotspot
```
1. Trên thiết bị A: HotspotManager.startHotspot(port, listener)
2. Verify callback onHotspotStarted → log SSID, password, IP
3. Trên thiết bị B: vào Settings WiFi → tìm network SSID → connect thủ công
4. Verify kết nối thành công
5. Tắt: HotspotManager.stopHotspot()
```

### Test 3: File Transfer End-to-End
```
1. Thiết bị A (Receiver): 
   - TransferService.startReceive(context, port=8888)
   
2. Thiết bị B (Sender):
   - Kết nối WiFi vào hotspot A
   - TransferService.startSend(context, ip, port, files)
   
3. Verify:
   - Sender: StateFlow → Connecting → Connected → Transferring → Complete
   - Receiver: file được lưu vào Downloads/ShareFile/
   - Notification hiển thị progress
```
