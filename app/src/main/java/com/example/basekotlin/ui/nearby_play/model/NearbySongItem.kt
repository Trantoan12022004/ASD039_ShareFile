package com.example.basekotlin.ui.nearby_play.model

data class NearbySongItem(
    val id: String,
    val title: String,
    val artist: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val uriString: String = "",
    var isPlaying: Boolean = false
)
