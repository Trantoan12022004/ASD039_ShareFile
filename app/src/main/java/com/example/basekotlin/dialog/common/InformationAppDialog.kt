package com.example.basekotlin.dialog.common

import android.content.Context
import android.provider.Settings.Global.getString
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailInformationBinding
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.model.MusicTrack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformationAppDialog(
    context: Context,
    private val appInfo: AppInfo,
) : BaseDialog<DialogDetailInformationBinding>(context, true) {

    override fun setBinding(): DialogDetailInformationBinding {
        return DialogDetailInformationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvLabelName.text = context.getString(R.string.file)
        binding.llArtist.gone()
        binding.tvValueName.text = appInfo.appName
        binding.tvValuePath.text = appInfo.apkFilePath
        binding.tvValueSize.text = formatFileSize(appInfo.sizeBytes)
        binding.tvValueDate.text = formatDate(appInfo.lastUpdateTimeMillis)
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
        val date = Date(dateSeconds * 1000L)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
}
