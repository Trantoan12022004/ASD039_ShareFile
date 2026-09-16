package com.example.basekotlin.util.transfer

import com.google.gson.Gson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

object FileTransferProtocol {

    const val BUFFER_SIZE = 64 * 1024
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 30_000
    const val MAX_PAYLOAD_SIZE = 10 * 1024 * 1024 // Giới hạn an toàn 10MB tránh OutOfMemoryError

    // Các message types sẵn có
    const val MSG_HANDSHAKE = 1
    const val MSG_ACK_HANDSHAKE = 2
    const val MSG_REJECT = 3
    const val MSG_FILE_META = 4
    const val MSG_ACK_META = 5
    const val MSG_FILE_DATA = 6
    const val MSG_ACK_FILE = 7
    const val MSG_TRANSFER_COMPLETE = 8
    const val MSG_ACK_COMPLETE = 9
    const val MSG_CANCEL = 10
    const val MSG_ERROR = 11

    // Mở rộng các message types mới cho Chat, Heartbeat & Control
    const val MSG_CHAT_TEXT = 20       // Gửi/nhận tin nhắn văn bản
    const val MSG_PING = 21            // Heartbeat ping (Sender -> Receiver)
    const val MSG_PONG = 22            // Heartbeat pong (Receiver -> Sender)
    const val MSG_CANCEL_FILE = 23     // Hủy một file cụ thể
    const val MSG_RETRY_FILE = 24      // Gửi lại một file cụ thể

    fun getMessageTypeName(type: Int): String = when (type) {
        MSG_HANDSHAKE -> "MSG_HANDSHAKE"
        MSG_ACK_HANDSHAKE -> "MSG_ACK_HANDSHAKE"
        MSG_REJECT -> "MSG_REJECT"
        MSG_FILE_META -> "MSG_FILE_META"
        MSG_ACK_META -> "MSG_ACK_META"
        MSG_FILE_DATA -> "MSG_FILE_DATA"
        MSG_ACK_FILE -> "MSG_ACK_FILE"
        MSG_TRANSFER_COMPLETE -> "MSG_TRANSFER_COMPLETE"
        MSG_ACK_COMPLETE -> "MSG_ACK_COMPLETE"
        MSG_CANCEL -> "MSG_CANCEL"
        MSG_ERROR -> "MSG_ERROR"
        MSG_CHAT_TEXT -> "MSG_CHAT_TEXT"
        MSG_PING -> "MSG_PING"
        MSG_PONG -> "MSG_PONG"
        MSG_CANCEL_FILE -> "MSG_CANCEL_FILE"
        MSG_RETRY_FILE -> "MSG_RETRY_FILE"
        else -> "UNKNOWN_TYPE($type)"
    }

    data class HandshakeData(
        val deviceName: String,
        val fileCount: Int,
        val totalSize: Long
    )

    data class FileMetaData(
        val fileName: String = "",
        val fileSize: Long = 0L,
        val mimeType: String = "",
        val index: Int = 0,
        val fileId: String = "",
        val thumbnailBase64: String? = null
    )

    data class FileAck(
        val fileName: String = "",
        val receivedSize: Long = 0L,
        val success: Boolean = true,
        val fileId: String = ""
    )

    data class ChatMessageData(
        val messageId: String,
        val text: String,
        val timestamp: Long
    )

    data class FileControlData(
        val fileId: String,
        val fileName: String
    )

    // ===== SEND METHODS =====

    fun sendMessage(output: DataOutputStream, type: Int, payload: String) {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        output.writeInt(type)
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    fun sendChunk(output: DataOutputStream, buffer: ByteArray, length: Int) {
        output.writeInt(MSG_FILE_DATA)
        output.writeInt(length)
        output.write(buffer, 0, length)
        output.flush()
    }

    @Throws(IOException::class)
    fun readMessage(input: DataInputStream): Pair<Int, String> {
        val type = input.readInt()
        val length = input.readInt()
        // Kiểm tra an toàn độ dài gói tin, tránh cấp phát bộ nhớ khổng lồ khi stream bị lệch byte
        if (length < 0 || length > MAX_PAYLOAD_SIZE) {
            throw IOException("Độ dài gói tin không hợp lệ: $length bytes (type=$type)")
        }
        val buffer = ByteArray(length)
        input.readFully(buffer)
        return Pair(type, String(buffer, Charsets.UTF_8))
    }

    // Các hàm ACK
    fun sendAck(output: DataOutputStream, type: Int) {
        sendMessage(output, type, "{}")
    }

    fun sendFileAck(output: DataOutputStream, ack: FileAck) {
        sendMessage(output, MSG_ACK_FILE, Gson().toJson(ack))
    }

    fun sendHandshake(output: DataOutputStream, data: HandshakeData) {
        sendMessage(output, MSG_HANDSHAKE, Gson().toJson(data))
    }

    fun sendFileMeta(output: DataOutputStream, meta: FileMetaData) {
        sendMessage(output, MSG_FILE_META, Gson().toJson(meta))
    }

    fun sendChat(output: DataOutputStream, chat: ChatMessageData) {
        sendMessage(output, MSG_CHAT_TEXT, Gson().toJson(chat))
    }

    fun sendPing(output: DataOutputStream) {
        sendMessage(output, MSG_PING, "{}")
    }

    fun sendPong(output: DataOutputStream) {
        sendMessage(output, MSG_PONG, "{}")
    }

    fun sendCancelFile(output: DataOutputStream, data: FileControlData) {
        sendMessage(output, MSG_CANCEL_FILE, Gson().toJson(data))
    }

    // ===== PARSE METHODS =====

    fun parseChat(json: String): ChatMessageData =
        Gson().fromJson(json, ChatMessageData::class.java)

    fun parseFileControl(json: String): FileControlData =
        Gson().fromJson(json, FileControlData::class.java)

    fun parseHandshake(json: String): HandshakeData =
        Gson().fromJson(json, HandshakeData::class.java)

    fun parseFileMeta(json: String): FileMetaData =
        Gson().fromJson(json, FileMetaData::class.java)

    fun parseFileAck(json: String): FileAck =
        Gson().fromJson(json, FileAck::class.java)
}
