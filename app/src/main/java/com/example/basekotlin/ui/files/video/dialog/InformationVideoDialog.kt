package com.example.basekotlin.ui.files.video.dialog

import android.content.Context
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailVideoInformationBinding
import com.example.basekotlin.ui.files.video.model.VideoInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformationVideoDialog(
    context: Context,
    private val video: VideoInfo,
) : BaseDialog<DialogDetailVideoInformationBinding>(context, true) {

    override fun setBinding(): DialogDetailVideoInformationBinding {
        return DialogDetailVideoInformationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvValueName.text = video.displayName
        binding.tvValueDuration.text = formatDuration(video.durationMs)

        binding.tvValuePath.text = video.filePath
        binding.tvValueSize.text = formatFileSize(video.sizeBytes)

        val timeSeconds = if (video.dateAddedSeconds > 0L) {
            video.dateAddedSeconds
        } else {
            video.dateModifiedSeconds
        }
        binding.tvValueDate.text = formatDate(timeSeconds)
    }

    override fun bindView() {
        binding.btnGotIt.tap {
            dismiss()
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            sizeBytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", sizeBytes / gb)
            sizeBytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / mb)
            sizeBytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", sizeBytes / kb)
            else -> "$sizeBytes B"
        }
    }

    private fun formatDate(dateSeconds: Long): String {
        if (dateSeconds <= 0L) return "-"
        val date = Date(dateSeconds * 1000L)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
}
