package com.example.basekotlin.ui.files.zips.adapter

import android.annotation.SuppressLint
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
import com.example.basekotlin.model.ZipInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ZippedAdapter: BaseAdapter<ZipInfo, ItemDocCardBinding>() {

    var onItemClick: ((ZipInfo) -> Unit)? = null
    var onMoreClick: ((ZipInfo, View) -> Unit)? = null
    var onSelectToggle: ((ZipInfo) -> Unit)? = null
    var isSelectionMode: Boolean = false
    var selectedZips: Set<String> = emptySet()
    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemDocCardBinding {
        return ItemDocCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<ZipInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun setData(
        binding: ItemDocCardBinding,
        item: ZipInfo,
        layoutPosition: Int
    ) {
        binding.tvFileName.text = item.fileName
        val readableSize = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
        if (item.dateModifiedMillis > 0L) {
            val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateModifiedMillis))
            binding.tvFileInfo.text = "$readableSize • $formattedDate"
        } else {
            binding.tvFileInfo.text = readableSize
        }
        binding.imgThumbnail.setImageResource(R.drawable.ic_zip)
        bindSelectionUi(binding, item)

    }

    override fun onCLick(binding: ItemDocCardBinding, item: ZipInfo, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.setOnClickListener {
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

    private fun bindSelectionUi(binding: ItemDocCardBinding, item: ZipInfo) {
        if (isSelectionMode) {
            binding.imgCheckbox.visible()
            binding.btnMore.gone()
        } else {
            binding.imgCheckbox.gone()
            binding.btnMore.visible()
        }
        val isChecked = selectedZips.contains(item.filePath)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }
}