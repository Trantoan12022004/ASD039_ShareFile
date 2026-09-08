package com.example.basekotlin.ui.files.video.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Gravity
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogNowPlayingBinding
import com.example.basekotlin.ui.files.video.adapter.VideoQueueAdapter
import com.example.basekotlin.ui.files.video.model.VideoInfo

class VideoPlayingQueueDialog(
    context: Context,
    private val videos: List<VideoInfo>,
    private val currentIndex: Int,
    private val onVideoClick: (Int) -> Unit
) : BaseDialog<DialogNowPlayingBinding>(context, true) {
    private val queueAdapter = VideoQueueAdapter()
    val isLandscape = isLandscapeMode()


    override fun setBinding(): DialogNowPlayingBinding {
        return DialogNowPlayingBinding.inflate(layoutInflater)
    }

    override fun initView() {
        window?.let { win ->
            if (isLandscape) {
                // Khi xoay ngang: Chiều cao full màn hình, căn phải, hiệu ứng trượt từ phải sang
                val density = context.resources.displayMetrics.density
                val minWidthPx = (320 * density).toInt()
                val maxWidthPx = (420 * density).toInt()
                val calculatedWidth = (context.resources.displayMetrics.widthPixels * 0.45f).toInt()
                val dialogWidth = calculatedWidth.coerceIn(minWidthPx, maxWidthPx)
                win.setLayout(dialogWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                win.setGravity(Gravity.END)
                win.setWindowAnimations(R.style.RightDialogAnimation)
            } else {
                // Khi màn hình dọc: Chiều rộng full, căn đáy, trượt từ dưới lên
                win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                win.setGravity(Gravity.BOTTOM)
                win.setWindowAnimations(R.style.BottomDialogAnimation)
            }
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }
        binding.rvNowPlaying.layoutManager = LinearLayoutManager(context)
        binding.rvNowPlaying.adapter = queueAdapter
        queueAdapter.addListData(videos.toMutableList())
        binding.tvCount.text = videos.size.toString()

        queueAdapter.onClick = { index ->
            onVideoClick(index)
            dismiss()
        }
        binding.ivClose.tap { dismiss() }
    }
    private fun isLandscapeMode(): Boolean {
        val activity = context as? Activity
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE ||
                activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}