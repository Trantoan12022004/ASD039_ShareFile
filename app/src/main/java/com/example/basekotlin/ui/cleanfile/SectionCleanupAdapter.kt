package com.example.basekotlin.ui.cleanfile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemMediaCleanupBinding
import com.example.basekotlin.databinding.ItemMediaCleanupHeaderBinding
import com.example.basekotlin.model.CleanFileGroup
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType

class SectionCleanupAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    private val groups = mutableListOf<CleanFileGroup>()
    private val flatList = mutableListOf<Any>() // Contains CleanFileGroup or CleanFileItem
    val selectedPaths = mutableSetOf<String>()
    var onSelectToggle: ((CleanFileItem) -> Unit)? = null

    fun setGroups(newGroups: List<CleanFileGroup>) {
        groups.clear()
        groups.addAll(newGroups)
        rebuildFlatList()
    }

    private fun rebuildFlatList() {
        flatList.clear()
        for (group in groups) {
            flatList.add(group)
            if (group.isExpanded) {
                flatList.addAll(group.items)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (flatList[position]) {
            is CleanFileGroup -> VIEW_TYPE_HEADER
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun getItemCount(): Int = flatList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemMediaCleanupHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemMediaCleanupBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = flatList[position]
        if (holder is HeaderViewHolder && item is CleanFileGroup) {
            holder.bind(item)
        } else if (holder is ItemViewHolder && item is CleanFileItem) {
            holder.bind(item)
        }
    }

    fun selectAll(selectAll: Boolean) {
        selectedPaths.clear()
        if (selectAll) {
            for (group in groups) {
                selectedPaths.addAll(group.items.map { it.path })
            }
        }
        notifyDataSetChanged()
    }

    fun getAllItemCount(): Int {
        return groups.sumOf { it.items.size }
    }

    inner class HeaderViewHolder(private val binding: ItemMediaCleanupHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: CleanFileGroup) {
            binding.tvFolderName.text = group.groupName
            binding.tvGroupSize.text = "(${CleanFileUtils.formatFileSize(group.totalSizeBytes)})"

            // Mũi tên xoay khi expand/collapse
            binding.imgArrow.rotation = if (group.isExpanded) 180f else 0f

            binding.root.tap {
                group.isExpanded = !group.isExpanded
                rebuildFlatList()
            }
        }
    }

    inner class ItemViewHolder(private val binding: ItemMediaCleanupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CleanFileItem) {
            binding.tvFileName.text = item.name

            val sizeStr = CleanFileUtils.formatFileSize(item.sizeBytes)
            val dateStr = CleanFileUtils.formatDate(item.dateModifiedMillis)
            binding.tvFileInfo.text = if (dateStr.isNotEmpty()) "$dateStr - $sizeStr" else sizeStr

            val isSelected = selectedPaths.contains(item.path)
            binding.cbSelect.setImageResource(
                if (isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
            )

            // Load thumbnail
            loadThumbnail(binding, item)

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
            notifyItemChanged(bindingAdapterPosition)
            onSelectToggle?.invoke(item)
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
                else -> {
                    binding.imgThumbnail.setImageResource(R.drawable.ic_file)
                }
            }
        }
    }
}
