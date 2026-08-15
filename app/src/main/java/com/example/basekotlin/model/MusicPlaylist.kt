package com.example.basekotlin.model
// domain model playlist (đã join song count, cover...)
data class MusicPlaylist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAtMillis: Long
)