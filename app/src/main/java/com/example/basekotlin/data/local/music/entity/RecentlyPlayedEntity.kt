package com.example.basekotlin.data.local.music.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: Long,
    val lastPlayedAtMillis: Long,
    val playCount: Int
)