package com.example.basekotlin.ui.download.dialog

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailInformationBinding
import com.example.basekotlin.ui.download.model.DownloadItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformationDownloadDialog(
    context: Context,
    private val item: DownloadItem
) : BaseDialog<DialogDetailInformationBinding>(context, true) {

    override fun setBinding(): DialogDetailInformationBinding {
        return DialogDetailInformationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        super.initView()
        binding.tvLabelName.text = context.getString(R.string.file)
        binding.llArtist.gone()
        binding.tvValueName.text = item.name
        binding.tvLabelPath.text = context.getString(R.string.label_path)
        binding.tvValuePath.text = item.path
        binding.tvLabelSize.text = context.getString(R.string.label_size)
        binding.tvValueSize.text = formatFileSize(item.sizeBytes)
        binding.tvLabelDate.text = context.getString(R.string.label_date)
        binding.tvValueDate.text = formatDate(item.dateModifiedMillis)
    }

    override fun bindView() {
        super.bindView()
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
        return "$sizeBytes B"
    }

    private fun formatDate(dateMillis: Long): String {
        if (dateMillis <= 0L) {
            return "-"
        }
        val date = Date(dateMillis)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
}
