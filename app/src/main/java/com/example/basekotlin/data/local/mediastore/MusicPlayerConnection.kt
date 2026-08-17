package com.example.basekotlin.data.local.mediastore

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.basekotlin.data.local.music.MusicDatabase
import com.example.basekotlin.data.local.repository.music.MusicRepository
import com.example.basekotlin.data.local.repository.music.MusicRepositoryImpl
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.service.MusicService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Singleton bọc MediaController — điểm truy cập DUY NHẤT để mọi Activity/Fragment
// điều khiển MusicService. Tránh mỗi màn hình tự tạo MediaController riêng.
object MusicPlayerConnection {

    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackId = MutableStateFlow<Long?>(null)
    val currentTrackId: StateFlow<Long?> = _currentTrackId.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private var pendingTracks: List<MusicTrack>? = null
    private var pendingStartIndex: Int = 0

    // Danh sách bài hát đang phát (hàng đợi hiện tại) — dùng để tra cứu MusicTrack đầy đủ
    // (title, artist, ảnh...) từ mediaId khi track chuyển đổi.
    private val _queueTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val queueTracks: StateFlow<List<MusicTrack>> = _queueTracks.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Repository riêng của connection — dùng để ghi nhận lịch sử phát ngay khi track chuyển đổi,
    // không phụ thuộc vào bất kỳ ViewModel/Fragment nào đang mở.
    private var repository: MusicRepository? = null
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            _isPlaying.value = isPlayingNow
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            var trackId: Long? = null
            if (mediaItem != null) {
                trackId = mediaItem.mediaId.toLongOrNull()
            }
            _currentTrackId.value = trackId

            // Tìm MusicTrack tương ứng trong hàng đợi hiện tại để UI (SongPlay...) có đủ thông tin
            var matchedTrack: MusicTrack? = null
            if (trackId != null) {
                val currentQueue = _queueTracks.value
                for (track in currentQueue) {
                    if (track.id == trackId) {
                        matchedTrack = track
                        break
                    }
                }
            }
            _currentTrack.value = matchedTrack

            val activeController = controller
            if (activeController != null) {
                _durationMs.value = activeController.duration.coerceAtLeast(0L)
            }

            // Ghi nhận "đã phát" tại ĐIỂM DUY NHẤT này — áp dụng cho mọi nguồn phát:
            // All, Favorite, Playlist, Recently Played, Album, Artist, Search, Next/Previous...
            if (trackId != null) {
                val activeRepository = repository
                if (activeRepository != null) {
                    connectionScope.launch {
                        activeRepository.markPlayed(trackId)
                    }
                }
            }
        }
    }

    // Gọi 1 lần, ví dụ trong MyApplication.onCreate() hoặc lần đầu người dùng mở màn hình nhạc
    fun connect(context: Context) {
        if (controller != null) {
            return
        }
        val appContext = context.applicationContext

        if (repository == null) {
            repository = MusicRepositoryImpl(appContext, MusicDatabase.getInstance(appContext))
        }
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MusicService::class.java)
        )
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val newController = controllerFuture.get()
                newController.addListener(playerListener)
                controller = newController

                _shuffleModeEnabled.value = newController.shuffleModeEnabled
                _repeatMode.value = newController.repeatMode
            },
            MoreExecutors.directExecutor()
        )
    }

    // Phát 1 danh sách bài hát bắt đầu từ vị trí startIndex (dùng cho All/Playlist/Favorite...)
    fun playTracks(tracks: List<MusicTrack>, startIndex: Int) {
        // Ghi nhớ hàng đợi ngay để onMediaItemTransition tra cứu được MusicTrack đầy đủ
        _queueTracks.value = tracks

        val activeController = controller
        if (activeController == null) {
            pendingTracks = tracks
            pendingStartIndex = startIndex
            return
        }

        val mediaItems = mutableListOf<MediaItem>()
        for (track in tracks) {
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .build()
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.contentUri)
                .setMediaMetadata(metadata)
                .build()
            mediaItems.add(mediaItem)
        }

        activeController.setMediaItems(mediaItems, startIndex, 0L)
        activeController.prepare()
        activeController.play()
    }

    fun togglePlayPause() {
        val activeController = controller
        if (activeController != null) {
            if (activeController.isPlaying) {
                activeController.pause()
            } else {
                activeController.play()
            }
        }
    }

    fun skipToNext() {
        val activeController = controller
        if (activeController != null) {
            activeController.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val activeController = controller
        if (activeController != null) {
            activeController.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        val activeController = controller
        if (activeController != null) {
            activeController.seekTo(positionMs)
        }
    }

    fun skipToQueueItem(track: MusicTrack) {
        val activeController = controller
        if (activeController == null) {
            return
        }
        val currentQueue = _queueTracks.value
        var index = 0
        for (i in currentQueue.indices) {
            if (currentQueue[i].id == track.id) {
                index = i
                break
            }
        }
        activeController.seekToDefaultPosition(index)
    }

    fun getCurrentPositionMs(): Long {
        val activeController = controller
        if (activeController != null) {
            return activeController.currentPosition
        }
        return 0L
    }

    // Đọc trực tiếp duration từ controller — dùng để cập nhật liên tục vì lúc mới chuyển bài
    // (onMediaItemTransition) player có thể chưa kịp biết duration (trả về C.TIME_UNSET).
    fun getDurationMs(): Long {
        val activeController = controller
        if (activeController != null) {
            val duration = activeController.duration
            if (duration > 0) {
                return duration
            }
        }
        return 0L
    }

    fun setRepeatMode(repeatMode: Int) {
        val activeController = controller
        if (activeController != null) {
            activeController.repeatMode = repeatMode
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        val activeController = controller
        if (activeController != null) {
            activeController.shuffleModeEnabled = enabled
        }
    }

    // Bật/tắt shuffle — ExoPlayer tự xáo thứ tự phát khi shuffleModeEnabled = true
    fun toggleShuffleMode() {
        val activeController = controller
        if (activeController != null) {
            val newEnabled = !activeController.shuffleModeEnabled
            activeController.shuffleModeEnabled = newEnabled
        }
    }

    // Chuyển vòng: OFF → ONE → ALL → OFF (giống app nhạc cũ)
    fun toggleRepeatMode() {
        val activeController = controller
        if (activeController != null) {
            val currentMode = activeController.repeatMode
            val nextMode: Int
            if (currentMode == Player.REPEAT_MODE_OFF) {
                nextMode = Player.REPEAT_MODE_ONE
            } else if (currentMode == Player.REPEAT_MODE_ONE) {
                nextMode = Player.REPEAT_MODE_ALL
            } else {
                nextMode = Player.REPEAT_MODE_OFF
            }
            activeController.repeatMode = nextMode
        }
    }

    // Gọi khi app bị kill hẳn (MyApplication) nếu muốn giải phóng — thường không cần gọi
    // ở từng Activity/Fragment vì controller dùng chung toàn app.
    fun release() {
        val activeController = controller
        if (activeController != null) {
            activeController.removeListener(playerListener)
            activeController.release()
            controller = null
        }
    }
}