package com.example.basekotlin.ui.files.photos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.databinding.ItemPhotoDetailBinding
import com.example.basekotlin.model.PhotoInfo


class PhotoDetailAdapter : BaseAdapter<PhotoInfo, ItemPhotoDetailBinding>() {
    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemPhotoDetailBinding {
        return ItemPhotoDetailBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<PhotoInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemPhotoDetailBinding,
        item: PhotoInfo,
        layoutPosition: Int
    ) {
        val context: Context = binding.root.context
        // Tải ảnh hiển thị ở kích thước vừa khung hình
        Glide.with(context)
            .load(item.contentUri)
            .fitCenter()
            .into(binding.ivDetailPhoto)
    }
}