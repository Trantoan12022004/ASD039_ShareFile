package com.example.basekotlin.service.clone

import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import com.example.basekotlin.R
import com.example.basekotlin.service.transfer.HotspotManager
import com.example.basekotlin.ui.clone_phone.model.*
import com.example.basekotlin.ui.transfer.data.SendFileRepository
import com.example.basekotlin.ui.transfer.model.BaseFileItem
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.model.TransferableItem
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.QrCodeHelper
import com.example.basekotlin.util.transfer.TransferNotificationHelper
import com.example.basekotlin.util.transfer.WifiHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ClonePhoneService : Service() {

    companion object {
        private const val TAG = "ClonePhoneService"
        const val ACTION_START_HOST = "ACTION_START_HOST"
        const val ACTION_START_CLIENT = "ACTION_START_CLIENT"
        const val ACTION_DISCONNECT = "ACTION_DISCONNECT"
        const val EXTRA_HOST_IP = "EXTRA_HOST_IP"
        const val EXTRA_PORT = "EXTRA_PORT"

        const val MSG_HANDSHAKE = 101
        const val MSG_CATEGORY_START = 102
        const val MSG_FILE_START = 103
        const val MSG_FILE_CHUNK = 104
        const val MSG_FILE_END = 105
        const val MSG_CATEGORY_END = 106
        const val MSG_ALL_DONE = 107
        const val MSG_CANCEL = 108

        private const val BUFFER_SIZE = 32 * 1024

        fun startHost(context: Context, port: Int = 8889) {
            val intent = Intent(context, ClonePhoneService::class.java).apply {
                action = ACTION_START_HOST
                putExtra(EXTRA_PORT, port)
            }
            context.startForegroundService(intent)
        }

        fun startClient(context: Context, hostIp: String, port: Int = 8889) {
            val intent = Intent(context, ClonePhoneService::class.java).apply {
                action = ACTION_START_CLIENT
                putExtra(EXTRA_HOST_IP, hostIp)
                putExtra(EXTRA_PORT, port)
            }
            context.startForegroundService(intent)
        }

        fun disconnect(context: Context) {
            val intent = Intent(context, ClonePhoneService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val binder = ServiceBinder()

    inner class ServiceBinder : Binder() {
        fun getService(): ClonePhoneService = this@ClonePhoneService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // StateFlows cho UI
    val qrBitmap = MutableStateFlow<Bitmap?>(null)
    val hostIp = MutableStateFlow("")
    val connectionInfo = MutableStateFlow<ConnectionInfo?>(null)
    val isConnected = MutableStateFlow(false)
    val overallState = MutableStateFlow(CloneOverallState())
    val categoryStates = MutableStateFlow<Map<CloneCategory, CategoryState>>(emptyMap())

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var hotspotManager: HotspotManager? = null
    private val isRunning = AtomicBoolean(false)
    private var selectedCategoriesToSend: Set<CloneCategory> = emptySet()

    override fun onCreate() {
        super.onCreate()
        TransferNotificationHelper.createChannel(this)
        val notif = TransferNotificationHelper.createTransferNotification(
            this,
            getString(R.string.clone_phone),
            getString(R.string.connecting)
        )
        startForeground(TransferNotificationHelper.NOTIFICATION_ID, notif.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_HOST -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8889)
                startHostMode(port)
            }
            ACTION_START_CLIENT -> {
                val ip = intent.getStringExtra(EXTRA_HOST_IP) ?: ""
                val port = intent.getIntExtra(EXTRA_PORT, 8889)
                startClientMode(ip, port)
            }
            ACTION_DISCONNECT -> {
                cleanupAndStop()
            }
        }
        return START_NOT_STICKY
    }

    fun setSelectedCategories(categories: Set<CloneCategory>) {
        selectedCategoriesToSend = categories
    }

    // ==========================================
    // ============ HOST (MÁY CŨ - SENDER) ======
    // ==========================================

    private fun startHostMode(port: Int) {
        if (isRunning.getAndSet(true)) return
        overallState.value = overallState.value.copy(isOldDevice = true, isTransferring = false)

        val myDeviceName = NetworkUtils.getDeviceName()
        val isWifi = NetworkUtils.isWifiConnected(this)

        if (isWifi) {
            val ip = NetworkUtils.getWifiIpAddress()
            hostIp.value = ip
            val info = ConnectionInfo(
                ssid = NetworkUtils.getConnectedWifiName(this),
                password = "",
                ipAddress = ip,
                port = port,
                deviceName = myDeviceName
            )
            connectionInfo.value = info
            serviceScope.launch {
                qrBitmap.value = QrCodeHelper.generateQrBitmap(info)
            }
            listenForClient(port)
        } else {
            hotspotManager = HotspotManager(this)
            hotspotManager?.startHotspot(port, object : HotspotManager.HotspotListener {
                override fun onHotspotStarted(info: ConnectionInfo) {
                    hostIp.value = info.ipAddress
                    connectionInfo.value = info
                    serviceScope.launch {
                        qrBitmap.value = QrCodeHelper.generateQrBitmap(info)
                    }
                    listenForClient(port)
                }

                override fun onHotspotFailed(errorCode: Int) {
                    Log.e(TAG, "Khởi tạo Hotspot thất bại: $errorCode")
                }

                override fun onHotspotStopped() {}
            })
        }
    }

    private fun listenForClient(port: Int) {
        serviceScope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                Log.d(TAG, "ServerSocket chờ máy mới kết nối tại cổng $port...")

                val socket = serverSocket!!.accept()
                clientSocket = socket
                isConnected.value = true
                Log.d(TAG, "Máy mới đã kết nối: ${socket.inetAddress.hostAddress}")

                // Bắt đầu truyền dữ liệu
                handleHostTransfer(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi ServerSocket", e)
            }
        }
    }

    private suspend fun handleHostTransfer(socket: Socket) = withContext(Dispatchers.IO) {
        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            val repo = SendFileRepository(this@ClonePhoneService)

            // 1. Quét dữ liệu cho các danh mục đã chọn
            val categoryDataMap = mutableMapOf<CloneCategory, List<TransferableItem>>()
            for (cat in selectedCategoriesToSend) {
                val list = when (cat) {
                    CloneCategory.CONTACTS -> repo.queryContacts()
                    CloneCategory.PHOTOS -> repo.queryPhotos()
                    CloneCategory.VIDEOS -> repo.queryVideos()
                    CloneCategory.AUDIOS -> repo.queryMusic()
                    CloneCategory.DOCUMENTS -> repo.queryDocuments()
                    CloneCategory.APPS -> repo.queryInstalledApps() + repo.queryNotInstalledApks()
                }
                categoryDataMap[cat] = list
            }

            // 2. Tính tổng dung lượng và số lượng item
            var grandTotalBytes = 0L
            val catStateMap = mutableMapOf<CloneCategory, CategoryState>()
            for (cat in CloneCategory.values()) {
                val isSelected = selectedCategoriesToSend.contains(cat)
                val list = categoryDataMap[cat] ?: emptyList()
                val totalBytes = list.sumOf { it.sizeBytes }
                grandTotalBytes += totalBytes
                catStateMap[cat] = CategoryState(
                    category = cat,
                    isSelected = isSelected,
                    status = if (isSelected) CategoryTransferStatus.WAITING else CategoryTransferStatus.WAITING,
                    itemCount = list.size,
                    totalBytes = totalBytes
                )
            }
            categoryStates.value = catStateMap
            overallState.value = overallState.value.copy(
                isTransferring = true,
                totalBytes = grandTotalBytes,
                totalTransferredBytes = 0L,
                overallPercent = 0
            )

            // 3. Gửi Handshake Packet chứa cấu hình các mục sang Máy Mới
            val handshakeList = catStateMap.values.toList()
            val handshakeJson = gson.toJson(handshakeList)
            sendPacket(output, MSG_HANDSHAKE, handshakeJson.toByteArray(Charsets.UTF_8))

            // Delay nhỏ đảm bảo phía máy mới xử lý xong handshake
            delay(500)

            var cumulativeTransferredBytes = 0L

            // 4. Lần lượt truyền từng danh mục đã chọn
            for (cat in CloneCategory.values()) {
                if (!selectedCategoriesToSend.contains(cat)) continue

                val items = categoryDataMap[cat] ?: emptyList()
                var categoryTransferredBytes = 0L
                val categoryTotalBytes = catStateMap[cat]?.totalBytes ?: 0L

                // Cập nhật trạng thái sang PROGRESS
                updateCategoryStatus(cat, CategoryTransferStatus.PROGRESS, 0, 0L)
                sendPacket(output, MSG_CATEGORY_START, cat.name.toByteArray(Charsets.UTF_8))

                try {
                    for (item in items) {
                        // Lấy file hoặc export vCard cho Contact
                        val (inputStream, fileName, fileSize) = prepareStreamForItem(item)
                        if (inputStream == null) continue

                        // Gửi File Start
                        val fileMetaJson = gson.toJson(mapOf(
                            "category" to cat.name,
                            "fileName" to fileName,
                            "fileSize" to fileSize,
                            "mimeType" to item.mimeType
                        ))
                        sendPacket(output, MSG_FILE_START, fileMetaJson.toByteArray(Charsets.UTF_8))

                        // Gửi Chunks
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var fileBytesSent = 0L

                        inputStream.use { stream ->
                            while (stream.read(buffer).also { bytesRead = it } != -1) {
                                output.writeInt(MSG_FILE_CHUNK)
                                output.writeInt(bytesRead)
                                output.write(buffer, 0, bytesRead)
                                output.flush()

                                fileBytesSent += bytesRead
                                categoryTransferredBytes += bytesRead
                                cumulativeTransferredBytes += bytesRead

                                // Cập nhật % danh mục và tổng
                                val catPercent = if (categoryTotalBytes > 0) {
                                    ((categoryTransferredBytes * 100) / categoryTotalBytes).toInt().coerceIn(0, 100)
                                } else 100

                                val overallPercent = if (grandTotalBytes > 0) {
                                    ((cumulativeTransferredBytes * 100) / grandTotalBytes).toInt().coerceIn(0, 100)
                                } else 100

                                updateCategoryStatus(cat, CategoryTransferStatus.PROGRESS, catPercent, categoryTransferredBytes)
                                overallState.value = overallState.value.copy(
                                    overallPercent = overallPercent,
                                    totalTransferredBytes = cumulativeTransferredBytes
                                )
                            }
                        }

                        // Gửi File End
                        sendPacket(output, MSG_FILE_END, fileName.toByteArray(Charsets.UTF_8))
                    }

                    // Hoàn thành danh mục
                    updateCategoryStatus(cat, CategoryTransferStatus.COMPLETE, 100, categoryTotalBytes)
                    sendPacket(output, MSG_CATEGORY_END, cat.name.toByteArray(Charsets.UTF_8))
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi truyền danh mục $cat", e)
                    updateCategoryStatus(cat, CategoryTransferStatus.FAIL, 0, 0L)
                }
            }

            // 5. Gửi bản tin hoàn tất tất cả
            sendPacket(output, MSG_ALL_DONE, ByteArray(0))
            overallState.value = overallState.value.copy(
                isTransferring = false,
                isDone = true,
                overallPercent = 100,
                totalTransferredBytes = grandTotalBytes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong handleHostTransfer", e)
        }
    }

    // ==========================================
    // ============ CLIENT (MÁY MỚI - RECEIVER) =
    // ==========================================

    private fun startClientMode(ip: String, port: Int) {
        if (isRunning.getAndSet(true)) return
        overallState.value = overallState.value.copy(isOldDevice = false, isTransferring = false)

        serviceScope.launch {
            try {
                Log.d(TAG, "Máy mới đang kết nối tới Máy cũ tại $ip:$port...")
                val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val wifiNetwork = WifiHelper.activeWifiNetwork ?: cm.allNetworks.firstOrNull { net ->
                    cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }
                if (wifiNetwork != null) {
                    try { cm.bindProcessToNetwork(wifiNetwork) } catch (_: Exception) {}
                }

                val socket = if (wifiNetwork != null) wifiNetwork.socketFactory.createSocket() else Socket()
                wifiNetwork?.bindSocket(socket)
                socket.connect(InetSocketAddress(ip, port), 10000)

                clientSocket = socket
                isConnected.value = true
                Log.d(TAG, "Máy mới kết nối thành công tới Máy cũ!")

                handleClientReceive(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kết nối Socket phía Client", e)
            }
        }
    }

    private suspend fun handleClientReceive(socket: Socket) = withContext(Dispatchers.IO) {
        var input: DataInputStream? = null
        try {
            input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            val baseCloneDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ShareFile/Clone"
            )
            if (!baseCloneDir.exists()) baseCloneDir.mkdirs()

            var grandTotalBytes = 0L
            var cumulativeTransferredBytes = 0L
            var currentFileFos: FileOutputStream? = null
            var currentTargetFile: File? = null
            var currentCat: CloneCategory? = null
            var currentCatTotalBytes = 0L
            var currentCatTransferredBytes = 0L

            while (isActive && !socket.isClosed) {
                val msgType = input.readInt()
                val len = input.readInt()
                val payload = if (len > 0) {
                    val bytes = ByteArray(len)
                    input.readFully(bytes)
                    bytes
                } else ByteArray(0)

                when (msgType) {
                    MSG_HANDSHAKE -> {
                        val json = String(payload, Charsets.UTF_8)
                        val type = object : TypeToken<List<CategoryState>>() {}.type
                        val list: List<CategoryState> = gson.fromJson(json, type)
                        val map = list.associateBy { it.category }
                        categoryStates.value = map
                        grandTotalBytes = list.filter { it.isSelected }.sumOf { it.totalBytes }
                        overallState.value = overallState.value.copy(
                            isTransferring = true,
                            totalBytes = grandTotalBytes,
                            totalTransferredBytes = 0L,
                            overallPercent = 0
                        )
                    }

                    MSG_CATEGORY_START -> {
                        val catName = String(payload, Charsets.UTF_8)
                        currentCat = try { CloneCategory.valueOf(catName) } catch (_: Exception) { null }
                        currentCatTransferredBytes = 0L
                        currentCat?.let { cat ->
                            currentCatTotalBytes = categoryStates.value[cat]?.totalBytes ?: 0L
                            updateCategoryStatus(cat, CategoryTransferStatus.PROGRESS, 0, 0L)
                        }
                    }

                    MSG_FILE_START -> {
                        val metaJson = String(payload, Charsets.UTF_8)
                        val map: Map<String, Any> = gson.fromJson(metaJson, object : TypeToken<Map<String, Any>>() {}.type)
                        val catName = map["category"] as? String ?: "OTHERS"
                        val fileName = map["fileName"] as? String ?: "file_${System.currentTimeMillis()}"

                        val folder = File(baseCloneDir, catName)
                        if (!folder.exists()) folder.mkdirs()
                        currentTargetFile = File(folder, fileName)
                        currentFileFos = FileOutputStream(currentTargetFile)
                    }

                    MSG_FILE_CHUNK -> {
                        currentFileFos?.write(payload)
                        currentCatTransferredBytes += payload.size
                        cumulativeTransferredBytes += payload.size

                        currentCat?.let { cat ->
                            val catPercent = if (currentCatTotalBytes > 0) {
                                ((currentCatTransferredBytes * 100) / currentCatTotalBytes).toInt().coerceIn(0, 100)
                            } else 100
                            updateCategoryStatus(cat, CategoryTransferStatus.PROGRESS, catPercent, currentCatTransferredBytes)
                        }

                        val overallPercent = if (grandTotalBytes > 0) {
                            ((cumulativeTransferredBytes * 100) / grandTotalBytes).toInt().coerceIn(0, 100)
                        } else 100

                        overallState.value = overallState.value.copy(
                            overallPercent = overallPercent,
                            totalTransferredBytes = cumulativeTransferredBytes
                        )
                    }

                    MSG_FILE_END -> {
                        try {
                            currentFileFos?.flush()
                            currentFileFos?.close()
                        } catch (_: Exception) {}
                        currentFileFos = null

                        // Quét file vào MediaStore để xuất hiện ngay trong thư viện
                        currentTargetFile?.let { f ->
                            MediaScannerConnection.scanFile(this@ClonePhoneService, arrayOf(f.absolutePath), null, null)
                        }
                    }

                    MSG_CATEGORY_END -> {
                        currentCat?.let { cat ->
                            updateCategoryStatus(cat, CategoryTransferStatus.COMPLETE, 100, currentCatTotalBytes)
                        }
                    }

                    MSG_ALL_DONE -> {
                        overallState.value = overallState.value.copy(
                            isTransferring = false,
                            isDone = true,
                            overallPercent = 100,
                            totalTransferredBytes = grandTotalBytes
                        )
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong handleClientReceive", e)
        }
    }

    private fun sendPacket(output: DataOutputStream, type: Int, bytes: ByteArray) {
        output.writeInt(type)
        output.writeInt(bytes.size)
        if (bytes.isNotEmpty()) {
            output.write(bytes)
        }
        output.flush()
    }

    private fun updateCategoryStatus(cat: CloneCategory, status: CategoryTransferStatus, percent: Int, transferred: Long) {
        val currentMap = categoryStates.value.toMutableMap()
        val old = currentMap[cat] ?: CategoryState(category = cat, isSelected = true)
        currentMap[cat] = old.copy(
            status = status,
            progressPercent = percent,
            transferredBytes = transferred
        )
        categoryStates.value = currentMap
    }

    private fun prepareStreamForItem(item: TransferableItem): Triple<InputStream?, String, Long> {
        return if (item.category == com.example.basekotlin.ui.transfer.model.FileCategory.CONTACTS) {
            val vcf = exportContactToVcf(item)
            if (vcf != null && vcf.exists()) {
                Triple(FileInputStream(vcf), vcf.name, vcf.length())
            } else {
                Triple(null, item.displayName, 0L)
            }
        } else {
            try {
                val stream = contentResolver.openInputStream(item.uri)
                Triple(stream, item.displayName, item.sizeBytes)
            } catch (e: Exception) {
                Triple(null, item.displayName, item.sizeBytes)
            }
        }
    }

    private fun exportContactToVcf(item: TransferableItem): File? {
        return try {
            val contactId = item.id.removePrefix("contact_")
            val lookupUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val cursor = contentResolver.query(lookupUri, arrayOf(ContactsContract.Contacts.LOOKUP_KEY), null, null, null)
            val lookupKey = cursor?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return null
            val vcardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
            val vcardData = contentResolver.openInputStream(vcardUri)?.use { it.readBytes() } ?: return null
            val safeName = item.displayName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val vcfFile = File(cacheDir, "${safeName}.vcf")
            vcfFile.writeBytes(vcardData)
            vcfFile
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanupAndStop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        hotspotManager?.stopHotspot()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupAndStop()
        serviceScope.cancel()
    }
}
