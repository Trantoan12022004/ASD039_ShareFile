package com.example.basekotlin.ui.files.video.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemVideoBinding
import com.example.basekotlin.ui.files.video.model.VideoInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoAdapter : BaseAdapter<VideoInfo, ItemVideoBinding>() {

    var onItemClick: ((VideoInfo) -> Unit)? = null
    var onItemLongClick: ((VideoInfo) -> Unit)? = null
    var onSelectToggle: ((VideoInfo) -> Unit)? = null

    var isSelectionMode: Boolean = false
    var selectedVideos: Set<String> = emptySet()

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemVideoBinding {
        return ItemVideoBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun addListData(newList: MutableList<VideoInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun setData(binding: ItemVideoBinding, item: VideoInfo, layoutPosition: Int) {
        val context = binding.root.context

        // 1. Tải thumbnail video qua Glide
        Glide.with(context)
            .asBitmap()
            .load(item.contentUri)
            .placeholder(R.drawable.ic_video)
            .centerCrop()
            .into(binding.imgThumbnail)

        // Hiển thị tên video và thông tin ngày tháng
        binding.tvTitle.text = item.displayName
        binding.tvDate.text = "${formatDate(item.dateModifiedSeconds)} - ${formatFileSize(item.sizeBytes)}"

        // 2. Hiển thị thời lượng video (Duration)
        binding.tvDuration.text = formatDuration(item.durationMs)

        // 3. Trạng thái Checkbox khi ở Selection Mode
        if (isSelectionMode) {
            binding.ivCheckBox.visible()
            val isChecked = selectedVideos.contains(item.filePath)
            val iconRes = if (isChecked) R.drawable.check_box else R.drawable.check_box_outline_blank
            binding.ivCheckBox.setImageResource(iconRes)
        } else {
            binding.ivCheckBox.gone()
        }
    }

    override fun onCLick(binding: ItemVideoBinding, item: VideoInfo, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.root.setOnClickListener {
            if (isSelectionMode) {
                onSelectToggle?.invoke(item)
            } else {
                onItemClick?.invoke(item)
            }
        }

        binding.ivCheckBox.tap {
            onSelectToggle?.invoke(item)
        }

        binding.root.setOnLongClickListener {
            if (!isSelectionMode) {
                if (onItemLongClick != null) {
                    onItemLongClick?.invoke(item)
                } else {
                    onSelectToggle?.invoke(item)
                }
            }
            true
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            sizeBytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", sizeBytes / gb)
            sizeBytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / mb)
            sizeBytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", sizeBytes / kb)
            else -> "$sizeBytes B"
        }
    }

    private fun formatDate(dateSeconds: Long): String {
        if (dateSeconds <= 0L) return "-"
        val date = Date(dateSeconds * 1000L)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
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
