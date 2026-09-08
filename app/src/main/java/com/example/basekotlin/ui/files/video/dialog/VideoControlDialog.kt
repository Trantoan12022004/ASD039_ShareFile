package com.example.basekotlin.ui.files.video.dialog

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogVideoControlBinding
import com.example.basekotlin.ui.files.video.model.VideoAspectRatio
@OptIn(UnstableApi::class)
class VideoControlDialog(
    private val activity: Activity,
    private val currentRatio: VideoAspectRatio,
    private val isLoopMode: Boolean,
    private val onDelete: () -> Unit,
    private val onShare: () -> Unit,
    private val onSend: () -> Unit,
    private val onMore: () -> Unit,
    private val onAspectRatioChanged: (VideoAspectRatio) -> Unit,
    private val onPlayingModeChanged: (isLoop: Boolean) -> Unit
) : BaseDialog<DialogVideoControlBinding>(activity, true) {

    private val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var selectedRatio: VideoAspectRatio = currentRatio
    private var selectedLoopMode: Boolean = isLoopMode

    override fun setBinding(): DialogVideoControlBinding {
        return DialogVideoControlBinding.inflate(layoutInflater)
    }

    override fun initView() {

        val isLandscape = isLandscapeMode()

        // Cấu hình hiển thị dạng Bottom Dialog
        window?.let { win ->
            if (isLandscape) {
                // Khi xoay ngang: Chiều cao full màn hình, chiều rộng chiếm ~45% bề ngang, căn phải
                val density = activity.resources.displayMetrics.density
                val minWidthPx = (320 * density).toInt()
                val maxWidthPx = (420 * density).toInt()
                val calculatedWidth = (activity.resources.displayMetrics.widthPixels * 0.45f).toInt()
                val dialogWidth = calculatedWidth.coerceIn(minWidthPx, maxWidthPx)
                win.setLayout(dialogWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                win.setGravity(Gravity.END)
                win.setWindowAnimations(R.style.RightDialogAnimation)
            } else {
                // Khi màn hình dọc: Chiều rộng full, chiều cao wrap_content, căn đáy
                win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                win.setGravity(Gravity.BOTTOM)
                win.setWindowAnimations(R.style.BottomDialogAnimation)
            }
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }

        setupVolumeSeekBar()
        setupBrightnessSeekBar()
        updateAspectRatioChips(selectedRatio)
        updatePlayingModeChips(selectedLoopMode)
    }
    private fun isLandscapeMode(): Boolean {
        val orientation = activity.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE ||
                activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun bindView() {
        // 1. Hàng 4 nút tròn trên cùng
        binding.btnSend.tap {
            dismiss()
            onSend()
        }

        binding.btnShare.tap {
            dismiss()
            onShare()
        }

        binding.btnDelete.tap {
            dismiss()
            onDelete()
        }

        // Bấm btnMore ở dialog này -> Mở showMoreMenu (VideoMoreDialog)
        binding.btnMore.tap {
            dismiss()
            onMore()
        }

        // 2. Các nút chọn Aspect Ratio
        binding.btnRatioBestFit.tap { selectAspectRatio(VideoAspectRatio.FILL) }
        binding.btnRatioFill.tap { selectAspectRatio(VideoAspectRatio.FILL) }
        binding.btnRatio11.tap { selectAspectRatio(VideoAspectRatio.RATIO_1_1) }
        binding.btnRatio43.tap { selectAspectRatio(VideoAspectRatio.RATIO_4_3) }
        binding.btnRatio169.tap { selectAspectRatio(VideoAspectRatio.RATIO_16_9) }

        // 3. Các nút chọn Playing Mode
        binding.btnModeAutoPlay.tap { selectPlayingMode(isLoop = false) }
        binding.btnModeLoop.tap { selectPlayingMode(isLoop = true) }
    }

    // ================= VOLUME =================
    private fun setupVolumeSeekBar() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val progress = if (maxVolume > 0) (currentVolume * 100) / maxVolume else 0

        binding.sbVolume.max = 100
        binding.sbVolume.progress = progress
        updateVolumeIcon(progress)

        binding.sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val targetVolume = (progress * maxVolume) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                    updateVolumeIcon(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateVolumeIcon(progress: Int) {
        if (progress == 0) {
            binding.ivVolume.setImageResource(R.drawable.ic_volume_off)
        } else {
            binding.ivVolume.setImageResource(R.drawable.ic_volume_up)
        }
    }

    // ================= BRIGHTNESS =================
    private fun setupBrightnessSeekBar() {
        val currentBrightness = activity.window.attributes.screenBrightness
        val initialProgress = if (currentBrightness >= 0f) {
            (currentBrightness * 100).toInt()
        } else {
            try {
                val sysBrightness = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (sysBrightness * 100) / 255
            } catch (_: Exception) {
                50
            }
        }

        binding.sbBrightness.max = 100
        binding.sbBrightness.progress = initialProgress

        binding.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val lp = activity.window.attributes
                    lp.screenBrightness = progress.coerceAtLeast(1) / 100f
                    activity.window.attributes = lp
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ================= ASPECT RATIO =================
    private fun selectAspectRatio(ratio: VideoAspectRatio) {
        selectedRatio = ratio
        updateAspectRatioChips(ratio)
        onAspectRatioChanged(ratio)
    }

    private fun updateAspectRatioChips(selected: VideoAspectRatio) {
        val chips = listOf(
            binding.btnRatioBestFit to VideoAspectRatio.BEST_FIT,
            binding.btnRatioFill to VideoAspectRatio.FILL,
            binding.btnRatio11 to VideoAspectRatio.RATIO_1_1,
            binding.btnRatio43 to VideoAspectRatio.RATIO_4_3,
            binding.btnRatio169 to VideoAspectRatio.RATIO_16_9
        )

        for ((chip, ratio) in chips) {
            val isSelected = (ratio == selected)
            setChipStyle(chip, isSelected)
        }
    }

    // ================= PLAYING MODE =================
    private fun selectPlayingMode(isLoop: Boolean) {
        selectedLoopMode = isLoop
        updatePlayingModeChips(isLoop)
        onPlayingModeChanged(isLoop)
    }

    private fun updatePlayingModeChips(isLoop: Boolean) {
        setChipStyle(binding.btnModeAutoPlay, !isLoop)
        setChipStyle(binding.btnModeLoop, isLoop)
    }

    private fun setChipStyle(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected)
            chip.setTextColor(Color.WHITE)
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected)
            chip.setTextColor(Color.BLACK)
        }
    }
}
