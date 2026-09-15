package com.example.basekotlin.ui.transfer.send.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseApdaterSelected
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemFile1Binding
import com.example.basekotlin.ui.transfer.model.FileCategory
import com.example.basekotlin.ui.transfer.model.TransferableItem
import java.io.File

class FileItemAdapter : BaseApdaterSelected<TransferableItem, ItemFile1Binding>() {
    var onClick: ((TransferableItem) -> Unit)? = null

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemFile1Binding {
            return ItemFile1Binding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<TransferableItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemKey(item: TransferableItem): Any = item.id

    override fun setData(
        binding: ItemFile1Binding,
        item: TransferableItem,
        layoutPosition: Int
    ) {
        binding.tvTitle.text = item.displayName
        binding.tvDesc.text = item.subInfo
        // Hiển thị ảnh/icon theo loại
        when (item.category) {
            FileCategory.VIDEO -> {
                Glide.with(binding.root.context)
                    .load(item.thumbnailUri)
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .into(binding.imgThumbnail)
            }
            FileCategory.PHOTO -> {
                Glide.with(binding.root.context)
                    .load(item.thumbnailUri)
                    .placeholder(R.drawable.ic_file)
                    .error(R.drawable.ic_file)
                    .into(binding.imgThumbnail)
            }
            FileCategory.MUSIC -> {
                Glide.with(binding.root.context)
                    .load(item.thumbnailUri)
                    .placeholder(R.drawable.ic_audio)
                    .error(R.drawable.ic_audio)
                    .into(binding.imgThumbnail)
            }
            else -> {
                binding.imgThumbnail.setImageResource(getDocumentIconRes(item.displayName))
            }
        }
        // Ẩn divider cho item cuối cùng trong nhóm
        if (layoutPosition == listData.size - 1) {
            binding.divider.gone()
        } else {
            binding.divider.visible()
        }

    }

    override fun setSelection(binding: ItemFile1Binding, item: TransferableItem, layoutPosition: Int) {
        val isSelected = selectedKeys.contains(item.id)
        if (isSelected) {
            binding.ivCheckBox.visible()
        } else {
            binding.ivCheckBox.gone()
        }
    }

    override fun onCLick(binding: ItemFile1Binding, item: TransferableItem, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.setOnClickListener {
            onClick?.invoke(item)
        }
    }
    private fun getDocumentIconRes(fileName: String): Int {
        val ext = File(fileName).extension.lowercase()
        return when (ext) {
            "pdf" -> R.drawable.ic_pdf
            "xls", "xlsx", "csv" -> R.drawable.ic_excel
            "doc", "docx" -> R.drawable.ic_doc
            "ppt", "pptx" -> R.drawable.ic_ppt
            "txt" -> R.drawable.ic_txt
            "zip", "rar", "7z" -> R.drawable.ic_zip
            else -> R.drawable.ic_file
        }
    }
}