package com.example.basekotlin.util

import com.example.basekotlin.model.LyricLine

object LrcParser {

    private val timeTagRegex = Regex("""\[(\d{2}):(\d{2})[.:](\d{1,3})]""")

    // Hiện lyric sớm hơn 500ms so với mốc thời gian thật trong file .lrc, bù độ trễ giữa
    // lúc mắt đọc chữ và lúc tai nghe câu hát - giống cách RetroMusicPlayer làm
    // (AbsSynchronizedLyrics.TIME_OFFSET_MS).
    private const val ACTIVE_LINE_TIME_OFFSET_MS = 500L

    fun parse(rawContent: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val rawLines = rawContent.split("\n")

        for (rawLine in rawLines) {
            val matches = timeTagRegex.findAll(rawLine).toList()
            if (matches.isEmpty()) {
                continue
            }

            val lastMatch = matches.last()
            val text = rawLine.substring(lastMatch.range.last + 1).trim()
            val displayText: String
            if (text.isEmpty()) {
                displayText = "♪♪♪"
            } else {
                displayText = text
            }

            for (match in matches) {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                var fraction = match.groupValues[3]
                if (fraction.length == 1) {
                    fraction = fraction + "00"
                } else if (fraction.length == 2) {
                    fraction = fraction + "0"
                }
                val milliseconds = fraction.toLong()

                val timeMs = minutes * 60000L + seconds * 1000L + milliseconds
                result.add(LyricLine(timeMs = timeMs, text = displayText))
            }
        }

        result.sortBy { it.timeMs }
        return result
    }

    // Tìm chỉ số dòng lyric đang được phát tại thời điểm positionMs.
    // Trả về -1 nếu positionMs còn nhỏ hơn mốc thời gian của dòng đầu tiên (chưa tới lyric nào).
    // Dùng binary search vì "lines" đã được sort tăng dần theo timeMs (xem parse() ở trên).
    fun findActiveIndex(lines: List<LyricLine>, positionMs: Long): Int {
        val adjustedPositionMs = positionMs + ACTIVE_LINE_TIME_OFFSET_MS

        var low = 0
        var high = lines.size - 1
        var resultIndex = -1
        while (low <= high) {
            val mid = (low + high) / 2
            val lineTimeMs = lines[mid].timeMs
            if (lineTimeMs <= adjustedPositionMs) {
                resultIndex = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return resultIndex
    }
}