package com.example.basekotlin.ui.files.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemFolderBinding
import com.example.basekotlin.model.MusicFolder

class MusicFolderAdapter : BaseAdapter<MusicFolder, ItemFolderBinding>() {
    var onFoledrClick: ((MusicFolder) -> Unit)? = null
    var onMoreClick: ((MusicFolder, View) -> Unit)? = null
    var onSelectToggle: ((MusicFolder) -> Unit)? = null
    var isSelectionMode: Boolean = false
    var selectedPaths: Set<String> = emptySet()

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemFolderBinding {
        return ItemFolderBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<MusicFolder>) {
        this.listData.clear()
        this.listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemFolderBinding,
        item: MusicFolder,
        layoutPosition: Int
    ) {
        binding.tvFolderName.text = item.folderName
        binding.tvFolderInfo.text = binding.root.context.getString(R.string.song_count, item.trackCount)

        if (isSelectionMode) {
            binding.imgCheckbox.visibility = View.VISIBLE
            binding.btnMore.visibility = View.GONE
        } else {
            binding.imgCheckbox.visibility = View.GONE
            binding.btnMore.visibility = View.VISIBLE
        }

        val isChecked = selectedPaths.contains(item.folderPath)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    override fun onCLick(binding: ItemFolderBinding, item: MusicFolder, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.tap {
            val clickListener = onFoledrClick
            if (clickListener != null) {
                clickListener(item)
            }
        }
        binding.btnMore.tap {
            val clickListener = onMoreClick
            if (clickListener != null) {
                clickListener(item, binding.btnMore)
            }
        }
        binding.imgCheckbox.tap {
            val toggleListener = onSelectToggle
            if (toggleListener != null) {
                toggleListener(item)
            }
        }
        binding.root.setOnLongClickListener {
            if (isSelectionMode == false) {
                val toggleListener = onSelectToggle
                if (toggleListener != null) {
                    toggleListener(item)
                }
            }
            true
        }
    }
}
