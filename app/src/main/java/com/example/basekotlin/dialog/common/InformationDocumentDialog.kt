package com.example.basekotlin.dialog.common

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailInformationBinding
import com.example.basekotlin.model.DocumentInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformationDocumentDialog(
    context: Context,
    private val doc: DocumentInfo,
) : BaseDialog<DialogDetailInformationBinding>(context, true) {

    override fun setBinding(): DialogDetailInformationBinding {
        return DialogDetailInformationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvLabelName.text = context.getString(R.string.file)
        binding.llArtist.gone()
        binding.tvValueName.text = doc.fileName
        binding.tvValuePath.text = doc.filePath
        binding.tvValueSize.text = formatFileSize(doc.sizeBytes)
        binding.tvValueDate.text = formatDate(doc.dateModifiedMillis)
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

    private fun formatDate(dateMillis: Long): String {
        if (dateMillis <= 0L) {
            return "-"
        }
        val date = Date(dateMillis)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
}
