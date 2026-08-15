package com.example.basekotlin.data.local.music

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.basekotlin.data.local.music.dao.FavoriteSongDao
import com.example.basekotlin.data.local.music.dao.PlaylistDao
import com.example.basekotlin.data.local.music.dao.RecentlyPlayedDao
import com.example.basekotlin.data.local.music.entity.FavoriteSongEntity
import com.example.basekotlin.data.local.music.entity.PlaylistEntity
import com.example.basekotlin.data.local.music.entity.PlaylistSongCrossRef
import com.example.basekotlin.data.local.music.entity.RecentlyPlayedEntity

@Database(
    entities = [
        FavoriteSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentlyPlayedEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun favoriteSongDao(): FavoriteSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        // Singleton, tránh mở nhiều connection tới cùng 1 file db.
        fun getInstance(context: Context): MusicDatabase {
            val existingInstance = instance
            if (existingInstance != null) {
                return existingInstance
            }
            synchronized(this) {
                val currentInstance = instance
                if (currentInstance != null) {
                    return currentInstance
                }
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                ).build()
                instance = newInstance
                return newInstance
            }
        }
    }
}