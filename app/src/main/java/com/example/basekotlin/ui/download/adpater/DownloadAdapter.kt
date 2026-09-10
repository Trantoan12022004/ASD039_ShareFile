package com.example.basekotlin.ui.download.adpater

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.appstore.ReceivedAppSource
import com.example.basekotlin.databinding.ItemDocCardBinding
import com.example.basekotlin.ui.download.model.DownloadItem
import com.example.basekotlin.ui.download.model.DownloadType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadAdapter : BaseAdapter<DownloadItem, ItemDocCardBinding>() {

    var onItemClick: ((DownloadItem) -> Unit)? = null
    var onMoreClick: ((DownloadItem, View) -> Unit)? = null
    var onSelectToggle: ((DownloadItem) -> Unit)? = null

    var isSelectionMode: Boolean = false
    var selectedPaths: Set<String> = emptySet()

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemDocCardBinding {
        return ItemDocCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<DownloadItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemDocCardBinding, item: DownloadItem, layoutPosition: Int) {
        // Hiển thị tên file hoặc tên App nếu là file APK
        binding.tvFileName.text = item.appName ?: item.name

        // Hiển thị kích thước & ngày tải về
        val context = binding.root.context
        val readableSize = Formatter.formatShortFileSize(context, item.sizeBytes)
        if (item.dateModifiedMillis > 0L) {
            val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateModifiedMillis))
            binding.tvFileInfo.text = "$readableSize • $formattedDate"
        } else {
            binding.tvFileInfo.text = readableSize
        }

        // Tải ảnh đại diện/icon theo loại dữ liệu
        bindThumbnail(binding, item)

        // Hiển thị trạng thái chọn (Selection UI)
        bindSelectionUi(binding, item)
    }

    override fun onCLick(binding: ItemDocCardBinding, item: DownloadItem, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.root.tap {
            if (isSelectionMode) {
                onSelectToggle?.invoke(item)
            } else {
                onItemClick?.invoke(item)
            }
        }

        binding.btnMore.tap {
            onMoreClick?.invoke(item, binding.btnMore)
        }

        binding.imgCheckbox.tap {
            onSelectToggle?.invoke(item)
        }

        binding.root.setOnLongClickListener {
            if (!isSelectionMode) {
                onSelectToggle?.invoke(item)
            }
            true
        }
    }

    private fun bindThumbnail(binding: ItemDocCardBinding, item: DownloadItem) {
        val context = binding.root.context
        when (item.type) {
            DownloadType.PHOTOS -> {
                Glide.with(context)
                    .load(item.path)
                    .placeholder(R.drawable.ic_file)
                    .into(binding.imgThumbnail)
            }
            DownloadType.VIDEOS -> {
                Glide.with(context)
                    .load(item.path)
                    .placeholder(R.drawable.ic_video)
                    .into(binding.imgThumbnail)
            }
            DownloadType.MUSIC -> {
                binding.imgThumbnail.setImageResource(R.drawable.ic_audio)
            }
            DownloadType.APPS -> {
                val icon = ReceivedAppSource.loadIcon(context, item.path)
                if (icon != null) {
                    binding.imgThumbnail.setImageDrawable(icon)
                } else {
                    binding.imgThumbnail.setImageResource(R.drawable.ic_installed)
                }
            }
            DownloadType.ALL -> {
                when (item.extension) {
                    "jpg", "jpeg", "png", "webp", "gif" -> {
                        Glide.with(context).load(item.path).placeholder(R.drawable.ic_file).into(binding.imgThumbnail)
                    }
                    "mp4", "mkv", "avi", "mov" -> {
                        Glide.with(context).load(item.path).placeholder(R.drawable.ic_video).into(binding.imgThumbnail)
                    }
                    "mp3", "wav", "m4a", "flac" -> {
                        binding.imgThumbnail.setImageResource(R.drawable.ic_audio)
                    }
                    "apk" -> {
                        val icon = ReceivedAppSource.loadIcon(context, item.path)
                        if (icon != null) {
                            binding.imgThumbnail.setImageDrawable(icon)
                        } else {
                            binding.imgThumbnail.setImageResource(R.drawable.ic_installed)
                        }
                    }
                    else -> binding.imgThumbnail.setImageResource(R.drawable.ic_file)
                }
            }
        }
    }

    private fun bindSelectionUi(binding: ItemDocCardBinding, item: DownloadItem) {
        if (isSelectionMode) {
            binding.imgCheckbox.visible()
            binding.btnMore.gone()
        } else {
            binding.imgCheckbox.gone()
            binding.btnMore.visible()
        }

        val isChecked = selectedPaths.contains(item.path)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }
}
