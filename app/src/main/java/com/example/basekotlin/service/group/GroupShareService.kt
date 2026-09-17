package com.example.basekotlin.service.group

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.basekotlin.R
import com.example.basekotlin.ui.group_share.model.GroupMemberModel
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.model.OverallStats
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.model.TransferStatus1
import com.example.basekotlin.service.transfer.HotspotManager
import com.example.basekotlin.util.transfer.FileTransferProtocol
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.QrCodeHelper
import com.example.basekotlin.util.transfer.TransferNotificationHelper
import com.example.basekotlin.util.transfer.WifiHelper
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class GroupShareService : Service() {

    companion object {
        private const val TAG = "GroupShareService"
        const val ACTION_START_HOST = "ACTION_START_HOST"
        const val ACTION_START_CLIENT = "ACTION_START_CLIENT"
        const val ACTION_DISCONNECT = "ACTION_DISCONNECT"

        const val EXTRA_HOST_IP = "EXTRA_HOST_IP"
        const val EXTRA_PORT = "EXTRA_PORT"

        fun startHost(context: Context, port: Int = 8888) {
            val intent = Intent(context, GroupShareService::class.java).apply {
                action = ACTION_START_HOST
                putExtra(EXTRA_PORT, port)
            }
            context.startForegroundService(intent)
        }

        fun startClient(context: Context, hostIp: String, hostPort: Int = 8888) {
            val intent = Intent(context, GroupShareService::class.java).apply {
                action = ACTION_START_CLIENT
                putExtra(EXTRA_HOST_IP, hostIp)
                putExtra(EXTRA_PORT, hostPort)
            }
            context.startForegroundService(intent)
        }

        fun disconnect(context: Context) {
            val intent = Intent(context, GroupShareService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val gson = Gson()

    private val binder = ServiceBinder()
    inner class ServiceBinder : Binder() {
        fun getService(): GroupShareService = this@GroupShareService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // StateFlows
    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _hostIp = MutableStateFlow("")
    val hostIp: StateFlow<String> = _hostIp.asStateFlow()

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private val _connectionInfo = MutableStateFlow<ConnectionInfo?>(null)
    val connectionInfo: StateFlow<ConnectionInfo?> = _connectionInfo.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMemberModel>>(emptyList())
    val members: StateFlow<List<GroupMemberModel>> = _members.asStateFlow()

    private val _memberCount = MutableStateFlow(1)
    val memberCount: StateFlow<Int> = _memberCount.asStateFlow()

    private val _items = MutableStateFlow<List<ProgressItem>>(emptyList())
    val items: StateFlow<List<ProgressItem>> = _items.asStateFlow()

    private val _stats = MutableStateFlow(OverallStats())
    val stats: StateFlow<OverallStats> = _stats.asStateFlow()

    // Host variables
    private var serverSocket: ServerSocket? = null
    private var hotspotManager: HotspotManager? = null
    private val clientSessions = ConcurrentHashMap<String, ClientSession>()

    // Client variables
    private var clientSocket: Socket? = null
    private var clientInput: DataInputStream? = null
    private var clientOutput: DataOutputStream? = null
    private val clientWriteMutex = Mutex()

    private var isRunning = AtomicBoolean(false)

    // Quản lý trạng thái Cancel & Declined
    private val cancelledFileIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val fileDeclinedMembers = ConcurrentHashMap<String, MutableSet<String>>()

    // Quản lý thống kê thời gian truyền & dung lượng
    private val fileStartTimes = ConcurrentHashMap<String, Long>()
    private val fileCompletedDurations = ConcurrentHashMap<String, Long>()

    data class ClientSession(
        val id: String,
        var deviceName: String,
        val ip: String,
        val socket: Socket,
        val input: DataInputStream,
        val output: DataOutputStream,
        val writeMutex: Mutex = Mutex()
    ) {
        suspend fun sendMessage(type: Int, payload: String) {
            writeMutex.withLock {
                try {
                    val bytes = payload.toByteArray(Charsets.UTF_8)
                    output.writeInt(type)
                    output.writeInt(bytes.size)
                    output.write(bytes)
                    output.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi gửi tin tới client $deviceName: ${e.message}")
                }
            }
        }

        suspend fun sendChunk(buffer: ByteArray, length: Int) {
            writeMutex.withLock {
                try {
                    output.writeInt(FileTransferProtocol.MSG_FILE_DATA)
                    output.writeInt(length)
                    output.write(buffer, 0, length)
                    output.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi gửi chunk tới client $deviceName: ${e.message}")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        TransferNotificationHelper.createChannel(this)
        val notif = TransferNotificationHelper.createTransferNotification(
            this,
            getString(R.string.group_share),
            getString(R.string.group_desc)
        )
        startForeground(TransferNotificationHelper.NOTIFICATION_ID, notif.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_HOST -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8888)
                startHostMode(port)
            }
            ACTION_START_CLIENT -> {
                val ip = intent.getStringExtra(EXTRA_HOST_IP) ?: ""
                val port = intent.getIntExtra(EXTRA_PORT, 8888)
                startClientMode(ip, port)
            }
            ACTION_DISCONNECT -> {
                cleanupAndStop()
            }
        }
        return START_NOT_STICKY
    }

    // ==========================================
    // ============ STATS CALCULATION ===========
    // ==========================================

    private fun recalculateOverallStats() {
        val completedFiles = _items.value.filterIsInstance<ProgressItem.FileTransfer>()
            .filter { it.status == TransferStatus1.COMPLETED }

        // tvTotalSize: khi nhận hay truyền file xong thì cộng thêm vào tổng
        val totalBytes = completedFiles.sumOf { it.totalBytes }

        // tvTotalTime: nhận hay truyền file xong cộng tổng vào thời gian truyền file
        val totalTime = completedFiles.sumOf { fileCompletedDurations[it.id] ?: 0L }

        _stats.value = OverallStats(
            totalTransferredBytes = totalBytes,
            elapsedSeconds = totalTime
        )
    }

    // ==========================================
    // ============ HOST LOGIC ==================
    // ==========================================

    private fun startHostMode(port: Int) {
        if (isRunning.getAndSet(true)) return
        _isHost.value = true

        val myDeviceName = NetworkUtils.getDeviceName()
        _groupName.value = myDeviceName

        val isWifiConnected = NetworkUtils.isWifiConnected(this)
        if (isWifiConnected) {
            // Có Wi-Fi sẵn
            val ip = NetworkUtils.getWifiIpAddress()
            _hostIp.value = ip
            val wifiName = NetworkUtils.getConnectedWifiName(this)
            val connectionInfo = ConnectionInfo(
                ssid = wifiName,
                password = "",
                ipAddress = ip,
                port = port,
                deviceName = myDeviceName
            )
            _connectionInfo.value = connectionInfo
            serviceScope.launch {
                val bitmap = QrCodeHelper.generateQrBitmap(connectionInfo)
                _qrBitmap.value = bitmap
            }
            initHostMemberAndServer(port, ip)
        } else {
            // Không có Wi-Fi -> Bật LocalOnlyHotspot
            Log.d(TAG, "[HOST] Chế độ NO WIFI -> Bắt đầu khởi tạo HotspotManager")
            hotspotManager = HotspotManager(this)
            hotspotManager?.startHotspot(
                port = port,
                listener = object : HotspotManager.HotspotListener {
                    override fun onHotspotStarted(connectionInfo: ConnectionInfo) {
                        Log.d(TAG, "[HOST] Hotspot sẵn sàng: ${connectionInfo.ssid} | IP: ${connectionInfo.ipAddress}")
                        _hostIp.value = connectionInfo.ipAddress
                        _connectionInfo.value = connectionInfo
                        serviceScope.launch {
                            val bitmap = QrCodeHelper.generateQrBitmap(connectionInfo)
                            _qrBitmap.value = bitmap
                        }
                        initHostMemberAndServer(port, connectionInfo.ipAddress)
                    }

                    override fun onHotspotFailed(errorCode: Int) {
                        Log.e(TAG, "[HOST] Bật Hotspot thất bại, errorCode: $errorCode")
                        _isOnline.value = false
                    }

                    override fun onHotspotStopped() {
                        Log.d(TAG, "[HOST] Hotspot đã dừng")
                    }
                }
            )
        }
    }

    private fun initHostMemberAndServer(port: Int, ip: String) {
        val myDeviceName = NetworkUtils.getDeviceName()
        val hostMember = GroupMemberModel(
            deviceName = myDeviceName,
            ipAddress = ip,
            isHost = true,
            subInfo = ip
        )
        _members.value = listOf(hostMember)
        _memberCount.value = 1

        serviceScope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                Log.d(TAG, "[HOST] ServerSocket khởi động tại port $port, IP: $ip")

                while (isActive && !serverSocket!!.isClosed) {
                    val socket = serverSocket!!.accept()
                    val clientIp = socket.inetAddress.hostAddress ?: ""
                    Log.d(TAG, "[HOST] Thiết bị mới kết nối: $clientIp")

                    val sessionId = UUID.randomUUID().toString()
                    val dis = DataInputStream(BufferedInputStream(socket.getInputStream()))
                    val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                    val session = ClientSession(sessionId, "Member", clientIp, socket, dis, dos)
                    clientSessions[sessionId] = session

                    launch {
                        handleClientSession(session)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[HOST] ServerSocket kết thúc: ${e.message}")
            }
        }
    }

    private suspend fun handleClientSession(session: ClientSession) {
        try {
            while (serviceScope.isActive && !session.socket.isClosed) {
                val msgType = session.input.readInt()
                val len = session.input.readInt()
                if (len < 0 || len > FileTransferProtocol.MAX_PAYLOAD_SIZE) {
                    Log.e(TAG, "[HOST] Gói tin không hợp lệ từ ${session.deviceName}: len=$len")
                    break
                }
                val payloadBytes = ByteArray(len)
                session.input.readFully(payloadBytes)
                val payload = String(payloadBytes, Charsets.UTF_8)

                when (msgType) {
                    FileTransferProtocol.MSG_HANDSHAKE -> {
                        val handshake = gson.fromJson(payload, FileTransferProtocol.HandshakeData::class.java)
                        session.deviceName = handshake.deviceName
                        Log.d(TAG, "[HOST] Handshake từ ${session.deviceName} (${session.ip})")

                        updateHostMemberList()
                        appendItem(
                            ProgressItem.SystemEvent(
                                id = UUID.randomUUID().toString(),
                                message = getString(R.string.device_joined_fmt, session.deviceName)
                            )
                        )
                        // Gửi ACK Handshake
                        session.sendMessage(
                            FileTransferProtocol.MSG_ACK_HANDSHAKE,
                            gson.toJson(FileTransferProtocol.HandshakeData(_groupName.value, 0, 0L))
                        )
                        // Broadcast danh sách thành viên mới tới tất cả Client
                        broadcastMemberList()
                    }

                    FileTransferProtocol.MSG_CHAT_TEXT -> {
                        val chat = gson.fromJson(payload, FileTransferProtocol.ChatMessageData::class.java)
                        Log.d(TAG, "[HOST] Nhận tin nhắn chat từ ${session.deviceName}: ${chat.text}")

                        val textItem = ProgressItem.TextMessage(
                            id = chat.messageId,
                            text = chat.text,
                            isMe = false,
                            timeFormatted = timeFormat.format(Date(chat.timestamp)),
                            senderName = session.deviceName
                        )
                        appendItem(textItem)

                        // Relay broadcast tin nhắn đến tất cả các Client khác
                        relayMessageToOthers(session.id, msgType, payload)
                    }

                    FileTransferProtocol.MSG_FILE_META -> {
                        val meta = gson.fromJson(payload, FileTransferProtocol.FileMetaData::class.java)
                        Log.d(TAG, "[HOST] Nhận File Meta từ ${session.deviceName}: ${meta.fileName}")

                        cancelledFileIds.remove(meta.fileId)
                        fileDeclinedMembers.remove(meta.fileId)

                        // Relay meta tới tất cả các Client khác
                        relayMessageToOthers(session.id, msgType, payload)

                        // Chuẩn bị file lưu cục bộ ở máy Host
                        val saveDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShareFile")
                        if (!saveDir.exists()) saveDir.mkdirs()
                        val targetFile = File(saveDir, meta.fileName)

                        fileStartTimes[meta.fileId] = System.currentTimeMillis()

                        val existingIndex = _items.value.indexOfFirst { it.id == meta.fileId }
                        if (existingIndex != -1) {
                            val old = _items.value[existingIndex] as? ProgressItem.FileTransfer
                            if (old != null) {
                                val updated = _items.value.toMutableList()
                                updated[existingIndex] = old.copy(
                                    status = TransferStatus1.TRANSFERRING,
                                    bytesTransferred = 0,
                                    progressPercent = 0,
                                    errorMessage = null
                                )
                                _items.value = updated
                            }
                        } else {
                            val receiveItem = ProgressItem.FileTransfer(
                                id = meta.fileId,
                                file = TransferFile(android.net.Uri.fromFile(targetFile), meta.fileName, meta.fileSize, meta.mimeType),
                                isMe = false,
                                status = TransferStatus1.TRANSFERRING,
                                totalBytes = meta.fileSize,
                                thumbnailBase64 = meta.thumbnailBase64,
                                senderName = session.deviceName
                            )
                            appendItem(receiveItem)
                        }

                        session.sendMessage(FileTransferProtocol.MSG_ACK_META, "{}")

                        // Đọc các chunks dữ liệu
                        receiveAndRelayFileData(session, targetFile, meta)
                    }

                    FileTransferProtocol.MSG_CANCEL_FILE -> {
                        // Client hủy nhận file này
                        val control = gson.fromJson(payload, FileTransferProtocol.FileControlData::class.java)
                        val fileId = control.fileId
                        Log.w(TAG, "[HOST] Member ${session.deviceName} đã cancel file: $fileId")

                        val declinedSet = fileDeclinedMembers.getOrPut(fileId) { Collections.synchronizedSet(mutableSetOf()) }
                        declinedSet.add(session.id)

                        // Nếu TẤT CẢ Member đều đã cancel: Host hiển thị DECLINED (Từ chối)
                        if (declinedSet.size >= clientSessions.size) {
                            cancelledFileIds.add(fileId)
                            updateFileStatus(fileId, TransferStatus1.DECLINED)
                            Log.w(TAG, "[HOST] Tất cả member đã cancel -> file $fileId chuyển sang DECLINED")
                        }
                    }

                    FileTransferProtocol.MSG_PING -> {
                        session.sendMessage(FileTransferProtocol.MSG_PONG, "{}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "[HOST] Client ngắt kết nối: ${session.deviceName} (${e.message})")
        } finally {
            clientSessions.remove(session.id)
            try { session.socket.close() } catch (_: Exception) {}

            updateHostMemberList()
            appendItem(
                ProgressItem.SystemEvent(
                    id = UUID.randomUUID().toString(),
                    message = getString(R.string.device_left_fmt, session.deviceName)
                )
            )
            broadcastMemberList()
        }
    }

    private suspend fun receiveAndRelayFileData(
        senderSession: ClientSession,
        targetFile: File,
        meta: FileTransferProtocol.FileMetaData
    ) {
        var fos: FileOutputStream? = null
        var totalRead = 0L
        var isCancelled = false

        try {
            fos = FileOutputStream(targetFile)
            val buffer = ByteArray(FileTransferProtocol.BUFFER_SIZE)

            while (totalRead < meta.fileSize) {
                if (cancelledFileIds.contains(meta.fileId)) {
                    Log.w(TAG, "[HOST] File ${meta.fileId} đã bị hủy khi đang nhận/relay")
                    isCancelled = true
                    break
                }

                val type = senderSession.input.readInt()

                // Xử lý nếu bên gửi bất ngờ gửi MSG_CANCEL_FILE
                if (type == FileTransferProtocol.MSG_CANCEL_FILE) {
                    val len = senderSession.input.readInt()
                    val payloadBytes = ByteArray(len)
                    senderSession.input.readFully(payloadBytes)
                    cancelledFileIds.add(meta.fileId)
                    isCancelled = true
                    relayMessageToOthers(senderSession.id, FileTransferProtocol.MSG_CANCEL_FILE, String(payloadBytes, Charsets.UTF_8))
                    break
                }

                // Xử lý nếu có tin nhắn chat văn bản đến trong khi đang truyền file
                if (type == FileTransferProtocol.MSG_CHAT_TEXT) {
                    val len = senderSession.input.readInt()
                    val payloadBytes = ByteArray(len)
                    senderSession.input.readFully(payloadBytes)
                    val payload = String(payloadBytes, Charsets.UTF_8)
                    val chat = gson.fromJson(payload, FileTransferProtocol.ChatMessageData::class.java)
                    appendItem(ProgressItem.TextMessage(
                        id = chat.messageId,
                        text = chat.text,
                        isMe = false,
                        timeFormatted = timeFormat.format(Date(chat.timestamp)),
                        senderName = senderSession.deviceName
                    ))
                    relayMessageToOthers(senderSession.id, type, payload)
                    continue
                }

                if (type == FileTransferProtocol.MSG_PING) {
                    val len = senderSession.input.readInt()
                    if (len > 0) senderSession.input.skipBytes(len)
                    senderSession.sendMessage(FileTransferProtocol.MSG_PONG, "{}")
                    continue
                }

                if (type != FileTransferProtocol.MSG_FILE_DATA) {
                    val len = senderSession.input.readInt()
                    if (len > 0) senderSession.input.skipBytes(len)
                    break
                }

                val chunkLen = senderSession.input.readInt()
                if (chunkLen <= 0) break

                senderSession.input.readFully(buffer, 0, chunkLen)
                fos.write(buffer, 0, chunkLen)
                totalRead += chunkLen

                // Relay chunk tới tất cả các client khác chưa decline file này
                val declinedSet = fileDeclinedMembers[meta.fileId] ?: emptySet()
                clientSessions.values.filter { it.id != senderSession.id && !declinedSet.contains(it.id) }.forEach { other ->
                    other.sendChunk(buffer, chunkLen)
                }

                // Cập nhật tiến trình trên UI Host
                val percent = if (meta.fileSize > 0) ((totalRead * 100) / meta.fileSize).toInt() else 0
                updateItemProgress(meta.fileId, totalRead, percent)
            }

            fos.flush()

            if (isCancelled || cancelledFileIds.contains(meta.fileId)) {
                updateFileStatus(meta.fileId, TransferStatus1.CANCELED)
                try { targetFile.delete() } catch (_: Exception) {}
            } else if (totalRead >= meta.fileSize) {
                val startTime = fileStartTimes[meta.fileId] ?: System.currentTimeMillis()
                val durationSec = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(1)
                fileCompletedDurations[meta.fileId] = durationSec

                updateItemCompleted(meta.fileId, targetFile.absolutePath)
                MediaScannerConnection.scanFile(this, arrayOf(targetFile.absolutePath), arrayOf(meta.mimeType)) { _, _ -> }

                // Gửi ACK File về cho Sender
                senderSession.sendMessage(FileTransferProtocol.MSG_ACK_FILE, gson.toJson(FileTransferProtocol.FileAck(meta.fileName, totalRead, true, meta.fileId)))
                // Relay MSG_TRANSFER_COMPLETE tới các client khác
                relayMessageToOthers(senderSession.id, FileTransferProtocol.MSG_TRANSFER_COMPLETE, gson.toJson(FileTransferProtocol.FileControlData(meta.fileId, meta.fileName)))
            } else {
                updateFileStatus(meta.fileId, TransferStatus1.ERROR)
                try { targetFile.delete() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HOST] Lỗi nhận/relay file: ${e.message}")
            updateItemError(meta.fileId, e.message)
        } finally {
            try { fos?.close() } catch (_: Exception) {}
        }
    }

    private fun updateHostMemberList() {
        val list = mutableListOf<GroupMemberModel>()
        val myName = NetworkUtils.getDeviceName()
        list.add(GroupMemberModel(myName, _hostIp.value, isHost = true, subInfo = _hostIp.value))
        clientSessions.values.forEach { s ->
            list.add(GroupMemberModel(s.deviceName, s.ip, isHost = false, subInfo = s.ip))
        }
        _members.value = list
        _memberCount.value = list.size
    }

    private suspend fun broadcastMemberList() {
        val json = gson.toJson(_members.value)
        clientSessions.values.forEach { session ->
            session.sendMessage(FileTransferProtocol.MSG_ACK_HANDSHAKE, json)
        }
    }

    private suspend fun relayMessageToOthers(senderId: String, type: Int, payload: String) {
        clientSessions.values.filter { it.id != senderId }.forEach { session ->
            session.sendMessage(type, payload)
        }
    }

    // ==========================================
    // ============ CLIENT LOGIC ================
    // ==========================================

    private fun startClientMode(hostIp: String, hostPort: Int) {
        if (isRunning.getAndSet(true)) return
        _isHost.value = false
        _hostIp.value = hostIp
        _groupName.value = getString(R.string.group_name_default)

        val myDeviceName = NetworkUtils.getDeviceName()
        _members.value = listOf(
            GroupMemberModel(getString(R.string.role_host), hostIp, isHost = true, subInfo = hostIp),
            GroupMemberModel(myDeviceName, NetworkUtils.getWifiIpAddress(), isHost = false, subInfo = NetworkUtils.getWifiIpAddress())
        )
        _memberCount.value = 2

        serviceScope.launch {
            try {
                Log.d(TAG, "[CLIENT] Đang kết nối tới Host $hostIp:$hostPort...")
                val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val wifiNetwork = WifiHelper.activeWifiNetwork ?: cm.allNetworks.firstOrNull { net ->
                    val caps = cm.getNetworkCapabilities(net)
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }
                if (wifiNetwork != null) {
                    Log.d(TAG, "[CLIENT] Bind Socket vào Wi-Fi Network của Hotspot")
                    try {
                        cm.bindProcessToNetwork(wifiNetwork)
                    } catch (e: Exception) {
                        Log.e(TAG, "[CLIENT] Lỗi bindProcessToNetwork", e)
                    }
                }
                val socket = if (wifiNetwork != null) {
                    wifiNetwork.socketFactory.createSocket()
                } else {
                    Socket()
                }
                wifiNetwork?.bindSocket(socket)
                socket.connect(InetSocketAddress(hostIp, hostPort), 10000)

                clientSocket = socket
                clientInput = DataInputStream(BufferedInputStream(socket.getInputStream()))
                clientOutput = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

                // Gửi Handshake
                val handshake = FileTransferProtocol.HandshakeData(myDeviceName, 0, 0L)
                sendClientMessage(FileTransferProtocol.MSG_HANDSHAKE, gson.toJson(handshake))

                _isOnline.value = true
                Log.d(TAG, "[CLIENT] Kết nối thành công tới Host!")

                while (isActive && !socket.isClosed) {
                    val msgType = clientInput!!.readInt()
                    val len = clientInput!!.readInt()
                    if (len < 0 || len > FileTransferProtocol.MAX_PAYLOAD_SIZE) break
                    val payloadBytes = ByteArray(len)
                    clientInput!!.readFully(payloadBytes)
                    val payload = String(payloadBytes, Charsets.UTF_8)

                    when (msgType) {
                        FileTransferProtocol.MSG_ACK_HANDSHAKE -> {
                            try {
                                val memberList = gson.fromJson(payload, Array<GroupMemberModel>::class.java)
                                if (memberList != null && memberList.isNotEmpty()) {
                                    _members.value = memberList.toList()
                                    _memberCount.value = memberList.size
                                }
                            } catch (_: Exception) {}
                        }

                        FileTransferProtocol.MSG_CHAT_TEXT -> {
                            val chat = gson.fromJson(payload, FileTransferProtocol.ChatMessageData::class.java)
                            val textItem = ProgressItem.TextMessage(
                                id = chat.messageId,
                                text = chat.text,
                                isMe = false,
                                timeFormatted = timeFormat.format(Date(chat.timestamp)),
                                senderName = "Member"
                            )
                            appendItem(textItem)
                        }

                        FileTransferProtocol.MSG_FILE_META -> {
                            val meta = gson.fromJson(payload, FileTransferProtocol.FileMetaData::class.java)
                            cancelledFileIds.remove(meta.fileId)
                            fileDeclinedMembers.remove(meta.fileId)

                            val saveDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShareFile")
                            if (!saveDir.exists()) saveDir.mkdirs()
                            val targetFile = File(saveDir, meta.fileName)

                            fileStartTimes[meta.fileId] = System.currentTimeMillis()

                            val existingIndex = _items.value.indexOfFirst { it.id == meta.fileId }
                            if (existingIndex != -1) {
                                val old = _items.value[existingIndex] as? ProgressItem.FileTransfer
                                if (old != null) {
                                    val updated = _items.value.toMutableList()
                                    updated[existingIndex] = old.copy(
                                        status = TransferStatus1.TRANSFERRING,
                                        bytesTransferred = 0,
                                        progressPercent = 0,
                                        errorMessage = null
                                    )
                                    _items.value = updated
                                }
                            } else {
                                val receiveItem = ProgressItem.FileTransfer(
                                    id = meta.fileId,
                                    file = TransferFile(android.net.Uri.fromFile(targetFile), meta.fileName, meta.fileSize, meta.mimeType),
                                    isMe = false,
                                    status = TransferStatus1.TRANSFERRING,
                                    totalBytes = meta.fileSize,
                                    thumbnailBase64 = meta.thumbnailBase64,
                                    senderName = "Host"
                                )
                                appendItem(receiveItem)
                            }

                            receiveClientFileData(targetFile, meta)
                        }

                        FileTransferProtocol.MSG_CANCEL_FILE -> {
                            // Khi Host cancel -> Member bị cancel
                            val control = gson.fromJson(payload, FileTransferProtocol.FileControlData::class.java)
                            Log.w(TAG, "[CLIENT] Nhận lệnh Cancel từ Host cho file ${control.fileId}")
                            cancelledFileIds.add(control.fileId)
                            updateFileStatus(control.fileId, TransferStatus1.CANCELED)
                        }

                        FileTransferProtocol.MSG_PING -> {
                            sendClientMessage(FileTransferProtocol.MSG_PONG, "{}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[CLIENT] Mất kết nối tới Host: ${e.message}")
                _isOnline.value = false
            }
        }
    }

    private suspend fun receiveClientFileData(targetFile: File, meta: FileTransferProtocol.FileMetaData) {
        var fos: FileOutputStream? = null
        var totalRead = 0L
        var isCancelledByHost = false

        try {
            fos = FileOutputStream(targetFile)
            val buffer = ByteArray(FileTransferProtocol.BUFFER_SIZE)

            while (totalRead < meta.fileSize) {
                if (cancelledFileIds.contains(meta.fileId)) {
                    Log.w(TAG, "[CLIENT] File ${meta.fileId} đã bị hủy trong quá trình nhận")
                    break
                }

                val type = clientInput!!.readInt()

                // NẾU HOST GỬI LỆNH CANCEL TRONG KHI ĐANG NHẬN DỮ LIỆU:
                if (type == FileTransferProtocol.MSG_CANCEL_FILE) {
                    val len = clientInput!!.readInt()
                    val payloadBytes = ByteArray(len)
                    clientInput!!.readFully(payloadBytes)
                    Log.w(TAG, "[CLIENT] Nhận lệnh Cancel từ Host khi đang nhận file ${meta.fileId}")
                    cancelledFileIds.add(meta.fileId)
                    isCancelledByHost = true
                    break
                }

                // Nếu có tin nhắn chat tới trong khi đang nhận file
                if (type == FileTransferProtocol.MSG_CHAT_TEXT) {
                    val len = clientInput!!.readInt()
                    val payloadBytes = ByteArray(len)
                    clientInput!!.readFully(payloadBytes)
                    val chat = gson.fromJson(String(payloadBytes, Charsets.UTF_8), FileTransferProtocol.ChatMessageData::class.java)
                    val textItem = ProgressItem.TextMessage(
                        id = chat.messageId,
                        text = chat.text,
                        isMe = false,
                        timeFormatted = timeFormat.format(Date(chat.timestamp)),
                        senderName = "Member"
                    )
                    appendItem(textItem)
                    continue
                }

                if (type == FileTransferProtocol.MSG_PING) {
                    val len = clientInput!!.readInt()
                    if (len > 0) clientInput!!.skipBytes(len)
                    sendClientMessage(FileTransferProtocol.MSG_PONG, "{}")
                    continue
                }

                if (type != FileTransferProtocol.MSG_FILE_DATA) {
                    val len = clientInput!!.readInt()
                    if (len > 0) clientInput!!.skipBytes(len)
                    break
                }

                val chunkLen = clientInput!!.readInt()
                if (chunkLen <= 0) break

                clientInput!!.readFully(buffer, 0, chunkLen)
                fos.write(buffer, 0, chunkLen)
                totalRead += chunkLen

                val percent = if (meta.fileSize > 0) ((totalRead * 100) / meta.fileSize).toInt() else 0
                updateItemProgress(meta.fileId, totalRead, percent)
            }

            fos.flush()

            if (isCancelledByHost || cancelledFileIds.contains(meta.fileId)) {
                // CẬP NHẬT TRẠNG THÁI CANCELED, XÓA FILE TẠM, KHÔNG GỌI COMPLETED
                updateFileStatus(meta.fileId, TransferStatus1.CANCELED)
                try { targetFile.delete() } catch (_: Exception) {}
            } else if (totalRead >= meta.fileSize) {
                val startTime = fileStartTimes[meta.fileId] ?: System.currentTimeMillis()
                val durationSec = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(1)
                fileCompletedDurations[meta.fileId] = durationSec

                updateItemCompleted(meta.fileId, targetFile.absolutePath)
                MediaScannerConnection.scanFile(this, arrayOf(targetFile.absolutePath), arrayOf(meta.mimeType)) { _, _ -> }
            } else {
                updateFileStatus(meta.fileId, TransferStatus1.ERROR)
                try { targetFile.delete() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "[CLIENT] Lỗi ghi file nhận: ${e.message}")
            updateItemError(meta.fileId, e.message)
        } finally {
            try { fos?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun sendClientMessage(type: Int, payload: String) {
        clientWriteMutex.withLock {
            try {
                val out = clientOutput ?: return
                val bytes = payload.toByteArray(Charsets.UTF_8)
                out.writeInt(type)
                out.writeInt(bytes.size)
                out.write(bytes)
                out.flush()
            } catch (e: Exception) {
                Log.e(TAG, "[CLIENT] Lỗi gửi message: ${e.message}")
            }
        }
    }

    // ==========================================
    // ============ SENDING APIS ================
    // ==========================================

    fun sendChatMessage(text: String) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val chatData = FileTransferProtocol.ChatMessageData(id, text, now)
        val payload = gson.toJson(chatData)

        // Hiển thị ở máy mình bên phải
        appendItem(
            ProgressItem.TextMessage(
                id = id,
                text = text,
                isMe = true,
                timeFormatted = timeFormat.format(Date(now)),
                senderName = NetworkUtils.getDeviceName()
            )
        )

        serviceScope.launch {
            if (_isHost.value) {
                // Host broadcast tới tất cả các Client
                clientSessions.values.forEach { session ->
                    session.sendMessage(FileTransferProtocol.MSG_CHAT_TEXT, payload)
                }
            } else {
                // Client gửi lên Host
                sendClientMessage(FileTransferProtocol.MSG_CHAT_TEXT, payload)
            }
        }
    }

    fun addFilesToSend(files: List<TransferFile>) {
        serviceScope.launch {
            for (file in files) {
                val fileId = UUID.randomUUID().toString()
                cancelledFileIds.remove(fileId)
                fileDeclinedMembers.remove(fileId)

                val sendItem = ProgressItem.FileTransfer(
                    id = fileId,
                    file = file,
                    isMe = true,
                    status = TransferStatus1.TRANSFERRING,
                    totalBytes = file.size,
                    senderName = NetworkUtils.getDeviceName()
                )
                appendItem(sendItem)
                sendSingleTransferFile(sendItem)
            }
        }
    }

    private suspend fun sendSingleTransferFile(sendItem: ProgressItem.FileTransfer) {
        val fileId = sendItem.id
        val file = sendItem.file
        cancelledFileIds.remove(fileId)
        fileDeclinedMembers.remove(fileId)
        fileStartTimes[fileId] = System.currentTimeMillis()

        val meta = FileTransferProtocol.FileMetaData(
            fileName = file.name,
            fileSize = file.size,
            mimeType = file.mimeType,
            fileId = fileId
        )
        val metaPayload = gson.toJson(meta)

        // 1. Gửi Meta
        if (_isHost.value) {
            clientSessions.values.forEach { it.sendMessage(FileTransferProtocol.MSG_FILE_META, metaPayload) }
        } else {
            sendClientMessage(FileTransferProtocol.MSG_FILE_META, metaPayload)
        }

        // 2. Gửi Stream Data
        try {
            val inputStream = contentResolver.openInputStream(file.uri) ?: FileInputStream(file.uri.path ?: "")
            val buffer = ByteArray(FileTransferProtocol.BUFFER_SIZE)
            var totalSent = 0L
            var read: Int
            var isCancelled = false

            while (inputStream.read(buffer).also { read = it } != -1) {
                if (cancelledFileIds.contains(fileId)) {
                    Log.w(TAG, "File $fileId đã bị Cancel, dừng gửi stream")
                    isCancelled = true
                    break
                }

                if (_isHost.value) {
                    val declinedSet = fileDeclinedMembers[fileId] ?: emptySet()
                    val activeClients = clientSessions.values.filter { !declinedSet.contains(it.id) }

                    // Nếu tất cả Member đã cancel thì Host hiển thị DECLINED và dừng gửi
                    if (activeClients.isEmpty() && clientSessions.isNotEmpty()) {
                        Log.w(TAG, "Tất cả member đã cancel file $fileId -> Host hiển thị DECLINED")
                        updateFileStatus(fileId, TransferStatus1.DECLINED)
                        isCancelled = true
                        break
                    }

                    activeClients.forEach { it.sendChunk(buffer, read) }
                } else {
                    clientWriteMutex.withLock {
                        clientOutput?.writeInt(FileTransferProtocol.MSG_FILE_DATA)
                        clientOutput?.writeInt(read)
                        clientOutput?.write(buffer, 0, read)
                        clientOutput?.flush()
                    }
                }
                totalSent += read
                val percent = if (file.size > 0) ((totalSent * 100) / file.size).toInt() else 0
                updateItemProgress(fileId, totalSent, percent)
            }
            inputStream.close()

            if (!isCancelled) {
                val startTime = fileStartTimes[fileId] ?: System.currentTimeMillis()
                val durationSec = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(1)
                fileCompletedDurations[fileId] = durationSec

                updateItemCompleted(fileId, null)

                val completeData = gson.toJson(FileTransferProtocol.FileControlData(fileId, file.name))
                if (_isHost.value) {
                    clientSessions.values.forEach { it.sendMessage(FileTransferProtocol.MSG_TRANSFER_COMPLETE, completeData) }
                } else {
                    sendClientMessage(FileTransferProtocol.MSG_TRANSFER_COMPLETE, completeData)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi file: ${e.message}")
            updateItemError(fileId, e.message)
        }
    }

    // ==========================================
    // ============ CANCEL & RETRY APIS =========
    // ==========================================

    fun cancelFile(fileId: String) {
        val item = _items.value.find { it.id == fileId } as? ProgressItem.FileTransfer ?: return
        cancelledFileIds.add(fileId)

        if (_isHost.value) {
            // Khi nhấn cancel ở màn HOST -> các Member sẽ bị cancel
            Log.w(TAG, "[HOST] Bấm cancel file $fileId -> Hủy và gửi MSG_CANCEL_FILE cho các Member")
            updateFileStatus(fileId, TransferStatus1.CANCELED)

            serviceScope.launch {
                val payload = gson.toJson(FileTransferProtocol.FileControlData(fileId, item.file.name))
                clientSessions.values.forEach { it.sendMessage(FileTransferProtocol.MSG_CANCEL_FILE, payload) }
            }
        } else {
            // Khi nhấn cancel ở MEMBER -> cancel ở member, gửi lên Host
            Log.w(TAG, "[MEMBER] Bấm cancel file $fileId -> Hủy và gửi MSG_CANCEL_FILE lên Host")
            updateFileStatus(fileId, TransferStatus1.CANCELED)

            serviceScope.launch {
                val payload = gson.toJson(FileTransferProtocol.FileControlData(fileId, item.file.name))
                sendClientMessage(FileTransferProtocol.MSG_CANCEL_FILE, payload)
            }
        }
    }

    fun retryFile(fileId: String) {
        cancelledFileIds.remove(fileId)
        fileDeclinedMembers.remove(fileId)
        val item = _items.value.find { it.id == fileId } as? ProgressItem.FileTransfer ?: return

        if (item.isMe) {
            val transferringItem = item.copy(
                status = TransferStatus1.TRANSFERRING,
                bytesTransferred = 0,
                progressPercent = 0,
                errorMessage = null
            )
            val updated = _items.value.toMutableList()
            val index = updated.indexOfFirst { it.id == fileId }
            if (index != -1) {
                updated[index] = transferringItem
                _items.value = updated
            }
            serviceScope.launch {
                sendSingleTransferFile(transferringItem)
            }
        }
    }

    // ==========================================
    // ============ ITEM STATE HELPERS ==========
    // ==========================================

    private fun appendItem(item: ProgressItem) {
        val list = _items.value.toMutableList()
        list.add(item)
        _items.value = list
        recalculateOverallStats()
    }

    private fun updateItemProgress(fileId: String, transferred: Long, percent: Int) {
        val list = _items.value.toMutableList()
        val index = list.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val old = list[index] as? ProgressItem.FileTransfer ?: return
            list[index] = old.copy(
                bytesTransferred = transferred,
                progressPercent = percent
            )
            _items.value = list
        }
    }

    private fun updateItemCompleted(fileId: String, path: String?) {
        val list = _items.value.toMutableList()
        val index = list.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val old = list[index] as? ProgressItem.FileTransfer ?: return
            list[index] = old.copy(
                status = TransferStatus1.COMPLETED,
                savedPath = path ?: old.savedPath,
                progressPercent = 100
            )
            _items.value = list
            recalculateOverallStats()
        }
    }

    private fun updateItemError(fileId: String, error: String?) {
        val list = _items.value.toMutableList()
        val index = list.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val old = list[index] as? ProgressItem.FileTransfer ?: return
            list[index] = old.copy(
                status = TransferStatus1.ERROR,
                errorMessage = error
            )
            _items.value = list
            recalculateOverallStats()
        }
    }

    private fun updateFileStatus(fileId: String, status: TransferStatus1) {
        val list = _items.value.toMutableList()
        val index = list.indexOfFirst { it.id == fileId }
        if (index != -1) {
            val old = list[index] as? ProgressItem.FileTransfer ?: return
            list[index] = old.copy(status = status)
            _items.value = list
            recalculateOverallStats()
        }
    }

    private fun cleanupAndStop() {
        isRunning.set(false)
        try { hotspotManager?.stopHotspot() } catch (_: Exception) {}
        hotspotManager = null
        try { serverSocket?.close() } catch (_: Exception) {}
        clientSessions.values.forEach { try { it.socket.close() } catch (_: Exception) {} }
        clientSessions.clear()
        try { clientSocket?.close() } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        cleanupAndStop()
        super.onDestroy()
    }
}
