package com.example.basekotlin.util

import android.content.Context
import android.util.Log
import com.example.basekotlin.model.LyricLine
import com.example.basekotlin.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.Locale

// Nguồn lấy lyric cho 1 MusicTrack, thử lần lượt theo thứ tự ưu tiên:
// 1. Lyric NHÚNG bên trong chính file nhạc (thường gặp nhất trong thực tế):
//    - MP3: frame "USLT" trong tag ID3v2
//    - FLAC: comment "LYRICS"/"UNSYNCEDLYRICS" trong khối VORBIS_COMMENT
// 2. File .lrc nằm cùng thư mục với file nhạc (cùng tên, khác đuôi)
// 3. File .lrc lưu trong bộ nhớ riêng của app (dự phòng cho tính năng import sau này)
object LyricsSource {

    private const val TAG = "LyricsSource"

    suspend fun loadLyrics(context: Context, track: MusicTrack): List<LyricLine> {
        return withContext(Dispatchers.IO) {
            var content: String? = readEmbeddedLyrics(track)

            if (content == null) {
                val sidecarFile = resolveSidecarFile(track)
                if (sidecarFile != null && sidecarFile.exists()) {
                    content = sidecarFile.readText()
                }
            }

            if (content == null) {
                val internalFile = resolveInternalFile(context, track)
                if (internalFile.exists()) {
                    content = internalFile.readText()
                }
            }

            if (content == null) {
                emptyList()
            } else {
                LrcParser.parse(content)
            }
        }
    }

