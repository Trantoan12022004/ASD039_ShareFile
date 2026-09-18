package com.example.basekotlin.ui.nearby_play.protocol

import com.google.gson.Gson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

object NearbyPlayProtocol {
    const val DISCOVERY_PORT = 8891
    const val BEACON_PREFIX = "ASD_NEARBY_SPEAKER"

    const val MSG_HANDSHAKE = 101
    const val MSG_ACK_HANDSHAKE = 102
    const val MSG_PLAYLIST = 103
    const val MSG_SONG_HEADER = 104
    const val MSG_SONG_CHUNK = 105
    const val CMD_PLAY = 106
    const val CMD_PAUSE = 107
    const val CMD_SEEK = 108
    const val CMD_SELECT_TRACK = 109
    const val CMD_NEXT = 110
    const val CMD_PREVIOUS = 111
    const val CMD_DISCONNECT = 112
    const val STATUS_SYNC = 113

    data class HandshakeData(val deviceName: String)
    data class SongHeaderData(val index: Int, val fileName: String, val fileSize: Long)
    data class PlaybackStatusData(
        val currentIndex: Int,
        val isPlaying: Boolean,
        val currentPositionMs: Long,
        val durationMs: Long
    )

    fun sendMessage(output: DataOutputStream, type: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        output.writeInt(type)
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    fun sendChunk(output: DataOutputStream, bytes: ByteArray, offset: Int, length: Int) {
        output.writeInt(MSG_SONG_CHUNK)
        output.writeInt(length)
        output.write(bytes, offset, length)
        output.flush()
    }

    @Throws(IOException::class)
    fun readMessage(input: DataInputStream): Pair<Int, ByteArray> {
        val type = input.readInt()
        val length = input.readInt()
        if (length < 0 || length > 15 * 1024 * 1024) {
            throw IOException("Payload size invalid: $length")
        }
        val buffer = ByteArray(length)
        input.readFully(buffer)
        return Pair(type, buffer)
    }

    fun parseJson(bytes: ByteArray): String = String(bytes, Charsets.UTF_8)
}
