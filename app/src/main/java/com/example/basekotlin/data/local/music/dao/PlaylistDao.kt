package com.example.basekotlin.data.local.music.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.basekotlin.data.local.music.entity.PlaylistEntity
import com.example.basekotlin.data.local.music.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    /** Lấy danh sách playlist. */
    @Query("SELECT * FROM playlist ORDER BY createdAtMillis DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>



    /** Lấy danh sách ID bài hát trong playlist. */
    @Query("SELECT songId FROM playlist_song WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongIds(playlistId: Long): Flow<List<Long>>

    /** Tạo playlist mới. */
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    /** Thêm bài hát vào playlist. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    /** Xóa bài hát khỏi playlist. */
    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /** Xóa bài hát khỏi tất cả playlist — dùng khi bài hát bị xóa khỏi thiết bị. */
    @Query("DELETE FROM playlist_song WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: Long)

    /** Xóa playlist. */
    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    /** Đổi tên playlist. */
    @Query("UPDATE playlist SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)
}