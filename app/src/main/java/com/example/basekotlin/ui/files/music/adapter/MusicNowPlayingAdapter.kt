package com.example.basekotlin.ui.files.music.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemNowPlayingBinding
import com.example.basekotlin.model.MusicPlaylist
import com.example.basekotlin.model.MusicTrack

class MusicNowPlayingAdapter : BaseAdapter<MusicTrack, ItemNowPlayingBinding>() {
    var onTrackClick: ((MusicTrack) -> Unit)? = null
    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemNowPlayingBinding {
        return ItemNowPlayingBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<MusicTrack>) {
        this.listData.clear()
        this.listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemNowPlayingBinding,
        item: MusicTrack,
        layoutPosition: Int
    ) {

        binding.apply {
            tvFileName.text = item.title
            tvFileInfo.text = item.artist
        }

    }

    override fun onCLick(
        binding: ItemNowPlayingBinding,
        item: MusicTrack,
        layoutPosition: Int
    ) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.tap {
            val clickListener = onTrackClick
            if (clickListener != null) {
                clickListener(item)
            }
        }
    }
}