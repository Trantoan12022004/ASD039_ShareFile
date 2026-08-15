package com.example.basekotlin.ui.files.music.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemMyPlaylistBinding
import com.example.basekotlin.model.MusicPlaylist

class MusicPlaylistAdapter1 : BaseAdapter<MusicPlaylist, ItemMyPlaylistBinding>() {
    var onPlaylistClick: ((MusicPlaylist) -> Unit)? = null
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

        binding.apply {
            tvPlaylistName.text = item.name
            tvPlaylistCount.text = root.context.getString(R.string.song_count, item.trackCount)
            binding.btnMore.gone()
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
    }
}