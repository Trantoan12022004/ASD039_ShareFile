package com.example.basekotlin.ui.nearby_play.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemNearbyPlayBinding
import com.example.basekotlin.ui.nearby_play.model.NearbySongItem

class NearbySongsAdapter(
    private val onItemClick: (Int, NearbySongItem) -> Unit
) : BaseAdapter<NearbySongItem, ItemNearbyPlayBinding>() {

    companion object {
        private const val PAYLOAD_STATUS = "PAYLOAD_STATUS"
    }

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemNearbyPlayBinding {
        return ItemNearbyPlayBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<NearbySongItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemNearbyPlayBinding, item: NearbySongItem, layoutPosition: Int) {
        binding.tvNameSong.text = item.title
        updateStatusView(binding, item.isPlaying)

        binding.root.tap {
            onItemClick(layoutPosition, item)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_STATUS)) {
            val item = listData[position]
            updateStatusView(holder.binding, item.isPlaying)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun updateStatusView(binding: ItemNearbyPlayBinding, isPlaying: Boolean) {
        val ctx = binding.root.context
        if (isPlaying) {
            binding.ivStatus.setImageResource(R.drawable.play_arrow1)
            binding.tvStatus.text = ctx.getString(R.string.playing)
            binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.primary_35))
        } else {
            binding.ivStatus.setImageResource(R.drawable.music_note1)
            binding.tvStatus.text = ctx.getString(R.string.send)
            binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.sub_title))
        }
    }

    fun updatePlayingIndex(playingIndex: Int) {
        for (i in listData.indices) {
            val wasPlaying = listData[i].isPlaying
            val shouldPlay = (i == playingIndex)
            if (wasPlaying != shouldPlay) {
                listData[i].isPlaying = shouldPlay
                notifyItemChanged(i, PAYLOAD_STATUS)
            }
        }
    }
}
