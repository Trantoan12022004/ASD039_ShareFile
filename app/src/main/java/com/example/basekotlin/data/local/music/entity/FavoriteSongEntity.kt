package com.example.basekotlin.data.local.music.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_song")
data class FavoriteSongEntity(
    @PrimaryKey val songId: Long,
    val addedAtMillis: Long
)