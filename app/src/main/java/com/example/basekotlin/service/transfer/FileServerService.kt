package com.example.basekotlin.service.transfer

import android.content.Context
import android.media.MediaScannerConnection
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
import java.net.InetSocketAddress
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
class FileServerService(private val context: Context? = null) {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var dataInput: DataInputStream? = null
    private var dataOutput: DataOutputStream? = null

    // Thư mục lưu file nhận được: Download/ShareFile
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
     * Khởi tạo server socket với reuseAddress bật TRƯỚC KHI bind
     */
    fun startServer(port: Int) {
        // Đóng server cũ nếu còn tồn tại
        stopServer()

        // SỬA LỖI EADDRINUSE: Khởi tạo socket chưa bind -> bật reuseAddress = true -> bind sau
        serverSocket = ServerSocket().apply {
            reuseAddress = true
            soTimeout = 0 // chờ vô hạn cho client kết nối
            bind(InetSocketAddress(port))
        }
        Log.d(TAG, "[RECEIVER] [Server] ServerSocket bắt đầu lắng nghe trên port $port")
    }

    /**
     * Chờ client kết nối (blocking)
     * Gọi trên Dispatchers.IO
     */
    fun acceptConnection(): Socket {
        Log.d(TAG, "[RECEIVER] [Server] Đang đợi client kết nối (acceptConnection)...")
        val socket = serverSocket?.accept()
            ?: throw IllegalStateException("ServerSocket chưa khởi tạo")

        socket.soTimeout = FileTransferProtocol.READ_TIMEOUT_MS

        clientSocket = socket
        dataInput = DataInputStream(socket.getInputStream().buffered())
        dataOutput = DataOutputStream(socket.getOutputStream().buffered())

        Log.d(TAG, "[RECEIVER] [Server] Client đã kết nối thành công từ IP: ${socket.inetAddress.hostAddress}")
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
            Log.d(TAG, "[RECEIVER] [Server] Nhận Handshake từ thiết bị: ${handshake.deviceName} (Tổng ${handshake.fileCount} file, ${handshake.totalSize} bytes)")
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
                        Log.d(TAG, "[RECEIVER] [Server] Bắt đầu nhận file [${fileIndex + 1}/${handshake.fileCount}]: ${meta.fileName} (${meta.fileSize} bytes)")
                        listener.onFileReceiveStarted(meta)

                        // Gửi ACK meta (sẵn sàng nhận)
                        FileTransferProtocol.sendAck(output, FileTransferProtocol.MSG_ACK_META)

                        // Nhận file data
                        val savedPath = receiveFileData(input, meta, listener)

                        // Cập nhật MediaStore
                        context?.let { ctx ->
                            MediaScannerConnection.scanFile(ctx, arrayOf(savedPath), null, null)
                        }

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
                        Log.w(TAG, "[RECEIVER] [Server] Sender đã hủy phiên truyền file")
                        listener.onError("Sender đã hủy transfer")
                        return
                    }

                    else -> {
                        Log.e(TAG, "[RECEIVER] [Server] Message không mong đợi: type=$metaType")
                        listener.onError("Message không mong đợi: type=$metaType")
                        return
                    }
                }
            }

            // Bước 3: Nhận TRANSFER_COMPLETE
            val (completeType, _) = FileTransferProtocol.readMessage(input)
            if (completeType == FileTransferProtocol.MSG_TRANSFER_COMPLETE) {
                FileTransferProtocol.sendAck(output, FileTransferProtocol.MSG_ACK_COMPLETE)
                Log.d(TAG, "[RECEIVER] [Server] Toàn bộ phiên nhận file HOÀN THÀNH!")
                listener.onTransferComplete()
            }

        } catch (e: Exception) {
            Log.e(TAG, "[RECEIVER] [Server] Lỗi khi nhận file", e)
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

        Log.d(TAG, "[RECEIVER] [Server] Nhận xong: ${meta.fileName} (${totalReceived} bytes) → ${targetFile.absolutePath}")
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
            Log.e(TAG, "[RECEIVER] [Server] Lỗi khi dừng server", e)
        } finally {
            dataInput = null
            dataOutput = null
            clientSocket = null
            serverSocket = null
        }
        Log.d(TAG, "[RECEIVER] [Server] Server đã dừng")
    }
}

