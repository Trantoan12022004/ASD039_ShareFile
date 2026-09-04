package com.example.basekotlin.ui.files.pdfconverter.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemPdfBinding
import com.example.basekotlin.model.DocumentInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfAdapter : BaseAdapter<DocumentInfo, ItemPdfBinding>() {

    var onItemClick: ((DocumentInfo) -> Unit)? = null
    var selectedDocs: Set<String> = emptySet()

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemPdfBinding {
        return ItemPdfBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<DocumentInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemPdfBinding, item: DocumentInfo, layoutPosition: Int) {
        binding.tvFileName.text = item.fileName

        val readableSize = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
        if (item.dateModifiedMillis > 0L) {
            val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateModifiedMillis))
            binding.tvFileInfo.text = "$readableSize • $formattedDate"
        } else {
            binding.tvFileInfo.text = readableSize
        }
        binding.imgThumbnail.setImageResource(R.drawable.ic_pdf)

        // Cập nhật trạng thái checkbox
        val isChecked = selectedDocs.contains(item.filePath)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    override fun onCLick(binding: ItemPdfBinding, item: DocumentInfo, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.root.tap {
            onItemClick?.invoke(item)
        }
    }
}
