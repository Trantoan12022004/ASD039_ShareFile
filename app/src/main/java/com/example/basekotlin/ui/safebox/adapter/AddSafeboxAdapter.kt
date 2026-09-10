package com.example.basekotlin.ui.safebox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.databinding.ItemPdfBinding
import com.example.basekotlin.ui.safebox.model.SafeBoxCandidateItem
import java.io.File
import java.util.Locale

class AddSafeboxAdapter : BaseAdapter<SafeBoxCandidateItem, ItemPdfBinding>() {

    val selectedItems: List<SafeBoxCandidateItem>
        get() = listData.filter { it.isSelected }

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemPdfBinding {
        return ItemPdfBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<SafeBoxCandidateItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemPdfBinding,
        item: SafeBoxCandidateItem,
        layoutPosition: Int
    ) {
        val context = binding.root.context
        binding.tvFileName.text = item.fileName
        binding.tvFileInfo.text = formatFileSize(item.fileSize)

        // Hiển thị thumbnail theo loại file
        when (item.fileType) {
            SafeBoxFileType.PICTURES, SafeBoxFileType.VIDEOS -> {
                Glide.with(context)
                    .load(item.contentUri ?: item.filePath)
                    .placeholder(if (item.fileType == SafeBoxFileType.PICTURES) R.drawable.ic_photo_expand else R.drawable.ic_video_expand)
                    .into(binding.imgThumbnail)
            }
            SafeBoxFileType.AUDIO -> {
                Glide.with(context)
                    .load(item.contentUri)
                    .placeholder(R.drawable.ic_audio)
                    .error(R.drawable.ic_audio)
                    .into(binding.imgThumbnail)
            }
            SafeBoxFileType.DOCUMENTS -> {
                binding.imgThumbnail.setImageResource(getDocumentIconRes(item.filePath))
            }
            SafeBoxFileType.OTHERS -> {
                if (item.filePath.endsWith(".apk", ignoreCase = true)) {
                    val pm = context.packageManager
                    val pi = pm.getPackageArchiveInfo(item.filePath, 0)
                    if (pi != null) {
                        pi.applicationInfo?.sourceDir = item.filePath
                        pi.applicationInfo?.publicSourceDir = item.filePath
                        val icon = pi.applicationInfo?.loadIcon(pm)
                        binding.imgThumbnail.setImageDrawable(icon)
                    } else {
                        binding.imgThumbnail.setImageResource(R.drawable.ic_other_expand)
                    }
                } else {
                    binding.imgThumbnail.setImageResource(R.drawable.ic_other_expand)
                }
            }
        }

        // Cập nhật trạng thái Checkbox
        val checkRes = if (item.isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
        binding.imgCheckbox.setImageResource(checkRes)
    }

    override fun onCLick(
        binding: ItemPdfBinding,
        item: SafeBoxCandidateItem,
        layoutPosition: Int
    ) {
        val toggleSelection = {
            item.isSelected = !item.isSelected
            val checkRes = if (item.isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
            binding.imgCheckbox.setImageResource(checkRes)
        }
        binding.root.tap { toggleSelection() }
        binding.imgCheckbox.tap { toggleSelection() }
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

    private fun getDocumentIconRes(filePath: String): Int {
        val extension = File(filePath).extension.lowercase()
        return when (extension) {
            "pdf" -> R.drawable.ic_pdf
            "xls", "xlsx", "xlsm", "csv" -> R.drawable.ic_excel
            "ppt", "pptx", "pps", "ppsx" -> R.drawable.ic_ppt
            "txt", "log" -> R.drawable.ic_txt
            "doc", "docx" -> R.drawable.ic_doc
            "wps", "wpt", "wpp", "wet" -> R.drawable.ic_wps
            else -> R.drawable.ic_file
        }
    }
}
