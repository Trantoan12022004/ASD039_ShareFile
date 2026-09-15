package com.example.basekotlin.service.transfer

import android.content.Context
import android.net.Network
import android.net.Uri
import android.util.Log
import com.example.basekotlin.ui.transfer.model.FileProgress
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.util.transfer.FileTransferProtocol
import com.example.basekotlin.util.transfer.FileTransferProtocol.BUFFER_SIZE
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.WifiHelper
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
        private const val TAG = "DEBUG_SEND_FILE"
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
     * @param wifiNetwork Network object được capture từ WifiHelper.onAvailable() và truyền qua Intent
     */
    fun connect(ip: String, port: Int, wifiNetwork: Network? = null) {
        Log.d(TAG, "[SENDER] [FileSender] Bắt đầu kết nối TCP Socket tới $ip:$port...")

        // Ưu tiên Network được truyền qua Intent (đã capture tại thời điểm onAvailable, không bị race condition)
        // Fallback: đọc từ static field WifiHelper.activeWifiNetwork
        val network = wifiNetwork ?: WifiHelper.activeWifiNetwork
        val targetSocket = if (network != null) {
            Log.d(TAG, "[SENDER] [FileSender] Tìm thấy Wi-Fi Network -> Sử dụng network.socketFactory (Bỏ qua 4G)")
            network.socketFactory.createSocket()
        } else {
            Log.d(TAG, "[SENDER] [FileSender] Không có Wi-Fi Network -> Sử dụng Socket mặc định")
            Socket()
        }

        // FIX ENETUNREACH: Bind socket vào đúng Wi-Fi network trước khi connect
        // socketFactory.createSocket() tạo unconnected socket, trên một số thiết bị/ROM
        // chưa bind vào network interface → kernel route qua interface mặc định (cellular/IMS)
        // bindSocket() ép kernel gán socket vào đúng interface Wi-Fi hotspot
        if (network != null) {
            try {
                network.bindSocket(targetSocket)
                Log.d(TAG, "[SENDER] [FileSender] Đã bindSocket vào Wi-Fi network thành công")
            } catch (e: Exception) {
                // socketFactory có thể đã bind sẵn trên một số thiết bị → bỏ qua lỗi
                Log.w(TAG, "[SENDER] [FileSender] bindSocket thất bại (socketFactory có thể đã bind), tiếp tục connect", e)
            }
        }

        socket = targetSocket.apply {
            soTimeout = FileTransferProtocol.READ_TIMEOUT_MS
            connect(
                InetSocketAddress(ip, port),
                FileTransferProtocol.CONNECT_TIMEOUT_MS
            )
        }
        dataInput = DataInputStream(socket!!.getInputStream().buffered())
        dataOutput = DataOutputStream(socket!!.getOutputStream().buffered())
        Log.d(TAG, "[SENDER] [FileSender] Đã kết nối TCP Socket thành công từ ${socket!!.localAddress} tới $ip:$port!")
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
            Log.d(TAG, "[SENDER] [FileSender] Gửi Handshake: Device=${handshake.deviceName}, ${handshake.fileCount} files (${handshake.totalSize} bytes)")
            FileTransferProtocol.sendHandshake(output, handshake)

            // Đợi ACK handshake
            val (ackType, _) = FileTransferProtocol.readMessage(input)
            when (ackType) {
                FileTransferProtocol.MSG_ACK_HANDSHAKE -> {
                    Log.d(TAG, "[SENDER] [FileSender] Receiver đã chấp nhận Handshake!")
                    listener.onHandshakeAccepted()
                }
                FileTransferProtocol.MSG_REJECT -> {
                    Log.w(TAG, "[SENDER] [FileSender] Receiver từ chối Handshake")
                    listener.onHandshakeRejected()
                    return
                }
                else -> {
                    Log.e(TAG, "[SENDER] [FileSender] Phản hồi Handshake không hợp lệ: $ackType")
                    listener.onError("Phản hồi handshake không hợp lệ")
                    return
                }
            }

            // Bước 2: Gửi từng file
            files.forEachIndexed { index, file ->
                Log.d(TAG, "[SENDER] [FileSender] Chuẩn bị gửi file [${index + 1}/${files.size}]: ${file.name} (${file.size} bytes)")
                listener.onFileSendStarted(file, index)
                sendSingleFile(file, index, listener)
                listener.onFileSent(file, index)
            }

            // Bước 3: Gửi TRANSFER_COMPLETE
            Log.d(TAG, "[SENDER] [FileSender] Đã gửi hết các file, gửi MSG_TRANSFER_COMPLETE...")
            FileTransferProtocol.sendMessage(
                output,
                FileTransferProtocol.MSG_TRANSFER_COMPLETE,
                "{}"
            )

            // Đợi ACK complete
            val (completeAckType, _) = FileTransferProtocol.readMessage(input)
            if (completeAckType == FileTransferProtocol.MSG_ACK_COMPLETE) {
                Log.d(TAG, "[SENDER] [FileSender] Toàn bộ quá trình gửi file HOÀN TẤT THÀNH CÔNG!")
                listener.onTransferComplete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "[SENDER] [FileSender] Lỗi khi gửi file", e)
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
        Log.d(TAG, "[SENDER] [FileSender] Gửi Meta file: ${meta.fileName} (size=${meta.fileSize})")
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

        Log.d(TAG, "[SENDER] [FileSender] Đã gửi xong file: ${file.name} ($totalSent bytes)")
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
            Log.e(TAG, "[SENDER] [FileSender] Lỗi khi gửi cancel", e)
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
            Log.e(TAG, "[SENDER] [FileSender] Lỗi khi ngắt kết nối", e)
        } finally {
            dataInput = null
            dataOutput = null
            socket = null
        }
        Log.d(TAG, "[SENDER] [FileSender] Đã ngắt kết nối Socket")
    }
}
