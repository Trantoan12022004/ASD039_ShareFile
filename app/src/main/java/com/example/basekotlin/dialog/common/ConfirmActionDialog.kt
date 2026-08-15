package com.example.basekotlin.dialog.common

import android.content.Context
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogConfirmActionBinding

// Dialog xác nhận dùng chung cho mọi hành động "nguy hiểm": xoá lịch sử, xoá bài hát,
// xoá playlist, clear queue... Truyền title/message/positiveText tuỳ ngữ cảnh gọi.
class ConfirmActionDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val positiveText: String,
    private val onConfirm: () -> Unit
) : BaseDialog<DialogConfirmActionBinding>(context, true) {

    override fun setBinding(): DialogConfirmActionBinding {
        return DialogConfirmActionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvTitleConfirm.text = title
        binding.tvMessageConfirm.text = message
        binding.tvPositiveConfirm.text = positiveText
    }

    override fun bindView() {
        binding.btnCancelConfirm.tap { dismiss() }
        binding.btnPositiveConfirm.tap {
            dismiss()
            onConfirm.invoke()
        }
    }
}