package com.example.basekotlin.ui.cleanfile.messenger.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemMediaCleanupBinding
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import com.example.basekotlin.ui.cleanfile.CleanFileUtils

class CleanerDetailAdapter : BaseAdapter<CleanFileItem, ItemMediaCleanupBinding>() {

    val selectedPaths = mutableSetOf<String>()
    var onSelectToggle: ((CleanFileItem) -> Unit)? = null

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemMediaCleanupBinding {
        return ItemMediaCleanupBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<CleanFileItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemMediaCleanupBinding,
        item: CleanFileItem,
        layoutPosition: Int
    ) {
        binding.tvFileName.text = item.name

        val sizeStr = CleanFileUtils.formatFileSize(item.sizeBytes)
        val dateStr = CleanFileUtils.formatDate(item.dateModifiedMillis)
        binding.tvFileInfo.text = if (dateStr.isNotEmpty()) "$dateStr - $sizeStr" else sizeStr

        val isSelected = selectedPaths.contains(item.path)
        binding.cbSelect.setImageResource(
            if (isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
        )

        loadThumbnail(binding, item)
    }

    override fun onCLick(
        binding: ItemMediaCleanupBinding,
        item: CleanFileItem,
        layoutPosition: Int
    ) {
        binding.root.tap {
            toggleSelect(item)
        }

        binding.cbSelect.tap {
            toggleSelect(item)
        }
    }

    private fun toggleSelect(item: CleanFileItem) {
        if (selectedPaths.contains(item.path)) {
            selectedPaths.remove(item.path)
        } else {
            selectedPaths.add(item.path)
        }
        notifyDataSetChanged()
        onSelectToggle?.invoke(item)
    }

    fun selectAll(selectAll: Boolean) {
        selectedPaths.clear()
        if (selectAll) {
            selectedPaths.addAll(listData.map { it.path })
        }
        notifyDataSetChanged()
    }

    private fun loadThumbnail(binding: ItemMediaCleanupBinding, item: CleanFileItem) {
        when (item.type) {
            CleanFileType.PHOTO, CleanFileType.VIDEO -> {
                Glide.with(binding.root.context)
                    .load(item.thumbnailUri ?: item.path)
                    .placeholder(if (item.type == CleanFileType.VIDEO) R.drawable.ic_cat_videos else R.drawable.ic_cat_photos)
                    .error(if (item.type == CleanFileType.VIDEO) R.drawable.ic_cat_videos else R.drawable.ic_cat_photos)
                    .centerCrop()
                    .into(binding.imgThumbnail)
            }
            CleanFileType.AUDIO -> {
                binding.imgThumbnail.setImageResource(R.drawable.ic_cat_music)
            }
            CleanFileType.DOCUMENT -> {
                binding.imgThumbnail.setImageResource(R.drawable.ic_cat_document)
            }
            else -> {
                binding.imgThumbnail.setImageResource(R.drawable.ic_file)
            }
        }
    }
}
