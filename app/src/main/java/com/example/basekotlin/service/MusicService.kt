package com.example.basekotlin.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.basekotlin.ui.main.MainActivity

// Service phát nhạc nền, sống độc lập với Activity/Fragment.
// Chỉ có 1 ExoPlayer + 1 MediaSession duy nhất cho toàn app.
// UI không giữ Player trực tiếp mà kết nối qua MediaController (xem MusicPlayerConnection).
class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Bước 1: khai báo audio attributes cho nội dung nhạc
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Bước 2: khởi tạo ExoPlayer
        // setAudioAttributes(.., true) -> ExoPlayer tự xin/nhả audio focus khi phát/dừng
        // setHandleAudioBecomingNoisy(true) -> tự pause khi rút tai nghe/ngắt bluetooth
        // setWakeMode(NETWORK) -> giữ CPU/network khi phát nhạc từ URI local hoặc mạng
        val newPlayer = ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        newPlayer.addListener(playerListener)
        player = newPlayer

        // Bước 3: intent mở lại màn hình đang phát khi người dùng bấm vào notification
        val contentIntent = Intent(applicationContext, MainActivity::class.java)
        var pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT
        pendingIntentFlags = pendingIntentFlags or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE_CONTENT_INTENT,
            contentIntent,
            pendingIntentFlags
        )

        // Bước 4: tạo MediaSession gắn với player
        // Hệ thống sẽ tự sinh notification MediaStyle + tự startForeground khi bắt đầu phát
        val newSession = MediaSession.Builder(applicationContext, newPlayer)
            .setSessionActivity(contentPendingIntent)
            .setCallback(sessionCallback)
            .build()
        mediaSession = newSession
    }

    // Bắt buộc override — trả về session cho MediaController kết nối tới
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            // Khi phát hết danh sách và không bật repeat thì dừng service để giải phóng tài nguyên
            if (playbackState == Player.STATE_ENDED) {
                val currentPlayer = player
                if (currentPlayer != null) {
                    if (currentPlayer.repeatMode == Player.REPEAT_MODE_OFF) {
                        stopSelf()
                    }
                }
            }
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        // Nơi mở rộng sau này:
        // - override onConnect(): giới hạn quyền điều khiển theo controllerInfo (ví dụ chặn app lạ)
        // - override onCustomCommand(): nhận lệnh tuỳ biến, ví dụ "TOGGLE_FAVORITE" từ notification
        //   để gọi vào MusicRepository.toggleFavorite(songId) mà không cần thêm API.
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Nếu người dùng vuốt app khỏi Recent Apps mà nhạc không đang phát thì dừng service luôn
        var shouldStop = true
        val currentPlayer = player
        if (currentPlayer != null) {
            if (currentPlayer.playWhenReady) {
                shouldStop = false
            }
        }
        if (shouldStop) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val currentSession = mediaSession
        if (currentSession != null) {
            currentSession.player.release()
            currentSession.release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CODE_CONTENT_INTENT = 1001
    }
}