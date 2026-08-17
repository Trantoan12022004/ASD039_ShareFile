package com.example.basekotlin.dialog.common

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailInformationBinding
import com.example.basekotlin.model.PhotoInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformationPhotoDialog(
    context: Context,
    private val photo: PhotoInfo,
) : BaseDialog<DialogDetailInformationBinding>(context, true) {

    override fun setBinding(): DialogDetailInformationBinding {
        return DialogDetailInformationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvLabelName.text = context.getString(R.string.label_name)
        binding.llArtist.gone() // Ẩn dòng nghệ sĩ của bài hát
        binding.tvValueName.text = photo.displayName
        binding.tvValuePath.text = photo.filePath
        binding.tvValueSize.text = formatFileSize(photo.sizeBytes)

        val timeSeconds = if (photo.dateAddedSeconds > 0L) {
            photo.dateAddedSeconds
        } else {
            photo.dateModifiedSeconds
        }
        binding.tvValueDate.text = formatDate(timeSeconds)
    }

    override fun bindView() {
        binding.btnGotIt.tap {
            dismiss()
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        if (sizeBytes >= gb) {
            val value = sizeBytes / gb
            return String.format(Locale.getDefault(), "%.1f GB", value)
        }
        if (sizeBytes >= mb) {
            val value = sizeBytes / mb
            return String.format(Locale.getDefault(), "%.1f MB", value)
        }
        if (sizeBytes >= kb) {
            val value = sizeBytes / kb
            return String.format(Locale.getDefault(), "%.1f KB", value)
        }
        return sizeBytes.toString() + " B"
    }

    private fun formatDate(dateSeconds: Long): String {
        if (dateSeconds <= 0L) {
            return "-"
        }
        val date = Date(dateSeconds * 1000L)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
}