    // Bước 1: xác định định dạng file theo đuôi mở rộng rồi đọc lyric nhúng tương ứng
    private fun readEmbeddedLyrics(track: MusicTrack): String? {
        if (track.filePath.isEmpty()) {
            return null
        }

        val audioFile = File(track.filePath)
        if (!audioFile.exists()) {
            return null
        }

        val extension = audioFile.extension.lowercase(Locale.ROOT)
        var embeddedLyrics: String? = null

        try {
            if (extension == "mp3") {
                embeddedLyrics = readId3v2Lyrics(audioFile)
            } else if (extension == "flac") {
                embeddedLyrics = readFlacVorbisLyrics(audioFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "readEmbeddedLyrics failed for " + audioFile.path, e)
            embeddedLyrics = null
        }

        return embeddedLyrics
    }

    // ===================== MP3 (ID3v2) =====================

    // Đọc frame "USLT" (Unsynchronised lyrics) trong tag ID3v2 ở đầu file MP3.
    // Nhiều phần mềm gắn tag (Mp3tag, foobar2000...) lưu luôn text dạng LRC có mốc thời gian
    // ngay trong frame này, nên sau khi đọc ra vẫn đưa qua LrcParser.parse như bình thường.
    private fun readId3v2Lyrics(file: File): String? {
        var randomAccessFile: RandomAccessFile? = null
        var result: String? = null

        try {
            randomAccessFile = RandomAccessFile(file, "r")

            val header = ByteArray(ID3_HEADER_SIZE)
            val headerBytesRead = randomAccessFile.read(header)
            if (headerBytesRead < ID3_HEADER_SIZE) {
                return null
            }

            val isId3Tag = header[0] == 'I'.code.toByte() &&
                header[1] == 'D'.code.toByte() &&
                header[2] == '3'.code.toByte()
            if (!isId3Tag) {
                return null
            }

            val majorVersion = header[3].toInt() and 0xFF
            val flags = header[5].toInt() and 0xFF
            val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
            val tagEnd = ID3_HEADER_SIZE + tagSize

            var offset = ID3_HEADER_SIZE

            // Bit thứ 6 của flags báo có extended header, cần nhảy qua để tới frame đầu tiên
            val hasExtendedHeader = (flags and 0x40) != 0
            if (hasExtendedHeader) {
                val extHeaderSizeBytes = ByteArray(4)
                randomAccessFile.seek(offset.toLong())
                randomAccessFile.read(extHeaderSizeBytes)

                var extHeaderSize = 0
                if (majorVersion >= 4) {
                    extHeaderSize = synchsafeToInt(
                        extHeaderSizeBytes[0],
                        extHeaderSizeBytes[1],
                        extHeaderSizeBytes[2],
                        extHeaderSizeBytes[3]
                    )
                } else {
                    extHeaderSize = bigEndianToInt(
                        extHeaderSizeBytes[0],
                        extHeaderSizeBytes[1],
                        extHeaderSizeBytes[2],
                        extHeaderSizeBytes[3]
                    )
                }
                offset = offset + extHeaderSize
            }

            // Duyệt tuần tự từng frame cho tới hết vùng tag, dừng sớm nếu gặp USLT
            var keepSearching = true
            while (keepSearching && offset < tagEnd) {
                val frameHeader = ByteArray(ID3_FRAME_HEADER_SIZE)
                randomAccessFile.seek(offset.toLong())
                val frameHeaderBytesRead = randomAccessFile.read(frameHeader)
                if (frameHeaderBytesRead < ID3_FRAME_HEADER_SIZE) {
                    keepSearching = false
                } else if (frameHeader[0] == 0.toByte()) {
                    // Byte đầu = 0 nghĩa là hết frame thật, phần còn lại chỉ là padding
                    keepSearching = false
                } else {
                    val frameId = String(frameHeader, 0, 4, Charsets.US_ASCII)

                    var frameSize = 0
                    if (majorVersion >= 4) {
                        frameSize = synchsafeToInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
                    } else {
                        frameSize = bigEndianToInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
                    }

                    val frameDataStart = offset + ID3_FRAME_HEADER_SIZE

                    if (frameId == "USLT" && frameSize > 0) {
                        val frameData = ByteArray(frameSize)
                        randomAccessFile.seek(frameDataStart.toLong())
                        randomAccessFile.read(frameData)
                        result = decodeUsltFrame(frameData)
                        keepSearching = false
                    } else {
                        offset = frameDataStart + frameSize
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "readId3v2Lyrics failed for " + file.path, e)
            result = null
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close()
            }
        }

        return result
    }

    // Nội dung frame USLT: 1 byte encoding + 3 byte mã ngôn ngữ + content descriptor
    // (có thể rỗng, kết thúc bằng ký tự null) + phần lyric thật sự.
    private fun decodeUsltFrame(frameData: ByteArray): String? {
        if (frameData.size <= 4) {
            return null
        }

        val encodingByte = frameData[0].toInt() and 0xFF

        var charset: Charset = Charsets.ISO_8859_1
        var terminatorSize = 1
        if (encodingByte == 1) {
            charset = Charsets.UTF_16
            terminatorSize = 2
        } else if (encodingByte == 2) {
            charset = Charsets.UTF_16BE
            terminatorSize = 2
        } else if (encodingByte == 3) {
            charset = Charsets.UTF_8
            terminatorSize = 1
        }

        // Content descriptor bắt đầu ngay sau 1 byte encoding + 3 byte language code
        var descriptorEnd = 4
        var foundTerminator = false
        while (!foundTerminator && descriptorEnd < frameData.size) {
            if (terminatorSize == 1) {
                if (frameData[descriptorEnd] == 0.toByte()) {
                    foundTerminator = true
                } else {
                    descriptorEnd = descriptorEnd + 1
                }
            } else {
                val hasNextByte = descriptorEnd + 1 < frameData.size
                if (hasNextByte && frameData[descriptorEnd] == 0.toByte() && frameData[descriptorEnd + 1] == 0.toByte()) {
                    foundTerminator = true
                } else {
                    descriptorEnd = descriptorEnd + 2
                }
            }
        }

        val lyricsStart = descriptorEnd + terminatorSize
        if (lyricsStart >= frameData.size) {
            return null
        }

        val lyricsBytes = frameData.copyOfRange(lyricsStart, frameData.size)
        val lyricsText = String(lyricsBytes, charset).trim()

        if (lyricsText.isEmpty()) {
            return null
        }
        return lyricsText
    }

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
        val v0 = b0.toInt() and 0x7F
        val v1 = b1.toInt() and 0x7F
        val v2 = b2.toInt() and 0x7F
        val v3 = b3.toInt() and 0x7F
        return (v0 shl 21) or (v1 shl 14) or (v2 shl 7) or v3
    }

    private fun bigEndianToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
        val v0 = b0.toInt() and 0xFF
        val v1 = b1.toInt() and 0xFF
        val v2 = b2.toInt() and 0xFF
        val v3 = b3.toInt() and 0xFF
        return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or v3
    }

    // ===================== FLAC (Vorbis Comment) =====================

