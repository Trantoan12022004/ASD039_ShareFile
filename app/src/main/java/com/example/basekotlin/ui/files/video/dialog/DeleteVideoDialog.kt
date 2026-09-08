package com.example.basekotlin.ui.files.video.dialog

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDeleteBinding
import com.example.basekotlin.ui.files.video.model.VideoInfo

class DeleteVideoDialog(
    context: Context,
    private val selectedVideos: List<VideoInfo>,
    private val onConfirm: () -> Unit
) : BaseDialog<DialogDeleteBinding>(context, true) {
    override fun setBinding(): DialogDeleteBinding {
        return DialogDeleteBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvTitleConfirm.text = context.getString(R.string.delete_video)
        binding.tvMessageConfirm.text = context.getString(R.string.delete_video_desc, selectedVideos.size)
    }

    override fun bindView() {
        binding.btnCancelConfirm.tap {
            dismiss()
        }
        binding.btnPositiveConfirm.tap {
            dismiss()
            onConfirm()
        }
    }
}