package com.example.basekotlin.service.transfer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Network
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
import com.example.basekotlin.util.transfer.WifiHelper
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
        private const val TAG = "DEBUG_SEND_FILE"
        private const val ACTION_SEND = "ACTION_SEND"
        private const val ACTION_RECEIVE = "ACTION_RECEIVE"
        private const val ACTION_CANCEL = "ACTION_CANCEL"
        private const val EXTRA_IP = "EXTRA_IP"
        private const val EXTRA_PORT = "EXTRA_PORT"
        private const val EXTRA_FILES = "EXTRA_FILES"
        private const val EXTRA_NETWORK = "EXTRA_NETWORK"

        /**
         * Start service ở chế độ SEND
         */
        fun startSend(
            context: Context,
            ip: String,
            port: Int,
            files: ArrayList<TransferFile>,
            wifiNetwork: Network? = null
        ) {
            Log.d(TAG, "[TransferService] startSend gọi với IP: $ip, Port: $port, ${files.size} files")
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_SEND
                putExtra(EXTRA_IP, ip)
                putExtra(EXTRA_PORT, port)
                putParcelableArrayListExtra(EXTRA_FILES, files)
                // Truyền Network qua Intent để tránh race condition với static field
                if (wifiNetwork != null) {
                    putExtra(EXTRA_NETWORK, wifiNetwork)
                }
            }
            context.startForegroundService(intent)
        }

        /**
         * Start service ở chế độ RECEIVE
         */
        fun startReceive(context: Context, port: Int) {
            Log.d(TAG, "[TransferService] startReceive gọi với Port: $port")
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

    // Guard: chặn việc gọi startSend nhiều lần liên tiếp từ các intent trùng lặp
    private var isSending = false

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
                if (isSending) {
                    Log.w(TAG, "[TransferService] Đã đang gửi, bỏ qua intent trùng lặp")
                    return START_NOT_STICKY
                }
                isSending = true

                val ip = intent.getStringExtra(EXTRA_IP) ?: return START_NOT_STICKY
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                val files = intent.getParcelableArrayListExtra<TransferFile>(EXTRA_FILES)
                    ?: return START_NOT_STICKY
                // Lấy Network object từ Intent (Parcelable) — đã được capture tại thời điểm onAvailable
                @Suppress("DEPRECATION")
                val wifiNetwork = intent.getParcelableExtra<Network>(EXTRA_NETWORK)

                startForegroundNotification(
                    getString(R.string.transfer_notification_sending)
                )
                startSending(ip, port, files, wifiNetwork)
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
        WifiHelper.disconnectActiveWifi(applicationContext)
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

    private fun startSending(ip: String, port: Int, files: List<TransferFile>, wifiNetwork: Network? = null) {
        transferJob?.cancel()
        transferJob = serviceScope.launch {
            _transferState.value = TransferState.Connecting
            transferStartTime = System.currentTimeMillis()

            val sender = FileSenderService(applicationContext)
            fileSenderService = sender

            try {
                // Kết nối đến receiver, truyền Network object được capture từ Hotspot
                sender.connect(ip, port, wifiNetwork)

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
                WifiHelper.disconnectActiveWifi(applicationContext)
                isSending = false
            }
        }
    }

    // ========== RECEIVE MODE ==========

    private fun startReceiving(port: Int) {
        transferJob?.cancel()
        // Đảm bảo dừng và giải phóng server socket cũ trước khi tạo phiên mới
        fileServerService?.stopServer()
        fileServerService = null

        transferJob = serviceScope.launch {
            _transferState.value = TransferState.Connecting
            transferStartTime = System.currentTimeMillis()

            val server = FileServerService(applicationContext)
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
                        Log.d(TAG, "[TransferService] Client đã kết nối: ${handshake.deviceName}")
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
        WifiHelper.disconnectActiveWifi(applicationContext)
        isSending = false
        _transferState.value = TransferState.Idle
        TransferNotificationHelper.cancelNotification(this)
        stopSelf()
    }
}
