package com.example.basekotlin.ui.files.music.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemSelectPlaylistBinding
import com.example.basekotlin.model.MusicPlaylist

class MusicPlaylistAdapter : BaseAdapter<MusicPlaylist, ItemSelectPlaylistBinding>() {
    var onPlaylistClick: ((MusicPlaylist) -> Unit)? = null
    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemSelectPlaylistBinding {
        return ItemSelectPlaylistBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<MusicPlaylist>) {
        this.listData.clear()
        this.listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemSelectPlaylistBinding,
        item: MusicPlaylist,
        layoutPosition: Int
    ) {
        binding.tvPlaylistName.text = item.name
    }

    override fun onCLick(
        binding: ItemSelectPlaylistBinding,
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
    }
}