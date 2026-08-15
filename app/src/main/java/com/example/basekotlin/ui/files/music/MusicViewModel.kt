package com.example.basekotlin.ui.files.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.music.MusicDatabase
import com.example.basekotlin.data.local.repository.MusicRepository
import com.example.basekotlin.data.local.repository.MusicRepositoryImpl
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicFolder
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicSelectionTarget
import com.example.basekotlin.model.MusicSortOption
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.RenameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MusicRepository
    init {
        val appContext = application.applicationContext
        val database = MusicDatabase.getInstance(appContext)
        repository = MusicRepositoryImpl(appContext, database)
    }
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _sortOption = MutableStateFlow(MusicSortOption.TITLE_ASC)

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode
    private val _selectionTarget = MutableStateFlow(MusicSelectionTarget.TRACK)
    val selectionTarget: StateFlow<MusicSelectionTarget> = _selectionTarget
    private val _selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTrackIds: StateFlow<Set<Long>> = _selectedTrackIds
    private val _selectedFolderPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolderPaths: StateFlow<Set<String>> = _selectedFolderPaths
    private val _selectedPlaylistIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPlaylistIds: StateFlow<Set<Long>> = _selectedPlaylistIds

    val allTracks: StateFlow<List<MusicTrack>> = combine(
        repository.observeAllTracks(),
        _sortOption
    ) { tracks, sort ->
        applyFilterAndSort(tracks, "", sort)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchResults: StateFlow<List<MusicTrack>> = combine(
        allTracks,
        _searchQuery
    ) { tracks, query ->
        // Giữ nguyên thứ tự sort hiện tại, chỉ lọc theo từ khóa
        if (query.isBlank()) {
            tracks
        } else {
            val filtered = mutableListOf<MusicTrack>()
            for (track in tracks) {
                val titleMatch = track.title.contains(query, ignoreCase = true)
                val artistMatch = track.artist.contains(query, ignoreCase = true)
                if (titleMatch || artistMatch) {
                    filtered.add(track)
                }
            }
            filtered
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteTracks: StateFlow<List<MusicTrack>> = repository.observeFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<MusicFolder>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentlyPlayedTracks: StateFlow<List<MusicTrack>> = repository.observeRecentlyPlayedTracks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<MusicPlaylist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOption(option: MusicSortOption) {
        _sortOption.value = option
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    fun refreshAllTracks() {
        viewModelScope.launch {
            repository.refreshAllTracks()
        }
    }
    fun markPlayed(songId: Long) {
        viewModelScope.launch {
            repository.markPlayed(songId)
        }
    }

    fun removeFromRecentlyPlayed(songId: Long) {
        viewModelScope.launch {
            repository.removeFromRecentlyPlayed(songId)
        }
    }
    fun clearRecentlyPlayedHistory() {
        viewModelScope.launch {
            repository.clearRecentlyPlayedHistory()
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newPlaylistId = repository.createPlaylist(name)
            onCreated(newPlaylistId)
        }
    }

    fun deletePlaylist(playlistId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            onDone()
        }
    }

    fun deletePlaylists(playlistIds: List<Long>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            for (playlistId in playlistIds) {
                repository.deletePlaylist(playlistId)
            }
            onDone()
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
            onDone()
        }
    }

    fun addTrackToPlaylist(playlistId: Long, songId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val wasAdded = repository.addTrackToPlaylist(playlistId, songId)
            onResult(wasAdded)
        }
    }

    fun addTracksToFavorite(trackIds: List<Long>, onDone: (addedCount: Int) -> Unit) {
        viewModelScope.launch {
            var addedCount = 0
            for (trackId in trackIds) {
                val wasAdded = repository.addToFavorite(trackId)
                if (wasAdded) {
                    addedCount++
                }
            }
            onDone(addedCount)
        }
    }

    fun getTracksInFolder(folderPath: String): List<MusicTrack> {
        val result = mutableListOf<MusicTrack>()
        val all = allTracks.value
        for (track in all) {
            if (track.relativeFolderPath == folderPath) {
                result.add(track)
            }
        }
        return result
    }

    fun getTracksInSelectedFolders(): List<MusicTrack> {
        val selectedPaths = _selectedFolderPaths.value
        val result = mutableListOf<MusicTrack>()
        val all = allTracks.value
        for (track in all) {
            if (selectedPaths.contains(track.relativeFolderPath)) {
                result.add(track)
            }
        }
        return result
    }

    fun getPlaylistTracks(playlistId: Long, onResult: (List<MusicTrack>) -> Unit) {
        viewModelScope.launch {
            val tracks = repository.getPlaylistTracksOnce(playlistId)
            onResult(tracks)
        }
    }

    fun getTracksOfSelectedPlaylists(onResult: (List<MusicTrack>) -> Unit) {
        viewModelScope.launch {
            val selectedIds = _selectedPlaylistIds.value
            val result = mutableListOf<MusicTrack>()
            val seenIds = mutableSetOf<Long>()
            for (playlistId in selectedIds) {
                val tracks = repository.getPlaylistTracksOnce(playlistId)
                for (track in tracks) {
                    if (seenIds.contains(track.id) == false) {
                        seenIds.add(track.id)
                        result.add(track)
                    }
                }
            }
            onResult(result)
        }
    }

    fun isPlaylistNameTaken(name: String, excludePlaylistId: Long = -1L): Boolean {
        // So sánh tên đã trim, không phân biệt hoa thường để tránh trùng "Chill" và "chill "
        val trimmedName = name.trim()
        val currentPlaylists = playlists.value
        for (existingPlaylist in currentPlaylists) {
            if (existingPlaylist.id == excludePlaylistId) {
                continue
            }
            val isSameName = existingPlaylist.name.trim().equals(trimmedName, ignoreCase = true)
            if (isSameName) {
                return true
            }
        }
        return false
    }

    fun isFolderNameTaken(currentFolderPath: String, newName: String): Boolean {
        val trimmedName = newName.trim()
        val currentParent = getFolderParentPath(currentFolderPath)
        val currentFolders = folders.value
        for (existingFolder in currentFolders) {
            if (existingFolder.folderPath == currentFolderPath) {
                continue
            }
            val existingParent = getFolderParentPath(existingFolder.folderPath)
            if (existingParent == currentParent) {
                val isSameName = existingFolder.folderName.equals(trimmedName, ignoreCase = true)
                if (isSameName) {
                    return true
                }
            }
        }
        return false
    }

    private fun getFolderParentPath(path: String): String {
        var trimmed = path
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        val lastSlash = trimmed.lastIndexOf('/')
        if (lastSlash < 0) {
            return ""
        }
        return trimmed.substring(0, lastSlash + 1)
    }
    fun deleteTracks(tracks: List<MusicTrack>, onResult: (DeleteResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteTracks(tracks)
            onResult(result)
        }
    }

    fun renameTrack(track: MusicTrack, newTitle: String, onResult: (RenameResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameTrack(track, newTitle)
            onResult(result)
        }
    }

    fun renameFolder(folderPath: String, newFolderName: String, onResult: (RenameResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameFolder(folderPath, newFolderName)
            onResult(result)
        }
    }

    fun enterSelectionMode(initialTrackId: Long? = null) {
        _isSelectionMode.value = true
        _selectionTarget.value = MusicSelectionTarget.TRACK
        _selectedFolderPaths.value = emptySet()
        _selectedPlaylistIds.value = emptySet()
        if (initialTrackId != null) {
            _selectedTrackIds.value = setOf(initialTrackId)
        }
    }

    fun enterFolderSelectionMode(initialFolderPath: String? = null) {
        _isSelectionMode.value = true
        _selectionTarget.value = MusicSelectionTarget.FOLDER
        _selectedTrackIds.value = emptySet()
        _selectedPlaylistIds.value = emptySet()
        if (initialFolderPath != null) {
            _selectedFolderPaths.value = setOf(initialFolderPath)
        } else {
            _selectedFolderPaths.value = emptySet()
        }
    }

    fun enterPlaylistSelectionMode(initialPlaylistId: Long? = null) {
        _isSelectionMode.value = true
        _selectionTarget.value = MusicSelectionTarget.PLAYLIST
        _selectedTrackIds.value = emptySet()
        _selectedFolderPaths.value = emptySet()
        if (initialPlaylistId != null) {
            _selectedPlaylistIds.value = setOf(initialPlaylistId)
        } else {
            _selectedPlaylistIds.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectionTarget.value = MusicSelectionTarget.TRACK
        _selectedTrackIds.value = emptySet()
        _selectedFolderPaths.value = emptySet()
        _selectedPlaylistIds.value = emptySet()
    }

    fun toggleTrackSelection(trackId: Long) {
        val currentSet = _selectedTrackIds.value.toMutableSet()
        if (currentSet.contains(trackId)) {
            currentSet.remove(trackId)
        } else {
            currentSet.add(trackId)
        }
        _selectedTrackIds.value = currentSet
    }

    fun toggleFolderSelection(folderPath: String) {
        val currentSet = _selectedFolderPaths.value.toMutableSet()
        if (currentSet.contains(folderPath)) {
            currentSet.remove(folderPath)
        } else {
            currentSet.add(folderPath)
        }
        _selectedFolderPaths.value = currentSet
    }

    fun togglePlaylistSelection(playlistId: Long) {
        val currentSet = _selectedPlaylistIds.value.toMutableSet()
        if (currentSet.contains(playlistId)) {
            currentSet.remove(playlistId)
        } else {
            currentSet.add(playlistId)
        }
        _selectedPlaylistIds.value = currentSet
    }

    fun selectAllTracks(trackIds: List<Long>) {
        _selectedTrackIds.value = trackIds.toSet()
    }

    fun selectAllFolders(folderPaths: List<String>) {
        _selectedFolderPaths.value = folderPaths.toSet()
    }

    fun selectAllPlaylists(playlistIds: List<Long>) {
        _selectedPlaylistIds.value = playlistIds.toSet()
    }

    fun clearTrackSelection() {
        _selectedTrackIds.value = emptySet()
    }

    fun clearFolderSelection() {
        _selectedFolderPaths.value = emptySet()
    }

    fun clearPlaylistSelection() {
        _selectedPlaylistIds.value = emptySet()
    }

    private fun applyFilterAndSort(
        tracks: List<MusicTrack>,
        query: String,
        sort: MusicSortOption
    ): List<MusicTrack> {
        var filtered = tracks
        if (query.isNotBlank()) {
            filtered = filtered.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                        track.artist.contains(query, ignoreCase = true)
            }
        }
        val sorted = when (sort) {
            MusicSortOption.TITLE_ASC -> filtered.sortedBy { it.title }
            MusicSortOption.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAddedSeconds }
            MusicSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            MusicSortOption.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
        }
        return sorted
    }
}