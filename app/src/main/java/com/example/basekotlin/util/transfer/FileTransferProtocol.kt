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
