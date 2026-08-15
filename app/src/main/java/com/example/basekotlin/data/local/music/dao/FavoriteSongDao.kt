package com.example.basekotlin.data.local.music.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.basekotlin.data.local.music.entity.FavoriteSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSongDao {

    // Lắng nghe realtime toàn bộ danh sách bài hát yêu thích, mới thêm hiển thị lên trước
    @Query("SELECT * FROM favorite_song ORDER BY addedAtMillis DESC")
    fun observeAll(): Flow<List<FavoriteSongEntity>>

    // Kiểm tra nhanh 1 bài hát có đang được yêu thích hay không, phục vụ hiển thị icon trái tim
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_song WHERE songId = :songId)")
    fun observeIsFavorite(songId: Long): Flow<Boolean>

    // Bỏ qua nếu bản ghi đã tồn tại, tránh crash khi user bấm yêu thích 2 lần liên tiếp
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteSongEntity)

    @Delete
    suspend fun delete(entity: FavoriteSongEntity)

    @Query("DELETE FROM favorite_song WHERE songId = :songId")
    suspend fun deleteById(songId: Long)
}