package com.example.basekotlin.data.local.music.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.basekotlin.data.local.music.entity.RecentlyPlayedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyPlayedDao {

    // Bài phát gần nhất hiển thị đầu tiên; giới hạn số lượng để tránh danh sách phình to vô hạn
    @Query("SELECT * FROM recently_played ORDER BY lastPlayedAtMillis DESC LIMIT :limit")
    fun observeAll(limit: Int = 100): Flow<List<RecentlyPlayedEntity>>

    @Query("SELECT * FROM recently_played WHERE songId = :songId")
    suspend fun getById(songId: Long): RecentlyPlayedEntity?

    // Ghi đè bản ghi cũ nếu songId đã tồn tại, dùng cho trường hợp update lastPlayedAtMillis + playCount
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: RecentlyPlayedEntity)

    @Delete
    suspend fun delete(entity: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE songId = :songId")
    suspend fun deleteById(songId: Long)

    @Query("DELETE FROM recently_played")
    suspend fun clearAll()
}