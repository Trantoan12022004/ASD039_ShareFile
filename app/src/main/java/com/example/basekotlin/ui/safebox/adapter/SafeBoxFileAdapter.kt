package com.example.basekotlin.ui.safebox.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import com.example.basekotlin.databinding.ItemSongCardBinding
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.model.DocumentType
import java.io.File
import java.util.Locale

class SafeBoxFileAdapter: BaseAdapter<SafeBoxFile, ItemSongCardBinding>() {

    var onItemClick: ((SafeBoxFile) -> Unit)? = null
    var onMoreClick: ((SafeBoxFile, View) -> Unit)? = null

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemSongCardBinding {
        return ItemSongCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<SafeBoxFile>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemSongCardBinding,
        item: SafeBoxFile,
        layoutPosition: Int
    ) {
        binding.tvFileName.text = item.fileName
        binding.tvFileInfo.text = formatFileSize(item.fileSize)
        val context = binding.root.context
        when (item.fileType) {
            SafeBoxFileType.PICTURES, SafeBoxFileType.VIDEOS -> {
                Glide.with(context)
                    .load(item.safeboxPath)
                    .placeholder(if (item.fileType == SafeBoxFileType.PICTURES) R.drawable.ic_file else R.drawable.ic_video)
                    .into(binding.imgThumbnail)
            }
            SafeBoxFileType.AUDIO -> {
                Glide.with(context)
                    .load(item.safeboxPath)
                    .placeholder(R.drawable.ic_audio)
                    .error(R.drawable.ic_audio)
                    .into(binding.imgThumbnail)
            }
            SafeBoxFileType.DOCUMENTS -> {
                binding.imgThumbnail.setImageResource(getDocumentIconRes(item))
            }
            SafeBoxFileType.OTHERS -> {
                binding.imgThumbnail.setImageResource(R.drawable.ic_file)
            }
        }

    }

    override fun onCLick(binding: ItemSongCardBinding, item: SafeBoxFile, layoutPosition: Int) {
        binding.root.tap {
            onItemClick?.invoke(item)
        }
        binding.btnMore.tap {
            onMoreClick?.invoke(item, it)
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
    private fun getDocumentIconRes(item: SafeBoxFile): Int {
        val file = File(item.safeboxPath)
        val extension = file.extension.lowercase()

        return when (extension) {
            "pdf" -> R.drawable.ic_pdf

            "xls", "xlsx", "xlsm", "csv" -> R.drawable.ic_excel

            "ppt", "pptx", "pps", "ppsx" -> R.drawable.ic_ppt

            "txt", "log" -> R.drawable.ic_txt

            "doc", "docx" -> R.drawable.ic_doc

            "wps", "wpt", "wpp", "wet" -> R.drawable.ic_wps

            else -> R.drawable.ic_file
        }
    }

    private fun getAudioArtwork(filePath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(filePath)

            val artBytes = retriever.embeddedPicture

            if (artBytes != null) {
                BitmapFactory.decodeByteArray(
                    artBytes,
                    0,
                    artBytes.size
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}