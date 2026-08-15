package com.example.basekotlin.ui.files.music.ringtone

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityRingtoneBinding
import com.example.basekotlin.dialog.common.SaveRingtoneDialog
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.util.AlbumArtUtils
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class RingtoneActivity : BaseActivity<ActivityRingtoneBinding>(ActivityRingtoneBinding::inflate) {

    private val viewModel: RingtoneViewModel by viewModels()
    private var previewPlayer: ExoPlayer? = null
    private var sliderInitialized = false

    override fun getData() {
        val trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L)
        if (trackId <= 0L) {
            finishThisActivity()
            return
        }
        viewModel.loadTrack(trackId)
    }

    override fun initView() {
        binding.rangeSlider.setCustomThumbDrawable(R.drawable.slider_thumb_capsule)
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap { onBack() }

        binding.btnPreviewRingtone.tap {
            viewModel.previewSelection()
        }

        binding.btnSaveRingtone.tap {
            showSaveConfirmDialog()
        }

        binding.rangeSlider.addOnChangeListener { _, _, _ ->
            val values = binding.rangeSlider.values
            val startMs = values[0].toLong()
            val endMs = values[1].toLong()
            viewModel.updateSelection(startMs, endMs)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun showSaveConfirmDialog() {
        val state = viewModel.uiState.value
        val track = state.track
        if (track == null) {
            return
        }

        val startText = formatTime(state.startMs)
        val endText = formatTime(state.endMs)

        SaveRingtoneDialog(
            context = this,
            startText = startText,
            endText = endText,
            onConfirm = {
                viewModel.saveRingtone()
            }
        ).show()
    }

    private fun handleUiState(state: RingtoneViewModel.UiState) {
        val track = state.track
        if (track != null) {
            bindTrackInfo(track)
            binding.tvStartPoint.text = formatTime(state.startMs)
            binding.tvEndPoint.text = formatTime(state.endMs)
        }

        updateActionButtons(state.isSaving)
        handlePreviewRequest(state)
        handleSuccessMessage(state.successMessage)
        handleErrorMessage(state.errorMessage)
    }

    private fun updateActionButtons(isSaving: Boolean) {
        if (isSaving) {
            binding.btnSaveRingtone.isEnabled = false
            binding.btnPreviewRingtone.isEnabled = false
        } else {
            binding.btnSaveRingtone.isEnabled = true
            binding.btnPreviewRingtone.isEnabled = true
        }
    }

    private fun handlePreviewRequest(state: RingtoneViewModel.UiState) {
        val request = state.previewRequest
        if (request == null) {
            return
        }

        val track = state.track
        if (track != null) {
            startPreview(track.contentUri, request.startMs, request.endMs)
        }

        viewModel.onPreviewHandled()
    }

    private fun handleSuccessMessage(message: String?) {
        if (message == null) {
            return
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        viewModel.onSuccessHandled()
    }

    private fun handleErrorMessage(message: String?) {
        if (message == null) {
            return
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        viewModel.onErrorHandled()
    }

    private fun bindTrackInfo(track: MusicTrack) {
        binding.cardSongInfo.tvFileName.text = track.title
        binding.cardSongInfo.tvFileInfo.text = track.artist
        binding.cardSongInfo.tvDuration.text = getString(
            R.string.duration_label,
            formatTime(track.durationMs)
        )

        val bitmap: Bitmap? = AlbumArtUtils.loadAlbumArt(
            this,
            track.contentUri,
            track.albumId
        )
        if (bitmap != null) {
            binding.cardSongInfo.imgThumbnail.setImageBitmap(bitmap)
        } else {
            binding.cardSongInfo.imgThumbnail.setImageResource(R.drawable.ic_audio)
        }

        setupRangeSliderIfNeeded(track.durationMs)
    }

    private fun setupRangeSliderIfNeeded(durationMs: Long) {
        if (sliderInitialized) {
            return
        }
        sliderInitialized = true

        binding.rangeSlider.valueFrom = 0f
        binding.rangeSlider.valueTo = durationMs.toFloat()

        val defaultEndMs = minOf(durationMs, DEFAULT_CLIP_END_MS)
        binding.rangeSlider.values = listOf(0f, defaultEndMs.toFloat())
        viewModel.updateSelection(0L, defaultEndMs)
    }

    private fun startPreview(uri: android.net.Uri, startMs: Long, endMs: Long) {
        releasePreviewPlayer()

        val player = ExoPlayer.Builder(this).build()
        previewPlayer = player

        val mediaItem = MediaItem.fromUri(uri)
        val factory = DefaultMediaSourceFactory(this)
        val fullSource = factory.createMediaSource(mediaItem)

        val startUs = startMs * 1_000L
        val endUs = endMs * 1_000L
        val clippingSource = ClippingMediaSource(fullSource, startUs, endUs)

        player.setMediaSource(clippingSource)
        player.prepare()
        player.play()
    }

    private fun releasePreviewPlayer() {
        val player = previewPlayer
        if (player != null) {
            player.release()
            previewPlayer = null
        }
    }

    override fun onStop() {
        releasePreviewPlayer()
        super.onStop()
    }

    private fun formatTime(timeMs: Long): String {
        val safeTimeMs = timeMs
        var normalizedTimeMs = safeTimeMs
        if (normalizedTimeMs < 0L) {
            normalizedTimeMs = 0L
        }

        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(normalizedTimeMs)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    companion object {
        const val EXTRA_TRACK_ID = "EXTRA_TRACK_ID"
        private const val DEFAULT_CLIP_END_MS = 30_000L
    }
}
