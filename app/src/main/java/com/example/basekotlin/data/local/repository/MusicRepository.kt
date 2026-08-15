package com.example.basekotlin.data.local.repository

import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicFolder
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.RenameResult
import kotlinx.coroutines.flow.Flow

//Repository — nơi ghép MediaStore + Room
interface MusicRepository {
    fun observeAllTracks(): Flow<List<MusicTrack>>
    fun observeFolders(): Flow<List<MusicFolder>>
    fun observeFavoriteTracks(): Flow<List<MusicTrack>>

    fun observeRecentlyPlayedTracks(): Flow<List<MusicTrack>>
    fun observePlaylists(): Flow<List<MusicPlaylist>>
    fun observePlaylistTracks(playlistId: Long): Flow<List<MusicTrack>>
    fun observeReceivedTracks(): Flow<List<MusicTrack>>
    fun observeIsFavorite(songId: Long): Flow<Boolean>
    fun observeTracksByFolder(folderPath: String): Flow<List<MusicTrack>>
    suspend fun removeFromRecentlyPlayed(songId: Long)
    suspend fun clearRecentlyPlayedHistory()


    suspend fun refreshAllTracks()
    suspend fun toggleFavorite(songId: Long)
    suspend fun markPlayed(songId: Long)
    suspend fun createPlaylist(name: String): Long
    suspend fun addTrackToPlaylist(playlistId: Long, songId: Long): Boolean
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun removeTrackFromPlaylist(playlistId: Long, songId: Long)
    suspend fun deleteTracks(tracks: List<MusicTrack>): DeleteResult
    suspend fun renameTrack(track: MusicTrack, newTitle: String): RenameResult
    suspend fun renameFolder(folderPath: String, newFolderName: String): RenameResult
    suspend fun getTrackById(trackId: Long): MusicTrack?
    suspend fun addToFavorite(songId: Long): Boolean
    suspend fun getPlaylistTracksOnce(playlistId: Long): List<MusicTrack>
}