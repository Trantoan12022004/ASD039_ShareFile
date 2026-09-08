package com.example.basekotlin.ui.files.video

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.hideNavigation
import com.example.basekotlin.base.hideStatusBar
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityVideoDetailBinding
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.ui.files.video.dialog.DeleteVideoDialog
import com.example.basekotlin.ui.files.video.dialog.InformationVideoDialog
import com.example.basekotlin.ui.files.video.dialog.RenameVideoDialog
import com.example.basekotlin.ui.files.video.dialog.VideoMoreDialog
import com.example.basekotlin.ui.files.video.dialog.VideoPlayingQueueDialog
import com.example.basekotlin.ui.files.video.model.VideoInfo
import com.example.basekotlin.util.VideoToAudioConverter
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs
import androidx.core.net.toUri
import com.example.basekotlin.ui.files.video.dialog.VideoControlDialog
import com.example.basekotlin.ui.files.video.model.VideoAspectRatio
import java.lang.ref.WeakReference

@UnstableApi
class VideoDetailActivity : BaseActivity<ActivityVideoDetailBinding>(ActivityVideoDetailBinding::inflate) {

    companion object {
        // Cờ nhận biết cửa sổ PiP có đang hoạt động hay không
        var isPipActive: Boolean = false
            private set
        private var pipInstance: WeakReference<VideoDetailActivity>? = null
        // Chuyển video trực tiếp vào cửa sổ PiP đang chạy mà không mở Activity mới
        fun playInPipIfActive(targetPosition: Int, newVideoList: List<VideoInfo>? = null): Boolean {
            if (!isPipActive) return false
            val activity = pipInstance?.get()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                if (!newVideoList.isNullOrEmpty()) {
                    activity.videoList = newVideoList.toMutableList()
                }
                activity.hideAllPipControls()
                activity.playVideoAt(targetPosition)
                activity.hideAllPipControls()
                activity.updatePipParamsForCurrentVideo()
                return true
            }
            isPipActive = false
            pipInstance = null
            return false
        }
    }

    // Hàm ẩn sạch toàn bộ controls, overlay, lock & unlock khi ở PiP
    fun hideAllPipControls() {
        hideControls()
        binding.btnLock.gone()
        binding.btnUnlock.gone()
        binding.touchOverlay.gone()
    }

    // Cập nhật tỷ lệ cửa sổ PiP theo kích thước video đang phát
    fun updatePipParamsForCurrentVideo() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        setPictureInPictureParams(pipParams)
    }

    private val viewModel: VideosViewModel by viewModels()

    private var exoPlayer: ExoPlayer? = null
    private var videoList: MutableList<VideoInfo> = mutableListOf()
    private var currentIndex: Int = 0
    private var isFirstLoad: Boolean = true

    // Trạng thái Controls & Player
    private var areControlsVisible: Boolean = true
    private var isLocked: Boolean = false
    private var isMuted: Boolean = false
    private var currentSpeedIndex: Int = 1 // Mặc định 1.0x
    private val playbackSpeeds = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f)

    private val aspectRatios = arrayOf(
        VideoAspectRatio.BEST_FIT,
        VideoAspectRatio.FILL,
        VideoAspectRatio.RATIO_4_3,
        VideoAspectRatio.RATIO_16_9,
        VideoAspectRatio.RATIO_1_1
    )
    private var currentAspectRatioIndex: Int = 0

    private var isUserSeeking: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Runnable tự động ẩn Controls sau 3.5s
    private val hideControlsRunnable = Runnable {
        hideControls()
    }
    // Biến lưu trạng thái Playing Mode (mặc định Auto Play = false)
    private var isLoopMode: Boolean = false


    // Runnable ẩn feedback overlay sau 800ms
    private val hideFeedbackRunnable = Runnable {
        binding.tvFeedbackOverlay.gone()
    }

    // Runnable cập nhật tiến trình SeekBar liên tục
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying && !isUserSeeking) {
                    val current = player.currentPosition
                    val total = player.duration.coerceAtLeast(0L)
                    binding.seekBarVideo.max = total.toInt()
                    binding.seekBarVideo.progress = current.toInt()
                    binding.tvCurrentTime.text = formatDuration(current)
                    binding.tvTotalTime.text = formatDuration(total)
                }
            }
            mainHandler.postDelayed(this, 500)
        }
    }

    override fun getData() {
        super.getData()
        currentIndex = intent.getIntExtra("EXTRA_CURRENT_POSITION", 0)
    }

    override fun initView() {
        initExoPlayer()
        setupGestureDetector()
        setupSeekBar()
    }
    // Áp dụng chế độ tỷ lệ lên PlayerView
    private fun applyAspectRatio(aspectRatio: VideoAspectRatio) {
        val params = binding.playerView.layoutParams as ConstraintLayout.LayoutParams
        params.dimensionRatio = aspectRatio.ratioString
        params.constrainedWidth = true
        params.constrainedHeight = true
        binding.playerView.layoutParams = params
        // Gán chế độ co dãn / crop tương ứng:
        // - Fill: RESIZE_MODE_FIT (giữ nguyên tỷ lệ gốc, hiển thị trọn vẹn)
        // - 4:3, 16:9, 1:1: RESIZE_MODE_ZOOM (crop video vừa khít khung)
        binding.playerView.resizeMode = aspectRatio.resizeMode
    }

    override fun bindView() {
        super.bindView()

        // 1. Header controls
        binding.btnBack.tap { onBack() }
        binding.btnSend.tap { shareCurrentVideo() }
        binding.btnMore.tap { showVideoControlDialog() }

        // 2. Main Center Playback Controls
        binding.btnPlayPause.tap { togglePlayPause() }
        binding.btnPrevious.tap { playPreviousVideo() }
        binding.btnNext.tap { playNextVideo() }

        // 3. Bottom Playback Controls
        binding.btnLock.tap { toggleLock() }
        binding.btnUnlock.tap { toggleLock() }
        binding.btnSpeed.tap { cyclePlaybackSpeed() }
        binding.btnVolume.tap { toggleVolume() }
        binding.btnAspectRatio.tap { cycleAspectRatio() }
        binding.btnOrientation.tap { toggleOrientation() }
        binding.btnPip.tap { enterPiPMode() }
        binding.btnPlayQueue.tap { showPlayingQueue() }

        // Lắng nghe dữ liệu danh sách video từ ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allVideosUi.collect { list ->
                    if (list.isNotEmpty()) {
                        videoList = list.toMutableList()
                        if (isFirstLoad) {
                            isFirstLoad = false
                            if (currentIndex in videoList.indices) {
                                playVideoAt(currentIndex)
                            }
                        }
                    }
                }
            }
        }
    }

    // Kiểm tra trạng thái màn hình ngang
    private fun isLandscapeMode(): Boolean {
        val orientation = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE ||
                requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    // Hiển thị VideoControlDialog
    private fun showVideoControlDialog() {
        if (currentIndex !in videoList.indices) return
        val currentVideo = videoList[currentIndex]
        // Nếu đang xoay ngang: Ẩn thanh bottom controls
        if (isLandscapeMode()) {
            binding.layoutBottomControls.gone()
        }
        val dialog = VideoControlDialog(
            activity = this,
            currentRatio = aspectRatios[currentAspectRatioIndex],
            isLoopMode = isLoopMode,
            onDelete = { showDeleteConfirmDialog(currentVideo) },
            onShare = { shareCurrentVideo() },
            onSend = { Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show() },
            onMore = { showMoreMenu() },
            onAspectRatioChanged = { newRatio ->
                currentAspectRatioIndex = aspectRatios.indexOf(newRatio).coerceAtLeast(0)
                applyAspectRatio(newRatio)
            },
            onPlayingModeChanged = { loop ->
                isLoopMode = loop
                exoPlayer?.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
        )
        // Khi dialog đóng -> Phục hồi hiển thị nếu controls đang mở và không bị khóa
        dialog.setOnDismissListener {
            if (!isLocked && areControlsVisible) {
                binding.layoutBottomControls.visible()
                scheduleHideControls()
            }
        }
        dialog.show()
    }

    // ================= 1. EXO PLAYER PLAYBACK =================
    private fun initExoPlayer() {
        binding.playerView.visible() // Đảm bảo player view hiển thị
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            binding.playerView.player = this
            binding.playerView.useController = false // Dùng toàn bộ custom controls

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.progressLoading.visible()
                        }
                        Player.STATE_READY -> {
                            binding.progressLoading.gone()
                            binding.tvTotalTime.text = formatDuration(duration.coerceAtLeast(0L))
                            updatePlayPauseButton(isPlaying)
                        }
                        Player.STATE_ENDED -> {
                            binding.progressLoading.gone()
                            updatePlayPauseButton(false)
                            if (!isInPictureInPictureMode && !isPipActive) {
                                showControls(keepVisible = true)
                            } else {
                                hideAllPipControls()
                            }
                            if (!isLoopMode && currentIndex < videoList.size - 1) {
                                playNextVideo()
                            }
                        }
                        else -> {
                            binding.progressLoading.gone()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                    if (isInPictureInPictureMode || isPipActive) {
                        hideAllPipControls()
                        return
                    }
                    if (isPlaying) {
                        scheduleHideControls()
                    } else {
                        showControls(keepVisible = true)
                    }
                }
            })
            mainHandler.post(updateProgressRunnable)
        }
    }

    // Phát video tại vị trí index
    private fun playVideoAt(index: Int) {
        if (index !in videoList.indices) return
        currentIndex = index
        val video = videoList[currentIndex]

        binding.tvVideoTitle.text = video.displayName
        binding.tvTotalTime.text = formatDuration(video.durationMs)
        binding.seekBarVideo.progress = 0
        binding.tvCurrentTime.text = formatDuration(0L)

        if (isInPictureInPictureMode || isPipActive) {
            hideAllPipControls()
        }

        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(video.contentUri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }

        if (isInPictureInPictureMode || isPipActive) {
            hideAllPipControls()
        } else {
            scheduleHideControls()
        }
    }

    // Bật/Tạm dừng phát video
    private fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
            scheduleHideControls()
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.btn_pause1)
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.btn_play1)
        }
    }

    private fun playPreviousVideo() {
        if (currentIndex > 0) {
            playVideoAt(currentIndex - 1)
        } else {
            Toast.makeText(this, getString(R.string.no_previous_video), Toast.LENGTH_SHORT).show()
        }
    }

    private fun playNextVideo() {
        if (currentIndex < videoList.size - 1) {
            playVideoAt(currentIndex + 1)
        } else {
            Toast.makeText(this, getString(R.string.no_next_video), Toast.LENGTH_SHORT).show()
        }
    }

    // ================= 2. SEEKBAR =================
    private fun setupSeekBar() {
        binding.seekBarVideo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                mainHandler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                val targetPosition = seekBar?.progress?.toLong() ?: 0L
                exoPlayer?.seekTo(targetPosition)
                scheduleHideControls()
            }
        })
    }

    // ================= 3. GESTURE & TOUCH INTERACTIONS =================
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetector() {
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (isLocked) {
                        // Khi đang khóa, chạm nhẹ hiện nút Unlock trong thoáng chốc
                        binding.btnUnlock.visible()
                        mainHandler.removeCallbacks(hideFeedbackRunnable)
                        mainHandler.postDelayed({
                            if (isLocked) binding.btnUnlock.visible()
                        }, 2000)
                    } else {
                        if (areControlsVisible) hideControls() else showControls()
                    }
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (isLocked) return false
                    val screenWidth = binding.root.width
                    if (e.x < screenWidth / 2) {
                        // Double tap bên trái: tua lùi 10s
                        rewind10Seconds()
                    } else {
                        // Double tap bên phải: tua tới 10s
                        forward10Seconds()
                    }
                    return true
                }

                // Hỗ trợ cử chỉ vuốt ngang chuyển video trước/sau
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (isLocked || e1 == null) return false
                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y
                    if (abs(diffX) > abs(diffY) && abs(diffX) > 100 && abs(velocityX) > 100) {
                        if (diffX > 0) {
                            // Vuốt từ trái sang phải -> Video trước đó
                            playPreviousVideo()
                        } else {
                            // Vuốt từ phải sang trái -> Video kế tiếp
                            playNextVideo()
                        }
                        return true
                    }
                    return false
                }
            })

        binding.touchOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun rewind10Seconds() {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition - 10000L).coerceAtLeast(0L)
            player.seekTo(newPosition)
            showFeedbackIndicator("-10s")
            scheduleHideControls()
        }
    }

    private fun forward10Seconds() {
        exoPlayer?.let { player ->
            val total = player.duration.coerceAtLeast(0L)
            val newPosition = (player.currentPosition + 10000L).coerceAtMost(total)
            player.seekTo(newPosition)
            showFeedbackIndicator("+10s")
            scheduleHideControls()
        }
    }

    // ================= 4. CONTROLS VISIBILITY & AUTO-HIDE =================
    private fun showControls(keepVisible: Boolean = false) {
        if (isLocked || isInPictureInPictureMode || isPipActive) return
        areControlsVisible = true
        binding.layoutHeader.visible()
        binding.layoutCenterControls.visible()
        binding.layoutBottomControls.visible()
        binding.btnLock.visible()

        if (!keepVisible) {
            scheduleHideControls()
        }
    }

    private fun hideControls() {
        areControlsVisible = false
        binding.layoutHeader.gone()
        binding.layoutCenterControls.gone()
        binding.layoutBottomControls.gone()
        binding.btnLock.gone()
    }

    private fun scheduleHideControls() {
        mainHandler.removeCallbacks(hideControlsRunnable)
        if (!isLocked && exoPlayer?.isPlaying == true) {
            mainHandler.postDelayed(hideControlsRunnable, 3500)
        }
    }

    private fun showFeedbackIndicator(text: String) {
        binding.tvFeedbackOverlay.text = text
        binding.tvFeedbackOverlay.visible()
        mainHandler.removeCallbacks(hideFeedbackRunnable)
        mainHandler.postDelayed(hideFeedbackRunnable, 800)
    }

    // ================= 5. BOTTOM CONTROL ACTIONS =================
    // 5.1. Thay đổi tốc độ phát video
    private fun cyclePlaybackSpeed() {
        currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size
        val speed = playbackSpeeds[currentSpeedIndex]
        exoPlayer?.setPlaybackSpeed(speed)
        binding.btnSpeed.text = "${speed}x"
        showFeedbackIndicator("${speed}x")
        scheduleHideControls()
    }

    // 5.2. Bật/Tắt âm thanh (Mute/Unmute)
    private fun toggleVolume() {
        isMuted = !isMuted
        exoPlayer?.volume = if (isMuted) 0f else 1f
        binding.btnVolume.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)
        showFeedbackIndicator(if (isMuted) "Mute" else "Unmute")
        scheduleHideControls()
    }

    // 5.3. Thay đổi tỷ lệ khung hình (Aspect Ratio)
    private fun cycleAspectRatio() {
        currentAspectRatioIndex = (currentAspectRatioIndex + 1) % aspectRatios.size
        val currentRatio = aspectRatios[currentAspectRatioIndex]
        applyAspectRatio(currentRatio)
        showFeedbackIndicator(currentRatio.label)
        scheduleHideControls()
    }

    // 5.4. Xoay màn hình Portrait / Landscape
    private fun toggleOrientation() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        scheduleHideControls()
    }

    // 5.5. Chế độ Picture-in-Picture
    private fun enterPiPMode() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            Toast.makeText(this, getString(R.string.pip_not_supported), Toast.LENGTH_SHORT).show()
            return
        }
        // 1. Kiểm tra quyền PiP của ứng dụng
        if (!hasPipPermission()) {
            openPipSettings()
            return
        }
        // 2. Nếu đã có quyền -> Tiến hành vào PiP
        try {
            hideControls()

            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()

            enterPictureInPictureMode(pipParams)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.pip_not_supported), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPipPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
        }
    }

    // Mở trang cài đặt Picture-in-Picture của ứng dụng
    private fun openPipSettings() {
        val intent = Intent(
            "android.settings.PICTURE_IN_PICTURE_SETTINGS",
            "package:$packageName".toUri()
        )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback nếu một số máy ROM tùy biến không mở được intent riêng của PiP
            val fallbackIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri()
            )
            startActivity(fallbackIntent)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipActive = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            pipInstance = WeakReference(this)
            hideAllPipControls()
        } else {
            pipInstance = null
            binding.touchOverlay.visible()
            if (isLocked) {
                binding.btnUnlock.visible()
            } else {
                showControls()
            }
        }
    }


    // 5.6. Khóa / Mở khóa controls
    private fun toggleLock() {
        if (isLocked) {
            isLocked = false
            binding.btnUnlock.gone()
            showControls()
            showFeedbackIndicator(getString(R.string.screen_unlocked))
        } else {
            isLocked = true
            hideControls()
            binding.btnUnlock.visible()
            showFeedbackIndicator(getString(R.string.screen_locked))
        }
    }

    // 5.7. Hiển thị Playing Queue Bottom Sheet
    private fun showPlayingQueue() {
        // Nếu đang xoay ngang: Ẩn thanh bottom controls
        if (isLandscapeMode()) {
            binding.layoutBottomControls.gone()
        }
        val dialog = VideoPlayingQueueDialog(
            context = this,
            videos = videoList,
            currentIndex = currentIndex,
            onVideoClick = { selectedIndex ->
                playVideoAt(selectedIndex)
            }
        )
        // Khi dialog đóng -> Phục hồi hiển thị nếu controls đang mở và không bị khóa
        dialog.setOnDismissListener {
            if (!isLocked && areControlsVisible) {
                binding.layoutBottomControls.visible()
                scheduleHideControls()
            }
        }
        dialog.show()
    }

    // ================= 6. HEADER ACTIONS & MORE MENU =================
    // Chia sẻ video hiện tại
    private fun shareCurrentVideo() {
        if (currentIndex !in videoList.indices) return
        val currentVideo = videoList[currentIndex]
        val file = File(currentVideo.filePath)
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show()
        }
    }

    // Mở menu More cho video hiện tại
    private fun showMoreMenu() {
        if (currentIndex !in videoList.indices) return
        val currentVideo = videoList[currentIndex]

        VideoMoreDialog(
            context = this,
            selectedVideos = listOf(currentVideo),
            isFolderTab = false,
            onRename = { video -> showRenameDialog(video) },
            onToMp3 = { video -> showToMp3Dialog(video) },
            onInformation = { video -> InformationVideoDialog(this, video).show() },
            onMoveSafeBox = { /* TODO: SafeBox feature */ }
        ).show()
    }

    // Hiển thị dialog đổi tên video
    private fun showRenameDialog(video: VideoInfo) {
        val targetFile = File(video.filePath)
        val initName = targetFile.nameWithoutExtension
        val dialog = RenameVideoDialog(
            context = this,
            initText = initName,
            validate = { enteredName ->
                val ext = targetFile.extension
                val candidateName = if (ext.isNotEmpty()) "$enteredName.$ext" else enteredName
                val parentDir = targetFile.parentFile
                if (parentDir != null) {
                    val destFile = File(parentDir, candidateName)
                    if (destFile.exists() && destFile.absolutePath != targetFile.absolutePath) {
                        getString(R.string.text_input_failed1)
                    } else null
                } else null
            }
        ) { enteredNewName ->
            val ext = targetFile.extension
            val finalName = if (ext.isNotEmpty()) "$enteredNewName.$ext" else enteredNewName
            val destFile = File(targetFile.parentFile, finalName)
            if (targetFile.renameTo(destFile)) {
                Toast.makeText(this, getString(R.string.rename_video_success), Toast.LENGTH_SHORT).show()
                binding.tvVideoTitle.text = finalName
                viewModel.refreshAllVideos()
            } else {
                Toast.makeText(this, getString(R.string.rename_video_failed), Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // Hiển thị dialog Convert sang MP3
    private fun showToMp3Dialog(video: VideoInfo) {
        val file = File(video.filePath)
        val defaultMp3Name = "${file.nameWithoutExtension}.mp3"
        TextInputDialog(
            context = this,
            title = getString(R.string.to_mp3),
            hint = getString(R.string.enter_file_name),
            initialText = defaultMp3Name,
            positiveText = getString(R.string.convert_to_mp3)
        ) { chosenFileName ->
            convertVideoToMp3(video, chosenFileName)
        }.show()
    }

    private fun convertVideoToMp3(video: VideoInfo, outputFileName: String) {
        Toast.makeText(this, getString(R.string.converting_to_mp3), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val convertedFile = VideoToAudioConverter.convertVideoToMp3(
                context = this@VideoDetailActivity,
                videoPath = video.filePath,
                outputFileName = outputFileName
            )
            if (convertedFile != null) {
                Toast.makeText(this@VideoDetailActivity, getString(R.string.convert_to_mp3_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@VideoDetailActivity, getString(R.string.convert_to_mp3_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Xác nhận và xóa video hiện tại
    private fun showDeleteConfirmDialog(video: VideoInfo) {
        DeleteVideoDialog(
            context = this,
            selectedVideos = listOf(video),
            onConfirm = { performDeleteVideo(video) }
        ).show()
    }

    private fun performDeleteVideo(video: VideoInfo) {
        val file = File(video.filePath)
        val isDeleted = file.delete()
        if (isDeleted) {
            Toast.makeText(this, getString(R.string.delete_video_success), Toast.LENGTH_SHORT).show()
            videoList.removeAt(currentIndex)
            viewModel.refreshAllVideos()
            if (videoList.isEmpty()) {
                finishThisActivity()
            } else {
                currentIndex = currentIndex.coerceAtMost(videoList.size - 1)
                playVideoAt(currentIndex)
            }
        } else {
            Toast.makeText(this, getString(R.string.delete_video_failed), Toast.LENGTH_SHORT).show()
        }
    }

    // ================= 7. TIỆN ÍCH & LIFECYCLE =================
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    override fun onResume() {
        super.onResume()
        // Đảm bảo chế độ fullscreen toàn màn hình
        window.decorView.post {
            hideNavigationAndStatusBar()
        }
    }

    override fun onPause() {
        super.onPause()
        // Nếu không ở chế độ PiP thì tạm dừng video
        if (!isInPictureInPictureMode) {
            exoPlayer?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing || !isInPictureInPictureMode) {
            exoPlayer?.pause()
        }
    }

    override fun onDestroy() {
        isPipActive = false
        pipInstance = null
        mainHandler.removeCallbacksAndMessages(null)
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    override fun onBack() {
        setResult(RESULT_OK)
        finishThisActivity()
    }

    private fun hideNavigationAndStatusBar() {
        try {
            window.hideNavigation()
            window.hideStatusBar()
        } catch (_: Exception) {}
    }
}
