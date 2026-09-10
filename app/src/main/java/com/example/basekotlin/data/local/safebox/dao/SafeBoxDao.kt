package com.example.basekotlin.data.local.safebox.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeBoxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: SafeBoxFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<SafeBoxFile>): List<Long>

    @Delete
    suspend fun delete(file: SafeBoxFile): Int

    @Query("DELETE FROM safebox_files WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM safebox_files WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("SELECT * FROM safebox_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): SafeBoxFile?

    @Query("SELECT * FROM safebox_files WHERE fileType = :fileType ORDER BY dateAdded DESC")
    fun observeFilesByType(fileType: SafeBoxFileType): Flow<List<SafeBoxFile>>

    @Query("SELECT * FROM safebox_files ORDER BY dateAdded DESC")
    fun observeAllFiles(): Flow<List<SafeBoxFile>>

    @Query("SELECT COUNT(*) FROM safebox_files WHERE fileType = :fileType")
    fun observeCountByType(fileType: SafeBoxFileType): Flow<Int>

    @Query("SELECT COUNT(*) FROM safebox_files WHERE fileType = :fileType")
    suspend fun getCountByType(fileType: SafeBoxFileType): Int
}
