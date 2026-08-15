package com.example.basekotlin.ui.files.music.adapter

import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemSongCardBinding
import com.example.basekotlin.model.MusicTrack
import java.util.Locale
import java.util.concurrent.TimeUnit

class MusicTrackAdapter : BaseAdapter<MusicTrack, ItemSongCardBinding>() {

    var onTrackClick: ((MusicTrack) -> Unit)? = null
    var onMoreClick: ((MusicTrack, View) -> Unit)? = null
    var onSelectToggle: ((MusicTrack) -> Unit)? = null
    var isSelectionMode: Boolean = false
    var selectedIds: Set<Long> = emptySet()

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemSongCardBinding {
        return ItemSongCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<MusicTrack>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemSongCardBinding, item: MusicTrack, layoutPosition: Int) {

        binding.tvFileName.text = item.title

        val formattedDuration = formatDuration(item.durationMs)
        val infoText = item.artist + " • " + formattedDuration
        binding.tvFileInfo.text = infoText
        loadAlbumArt(binding, item)

        // Bước 1: hiện/ẩn checkbox và nút more tuỳ theo chế độ
        if (isSelectionMode) {
            binding.imgCheckbox.visibility = View.VISIBLE
            binding.btnMore.visibility = View.GONE
        } else {
            binding.imgCheckbox.visibility = View.GONE
            binding.btnMore.visibility = View.VISIBLE
        }

        // Bước 2: cập nhật icon checked/unchecked
        val isChecked = selectedIds.contains(item.id)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    override fun onCLick(binding: ItemSongCardBinding, item: MusicTrack, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.root.tap {
            val clickListener = onTrackClick
            if (clickListener != null) {
                clickListener(item)
            }
        }

        binding.btnMore.tap {
            val moreListener = onMoreClick
            if (moreListener != null) {
                moreListener(item, binding.btnMore)
            }
        }

        binding.imgCheckbox.tap {
            val toggleListener = onSelectToggle
            if (toggleListener != null) {
                toggleListener(item)
            }
        }

        // Long-press: cách UX phổ biến để vào chế độ chọn nhanh, không bắt buộc
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

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    private fun loadAlbumArt(
        binding: ItemSongCardBinding,
        item: MusicTrack
    ) {
        val context = binding.root.context
        val defaultArt = R.drawable.ic_audio
        val thumbnailSize = Size(200, 200)

        // Bước 1: thử lấy art nhúng trong bài hát
        var bitmap: Bitmap? = null
        if (item.contentUri != Uri.EMPTY) {
            try {
                bitmap = context.contentResolver.loadThumbnail(
                    item.contentUri,
                    thumbnailSize,
                    null
                )
            } catch (e: Exception) {
                Log.e("DEBUG_imgThumbnail", "loadThumbnail track failed", e)
            }
        }

        // Bước 2: fallback sang art album trên MediaStore
        if (bitmap == null && item.albumId > 0L) {
            val albumUri = ContentUris.withAppendedId(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                item.albumId
            )
            try {
                bitmap = context.contentResolver.loadThumbnail(
                    albumUri,
                    thumbnailSize,
                    null
                )
            } catch (e: Exception) {
                Log.e("DEBUG_imgThumbnail", "loadThumbnail album failed", e)
            }
        }

        // Bước 3: hiển thị
        if (bitmap != null) {
            binding.imgThumbnail.setImageBitmap(bitmap)
        } else {
            binding.imgThumbnail.setImageResource(defaultArt)
        }
    }

}