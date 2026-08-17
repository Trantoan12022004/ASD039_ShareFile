package com.example.basekotlin.ui.files.music.folder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.music.MusicDatabase
import com.example.basekotlin.data.local.repository.music.MusicRepository
import com.example.basekotlin.data.local.repository.music.MusicRepositoryImpl
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.RenameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    init {
        val appContext = application.applicationContext
        val database = MusicDatabase.getInstance(appContext)
        repository = MusicRepositoryImpl(appContext, database)
    }

    private val _folderPath = MutableStateFlow("")
    private val _folderName = MutableStateFlow("")

    val folderName: StateFlow<String> = _folderName

    val tracks: StateFlow<List<MusicTrack>> = _folderPath
        .flatMapLatest { path ->
            if (path.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.observeTracksByFolder(path)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode
    private val _selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTrackIds: StateFlow<Set<Long>> = _selectedTrackIds
    val playlists: StateFlow<List<MusicPlaylist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())




    fun setFolder(path: String, name: String) {
        _folderPath.value = path
        _folderName.value = name
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

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, songId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val wasAdded = repository.addTrackToPlaylist(playlistId, songId)
            onResult(wasAdded)
        }
    }

    fun isPlaylistNameTaken(name: String): Boolean {
        // So sánh tên đã trim, không phân biệt hoa thường để tránh trùng "Chill" và "chill "
        val trimmedName = name.trim()
        val currentPlaylists = playlists.value
        for (existingPlaylist in currentPlaylists) {
            val isSameName = existingPlaylist.name.trim().equals(trimmedName, ignoreCase = true)
            if (isSameName) {
                return true
            }
        }
        return false
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

    fun enterSelectionMode(initialTrackId: Long? = null) {
        _isSelectionMode.value = true
        if (initialTrackId != null) {
            _selectedTrackIds.value = setOf(initialTrackId)
        }
    }
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedTrackIds.value = emptySet()
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
    fun selectAllTracks(trackIds: List<Long>) {
        _selectedTrackIds.value = trackIds.toSet()
    }
    fun clearTrackSelection() {
        _selectedTrackIds.value = emptySet()
    }
}