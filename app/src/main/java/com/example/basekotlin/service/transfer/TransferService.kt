package com.example.basekotlin.service.transfer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Network
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.basekotlin.R
import com.example.basekotlin.ui.transfer.model.OverallStats
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.model.TransferStatus1
import com.example.basekotlin.util.transfer.FileTransferProtocol
import com.example.basekotlin.util.transfer.TransferNotificationHelper
import com.example.basekotlin.util.transfer.NetworkUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class TransferService : Service() {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"
        const val ACTION_START_SENDER = "ACTION_START_SENDER"
        const val ACTION_START_RECEIVER = "ACTION_START_RECEIVER"
        const val ACTION_DISCONNECT = "ACTION_DISCONNECT"
        const val EXTRA_IP = "EXTRA_IP"
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_FILES = "EXTRA_FILES"
        const val EXTRA_PEER_NAME = "EXTRA_PEER_NAME"
        const val EXTRA_NETWORK = "EXTRA_NETWORK"

        val isConnected = MutableStateFlow(false)

        fun startSender(context: Context, ip: String, port: Int, peerName: String, files: ArrayList<TransferFile>, network: Network? = null) {
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_START_SENDER
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_PEER_NAME, peerName)
                putParcelableArrayListExtra(EXTRA_FILES, files)
                network?.let { putExtra(EXTRA_NETWORK, it) }
            }
            context.startForegroundService(intent)
        }

        fun startReceiver(context: Context, port: Int) {
            startReceive(context, port)
        }

        fun startReceive(context: Context, port: Int) {
            isConnected.value = false
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_START_RECEIVER
                putExtra(EXTRA_PORT, port)
            }
            context.startForegroundService(intent)
        }

        fun startSend(context: Context, ip: String, port: Int, peerName: String = "Receiver", files: ArrayList<TransferFile>, network: Network? = null) {
            startSender(context, ip, port, peerName, files, network)
        }

        fun disconnect(context: Context) {
            isConnected.value = false
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }

        fun cancel(context: Context) {
            disconnect(context)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Socket connections
    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var dataInput: DataInputStream? = null
    private var dataOutput: DataOutputStream? = null

    // StateFlows cho UI observe
    private val _isPeerOnline = MutableStateFlow(true)
    val isPeerOnline: StateFlow<Boolean> = _isPeerOnline.asStateFlow()

    private val _peerDeviceName = MutableStateFlow("")
    val peerDeviceName: StateFlow<String> = _peerDeviceName.asStateFlow()

    private val _items = MutableStateFlow<List<ProgressItem>>(emptyList())
    val items: StateFlow<List<ProgressItem>> = _items.asStateFlow()

    private val _stats = MutableStateFlow(OverallStats())
    val stats: StateFlow<OverallStats> = _stats.asStateFlow()

    // File transfer queue
    private val sendQueue = ConcurrentLinkedQueue<ProgressItem.FileTransfer>()
    private var isTransferWorkerActive = false
    private var lastPongReceivedTime = 0L

    // Quản lý file bị hủy
    private val cancelledFileIds = ConcurrentHashMap.newKeySet<String>()

    // Thông tin file đang nhận hiện tại
    private var currentReceivingFileId: String? = null
    private var currentReceivingMeta: FileTransferProtocol.FileMetaData? = null
    private var currentReceivingFos: FileOutputStream? = null
    private var currentReceivingTargetFile: File? = null
    private var currentReceivingTotalRead = 0L
    private var currentReceivingStartTime = 0L
    private var lastReceiveUiUpdateTime = 0L
    private var lastReceiveLoggedTime = 0L
    private var lastReceiveLoggedPercent = -1
    private var receiveBytesSinceLastLog = 0L

    // Đồng bộ luồng ghi Socket
    private val writeMutex = Mutex()
    private var metaAckDeferred: CompletableDeferred<Boolean>? = null

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    private fun createThumbnailBase64(file: TransferFile): String? {
        return try {
            val ext = File(file.name).extension.lowercase()
            val mime = file.mimeType.lowercase()
            if (mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp")) {
                contentResolver.openInputStream(file.uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bitmap = BitmapFactory.decodeStream(stream, null, options) ?: return null
                    val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun sendSafeMessage(type: Int, payload: String) {
        writeMutex.withLock {
            dataOutput?.let { out ->
                FileTransferProtocol.sendMessage(out, type, payload)
            }
        }
    }

    inner class ServiceBinder : Binder() {
        fun getService(): TransferService = this@TransferService
    }

    private val binder = ServiceBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TransferNotificationHelper.createChannel(this)
        val notification = TransferNotificationHelper.createTransferNotification(
            this,
            getString(R.string.transfer_notification_channel),
            getString(R.string.transfer_preparing)
        ).build()
        startForeground(TransferNotificationHelper.NOTIFICATION_ID, notification)

        when (intent?.action) {
            ACTION_START_SENDER -> {
                val ip = intent.getStringExtra(EXTRA_IP) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                val peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Receiver"
                val files = intent.getParcelableArrayListExtra<TransferFile>(EXTRA_FILES) ?: arrayListOf()
                @Suppress("DEPRECATION")
                val network = intent.getParcelableExtra<Network>(EXTRA_NETWORK)

                _peerDeviceName.value = peerName
                startAsSender(ip, port, network, files)
            }
            ACTION_START_RECEIVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                startAsReceiver(port)
            }
            ACTION_DISCONNECT -> {
                disconnectSession()
            }
        }
        return START_NOT_STICKY
    }

    // ===== KHỞI CHẠY PHÍA SENDER =====
    private fun startAsSender(ip: String, port: Int, network: Network?, initialFiles: List<TransferFile>) {
        serviceScope.launch {
            try {
                Log.d(TAG, "[SENDER] 🌐 Bắt đầu kết nối Socket tới Receiver: $ip:$port (Network bind: ${network != null})...")
                val socket = if (network != null) network.socketFactory.createSocket() else Socket()
                network?.bindSocket(socket)
                socket.connect(java.net.InetSocketAddress(ip, port), FileTransferProtocol.CONNECT_TIMEOUT_MS)
                Log.d(TAG, "[SENDER] 🟢 Socket kết nối THÀNH CÔNG! Local: ${socket.localSocketAddress} -> Remote: ${socket.remoteSocketAddress}")
                setupConnectedSocket(socket, isSender = true)

                addFilesToSend(initialFiles)
            } catch (e: Exception) {
                Log.e(TAG, "[SENDER] 🔴 Lỗi kết nối tới Receiver ($ip:$port): ${e.message}", e)
                _isPeerOnline.value = false
            }
        }
    }

    // ===== KHỞI CHẠY PHÍA RECEIVER =====
    private fun startAsReceiver(port: Int) {
        serviceScope.launch {
            try {
                serverSocket?.close()
                Log.d(TAG, "[RECEIVER] 📡 Khởi động ServerSocket trên port $port (reuseAddress = true)...")
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                Log.d(TAG, "[RECEIVER] ⏳ Đang chờ Sender kết nối (accept)...")
                val socket = serverSocket?.accept() ?: return@launch
                Log.d(TAG, "[RECEIVER] 🟢 Chấp nhận kết nối từ Sender IP: ${socket.inetAddress.hostAddress}:${socket.port}")
                setupConnectedSocket(socket, isSender = false)
            } catch (e: Exception) {
                Log.e(TAG, "[RECEIVER] 🔴 Lỗi server socket trên port $port: ${e.message}", e)
            }
        }
    }

    private fun setupConnectedSocket(socket: Socket, isSender: Boolean) {
        activeSocket = socket
        dataInput = DataInputStream(socket.getInputStream().buffered())
        dataOutput = DataOutputStream(socket.getOutputStream().buffered())
        _isPeerOnline.value = true
        isConnected.value = true
        lastPongReceivedTime = System.currentTimeMillis()
        Log.d(TAG, "[SESSION] 🔌 Đã khởi tạo Stream I/O thành công (Role: ${if (isSender) "SENDER" else "RECEIVER"})")

        startIncomingMessageLoop()
        startHeartbeatLoop()

        // Gửi thông tin thiết bị (Handshake) để đối phương hiển thị tên máy
        serviceScope.launch {
            val myDeviceName = NetworkUtils.getDeviceName()
            val handshake = FileTransferProtocol.HandshakeData(
                deviceName = myDeviceName,
                fileCount = 0,
                totalSize = 0L
            )
            val type = if (isSender) FileTransferProtocol.MSG_HANDSHAKE else FileTransferProtocol.MSG_ACK_HANDSHAKE
            sendSafeMessage(type, Gson().toJson(handshake))
            Log.d(TAG, "[HANDSHAKE] 📤 Đã gửi tên thiết bị: \"$myDeviceName\" (Role: ${if (isSender) "SENDER" else "RECEIVER"})")
        }
    }

    // ===== VÒNG LẶP ĐỌC GÓI TIN ĐẾN (FRAMED MESSAGES & CHUNKS) =====
    private fun startIncomingMessageLoop() {
        serviceScope.launch {
            val input = dataInput ?: return@launch
            Log.d(TAG, "[PROTOCOL] 🔄 Bắt đầu vòng lặp đọc gói tin đến (Incoming Message Loop)...")
            val chunkBuffer = ByteArray(FileTransferProtocol.BUFFER_SIZE)

            while (isActive && activeSocket?.isClosed == false) {
                try {
                    val type = input.readInt()
                    val length = input.readInt()
                    if (length < 0 || length > FileTransferProtocol.MAX_PAYLOAD_SIZE) {
                        throw IOException("Độ dài gói tin không hợp lệ: $length bytes (type=$type)")
                    }

                    // 1. Xử lý gói binary chunk của file
                    if (type == FileTransferProtocol.MSG_FILE_DATA) {
                        val fileId = currentReceivingFileId
                        val isCancelled = fileId == null || cancelledFileIds.contains(fileId)

                        if (!isCancelled) {
                            val fos = currentReceivingFos
                            var remainingToRead = length
                            var writeErrorOccurred = false
                            while (remainingToRead > 0) {
                                val toRead = minOf(remainingToRead, chunkBuffer.size)
                                input.readFully(chunkBuffer, 0, toRead)
                                if (!writeErrorOccurred && fos != null) {
                                    try {
                                        fos.write(chunkBuffer, 0, toRead)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "[RECEIVER] ❌ Lỗi ghi file vào đĩa: ${e.message}")
                                        writeErrorOccurred = true
                                    }
                                }
                                remainingToRead -= toRead
                            }

                            if (writeErrorOccurred) {
                                cleanupCurrentReceivingFile()
                                fileId?.let { updateFileError(it, "Disk write error") }
                                continue
                            }

                            currentReceivingTotalRead += length
                            receiveBytesSinceLastLog += length
                            lastPongReceivedTime = System.currentTimeMillis()

                            val meta = currentReceivingMeta
                            val totalBytes = meta?.fileSize ?: 0L
                            val currentPercent = if (totalBytes > 0) ((currentReceivingTotalRead * 100) / totalBytes).toInt() else 0

                            val now = System.currentTimeMillis()
                            if (now - lastReceiveUiUpdateTime >= 150 || currentReceivingTotalRead == totalBytes) {
                                lastReceiveUiUpdateTime = now
                                fileId?.let { notifyItemProgress(it, currentReceivingTotalRead, currentPercent) }
                            }

                            val timeDiff = now - lastReceiveLoggedTime
                            if (currentPercent / 10 > lastReceiveLoggedPercent / 10 || (timeDiff >= 1500 && currentPercent != lastReceiveLoggedPercent)) {
                                val speedMBps = if (timeDiff > 0) {
                                    val mb = receiveBytesSinceLastLog.toDouble() / (1024 * 1024)
                                    val sec = timeDiff.toDouble() / 1000
                                    String.format(Locale.US, "%.2f", mb / sec)
                                } else "0.00"

                                Log.d(TAG, "[RECEIVER] ⏳ Tiến độ nhận '${meta?.fileName}': $currentPercent% (${formatSize(currentReceivingTotalRead)} / ${formatSize(totalBytes)}) - $speedMBps MB/s")
                                lastReceiveLoggedPercent = currentPercent
                                lastReceiveLoggedTime = now
                                receiveBytesSinceLastLog = 0L
                            }
                        } else {
                            // File đã bị hủy -> đóng fos an toàn ngay trên luồng IO và xóa file rác
                            if (currentReceivingFos != null) {
                                cleanupCurrentReceivingFile()
                            }
                            // Bỏ qua dữ liệu chunk này để giữ sync cho socket
                            input.skipBytes(length)
                        }
                        continue
                    }

                    // 2. Xử lý các gói tin điều khiển / chat (chuỗi JSON UTF-8)
                    val payloadBuffer = ByteArray(length)
                    input.readFully(payloadBuffer)
                    val payload = String(payloadBuffer, Charsets.UTF_8)
                    val typeName = FileTransferProtocol.getMessageTypeName(type)
                    if (type != FileTransferProtocol.MSG_PING && type != FileTransferProtocol.MSG_PONG) {
                        Log.d(TAG, "[PROTOCOL] 📩 Nhận gói tin: $typeName (Payload length: ${payload.length})")
                    }

                    when (type) {
                        FileTransferProtocol.MSG_PING -> {
                            sendSafeMessage(FileTransferProtocol.MSG_PONG, "{}")
                        }
                        FileTransferProtocol.MSG_PONG -> {
                            lastPongReceivedTime = System.currentTimeMillis()
                            if (!_isPeerOnline.value) {
                                _isPeerOnline.value = true
                                Log.d(TAG, "[HEARTBEAT] 🟢 Đối phương online trở lại")
                            }
                        }
                        FileTransferProtocol.MSG_ACK_META -> {
                            Log.d(TAG, "[SENDER] 📥 Nhận phản hồi MSG_ACK_META từ Receiver (Sẵn sàng nhận dữ liệu)")
                            metaAckDeferred?.complete(true)
                        }
                        FileTransferProtocol.MSG_CHAT_TEXT -> {
                            val chat = FileTransferProtocol.parseChat(payload)
                            Log.d(TAG, "[CHAT] 💬 Nhận tin nhắn chat: \"${chat.text}\" (MsgID: ${chat.messageId})")
                            val item = ProgressItem.TextMessage(
                                id = chat.messageId,
                                text = chat.text,
                                isMe = false,
                                timeFormatted = timeFormat.format(Date(chat.timestamp)),
                                timestamp = chat.timestamp
                            )
                            appendItem(item)
                        }
                        FileTransferProtocol.MSG_HANDSHAKE -> {
                            val handshake = Gson().fromJson(payload, FileTransferProtocol.HandshakeData::class.java)
                            if (handshake.deviceName.isNotEmpty()) {
                                _peerDeviceName.value = handshake.deviceName
                                Log.d(TAG, "[RECEIVER] 📱 Nhận thông tin thiết bị gửi: \"${handshake.deviceName}\"")
                                val myDeviceName = NetworkUtils.getDeviceName()
                                val ackHandshake = FileTransferProtocol.HandshakeData(
                                    deviceName = myDeviceName,
                                    fileCount = 0,
                                    totalSize = 0L
                                )
                                sendSafeMessage(FileTransferProtocol.MSG_ACK_HANDSHAKE, Gson().toJson(ackHandshake))
                            }
                        }
                        FileTransferProtocol.MSG_ACK_HANDSHAKE -> {
                            val handshake = Gson().fromJson(payload, FileTransferProtocol.HandshakeData::class.java)
                            if (handshake.deviceName.isNotEmpty()) {
                                _peerDeviceName.value = handshake.deviceName
                                Log.d(TAG, "[SESSION] 📱 Nhận thông tin thiết bị đối phương: \"${handshake.deviceName}\"")
                            }
                        }
                        FileTransferProtocol.MSG_FILE_META -> {
                            val meta = FileTransferProtocol.parseFileMeta(payload)
                            handleFileMetaReceived(meta)
                        }
                        FileTransferProtocol.MSG_TRANSFER_COMPLETE -> {
                            val control = FileTransferProtocol.parseFileControl(payload)
                            handleFileTransferComplete(control.fileId)
                        }
                        FileTransferProtocol.MSG_CANCEL_FILE -> {
                            val cancelData = FileTransferProtocol.parseFileControl(payload)
                            Log.w(TAG, "[CONTROL] 🛑 Đối phương yêu cầu hủy file: ID=${cancelData.fileId}, Name=${cancelData.fileName}")
                            handleCancelReceived(cancelData.fileId)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[PROTOCOL] ⚠️ Mất kết nối hoặc ngắt stream: ${e.message}")
                    _isPeerOnline.value = false
                    metaAckDeferred?.complete(false)
                    val recFileId = currentReceivingFileId
                    cleanupCurrentReceivingFile()
                    if (recFileId != null) {
                        updateFileError(recFileId, getString(R.string.transfer_error_timeout))
                    }
                    break
                }
            }
            Log.d(TAG, "[PROTOCOL] 🛑 Vòng lặp đọc gói tin đã kết thúc.")
        }
    }

    private fun handleFileMetaReceived(meta: FileTransferProtocol.FileMetaData) {
        val saveDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ShareFile")
        if (!saveDir.exists()) saveDir.mkdirs()
        val targetFile = File(saveDir, meta.fileName)

        Log.d(TAG, "=================================================================")
        Log.d(TAG, "[RECEIVER] 📥 [1/3] Nhận yêu cầu truyền file từ Sender:")
        Log.d(TAG, "[RECEIVER]    Tên file: '${meta.fileName}' | ID: ${meta.fileId}")
        Log.d(TAG, "[RECEIVER]    Dung lượng: ${formatSize(meta.fileSize)} (${meta.fileSize} bytes) | Mime: ${meta.mimeType}")
        Log.d(TAG, "[RECEIVER] 💾 Đích lưu file: ${targetFile.absolutePath}")

        cleanupCurrentReceivingFile()

        cancelledFileIds.remove(meta.fileId)

        currentReceivingFileId = meta.fileId
        currentReceivingMeta = meta
        currentReceivingTargetFile = targetFile
        currentReceivingTotalRead = 0L
        currentReceivingStartTime = System.currentTimeMillis()
        lastReceiveLoggedPercent = -1
        lastReceiveLoggedTime = System.currentTimeMillis()
        receiveBytesSinceLastLog = 0L
        lastReceiveUiUpdateTime = 0L

        try {
            currentReceivingFos = FileOutputStream(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "[RECEIVER] ❌ Không thể tạo file để ghi: ${e.message}", e)
            updateFileError(meta.fileId, e.message)
            return
        }

        val existingIndex = _items.value.indexOfFirst { it.id == meta.fileId }
        if (existingIndex != -1) {
            val updatedList = _items.value.toMutableList()
            val oldItem = updatedList[existingIndex] as? ProgressItem.FileTransfer
            if (oldItem != null) {
                updatedList[existingIndex] = oldItem.copy(
                    status = TransferStatus1.TRANSFERRING,
                    bytesTransferred = 0,
                    progressPercent = 0,
                    thumbnailBase64 = meta.thumbnailBase64 ?: oldItem.thumbnailBase64
                )
                _items.value = updatedList
                recalculateOverallStats(updatedList)
            }
        } else {
            val receiveItem = ProgressItem.FileTransfer(
                id = meta.fileId,
                file = TransferFile(android.net.Uri.fromFile(targetFile), meta.fileName, meta.fileSize, meta.mimeType),
                isMe = false,
                status = TransferStatus1.TRANSFERRING,
                totalBytes = meta.fileSize,
                thumbnailBase64 = meta.thumbnailBase64
            )
            appendItem(receiveItem)
        }

        serviceScope.launch {
            Log.d(TAG, "[RECEIVER] 📤 [2/3] Gửi phản hồi MSG_ACK_META xác nhận sẵn sàng nhận dữ liệu...")
            sendSafeMessage(FileTransferProtocol.MSG_ACK_META, "{}")
        }
    }

    private fun handleFileTransferComplete(fileId: String) {
        if (currentReceivingFileId == fileId) {
            try {
                currentReceivingFos?.flush()
                currentReceivingFos?.close()
            } catch (e: Exception) {
                Log.e(TAG, "[RECEIVER] ❌ Lỗi đóng file: ${e.message}", e)
            }
            currentReceivingFos = null

            val targetFile = currentReceivingTargetFile
            val meta = currentReceivingMeta
            val duration = System.currentTimeMillis() - currentReceivingStartTime
            val avgSpeed = if (duration > 0 && meta != null) {
                val mb = meta.fileSize.toDouble() / (1024 * 1024)
                val sec = duration.toDouble() / 1000
                String.format(Locale.US, "%.2f", mb / sec)
            } else "0.00"

            Log.d(TAG, "[RECEIVER] 🎉 NHẬN THÀNH CÔNG file: '${meta?.fileName}' trong ${duration}ms (Tốc độ TB: $avgSpeed MB/s)")
            if (targetFile != null) {
                Log.d(TAG, "[RECEIVER] 📁 File đã lưu tại: ${targetFile.absolutePath}")
                updateFileCompleted(fileId, targetFile.absolutePath)

                try {
                    MediaScannerConnection.scanFile(this, arrayOf(targetFile.absolutePath), arrayOf(meta?.mimeType ?: "*/*")) { path, uri ->
                        Log.d(TAG, "[RECEIVER] 🔄 MediaScanner quét hoàn tất: path=$path, uri=$uri")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[RECEIVER] ⚠️ MediaScanner quét lỗi: ${e.message}")
                }
            }
            currentReceivingFileId = null
            currentReceivingMeta = null
            currentReceivingTargetFile = null
        }
    }

    private fun handleCancelReceived(fileId: String) {
        cancelledFileIds.add(fileId)
        if (currentReceivingFileId == fileId) {
            cleanupCurrentReceivingFile()
            updateFileStatus(fileId, TransferStatus1.CANCELED)
        } else {
            val item = _items.value.find { it.id == fileId } as? ProgressItem.FileTransfer
            if (item != null && item.isMe) {
                // Người nhận từ chối file mình gửi -> hiển thị DECLINED
                updateFileStatus(fileId, TransferStatus1.DECLINED)
            } else {
                updateFileStatus(fileId, TransferStatus1.CANCELED)
            }
        }
    }

    private fun cleanupCurrentReceivingFile() {
        try {
            currentReceivingFos?.close()
        } catch (e: Exception) { }
        currentReceivingFos = null
        val receivingId = currentReceivingFileId
        if (receivingId != null && cancelledFileIds.contains(receivingId)) {
            try {
                currentReceivingTargetFile?.delete()
            } catch (e: Exception) { }
        }
        currentReceivingTargetFile = null
        currentReceivingMeta = null
        currentReceivingFileId = null
    }

    // ===== HEARTBEAT KIỂM TRA ONLINE / OFFLINE & TIMEOUT 10S =====
    private fun startHeartbeatLoop() {
        serviceScope.launch {
            while (isActive && activeSocket?.isClosed == false) {
                delay(2000)
                try {
                    val elapsed = System.currentTimeMillis() - lastPongReceivedTime
                    if (elapsed > 10_000) {
                        if (_isPeerOnline.value) {
                            _isPeerOnline.value = false
                            Log.w(TAG, "[HEARTBEAT] ⚠️ Quá 10s không nhận được PONG từ ${_peerDeviceName.value} -> Mất kết nối!")
                            appendItem(ProgressItem.SystemEvent(UUID.randomUUID().toString(), "${_peerDeviceName.value} offline"))

                            val currentList = _items.value
                            val transferringItem = currentList.find {
                                it is ProgressItem.FileTransfer && it.status == TransferStatus1.TRANSFERRING
                            } as? ProgressItem.FileTransfer

                            transferringItem?.let {
                                updateFileError(it.id, getString(R.string.transfer_error_timeout))
                            }

                            try {
                                activeSocket?.close()
                            } catch (e: Exception) { }
                        }
                    } else {
                        sendSafeMessage(FileTransferProtocol.MSG_PING, "{}")
                    }
                } catch (e: Exception) {
                    // Nếu gửi ping lỗi, elapsed sẽ tiếp tục tăng và vượt 10s để xử lý timeout
                }
            }
        }
    }

    // ===== GỬI TIN NHẮN CHAT =====
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val chatData = FileTransferProtocol.ChatMessageData(msgId, text, now)

        val myChat = ProgressItem.TextMessage(msgId, text, isMe = true, timeFormat.format(Date(now)), now)
        appendItem(myChat)

        serviceScope.launch {
            try {
                Log.d(TAG, "[CHAT] 💬 Gửi tin nhắn chat: \"$text\"")
                sendSafeMessage(FileTransferProtocol.MSG_CHAT_TEXT, Gson().toJson(chatData))
            } catch (e: Exception) {
                Log.e(TAG, "[CHAT] ❌ Gửi tin nhắn thất bại: ${e.message}", e)
            }
        }
    }

    // ===== THÊM FILE ĐỂ GỬI (ĐƯỢC GỌI KHI BẤM BTN_ADD) =====
    fun addFilesToSend(files: List<TransferFile>) {
        if (files.isEmpty()) return
        val totalSize = files.sumOf { it.size }
        Log.d(TAG, "[SENDER] 📂 Thêm ${files.size} file vào hàng đợi gửi (Tổng dung lượng: ${formatSize(totalSize)}):")
        files.forEachIndexed { index, file ->
            Log.d(TAG, "   └─ File [${index + 1}/${files.size}]: '${file.name}', Size: ${formatSize(file.size)}, Mime: ${file.mimeType}, Uri: ${file.uri}")
        }
        val newItems = files.map { file ->
            ProgressItem.FileTransfer(
                id = UUID.randomUUID().toString(),
                file = file,
                isMe = true,
                status = TransferStatus1.QUEUED
            )
        }
        appendItems(newItems)
        sendQueue.addAll(newItems)
        Log.d(TAG, "[SENDER] 📋 Hàng đợi hiện có ${sendQueue.size} file đang chờ gửi")

        if (!isTransferWorkerActive) {
            startSendWorker()
        }
    }

    private fun startSendWorker() {
        serviceScope.launch {
            isTransferWorkerActive = true
            Log.d(TAG, "[SENDER] 🚀 Khởi động Worker gửi file (Số file trong queue: ${sendQueue.size})...")
            while (isActive && sendQueue.isNotEmpty()) {
                val item = sendQueue.poll() ?: break
                if (item.status == TransferStatus1.CANCELED || item.status == TransferStatus1.DECLINED || cancelledFileIds.contains(item.id)) {
                    Log.d(TAG, "[SENDER] ⏩ Bỏ qua file đã bị hủy: '${item.file.name}'")
                    continue
                }

                val output = dataOutput
                if (output == null) {
                    Log.e(TAG, "[SENDER] 🔴 dataOutput là null, không thể gửi file!")
                    break
                }

                val startTime = System.currentTimeMillis()
                var isCanceled = false
                try {
                    updateFileStatus(item.id, TransferStatus1.TRANSFERRING)

                    Log.d(TAG, "=================================================================")
                    Log.d(TAG, "[SENDER] 🚀 [1/3] Bắt đầu gửi file: '${item.file.name}'")
                    Log.d(TAG, "[SENDER]    ID: ${item.id} | Size: ${formatSize(item.file.size)} (${item.file.size} bytes)")
                    Log.d(TAG, "[SENDER]    Mime: ${item.file.mimeType} | Uri: ${item.file.uri}")

                    // 1. Gửi File Meta
                    val thumbBase64 = createThumbnailBase64(item.file)
                    val meta = FileTransferProtocol.FileMetaData(
                        fileId = item.id,
                        fileName = item.file.name,
                        fileSize = item.file.size,
                        mimeType = item.file.mimeType,
                        index = 0,
                        thumbnailBase64 = thumbBase64
                    )

                    val ackDeferred = CompletableDeferred<Boolean>()
                    metaAckDeferred = ackDeferred

                    Log.d(TAG, "[SENDER] 📤 [2/3] Gửi MSG_FILE_META tới Receiver...")
                    sendSafeMessage(FileTransferProtocol.MSG_FILE_META, Gson().toJson(meta))

                    // 2. Chờ ACK từ Receiver (timeout 10s)
                    Log.d(TAG, "[SENDER] ⏳ Chờ Receiver phản hồi MSG_ACK_META (timeout 10s)...")
                    val isAcked = withTimeoutOrNull(10_000) { ackDeferred.await() } ?: false
                    if (!isAcked) {
                        throw IOException("Receiver không phản hồi ACK Meta trong 10s hoặc đã từ chối!")
                    }
                    Log.d(TAG, "[SENDER] ✅ Receiver đã xác nhận ACK_META, bắt đầu bơm dữ liệu qua Socket!")

                    // 3. Đọc dữ liệu file từ ContentResolver và gửi từng chunk có frame MSG_FILE_DATA
                    contentResolver.openInputStream(item.file.uri)?.use { fileIn ->
                        val buffer = ByteArray(FileTransferProtocol.BUFFER_SIZE)
                        var bytesRead: Int
                        var transferred = 0L
                        var lastLoggedPercent = -1
                        var lastLoggedTime = System.currentTimeMillis()
                        var bytesSinceLastLog = 0L
                        var lastUiUpdateTime = 0L

                        while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                            if (!_isPeerOnline.value) {
                                Log.w(TAG, "[SENDER] 🛑 Đối phương đã mất kết nối (Offline) -> Dừng gửi file!")
                                throw IOException(getString(R.string.transfer_error_timeout))
                            }

                            val currentItem = _items.value.find { it.id == item.id } as? ProgressItem.FileTransfer
                            if (currentItem?.status == TransferStatus1.CANCELED || currentItem?.status == TransferStatus1.DECLINED || cancelledFileIds.contains(item.id)) {
                                isCanceled = true
                                Log.w(TAG, "[SENDER] 🛑 Quá trình gửi file '${item.file.name}' bị hủy giữa chừng!")
                                break
                            }

                            // Gửi binary chunk bọc trong mutex an toàn
                            writeMutex.withLock {
                                FileTransferProtocol.sendChunk(output, buffer, bytesRead)
                            }
                            transferred += bytesRead
                            bytesSinceLastLog += bytesRead

                            val currentPercent = if (item.totalBytes > 0) ((transferred * 100) / item.totalBytes).toInt() else 0

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdateTime >= 150 || transferred == item.totalBytes) {
                                lastUiUpdateTime = now
                                notifyItemProgress(item.id, transferred, currentPercent)
                            }

                            val timeDiff = now - lastLoggedTime
                            if (currentPercent / 10 > lastLoggedPercent / 10 || (timeDiff >= 1500 && currentPercent != lastLoggedPercent)) {
                                val speedMBps = if (timeDiff > 0) {
                                    val mb = bytesSinceLastLog.toDouble() / (1024 * 1024)
                                    val sec = timeDiff.toDouble() / 1000
                                    String.format(Locale.US, "%.2f", mb / sec)
                                } else "0.00"

                                Log.d(TAG, "[SENDER] ⏳ Tiến độ '${item.file.name}': $currentPercent% (${formatSize(transferred)} / ${formatSize(item.totalBytes)}) - $speedMBps MB/s")
                                lastLoggedPercent = currentPercent
                                lastLoggedTime = now
                                bytesSinceLastLog = 0L
                            }
                        }
                    } ?: throw IOException("Không thể mở InputStream từ Uri: ${item.file.uri}")

                    if (!isCanceled) {
                        // 4. Báo hoàn tất truyền file
                        writeMutex.withLock {
                            FileTransferProtocol.sendMessage(
                                output,
                                FileTransferProtocol.MSG_TRANSFER_COMPLETE,
                                Gson().toJson(FileTransferProtocol.FileControlData(item.id, item.file.name))
                            )
                        }
                        updateFileStatus(item.id, TransferStatus1.COMPLETED)
                        val duration = System.currentTimeMillis() - startTime
                        val avgSpeed = if (duration > 0) {
                            val mb = item.file.size.toDouble() / (1024 * 1024)
                            val sec = duration.toDouble() / 1000
                            String.format(Locale.US, "%.2f", mb / sec)
                        } else "0.00"
                        Log.d(TAG, "[SENDER] 🎉 GỬI THÀNH CÔNG file: '${item.file.name}' trong ${duration}ms (Tốc độ TB: $avgSpeed MB/s)")
                        Log.d(TAG, "=================================================================")
                    } else {
                        val currentItem = _items.value.find { it.id == item.id } as? ProgressItem.FileTransfer
                        if (currentItem?.status == TransferStatus1.CANCELED) {
                            sendSafeMessage(
                                FileTransferProtocol.MSG_CANCEL_FILE,
                                Gson().toJson(FileTransferProtocol.FileControlData(item.id, item.file.name))
                            )
                        }
                    }
                } catch (e: Exception) {
                    updateFileError(item.id, e.message)
                    Log.e(TAG, "[SENDER] ❌ LỖI GỬI FILE '${item.file.name}': ${e.message}", e)
                    Log.d(TAG, "=================================================================")
                } finally {
                    metaAckDeferred = null
                }
            }
            isTransferWorkerActive = false
            Log.d(TAG, "[SENDER] 🏁 Worker gửi file hoàn thành tất cả công việc (queue rỗng).")
        }
    }

    // ===== ĐIỀU KHIỂN HỦY / THỬ LẠI =====
    fun cancelFile(fileId: String) {
        val item = _items.value.find { it.id == fileId } as? ProgressItem.FileTransfer ?: return
        Log.w(TAG, "[CONTROL] 🛑 Người dùng bấm HỦY file: '${item.file.name}' (ID: $fileId)")
        cancelledFileIds.add(fileId)
        updateFileStatus(fileId, TransferStatus1.CANCELED)

        serviceScope.launch {
            try {
                sendSafeMessage(
                    FileTransferProtocol.MSG_CANCEL_FILE,
                    Gson().toJson(FileTransferProtocol.FileControlData(fileId, item.file.name))
                )
                Log.d(TAG, "[CONTROL] 📤 Đã gửi MSG_CANCEL_FILE tới đối phương cho file: '${item.file.name}'")
            } catch (e: Exception) {
                Log.e(TAG, "[CONTROL] ❌ Gửi thông báo hủy file thất bại: ${e.message}", e)
            }
        }
    }

    fun retryFile(fileId: String) {
        val item = _items.value.find { it.id == fileId } as? ProgressItem.FileTransfer ?: return
        Log.d(TAG, "[CONTROL] 🔄 Người dùng bấm THỬ LẠI file: '${item.file.name}' (ID: $fileId)")
        cancelledFileIds.remove(fileId)
        val retryItem = item.copy(
            status = TransferStatus1.QUEUED,
            bytesTransferred = 0,
            progressPercent = 0
        )
        val currentList = _items.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == fileId }
        if (index != -1) {
            currentList[index] = retryItem
            _items.value = currentList
            recalculateOverallStats(currentList)
        }
        sendQueue.add(retryItem)
        if (!isTransferWorkerActive) startSendWorker()
    }

    // Quản lý đếm xuôi thời gian truyền file (Elapsed time từ 0)
    private var transferTimerJob: Job? = null
    private var transferStartTimeMs: Long = 0L
    private var currentElapsedSeconds: Long = 0L

    private fun checkAndManageTimer(list: List<ProgressItem> = _items.value) {
        val hasActiveTransfer = list.filterIsInstance<ProgressItem.FileTransfer>()
            .any { it.status == TransferStatus1.TRANSFERRING || it.status == TransferStatus1.QUEUED }

        if (hasActiveTransfer) {
            // Nếu có file đang truyền hoặc chờ trong hàng đợi mà timer chưa chạy -> bắt đầu đếm từ 0
            if (transferTimerJob == null || transferTimerJob?.isActive == false) {
                transferStartTimeMs = System.currentTimeMillis()
                currentElapsedSeconds = 0L
                _stats.value = _stats.value.copy(elapsedSeconds = 0L)

                transferTimerJob = serviceScope.launch {
                    while (isActive) {
                        val elapsed = (System.currentTimeMillis() - transferStartTimeMs) / 1000L
                        currentElapsedSeconds = elapsed
                        _stats.value = _stats.value.copy(elapsedSeconds = elapsed)
                        delay(500L) // Cập nhật mượt mà mỗi nửa giây
                    }
                }
            }
        } else {
            // Đã truyền xong tất cả file hoặc đã bị hủy -> dừng timer và giữ nguyên thời gian đếm được
            transferTimerJob?.cancel()
            transferTimerJob = null
        }
    }

    private fun recalculateOverallStats(list: List<ProgressItem> = _items.value) {
        val fileTransfers = list.filterIsInstance<ProgressItem.FileTransfer>()
        val totalTransferred = fileTransfers.sumOf { it.bytesTransferred }

        _stats.value = _stats.value.copy(
            totalTransferredBytes = totalTransferred,
            elapsedSeconds = currentElapsedSeconds
        )
        checkAndManageTimer(list)
    }

    private fun appendItem(item: ProgressItem) {
        _items.value = _items.value + item
        if (item is ProgressItem.FileTransfer) {
            recalculateOverallStats(_items.value)
        }
    }

    private fun appendItems(newItems: List<ProgressItem>) {
        _items.value = _items.value + newItems
        if (newItems.any { it is ProgressItem.FileTransfer }) {
            recalculateOverallStats(_items.value)
        }
    }

    private fun updateFileStatus(fileId: String, status: TransferStatus1) {
        val currentList = _items.value
        val index = currentList.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val item = currentList[index] as? ProgressItem.FileTransfer ?: return
            val updatedList = currentList.toMutableList()
            updatedList[index] = item.copy(status = status)
            _items.value = updatedList
            recalculateOverallStats(updatedList)
        }
    }

    private fun notifyItemProgress(fileId: String, bytesTransferred: Long, progressPercent: Int) {
        val currentList = _items.value
        val index = currentList.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val oldItem = currentList[index] as? ProgressItem.FileTransfer ?: return
            val updatedList = currentList.toMutableList()
            updatedList[index] = oldItem.copy(
                bytesTransferred = bytesTransferred,
                progressPercent = progressPercent
            )
            _items.value = updatedList
            recalculateOverallStats(updatedList)
        }
    }

    private fun updateFileCompleted(fileId: String, savedPath: String) {
        val currentList = _items.value
        val index = currentList.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val oldItem = currentList[index] as? ProgressItem.FileTransfer ?: return
            val updatedList = currentList.toMutableList()
            updatedList[index] = oldItem.copy(
                status = TransferStatus1.COMPLETED,
                savedPath = savedPath
            )
            _items.value = updatedList
            recalculateOverallStats(updatedList)
        }
    }

    private fun updateFileError(fileId: String, errorMessage: String?) {
        val currentList = _items.value
        val index = currentList.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val oldItem = currentList[index] as? ProgressItem.FileTransfer ?: return
            val updatedList = currentList.toMutableList()
            updatedList[index] = oldItem.copy(
                status = TransferStatus1.ERROR,
                errorMessage = errorMessage
            )
            _items.value = updatedList
            recalculateOverallStats(updatedList)
        }
    }

    fun disconnectSession() {
        Log.d(TAG, "[SESSION] 🔌 Ngắt kết nối phiên làm việc TransferService (Đóng socket, serverSocket, stopSelf)...")
        isConnected.value = false
        _isPeerOnline.value = false
        metaAckDeferred?.complete(false)
        cleanupCurrentReceivingFile()
        transferTimerJob?.cancel()
        transferTimerJob = null
        currentElapsedSeconds = 0L
        _stats.value = OverallStats()
        try {
            activeSocket?.close()
        } catch (e: Exception) { }
        try {
            serverSocket?.close()
        } catch (e: Exception) { }
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "[SERVICE] 🛑 TransferService onDestroy()")
        serviceScope.cancel()
        disconnectSession()
        super.onDestroy()
    }
}
