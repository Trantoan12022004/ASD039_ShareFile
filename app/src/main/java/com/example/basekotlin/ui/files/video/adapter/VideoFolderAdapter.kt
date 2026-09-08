package com.example.basekotlin.ui.files.video.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemVideoFolderBinding
import com.example.basekotlin.ui.files.video.model.VideoFolder

class VideoFolderAdapter : BaseAdapter<VideoFolder, ItemVideoFolderBinding>() {

    var onClick: ((VideoFolder) -> Unit)? = null
    var onLongClick: ((VideoFolder) -> Unit)? = null
    var onSelectToggle: ((VideoFolder) -> Unit)? = null
    var isFolderSelectedChecker: ((String) -> Boolean)? = null

    var isSelectionMode: Boolean = false

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemVideoFolderBinding {
        return ItemVideoFolderBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<VideoFolder>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemVideoFolderBinding, item: VideoFolder, layoutPosition: Int) {
        val context = binding.root.context
        binding.tvFolderName.text = item.folderName
        binding.tvCount.text = "${item.videoCount} " + context.getString(R.string.files1)

        if (item.coverVideoUri != null) {
            Glide.with(context)
                .asBitmap()
                .load(item.coverVideoUri)
                .centerCrop()
                .placeholder(R.drawable.ic_no_folder)
                .error(R.drawable.ic_no_folder)
                .into(binding.imgThumbnail)
        } else {
            binding.imgThumbnail.setImageResource(R.drawable.ic_no_folder)
        }

        if (isSelectionMode) {
            binding.ivCheckBox.visible()
            val isChecked = isFolderSelectedChecker?.invoke(item.folderPath) ?: false
            val iconRes = if (isChecked) R.drawable.check_box else R.drawable.check_box_outline_blank
            binding.ivCheckBox.setImageResource(iconRes)
        } else {
            binding.ivCheckBox.gone()
        }
    }

    override fun onCLick(binding: ItemVideoFolderBinding, item: VideoFolder, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.root.setOnClickListener {
            if (isSelectionMode) {
                onSelectToggle?.invoke(item)
            } else {
                onClick?.invoke(item)
            }
        }

        binding.ivCheckBox.tap {
            onSelectToggle?.invoke(item)
        }

        binding.root.setOnLongClickListener {
            if (!isSelectionMode) {
                if (onLongClick != null) {
                    onLongClick?.invoke(item)
                } else {
                    onSelectToggle?.invoke(item)
                }
            }
            true
        }
    }
}