    // Đọc comment "LYRICS"/"UNSYNCEDLYRICS"/"SYNCEDLYRICS" trong khối VORBIS_COMMENT của FLAC.
    private fun readFlacVorbisLyrics(file: File): String? {
        var randomAccessFile: RandomAccessFile? = null
        var result: String? = null

        try {
            randomAccessFile = RandomAccessFile(file, "r")

            val magic = ByteArray(4)
            val magicBytesRead = randomAccessFile.read(magic)
            val isFlac = magicBytesRead == 4 &&
                magic[0] == 'f'.code.toByte() &&
                magic[1] == 'L'.code.toByte() &&
                magic[2] == 'a'.code.toByte() &&
                magic[3] == 'C'.code.toByte()
            if (!isFlac) {
                return null
            }

            var isLastBlock = false
            while (!isLastBlock && result == null) {
                val blockHeader = ByteArray(4)
                val blockHeaderBytesRead = randomAccessFile.read(blockHeader)
                if (blockHeaderBytesRead < 4) {
                    isLastBlock = true
                } else {
                    val firstByte = blockHeader[0].toInt() and 0xFF
                    isLastBlock = (firstByte and 0x80) != 0
                    val blockType = firstByte and 0x7F
                    val blockLength = bigEndianToInt24(blockHeader[1], blockHeader[2], blockHeader[3])

                    if (blockType == FLAC_VORBIS_COMMENT_BLOCK_TYPE) {
                        val blockData = ByteArray(blockLength)
                        randomAccessFile.read(blockData)
                        result = parseVorbisCommentLyrics(blockData)
                    } else {
                        randomAccessFile.seek(randomAccessFile.filePointer + blockLength)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "readFlacVorbisLyrics failed for " + file.path, e)
            result = null
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close()
            }
        }

        return result
    }

    // Cấu trúc khối VORBIS_COMMENT: vendor (độ dài + chuỗi) rồi tới danh sách comment
    // dạng "KEY=VALUE", mỗi comment cũng có 4 byte độ dài (little-endian) đứng trước.
    private fun parseVorbisCommentLyrics(blockData: ByteArray): String? {
        if (blockData.size < 8) {
            return null
        }

        var position = 0
        val vendorLength = littleEndianToInt(blockData, position)
        position = position + 4 + vendorLength

        if (position + 4 > blockData.size) {
            return null
        }
        val commentCount = littleEndianToInt(blockData, position)
        position = position + 4

        var result: String? = null
        var index = 0
        while (index < commentCount && result == null && position + 4 <= blockData.size) {
            val commentLength = littleEndianToInt(blockData, position)
            position = position + 4

            if (position + commentLength > blockData.size) {
                index = commentCount
            } else {
                val commentText = String(blockData, position, commentLength, Charsets.UTF_8)
                position = position + commentLength

                val separatorIndex = commentText.indexOf('=')
                if (separatorIndex > 0) {
                    val key = commentText.substring(0, separatorIndex).uppercase(Locale.ROOT)
                    val value = commentText.substring(separatorIndex + 1)
                    val isLyricsKey = key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "SYNCEDLYRICS"
                    if (isLyricsKey && value.isNotEmpty()) {
                        result = value
                    }
                }
                index = index + 1
            }
        }

        return result
    }

    private fun bigEndianToInt24(b0: Byte, b1: Byte, b2: Byte): Int {
        val v0 = b0.toInt() and 0xFF
        val v1 = b1.toInt() and 0xFF
        val v2 = b2.toInt() and 0xFF
        return (v0 shl 16) or (v1 shl 8) or v2
    }

    private fun littleEndianToInt(data: ByteArray, offset: Int): Int {
        val v0 = data[offset].toInt() and 0xFF
        val v1 = data[offset + 1].toInt() and 0xFF
        val v2 = data[offset + 2].toInt() and 0xFF
        val v3 = data[offset + 3].toInt() and 0xFF
        return v0 or (v1 shl 8) or (v2 shl 16) or (v3 shl 24)
    }

    // ===================== Fallback: file .lrc rời =====================

    private fun resolveSidecarFile(track: MusicTrack): File? {
        if (track.filePath.isEmpty()) {
            return null
        }
        val audioFile = File(track.filePath)
        val lrcName = audioFile.nameWithoutExtension + ".lrc"
        return File(audioFile.parentFile, lrcName)
    }

    private fun resolveInternalFile(context: Context, track: MusicTrack): File {
        val lyricsDir = File(context.filesDir, "lyrics")
        if (!lyricsDir.exists()) {
            lyricsDir.mkdirs()
        }
        return File(lyricsDir, track.id.toString() + ".lrc")
    }

    private const val ID3_HEADER_SIZE = 10
    private const val ID3_FRAME_HEADER_SIZE = 10
    private const val FLAC_VORBIS_COMMENT_BLOCK_TYPE = 4
}
