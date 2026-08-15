package com.example.basekotlin.data.local.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.basekotlin.data.local.mediastore.MediaStoreAudioSource
import com.example.basekotlin.data.local.mediastore.MediaStoreObserver
import com.example.basekotlin.data.local.music.MusicDatabase
import com.example.basekotlin.data.local.music.entity.FavoriteSongEntity
import com.example.basekotlin.data.local.music.entity.PlaylistEntity
import com.example.basekotlin.data.local.music.entity.PlaylistSongCrossRef
import com.example.basekotlin.data.local.music.entity.RecentlyPlayedEntity
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicFolder
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.RenameResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepositoryImpl(
    private val appContext: Context,
    private val database: MusicDatabase
) : MusicRepository {

    // Scope riêng của repository, sống cùng vòng đời app (không phải viewModelScope)
    // để chia sẻ 1 luồng MediaStore duy nhất cho mọi ViewModel/Fragment dùng chung.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val favoriteSongDao = database.favoriteSongDao()
    private val playlistDao = database.playlistDao()
    private val recentlyPlayedDao = database.recentlyPlayedDao()

    // Trigger thủ công cho pull-to-refresh, gộp chung với tín hiệu từ ContentObserver.
    private val manualRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // shareIn(replay = 1): chỉ đăng ký 1 ContentObserver duy nhất cho toàn app,
    // dù nhiều tab (All, Favorite, Folders...) cùng collect observeAllTracks().
    private val allTracksFlow: Flow<List<MusicTrack>> = merge(
        MediaStoreObserver.observeMediaStoreChanges(appContext),
        manualRefreshTrigger
    )
        .onStart { emit(Unit) }
        .map { MediaStoreAudioSource.queryAllTracks(appContext) }
        .flowOn(Dispatchers.IO)
        .shareIn(repositoryScope, SharingStarted.Eagerly, replay = 1)

    private val allTracksWithFavoriteFlow: Flow<List<MusicTrack>> = combine(
        allTracksFlow,
        favoriteSongDao.observeAll()
    ) { tracks, favoriteEntities ->
        // Gom songId đã yêu thích vào 1 Set để tra cứu O(1), tránh vòng lặp lồng nhau
        val favoriteIds = mutableSetOf<Long>()
        for (favorite in favoriteEntities) {
            favoriteIds.add(favorite.songId)
        }
        // Gắn cờ isFavorite đúng cho từng track ngay tại nguồn
        val result = mutableListOf<MusicTrack>()
        for (track in tracks) {
            val isFav = favoriteIds.contains(track.id)
            result.add(track.copy(isFavorite = isFav))
        }
        result
    }
    override fun observeAllTracks(): Flow<List<MusicTrack>> {
        return allTracksWithFavoriteFlow
    }

    override fun observeFolders(): Flow<List<MusicFolder>> {
        return observeAllTracks().map { tracks -> buildFolders(tracks) }
    }


    override fun observeFavoriteTracks(): Flow<List<MusicTrack>> {
        return observeAllTracks().map { tracks ->
            val favoriteTracks = mutableListOf<MusicTrack>()
            for (track in tracks) {
                if (track.isFavorite) {
                    favoriteTracks.add(track)
                }
            }
            favoriteTracks
        }
    }

    override fun observeRecentlyPlayedTracks(): Flow<List<MusicTrack>> {
        return combine(
            observeAllTracks(),
            recentlyPlayedDao.observeAll()
        ) { tracks, recentEntries ->
            val trackById = mutableMapOf<Long, MusicTrack>()
            for (track in tracks) {
                trackById[track.id] = track
            }
            val recentTracks = mutableListOf<MusicTrack>()
            for (entry in recentEntries) {
                val matchedTrack = trackById[entry.songId]
                if (matchedTrack != null) {
                    recentTracks.add(matchedTrack)
                }
            }
            recentTracks
        }
    }

    override fun observePlaylists(): Flow<List<MusicPlaylist>> {
        return playlistDao.observePlaylists().map { playlistEntities ->
            val playlists = mutableListOf<MusicPlaylist>()
            for (entity in playlistEntities) {
                val songIds = playlistDao.observeSongIds(entity.id).first()
                val playlist = MusicPlaylist(
                    id = entity.id,
                    name = entity.name,
                    trackCount = songIds.size,
                    createdAtMillis = entity.createdAtMillis
                )
                playlists.add(playlist)
            }
            playlists
        }
    }

    override fun observePlaylistTracks(playlistId: Long): Flow<List<MusicTrack>> {
        return combine(
            observeAllTracks(),
            playlistDao.observeSongIds(playlistId)
        ) { tracks, songIds ->
            val trackById = mutableMapOf<Long, MusicTrack>()
            for (track in tracks) {
                trackById[track.id] = track
            }
            val playlistTracks = mutableListOf<MusicTrack>()
            for (songId in songIds) {
                val matchedTrack = trackById[songId]
                if (matchedTrack != null) {
                    playlistTracks.add(matchedTrack)
                }
            }
            playlistTracks
        }
    }

    override fun observeReceivedTracks(): Flow<List<MusicTrack>> {
        return observeAllTracks().map { tracks ->
            val receivedTracks = mutableListOf<MusicTrack>()
            for (track in tracks) {
                if (track.relativeFolderPath.startsWith(RECEIVED_RELATIVE_PATH)) {
                    receivedTracks.add(track)
                }
            }
            receivedTracks
        }
    }

    override suspend fun refreshAllTracks() {
        manualRefreshTrigger.emit(Unit)
    }

    override suspend fun toggleFavorite(songId: Long) {
        val currentFavorites = favoriteSongDao.observeAll().first()
        var isCurrentlyFavorite = false
        for (favorite in currentFavorites) {
            if (favorite.songId == songId) {
                isCurrentlyFavorite = true
            }
        }
        if (isCurrentlyFavorite) {
            favoriteSongDao.deleteById(songId)
        } else {
            val newFavorite = FavoriteSongEntity(
                songId = songId,
                addedAtMillis = System.currentTimeMillis()
            )
            favoriteSongDao.insert(newFavorite)
        }
    }

    override suspend fun markPlayed(songId: Long) {
        val existingEntry = recentlyPlayedDao.getById(songId)
        val currentTimeMillis = System.currentTimeMillis()
        if (existingEntry == null) {
            val newEntry = RecentlyPlayedEntity(
                songId = songId,
                lastPlayedAtMillis = currentTimeMillis,
                playCount = 1
            )
            recentlyPlayedDao.insertOrReplace(newEntry)
        } else {
            val updatedEntry = existingEntry.copy(
                lastPlayedAtMillis = currentTimeMillis,
                playCount = existingEntry.playCount + 1
            )
            recentlyPlayedDao.insertOrReplace(updatedEntry)
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        val newPlaylist = PlaylistEntity(
            name = name,
            createdAtMillis = System.currentTimeMillis()
        )
        return playlistDao.insertPlaylist(newPlaylist)
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, songId: Long): Boolean {
        // Kiểm tra bài hát đã có trong playlist chưa trước khi thêm
        val currentSongIds = playlistDao.observeSongIds(playlistId).first()
        val songAlreadyExists = currentSongIds.contains(songId)
        if (songAlreadyExists) {
            return false
        }
        val nextPosition = currentSongIds.size
        val crossRef = PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = songId,
            position = nextPosition,
            addedAtMillis = System.currentTimeMillis()
        )
        playlistDao.addSongToPlaylist(crossRef)
        return true
    }

    private fun buildFolders(tracks: List<MusicTrack>): List<MusicFolder> {
        val groupedByFolder = mutableMapOf<String, MutableList<MusicTrack>>()
        for (track in tracks) {
            var folderList = groupedByFolder[track.relativeFolderPath]
            if (folderList == null) {
                folderList = mutableListOf()
                groupedByFolder[track.relativeFolderPath] = folderList
            }
            folderList.add(track)
        }

        val folders = mutableListOf<MusicFolder>()
        for (entry in groupedByFolder) {
            val path = entry.key
            val tracksInFolder = entry.value
            var totalDuration = 0L
            for (track in tracksInFolder) {
                totalDuration += track.durationMs
            }
            val folderName = File(path).name
            val folder = MusicFolder(
                folderPath = path,
                folderName = folderName,
                trackCount = tracksInFolder.size,
                totalDurationMs = totalDuration
            )
            folders.add(folder)
        }
        return folders
    }

    override fun observeIsFavorite(songId: Long): Flow<Boolean> {
        return favoriteSongDao.observeIsFavorite(songId)
    }

    // MusicRepositoryImpl.kt
    override suspend fun removeFromRecentlyPlayed(songId: Long) {
        recentlyPlayedDao.deleteById(songId)
    }

    override suspend fun clearRecentlyPlayedHistory() {
        recentlyPlayedDao.clearAll()
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    override suspend fun addToFavorite(songId: Long): Boolean {
        val currentFavorites = favoriteSongDao.observeAll().first()
        var isCurrentlyFavorite = false
        for (favorite in currentFavorites) {
            if (favorite.songId == songId) {
                isCurrentlyFavorite = true
                break
            }
        }
        if (isCurrentlyFavorite) {
            return false
        }
        val newFavorite = FavoriteSongEntity(
            songId = songId,
            addedAtMillis = System.currentTimeMillis()
        )
        favoriteSongDao.insert(newFavorite)
        return true
    }

    override suspend fun getPlaylistTracksOnce(playlistId: Long): List<MusicTrack> {
        return observePlaylistTracks(playlistId).first()
    }
    override suspend fun removeTrackFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun deleteTracks(tracks: List<MusicTrack>): DeleteResult {
        return withContext(Dispatchers.IO) {
            val uris = tracks.map { it.contentUri }
            val resolver = appContext.contentResolver
            var result: DeleteResult

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: luôn xin xác nhận qua IntentSender.
                // Khi user đồng ý, hệ thống TỰ xóa file lẫn bản ghi MediaStore — không cần gọi delete() lại.
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                    result = DeleteResult.NeedsUserConsent(pendingIntent.intentSender)
                } catch (error: Exception) {
                    result = DeleteResult.Failure(error)
                }
            } else {
                // API 29: thử xóa trực tiếp trước.
                try {
                    var deletedCount = 0
                    for (uri in uris) {
                        deletedCount += resolver.delete(uri, null, null)
                    }
                    if (deletedCount > 0) {
                        result = DeleteResult.Success
                    } else {
                        result = DeleteResult.Failure(Exception("Không có bản ghi nào bị xóa"))
                    }
                } catch (recoverableError: RecoverableSecurityException) {
                    // Bài không thuộc sở hữu app -> lấy intentSender để xin quyền,
                    // sau khi RESULT_OK phải gọi lại deleteTracks() (khác với nhánh R+ ở trên).
                    result = DeleteResult.NeedsUserConsent(recoverableError.userAction.actionIntent.intentSender)
                } catch (error: Exception) {
                    result = DeleteResult.Failure(error)
                }
            }

            // Dọn dữ liệu phụ thuộc trong Room CHỈ khi xóa MediaStore đã thành công thật sự
            if (result is DeleteResult.Success) {
                for (track in tracks) {
                    favoriteSongDao.deleteById(track.id)
                    recentlyPlayedDao.deleteById(track.id)
                    playlistDao.removeSongFromAllPlaylists(track.id)
                }
            }
            result
        }
    }


    override suspend fun renameTrack(track: MusicTrack, newTitle: String): RenameResult {
        return withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            val newDisplayName = buildNewDisplayName(track, newTitle) // "Ten moi.mp3"

            try {
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)

                val updatedRows = resolver.update(track.contentUri, values, null, null)
                if (updatedRows > 0) {
                    RenameResult.Success
                } else {
                    RenameResult.Failure(Exception("Không thể đổi tên file"))
                }
            } catch (e: RecoverableSecurityException) {
                RenameResult.NeedsUserConsent(e.userAction.actionIntent.intentSender)
            } catch (e: SecurityException) {
                try {
                    val pendingIntent = MediaStore.createWriteRequest(resolver, listOf(track.contentUri))
                    RenameResult.NeedsUserConsent(pendingIntent.intentSender)
                } catch (error: Exception) {
                    RenameResult.Failure(error)
                }
            } catch (e: Exception) {
                RenameResult.Failure(e)
            }
        }
    }

    override suspend fun getTrackById(trackId: Long): MusicTrack? {
        return withContext(Dispatchers.IO) {
            val tracks = allTracksWithFavoriteFlow.first()
            val tracks1 = allTracksWithFavoriteFlow
            Log.d("DEBUG_TRACK", tracks1.toString())
            Log.d("DEBUG_TRACK", tracks.toString())
            var foundTrack: MusicTrack? = null
            for (track in tracks) {
                if (track.id == trackId) {
                    foundTrack = track
                    break
                }
            }
            foundTrack
        }
    }

    override fun observeTracksByFolder(folderPath: String): Flow<List<MusicTrack>> {
        return observeAllTracks().map { tracks ->
            val tracksInFolder = mutableListOf<MusicTrack>()
            for (track in tracks) {
                if (track.relativeFolderPath == folderPath) {
                    tracksInFolder.add(track)
                }
            }
            tracksInFolder
        }
    }

    override suspend fun renameFolder(folderPath: String, newFolderName: String): RenameResult {
        return withContext(Dispatchers.IO) {
            val tracks = observeTracksByFolder(folderPath).first()
            if (tracks.isEmpty()) {
                RenameResult.Failure(Exception("Thư mục trống"))
            } else {
                val newRelativePath = buildNewRelativePath(folderPath, newFolderName)
                val resolver = appContext.contentResolver
                val uris = mutableListOf<Uri>()
                for (track in tracks) {
                    uris.add(track.contentUri)
                }

                try {
                    var updatedCount = 0
                    for (track in tracks) {
                        val values = ContentValues()
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, newRelativePath)
                        val updatedRows = resolver.update(track.contentUri, values, null, null)
                        if (updatedRows > 0) {
                            updatedCount++
                        }
                    }
                    if (updatedCount > 0) {
                        RenameResult.Success
                    } else {
                        RenameResult.Failure(Exception("Không thể đổi tên thư mục"))
                    }
                } catch (e: RecoverableSecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val pendingIntent = MediaStore.createWriteRequest(resolver, uris)
                            RenameResult.NeedsUserConsent(pendingIntent.intentSender)
                        } catch (error: Exception) {
                            RenameResult.NeedsUserConsent(e.userAction.actionIntent.intentSender)
                        }
                    } else {
                        RenameResult.NeedsUserConsent(e.userAction.actionIntent.intentSender)
                    }
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val pendingIntent = MediaStore.createWriteRequest(resolver, uris)
                            RenameResult.NeedsUserConsent(pendingIntent.intentSender)
                        } catch (error: Exception) {
                            RenameResult.Failure(error)
                        }
                    } else {
                        RenameResult.Failure(e)
                    }
                } catch (e: Exception) {
                    RenameResult.Failure(e)
                }
            }
        }
    }

    private fun buildNewRelativePath(oldPath: String, newFolderName: String): String {
        var trimmed = oldPath
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        val lastSlash = trimmed.lastIndexOf('/')
        var parentPath = ""
        if (lastSlash >= 0) {
            parentPath = trimmed.substring(0, lastSlash + 1)
        }
        var newPath = parentPath + newFolderName.trim()
        if (newPath.endsWith("/") == false) {
            newPath = newPath + "/"
        }
        return newPath
    }

    private fun buildNewDisplayName(track: MusicTrack, newTitle: String): String {
        val name = newTitle.trim()
        if (name.isEmpty()) throw IllegalArgumentException("Tên rỗng")

        var ext = ""
        if (track.filePath.isNotEmpty()) {
            ext = File(track.filePath).extension
        }
        if (ext.isEmpty()) return name
        return "$name.$ext"
    }

    companion object {
        // Thư mục quy ước cho tab Receive — tính năng Nearby/Hotspot/Clone Phone sau này sẽ ghi file vào đây.
        const val RECEIVED_RELATIVE_PATH = "ShareFile/Music/Received/"
    }
}