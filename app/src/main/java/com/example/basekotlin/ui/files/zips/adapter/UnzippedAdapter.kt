package com.example.basekotlin.ui.files.zips.adapter

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.databinding.ItemDocCardBinding
import com.example.basekotlin.model.UnzippedItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnzippedAdapter : BaseAdapter<UnzippedItem, ItemDocCardBinding>() {

    var onItemClick: ((UnzippedItem) -> Unit)? = null

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemDocCardBinding {
        return ItemDocCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<UnzippedItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun setData(
        binding: ItemDocCardBinding,
        item: UnzippedItem,
        layoutPosition: Int
    ) {
        binding.tvFileName.text = item.name
        binding.btnMore.gone()
        binding.imgCheckbox.gone()

        if (item.isDirectory) {
            // Hiển thị dạng Thư mục
            binding.imgThumbnail.setImageResource(R.drawable.ic_folder)
            val context = binding.root.context
            if (item.itemCount <= 1) {
                binding.tvFileInfo.text = context.getString(R.string.folder_item_count_single)
            } else {
                binding.tvFileInfo.text = context.getString(R.string.folder_item_count, item.itemCount)
            }
        } else {
            // Hiển thị dạng File (lấy icon theo đuôi mở rộng)
            val iconRes = getFileIcon(item.extension)
            binding.imgThumbnail.setImageResource(iconRes)

            val readableSize = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
            if (item.dateModifiedMillis > 0L) {
                val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateModifiedMillis))
                binding.tvFileInfo.text = "$readableSize • $formattedDate"
            } else {
                binding.tvFileInfo.text = readableSize
            }
        }
    }

    override fun onCLick(binding: ItemDocCardBinding, item: UnzippedItem, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    private fun getFileIcon(extension: String): Int {
        return when (extension) {
            "zip", "rar", "7z", "tar", "gz" -> R.drawable.ic_zip
            "pdf" -> R.drawable.ic_pdf
            "mp3", "wav", "m4a", "flac" -> R.drawable.ic_audio
            "doc", "docx" -> R.drawable.ic_doc
            "xls", "xlsx" -> R.drawable.ic_excel
            "ppt", "pptx" -> R.drawable.ic_ppt
            "txt" -> R.drawable.ic_txt
            else -> R.drawable.ic_file
        }
    }
}
