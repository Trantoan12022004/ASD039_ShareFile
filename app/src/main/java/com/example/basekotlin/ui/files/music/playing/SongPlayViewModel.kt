package com.example.basekotlin.ui.files.music.playing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.mediastore.MusicPlayerConnection
import com.example.basekotlin.data.local.music.MusicDatabase
import com.example.basekotlin.data.local.repository.MusicRepository
import com.example.basekotlin.data.local.repository.MusicRepositoryImpl
import com.example.basekotlin.model.LyricLine
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.util.LyricsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel cho màn hình đang phát nhạc.
// Không giữ Player trực tiếp mà chỉ đọc/ điều khiển qua MusicPlayerConnection (singleton dùng chung toàn app).
class SongPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository
    init {
        val appContext = application.applicationContext
        val database = MusicDatabase.getInstance(appContext)
        repository = MusicRepositoryImpl(appContext, database)
    }

    // Bài hát đang phát hiện tại (đã kèm đủ thông tin title/artist/ảnh...)
    val currentTrack: StateFlow<MusicTrack?> = MusicPlayerConnection.currentTrack

    val isCurrentTrackFavorite: StateFlow<Boolean> = currentTrack
        .flatMapLatest { track ->
            if (track == null) {
                flowOf(false)
            } else {
                repository.observeIsFavorite(track.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Trạng thái đang phát hay đang tạm dừng
    val isPlaying: StateFlow<Boolean> = MusicPlayerConnection.isPlaying

    val shuffleModeEnabled: StateFlow<Boolean> = MusicPlayerConnection.shuffleModeEnabled

    val repeatMode: StateFlow<Int> = MusicPlayerConnection.repeatMode

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.NoLyrics)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    sealed class LyricsUiState {
        object NoLyrics : LyricsUiState()
        data class Loaded(val lines: List<LyricLine>) : LyricsUiState()
    }

    init {
        // Mỗi khi bài hát đổi (kể cả lần đầu có track), tải lại lyric tương ứng.
        // MusicPlayerConnection.currentTrack là singleton dùng chung toàn app nên chỉ cần
        // collect ở đây là tự động bắt được mọi lần chuyển bài, kể cả next/prev từ notification.
        viewModelScope.launch {
            currentTrack.collect { track ->
                if (track == null) {
                    _lyricsState.value = LyricsUiState.NoLyrics
                } else {
                    val appContext = getApplication<Application>().applicationContext
                    val lines = LyricsSource.loadLyrics(appContext, track)
                    if (lines.isEmpty()) {
                        _lyricsState.value = LyricsUiState.NoLyrics
                    } else {
                        _lyricsState.value = LyricsUiState.Loaded(lines)
                    }
                }
            }
        }
    }

    fun toggleFavoriteForCurrentTrack() {
        val track = currentTrack.value
        if (track != null) {
            viewModelScope.launch {
                repository.toggleFavorite(track.id)
            }
        }
    }

    fun togglePlayPause() {
        MusicPlayerConnection.togglePlayPause()
    }

    fun toggleShuffleMode() {
        MusicPlayerConnection.toggleShuffleMode()
    }

    fun toggleRepeatMode() {
        MusicPlayerConnection.toggleRepeatMode()
    }

    fun skipToNext() {
        MusicPlayerConnection.skipToNext()
    }

    fun skipToPrevious() {
        MusicPlayerConnection.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        MusicPlayerConnection.seekTo(positionMs)
    }
    fun skipToQueueItem(track: MusicTrack) {
        MusicPlayerConnection.skipToQueueItem(track)
    }

    fun getCurrentPositionMs(): Long {
        return MusicPlayerConnection.getCurrentPositionMs()
    }

    fun getDurationMs(): Long {
        return MusicPlayerConnection.getDurationMs()
    }
}
