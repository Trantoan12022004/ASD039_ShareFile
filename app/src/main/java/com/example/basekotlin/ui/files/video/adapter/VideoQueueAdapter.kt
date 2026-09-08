package com.example.basekotlin.ui.files.video.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemNowPlayingBinding
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.ui.files.video.model.VideoInfo
import java.util.Locale

class VideoQueueAdapter : BaseAdapter<VideoInfo, ItemNowPlayingBinding>() {
    var onClick: ((Int) -> Unit)? = null
    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemNowPlayingBinding {
        return ItemNowPlayingBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<VideoInfo>) {
        this.listData.clear()
        this.listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemNowPlayingBinding,
        item: VideoInfo,
        layoutPosition: Int
    ) {
        binding.btnClose.gone()
        Glide.with(binding.imgThumbnail.context)
            .load(item.contentUri)
            .placeholder(R.drawable.ic_video)
            .into(binding.imgThumbnail)
        binding.apply {
            tvFileName.text = item.displayName
            tvFileInfo.text = formatDuration(item.durationMs)
        }
    }

    override fun onCLick(binding: ItemNowPlayingBinding, item: VideoInfo, layoutPosition: Int) {
        binding.root.tap {
            val clickListener = onClick
            if (clickListener != null) {
                clickListener(layoutPosition)
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}