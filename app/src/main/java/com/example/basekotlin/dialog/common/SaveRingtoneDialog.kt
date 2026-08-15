package com.example.basekotlin.dialog.common

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogConfirmActionBinding


class SaveRingtoneDialog(
    context: Context,
    private val startText: String,
    private val endText: String,
    private val onConfirm: () -> Unit
) : BaseDialog<DialogConfirmActionBinding>(context, true) {

    override fun setBinding(): DialogConfirmActionBinding {
        return DialogConfirmActionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvTitleConfirm.text = context.getString(R.string.save_ringtone)

        val fullText = context.getString(
            R.string.save_ringtone_confirm_message,
            startText,
            endText
        )
        binding.tvMessageConfirm.text = buildHighlightedMessage(fullText)

        binding.tvPositiveConfirm.text = context.getString(R.string.save_ringtone)

        // Nút Save dùng bg xanh (không phải đỏ delete)
        binding.btnPositiveConfirm.setBackgroundResource(R.drawable.bg_btn_create)
    }

    override fun bindView() {
        binding.btnCancelConfirm.tap { dismiss() }
        binding.btnPositiveConfirm.tap {
            dismiss()
            onConfirm.invoke()
        }
    }

    private fun buildHighlightedMessage(fullText: String): CharSequence {
        val highlight = context.getString(R.string.ringtones_folder)
        val start = fullText.indexOf(highlight)
        if (start < 0) {
            return fullText
        }

        val spannable = SpannableString(fullText)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#3D6A00")),
            start,
            start + highlight.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }
}