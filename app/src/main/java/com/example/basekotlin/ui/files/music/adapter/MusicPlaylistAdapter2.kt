package com.example.basekotlin.ui.files.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemMyPlaylistBinding
import com.example.basekotlin.model.MusicPlaylist

class MusicPlaylistAdapter2 : BaseAdapter<MusicPlaylist, ItemMyPlaylistBinding>() {
    var onPlaylistClick: ((MusicPlaylist) -> Unit)? = null
    var onMoreClick: ((MusicPlaylist, View) -> Unit)? = null
    var onSelectToggle: ((MusicPlaylist) -> Unit)? = null
    var isSelectionMode: Boolean = false
    var selectedIds: Set<Long> = emptySet()

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemMyPlaylistBinding {
        return ItemMyPlaylistBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<MusicPlaylist>) {
        this.listData.clear()
        this.listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemMyPlaylistBinding,
        item: MusicPlaylist,
        layoutPosition: Int
    ) {
        binding.tvPlaylistName.text = item.name
        binding.tvPlaylistCount.text = binding.root.context.getString(R.string.song_count, item.trackCount)

        if (isSelectionMode) {
            binding.imgCheckbox.visibility = View.VISIBLE
            binding.btnMore.visibility = View.GONE
        } else {
            binding.imgCheckbox.visibility = View.GONE
            binding.btnMore.visibility = View.VISIBLE
        }

        val isChecked = selectedIds.contains(item.id)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    override fun onCLick(
        binding: ItemMyPlaylistBinding,
        item: MusicPlaylist,
        layoutPosition: Int
    ) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.tap {
            val clickListener = onPlaylistClick
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
