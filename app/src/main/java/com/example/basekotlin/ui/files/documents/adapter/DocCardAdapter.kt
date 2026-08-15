package com.example.basekotlin.ui.files.documents.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemDocCardBinding
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.model.DocumentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocCardAdapter : BaseAdapter<DocumentInfo, ItemDocCardBinding>() {

    var onItemClick: ((DocumentInfo) -> Unit)? = null
    var onMoreClick: ((DocumentInfo, View) -> Unit)? = null
    var onSelectToggle: ((DocumentInfo) -> Unit)? = null
    var isSelectionMode: Boolean = false
    var selectedDocs: Set<String> = emptySet()

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemDocCardBinding {
        return ItemDocCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<DocumentInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemDocCardBinding, item: DocumentInfo, layoutPosition: Int) {
        binding.tvFileName.text = item.fileName

        val readableSize = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
        if (item.dateModifiedMillis > 0L) {
            val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateModifiedMillis))
            binding.tvFileInfo.text = "$readableSize • $formattedDate"
        } else {
            binding.tvFileInfo.text = readableSize
        }

        val iconRes = getDocumentIconRes(item)
        binding.imgThumbnail.setImageResource(iconRes)

        bindSelectionUi(binding, item)
    }

    override fun onCLick(binding: ItemDocCardBinding, item: DocumentInfo, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.root.tap {
            if (isSelectionMode) {
                onSelectToggle?.invoke(item)
            } else {
                onItemClick?.invoke(item)
            }
        }

        binding.btnMore.tap {
            onMoreClick?.invoke(item, binding.btnMore)
        }

        binding.imgCheckbox.tap {
            onSelectToggle?.invoke(item)
        }

        binding.root.setOnLongClickListener {
            if (!isSelectionMode) {
                onSelectToggle?.invoke(item)
            }
            true
        }
    }

    private fun bindSelectionUi(binding: ItemDocCardBinding, item: DocumentInfo) {
        if (isSelectionMode) {
            binding.imgCheckbox.visible()
            binding.btnMore.gone()
        } else {
            binding.imgCheckbox.gone()
            binding.btnMore.visible()
        }
        val isChecked = selectedDocs.contains(item.filePath)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    private fun getDocumentIconRes(item: DocumentInfo): Int {
        return when (item.documentType) {
            DocumentType.PDF -> R.drawable.ic_pdf
            DocumentType.EXCEL -> R.drawable.ic_excel
            DocumentType.PPT -> R.drawable.ic_ppt
            DocumentType.TXT -> R.drawable.ic_txt
            DocumentType.DOC -> R.drawable.ic_doc
            DocumentType.WPS -> R.drawable.ic_wps
            DocumentType.OTHER -> {
                when (item.extension.lowercase()) {
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
    }
}