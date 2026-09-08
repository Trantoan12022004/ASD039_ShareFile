package com.example.basekotlin.ui.files.video.dialog

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.DialogMoreVideoBinding
import com.example.basekotlin.ui.files.video.model.VideoInfo

class VideoMoreDialog(
    context: Context,
    private val selectedVideos: List<VideoInfo>,
    private val isFolderTab: Boolean = false,
    private val onRename: (VideoInfo) -> Unit,
    private val onToMp3: (VideoInfo) -> Unit,
    private val onInformation: (VideoInfo) -> Unit,
    private val onMoveSafeBox: (List<VideoInfo>) -> Unit,
) : BaseDialog<DialogMoreVideoBinding>(context, true){
    override fun setBinding(): DialogMoreVideoBinding {
        return DialogMoreVideoBinding.inflate(layoutInflater)
    }


    override fun initView() {
        val isLandscape = isLandscapeMode()

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

        if (isFolderTab) {
            binding.tvRename.gone()
            binding.dividerRename.gone()
            binding.tvInformation.gone()
            binding.dividerSafebox.gone()
            binding.tvToMp3.gone()
            binding.dividerToMp3.gone()
        } else {
            binding.tvRename.visible()
            binding.dividerRename.visible()
            binding.tvInformation.visible()
            binding.dividerSafebox.visible()
            binding.tvToMp3.visible()
            binding.dividerToMp3.visible()
        }
    }

    private fun isLandscapeMode(): Boolean {
        val activity = context as? Activity
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE ||
                activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun bindView() {
        // Đổi tên (chỉ khi chọn 1 video)
        binding.tvRename.tap {
            if (selectedVideos.size == 1) {
                onRename(selectedVideos[0])
                dismiss()
            } else {
                Toast.makeText(context, context.getString(R.string.please_select_one_item), Toast.LENGTH_SHORT).show()
            }
        }
        // Chuyển sang MP3 (chỉ khi chọn 1 video)
        binding.tvToMp3.tap {
            if (selectedVideos.size == 1) {
                onToMp3(selectedVideos[0])
                dismiss()
            } else {
                Toast.makeText(context, context.getString(R.string.please_select_one_item), Toast.LENGTH_SHORT).show()
            }
        }
        // Chuyển vào SafeBox
        binding.tvSafebox.tap {
            if (selectedVideos.isNotEmpty()) {
                onMoveSafeBox(selectedVideos)
                dismiss()
            }
        }
        // Xem thông tin chi tiết (chỉ khi chọn 1 video)
        binding.tvInformation.tap {
            if (selectedVideos.size == 1) {
                onInformation(selectedVideos[0])
                dismiss()
            } else {
                Toast.makeText(context, context.getString(R.string.please_select_one_item), Toast.LENGTH_SHORT).show()
            }
        }
    }
}