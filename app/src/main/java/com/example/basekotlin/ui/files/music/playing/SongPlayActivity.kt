package com.example.basekotlin.ui.files.music.playing

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.mediastore.MusicPlayerConnection
import com.example.basekotlin.databinding.ActivitySongPlayBinding
import com.example.basekotlin.databinding.PopupMoreBinding
import com.example.basekotlin.databinding.PopupPlayingMoreBinding
import com.example.basekotlin.dialog.common.MyPlaylistDialog
import com.example.basekotlin.dialog.common.NowPlayingDialog
import com.example.basekotlin.dialog.common.SelectPlaylistDialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.LyricLine
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.ui.files.music.MusicViewModel
import com.example.basekotlin.util.AlbumArtUtils
import com.example.basekotlin.util.LrcParser
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SongPlayActivity : BaseActivity<ActivitySongPlayBinding>(ActivitySongPlayBinding::inflate) {

    private val viewModel: SongPlayViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()

    // Animator xoay tròn liên tục cho đĩa vinyl
    private var vinylAnimator: ObjectAnimator? = null
    private var isVinylAnimatorStarted = false

    // Đang kéo seekBar hay không, để ticker không ghi đè progress trong lúc người dùng kéo
    private var isUserSeeking = false



    // Danh sách lyric của bài đang phát và chỉ số dòng đang được highlight (-1 = chưa xác định)
    private var lyricLines: List<LyricLine> = emptyList()
    private var currentLyricIndex = -1

    private var playLists: List<MusicPlaylist> = emptyList()
    private var queueTracks: List<MusicTrack> = emptyList()
    // Chiều cao 1 dòng lyric (px) - dùng để tính offset căn giữa và khoảng cách focus effect
    private var lyricItemHeightPx = 0
    private var lyricActiveColor = 0
    private var lyricInactiveColor = 0
    private val lyricColorEvaluator = ArgbEvaluator()

    override fun initView() {
        // Đảm bảo đã kết nối tới MusicService (idempotent, không tạo controller mới nếu đã có)
        MusicPlayerConnection.connect(this)

        setupVinylAnimator()
        setupTonearmPivot()
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap { onBack() }

        binding.btnPlayPause.tap { viewModel.togglePlayPause() }
        binding.btnNext.tap { viewModel.skipToNext() }
        binding.btnPrev.tap { viewModel.skipToPrevious() }
        binding.btnShuffle.tap { viewModel.toggleShuffleMode() }
        binding.btnRepeat.tap { viewModel.toggleRepeatMode() }
        binding.layoutToolbar.btnMore.tap {
            showMenuMore()
        }

        binding.btnFavorite.tap {
            viewModel.toggleFavoriteForCurrentTrack()
        }

        binding.btnPlaylist.tap {
            val currentTrack = viewModel.currentTrack.value ?: return@tap
            showMyPlaylistDialog(currentTrack)
        }

        binding.btnQueue.tap {
            showNowPlayingDialog()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val targetProgress = seekBar?.progress ?: 0
                viewModel.seekTo(targetProgress.toLong())
                isUserSeeking = false
            }
        })

        observeCurrentTrack()
        observePlaybackState()
        observeShuffleRepeat()
        observePlaybackProgress()
        observeLyrics()
        observeFavoriteState()
        observePlaylists()
        observeQueueTracks()
    }

    private fun showMyPlaylistDialog(track: MusicTrack) {
        MyPlaylistDialog(
            context = this,
            playlists = playLists,
            onCreateNewPlaylist = {
                showCreatePlaylistDialog(track)
            },
            onPlaylistSelected = { playlist ->
                musicViewModel.addTrackToPlaylist(playlist.id, track.id) { wasAdded ->
                    if (wasAdded) {
                        Toast.makeText(
                            this,
                            getString(R.string.song_added_to_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.song_already_in_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        ).show()
    }

    private fun showNowPlayingDialog() {
        NowPlayingDialog(
            context = this,
            tracks = queueTracks,
            onTrackClick = { track ->
                viewModel.skipToQueueItem(track)
            }
        ).show()
    }

    private fun showCreatePlaylistDialog(track: MusicTrack) {
        TextInputDialog(
            context = this,
            title = getString(R.string.create_new_playlist),
            hint = getString(R.string.create_new_playlist),
            positiveText = getString(R.string.create),
            validate = { enteredName ->
                val isNameTaken = musicViewModel.isPlaylistNameTaken(enteredName)
                if (isNameTaken) {
                    getString(R.string.text_input_failed1)
                } else {
                    null
                }
            }
        ) { enteredName ->
            musicViewModel.createPlaylist(enteredName) { newPlaylistId ->
                musicViewModel.addTrackToPlaylist(newPlaylistId, track.id)
            }
        }.show()
    }

    private fun observePlaylists(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicViewModel.playlists.collect { list ->
                    playLists = list
                }
            }
        }
    }

    private fun observeQueueTracks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                MusicPlayerConnection.queueTracks.collect { tracks ->
                    queueTracks = tracks
                }
            }
        }
    }
    private fun observeFavoriteState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isCurrentTrackFavorite.collect { isFavorite ->
                    if (isFavorite) {
                        binding.btnFavorite.setImageResource(R.drawable.ic_favorite_select)
                    } else {
                        binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                    }
                }
            }
        }
    }

    // Cập nhật tên bài hát, nghệ sĩ, ảnh mâm đĩa và tổng thời lượng khi track thay đổi
    private fun observeCurrentTrack() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTrack.collect { track ->
                    if (track != null) {
                        binding.layoutToolbar.tvTitle.text = track.title
                        binding.layoutToolbar.tvSubtitle.text = track.artist

                        updateVinylArt(track)

                        binding.seekBar.max = track.durationMs.toInt()
                        binding.tvTotalTime.text = formatTime(track.durationMs)
                    }
                }
            }
        }
    }

    // Cập nhật icon shuffle/repeat khi chế độ phát thay đổi
    private fun observeShuffleRepeat() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shuffleModeEnabled.collect { isShuffleOn ->
                    updateShuffleIcon(isShuffleOn)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.repeatMode.collect { mode ->
                    updateRepeatIcon(mode)
                }
            }
        }
    }

    // Shuffle: OFF = ic_shuffle2, ON = ic_shuffle2_select
    private fun updateShuffleIcon(isShuffleOn: Boolean) {
        val iconRes: Int
        if (isShuffleOn) {
            iconRes = R.drawable.ic_shuffle2_select
        } else {
            iconRes = R.drawable.ic_shuffle2
        }
        binding.btnShuffle.setImageResource(iconRes)
    }

    // Repeat: OFF → ic_repeat_none, ONE → ic_repeat_one, ALL → ic_repeat
    private fun updateRepeatIcon(repeatMode: Int) {
        val iconRes: Int
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            iconRes = R.drawable.ic_repeat_one
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            iconRes = R.drawable.ic_repeat
        } else {
            iconRes = R.drawable.ic_repeat_none
        }
        binding.btnRepeat.setImageResource(iconRes)
    }

    // Cập nhật icon play/pause, xoay đĩa và gạt cần kim khi trạng thái phát thay đổi
    private fun observePlaybackState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPlaying.collect { isPlaying ->
                    updatePlayPauseIcon(isPlaying)
                    updateVinylRotation(isPlaying)
                    animateTonearm(isPlaying)
                }
            }
        }
    }

    // Vòng lặp cập nhật seekBar + thời gian hiện tại + dòng lyric đang phát theo vị trí phát thực tế
    private fun observePlaybackProgress() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (!isUserSeeking) {
                        val durationMs = viewModel.getDurationMs()
                        if (durationMs > 0 && binding.seekBar.max != durationMs.toInt()) {
                            binding.seekBar.max = durationMs.toInt()
                            binding.tvTotalTime.text = formatTime(durationMs)
                        }

                        val positionMs = viewModel.getCurrentPositionMs()
                        binding.seekBar.progress = positionMs.toInt()
                        binding.tvCurrentTime.text = formatTime(positionMs)

                        updateLyricByPosition(positionMs)
                    }

                    delay(PROGRESS_TICK_INTERVAL_MS)
                }
            }
        }
    }

    // Tính dòng lyric đang phát tại positionMs, chỉ cập nhật UI khi dòng thay đổi.
    private fun updateLyricByPosition(positionMs: Long) {
        if (lyricLines.isEmpty()) {
            return
        }

        val newIndex = LrcParser.findActiveIndex(lyricLines, positionMs)
        if (newIndex == currentLyricIndex) {
            return
        }
        currentLyricIndex = newIndex

        val prevText: String
        if (newIndex - 1 >= 0) {
            prevText = lyricLines[newIndex - 1].text
        } else {
            prevText = ""
        }

        val currText: String
        if (newIndex >= 0) {
            currText = lyricLines[newIndex].text
        } else {
            currText = ""
        }

        val nextText: String
        if (newIndex + 1 < lyricLines.size) {
            nextText = lyricLines[newIndex + 1].text
        } else {
            nextText = ""
        }

        animateLyricLine(binding.tvLyricPrev, prevText)
        animateLyricLine(binding.tvLyricCurrent, currText)
        animateLyricLine(binding.tvLyricNext, nextText)
    }

    // Hiệu ứng "đẩy lên" giống RetroMusicPlayer (InlineLyricsController.animateLineChange):
    // đẩy TextView xuống dưới đúng bằng chiều cao của chính nó + ẩn đi, gán text mới, rồi animate
    // trượt lên vị trí thật + hiện rõ dần. Nếu text không đổi thì bỏ qua, tránh animate thừa.
    // Khoảng cách trượt CỐ ĐỊNH cho mọi dòng (không phụ thuộc chiều cao riêng từng dòng) để
    // cả 3 dòng luôn chuyển động đồng bộ, cảm giác mượt và thống nhất hơn.
    private val lyricSlideInterpolator = DecelerateInterpolator(1.5f)
    private fun animateLyricLine(textView: TextView, newText: String) {
        if (textView.text.toString() == newText) {
            return
        }
        textView.animate().cancel()
        // Bật hardware layer trong lúc animate alpha + translationY cùng lúc để GPU xử lý
        // việc dịch chuyển như 1 texture thay vì vẽ lại toàn bộ text mỗi frame - mượt hơn.
        textView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val slideDistancePx = LYRIC_SLIDE_DISTANCE_DP * resources.displayMetrics.density
        textView.alpha = 0f
        textView.translationY = slideDistancePx
        textView.text = newText
        textView.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(lyricSlideInterpolator)
            .setDuration(LYRIC_LINE_ANIM_DURATION_MS)
            .withEndAction { textView.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        if (isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }
    }

    // Nếu bài hát có ảnh thì thay vào ivVinylBase, nếu không thì giữ nguyên như XML gốc
    private fun updateVinylArt(track: MusicTrack) {
        val bitmap = AlbumArtUtils.loadAlbumArt(
            context = this,
            contentUri = track.contentUri,
            albumId = track.albumId
        )
        if (bitmap != null) {
            // Bo tròn ảnh trước khi gán vào để trông giống nhãn đĩa vinyl, không bị vuông góc
            val circularBitmap = AlbumArtUtils.getCircularBitmap(bitmap)
            binding.ivVinylBase.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.ivVinylBase.setImageBitmap(circularBitmap)
        } else {
            binding.ivVinylBase.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.ivVinylBase.setImageResource(R.drawable.ic_elip)
        }
    }

    private fun setupVinylAnimator() {
        val animator = ObjectAnimator.ofFloat(binding.flVinylContainer, View.ROTATION, 0f, 360f)
        animator.duration = VINYL_ROTATION_DURATION_MS
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        vinylAnimator = animator
    }

    private fun updateVinylRotation(isPlaying: Boolean) {
        val animator = vinylAnimator
        if (animator == null) {
            return
        }
        if (isPlaying) {
            if (isVinylAnimatorStarted) {
                animator.resume()
            } else {
                animator.start()
                isVinylAnimatorStarted = true
            }
        } else {
            if (isVinylAnimatorStarted) {
                animator.pause()
            }
        }
    }

    // Cần kim đĩa xoay quanh bản lề ở góc trên-phải của chính nó
    private fun setupTonearmPivot() {
        binding.ivTonearm.doOnLayout { tonearmView ->
            tonearmView.pivotX = tonearmView.width * TONEARM_PIVOT_X_FRACTION
            tonearmView.pivotY = tonearmView.height * TONEARM_PIVOT_Y_FRACTION
        }
    }

    // Khi phát nhạc thì gạt cần kim vào phần đĩa, khi dừng thì gạt ra vị trí nghỉ (như XML gốc)
    private fun animateTonearm(isPlaying: Boolean) {
        val targetRotation: Float
        if (isPlaying) {
            targetRotation = TONEARM_ON_DISC_ROTATION
        } else {
            targetRotation = TONEARM_RESTING_ROTATION
        }
        binding.ivTonearm.animate()
            .rotation(targetRotation)
            .setDuration(TONEARM_ANIMATION_DURATION_MS)
            .start()
    }

    // Cập nhật danh sách lyric + hiện/ẩn trạng thái "No lyrics" mỗi khi bài hát đổi
    private fun observeLyrics() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lyricsState.collect { state ->
                    if (state is SongPlayViewModel.LyricsUiState.NoLyrics) {
                        binding.llLyricsContainer.visibility = View.GONE
                        binding.tvNoLyrics.visibility = View.VISIBLE

                        lyricLines = emptyList()
                        currentLyricIndex = -1
                    } else if (state is SongPlayViewModel.LyricsUiState.Loaded) {
                        binding.llLyricsContainer.visibility = View.VISIBLE
                        binding.tvNoLyrics.visibility = View.GONE

                        lyricLines = state.lines
                        currentLyricIndex = -1

                        // Bài mới đổi -> xoá nội dung cũ ngay lập tức, không hiệu ứng.
                        binding.tvLyricPrev.text = ""
                        binding.tvLyricCurrent.text = ""
                        binding.tvLyricNext.text = ""
                    }
                }
            }
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMs.coerceAtLeast(0L))
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun showMenuMore() {
        // Dùng utils dùng chung để hiện popup: tự lật hướng, tự căn vị trí theo lề màn hình,
        // và không làm navigation bar đang ẩn bị hiện lại như PopupWindow mặc định.
        PopupMenuUtils.showAnchoredMenu(
            anchor = binding.layoutToolbar.btnMore,
            inflateBinding = { inflater -> PopupPlayingMoreBinding.inflate(inflater) },
            alignEndWithScreen = true,
        )
    }




    companion object {
        private const val LYRIC_LINE_ANIM_DURATION_MS = 320L
        private const val LYRIC_SLIDE_DISTANCE_DP = 20f
        private const val PROGRESS_TICK_INTERVAL_MS = 200L
        private const val VINYL_ROTATION_DURATION_MS = 6000L
        private const val TONEARM_ANIMATION_DURATION_MS = 400L
        private const val TONEARM_RESTING_ROTATION = 0f

        // Đĩa nhạc nằm ở bên trái, bản lề cần kim ở góc trên-phải nên phải xoay CÙNG chiều kim đồng hồ
        // (giá trị dương) để đầu kim gạt sang trái, vào đúng phần đĩa.
        private const val TONEARM_ON_DISC_ROTATION = 25f
        private const val TONEARM_PIVOT_X_FRACTION = 0.9f
        private const val TONEARM_PIVOT_Y_FRACTION = 0.05f
    }
}
