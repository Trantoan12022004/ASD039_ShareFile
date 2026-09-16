package com.example.basekotlin.ui.transfer.progress

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.*
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferStatus1
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import android.util.Base64
import android.widget.ImageView
import java.io.File

class ProgressAdapter(
    private val onCancelClick: (ProgressItem.FileTransfer) -> Unit,
    private val onRefreshClick: (ProgressItem.FileTransfer) -> Unit,
    private val onMoreClick: (ProgressItem.FileTransfer, android.view.View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FILE_SEND = 1
        private const val TYPE_FILE_RECEIVE = 2
        private const val TYPE_CHAT_SEND = 3
        private const val TYPE_CHAT_RECEIVE = 4
        private const val TYPE_SYSTEM_EVENT = 5

        const val PAYLOAD_PROGRESS = "PAYLOAD_PROGRESS"
        const val PAYLOAD_STATUS = "PAYLOAD_STATUS"
    }

    private val items = mutableListOf<ProgressItem>()

    fun submitList(newItems: List<ProgressItem>) {
        if (items.isEmpty()) {
            items.addAll(newItems)
            notifyDataSetChanged()
            return
        }

        // Nếu số lượng item thay đổi (thêm tin nhắn / thêm file)
        if (items.size != newItems.size) {
            val oldSize = items.size
            val newSize = newItems.size
            if (newSize > oldSize) {
                val added = newItems.subList(oldSize, newSize)
                items.addAll(added)
                notifyItemRangeInserted(oldSize, added.size)
            } else {
                items.clear()
                items.addAll(newItems)
                notifyDataSetChanged()
            }
            return
        }

        // Cùng số lượng item: Cập nhật cục bộ bằng PAYLOAD, không load lại Glide, không re-bind view
        for (i in newItems.indices) {
            val oldItem = items[i]
            val newItem = newItems[i]
            if (oldItem != newItem) {
                items[i] = newItem
                if (oldItem is ProgressItem.FileTransfer && newItem is ProgressItem.FileTransfer) {
                    var handled = false
                    if (oldItem.status != newItem.status) {
                        notifyItemChanged(i, PAYLOAD_STATUS)
                        handled = true
                    }
                    if (oldItem.bytesTransferred != newItem.bytesTransferred || oldItem.progressPercent != newItem.progressPercent) {
                        notifyItemChanged(i, PAYLOAD_PROGRESS)
                        handled = true
                    }
                    if (!handled) {
                        notifyItemChanged(i)
                    }
                } else {
                    notifyItemChanged(i)
                }
            }
        }
    }

    fun updateItemPayload(position: Int, payload: Any) {
        if (position in items.indices) {
            notifyItemChanged(position, payload)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ProgressItem.TextMessage -> if (item.isMe) TYPE_CHAT_SEND else TYPE_CHAT_RECEIVE
            is ProgressItem.FileTransfer -> if (item.isMe) TYPE_FILE_SEND else TYPE_FILE_RECEIVE
            is ProgressItem.SystemEvent -> TYPE_SYSTEM_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FILE_SEND -> FileSendViewHolder(ItemProgressSendBinding.inflate(inflater, parent, false))
            TYPE_FILE_RECEIVE -> FileReceiveViewHolder(ItemProgressReceiveBinding.inflate(inflater, parent, false))
            TYPE_CHAT_SEND -> ChatSendViewHolder(ItemChatSendBinding.inflate(inflater, parent, false))
            TYPE_CHAT_RECEIVE -> ChatReceiveViewHolder(ItemChatReceiveBinding.inflate(inflater, parent, false))
            else -> SystemEventViewHolder(ItemProgressSystemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FileSendViewHolder -> holder.bind(items[position] as ProgressItem.FileTransfer)
            is FileReceiveViewHolder -> holder.bind(items[position] as ProgressItem.FileTransfer)
            is ChatSendViewHolder -> holder.bind(items[position] as ProgressItem.TextMessage)
            is ChatReceiveViewHolder -> holder.bind(items[position] as ProgressItem.TextMessage)
            is SystemEventViewHolder -> holder.bind(items[position] as ProgressItem.SystemEvent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val item = items[position] as? ProgressItem.FileTransfer ?: return
        for (payload in payloads) {
            when (payload) {
                PAYLOAD_PROGRESS -> {
                    if (holder is FileSendViewHolder) holder.bindProgressOnly(item)
                    else if (holder is FileReceiveViewHolder) holder.bindProgressOnly(item)
                }
                PAYLOAD_STATUS -> {
                    if (holder is FileSendViewHolder) {
                        holder.bindStatusOnly(item)
                    } else if (holder is FileReceiveViewHolder) {
                        holder.bindStatusOnly(item)
                        // Khi hoàn tất nhận file, nạp lại thumbnail/icon thật từ file đã lưu trên đĩa
                        if (item.status == TransferStatus1.COMPLETED) {
                            holder.bindThumbnail(item)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ===== VIEW HOLDERS =====

    inner class FileSendViewHolder(private val b: ItemProgressSendBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ProgressItem.FileTransfer) {
            b.tvName.text = item.file.name
            b.tvSize.text = NetworkUtils.formatFileSize(item.file.size)

            bindThumbnail(b.imgThumbnail, item)

            bindProgressOnly(item)
            bindStatusOnly(item)

            b.ivCancel.setOnClickListener { onCancelClick(item) }
            b.ivRefresh.setOnClickListener { onRefreshClick(item) }
        }

        fun bindThumbnail(item: ProgressItem.FileTransfer) {
            this@ProgressAdapter.bindThumbnail(b.imgThumbnail, item)
        }

        fun bindProgressOnly(item: ProgressItem.FileTransfer) {
            if (item.status == TransferStatus1.TRANSFERRING) {
                b.tvSize.gone()
                b.tvSizeProgress.visible()
                b.tvSizeProgress.text = itemView.context.getString(
                    R.string.progress_size_ratio,
                    NetworkUtils.formatFileSize(item.bytesTransferred),
                    NetworkUtils.formatFileSize(item.totalBytes)
                )
                b.seekBar.visible()
                b.seekBar.progress = item.progressPercent
            } else {
                b.tvSize.visible()
                b.tvSizeProgress.gone()
                b.seekBar.gone()
            }
        }

        fun bindStatusOnly(item: ProgressItem.FileTransfer) {
            if (item.status == TransferStatus1.TRANSFERRING) {
                b.tvSize.gone()
                b.tvSizeProgress.visible()
                b.seekBar.visible()
            } else {
                b.tvSize.visible()
                b.tvSizeProgress.gone()
                b.seekBar.gone()
            }

            when (item.status) {
                TransferStatus1.COMPLETED -> {
                    b.ivDone.visible()
                    b.ivCancel.gone()
                    b.ivRefresh.gone()
                    b.tvStatusError.gone()
                }
                TransferStatus1.TRANSFERRING, TransferStatus1.QUEUED -> {
                    b.ivDone.gone()
                    b.ivCancel.visible()
                    b.ivRefresh.gone()
                    b.tvStatusError.gone()
                }
                TransferStatus1.CANCELED -> {
                    b.ivDone.gone()
                    b.ivCancel.gone()
                    b.ivRefresh.visible()
                    b.tvStatusError.visible()
                    b.tvStatusError.text = itemView.context.getString(R.string.status_canceled)
                }
                TransferStatus1.ERROR -> {
                    b.ivDone.gone()
                    b.ivCancel.gone()
                    b.ivRefresh.visible()
                    b.tvStatusError.visible()
                    b.tvStatusError.text = item.errorMessage ?: itemView.context.getString(R.string.status_error)
                }
                TransferStatus1.DECLINED -> {
                    b.ivDone.gone()
                    b.ivCancel.gone()
                    b.ivRefresh.visible()
                    b.tvStatusError.visible()
                    b.tvStatusError.text = itemView.context.getString(R.string.status_declined)
                }
            }
        }
    }

    inner class FileReceiveViewHolder(private val b: ItemProgressReceiveBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ProgressItem.FileTransfer) {
            b.tvName.text = item.file.name
            b.tvSize.text = NetworkUtils.formatFileSize(if (item.file.size > 0) item.file.size else item.totalBytes)

            bindThumbnail(b.imgThumbnail, item)

            bindProgressOnly(item)
            bindStatusOnly(item)

            b.ivCancel.setOnClickListener { onCancelClick(item) }
            b.ivMore.setOnClickListener { onMoreClick(item, b.ivMore) }
        }

        fun bindThumbnail(item: ProgressItem.FileTransfer) {
            this@ProgressAdapter.bindThumbnail(b.imgThumbnail, item)
        }

        fun bindProgressOnly(item: ProgressItem.FileTransfer) {
            if (item.status == TransferStatus1.TRANSFERRING) {
                b.tvSize.gone()
                b.tvSizeProgress.visible()
                b.tvSizeProgress.text = itemView.context.getString(
                    R.string.progress_size_ratio,
                    NetworkUtils.formatFileSize(item.bytesTransferred),
                    NetworkUtils.formatFileSize(item.totalBytes)
                )
                b.seekBar.visible()
                b.seekBar.progress = item.progressPercent
            } else {
                b.tvSize.visible()
                b.tvSizeProgress.gone()
                b.seekBar.gone()
            }
        }

        fun bindStatusOnly(item: ProgressItem.FileTransfer) {
            if (item.status == TransferStatus1.TRANSFERRING) {
                b.tvSize.gone()
                b.tvSizeProgress.visible()
                b.seekBar.visible()
            } else {
                b.tvSize.visible()
                b.tvSizeProgress.gone()
                b.seekBar.gone()
            }

            when (item.status) {
                TransferStatus1.COMPLETED -> {
                    b.ivMore.visible()
                    b.ivCancel.gone()
                    b.tvStatusError.gone()
                }
                TransferStatus1.TRANSFERRING -> {
                    b.ivMore.gone()
                    b.ivCancel.visible()
                    b.tvStatusError.gone()
                }
                TransferStatus1.CANCELED -> {
                    b.ivMore.gone()
                    b.ivCancel.gone()
                    b.tvStatusError.visible()
                    b.tvStatusError.text = itemView.context.getString(R.string.status_canceled)
                }
                TransferStatus1.ERROR -> {
                    b.ivMore.gone()
                    b.ivCancel.gone()
                    b.tvStatusError.visible()
                    b.tvStatusError.text = item.errorMessage ?: itemView.context.getString(R.string.status_error)
                }
                else -> {
                    b.ivMore.gone()
                    b.ivCancel.gone()
                    b.tvStatusError.gone()
                }
            }
        }
    }

    inner class ChatSendViewHolder(private val b: ItemChatSendBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ProgressItem.TextMessage) {
            b.tvMessage.text = item.text
            b.tvTime.text = item.timeFormatted
        }
    }

    inner class ChatReceiveViewHolder(private val b: ItemChatReceiveBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ProgressItem.TextMessage) {
            b.tvMessage.text = item.text
            b.tvTime.text = item.timeFormatted
        }
    }

    inner class SystemEventViewHolder(private val b: ItemProgressSystemBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ProgressItem.SystemEvent) {
            b.tvSystemMessage.text = item.message
        }
    }

    private fun bindThumbnail(imgThumbnail: ImageView, item: ProgressItem.FileTransfer) {
        val context = imgThumbnail.context
        val ext = File(item.file.name).extension.lowercase()
        val mime = item.file.mimeType.lowercase()

        val isImage = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
        val isVideo = mime.startsWith("video/") || ext in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp")
        val isAudio = mime.startsWith("audio/") || ext in listOf("mp3", "m4a", "wav", "aac", "flac", "ogg")
        val isApk = ext == "apk" || mime == "application/vnd.android.package-archive"

        when {
            isImage -> {
                if (item.isMe) {
                    Glide.with(imgThumbnail)
                        .load(item.file.uri)
                        .placeholder(R.drawable.ic_file)
                        .error(R.drawable.ic_file)
                        .into(imgThumbnail)
                } else {
                    if (item.status == TransferStatus1.COMPLETED && item.savedPath != null) {
                        Glide.with(imgThumbnail)
                            .load(File(item.savedPath))
                            .placeholder(R.drawable.ic_file)
                            .error(R.drawable.ic_file)
                            .into(imgThumbnail)
                    } else if (!item.thumbnailBase64.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(item.thumbnailBase64, Base64.DEFAULT)
                            Glide.with(imgThumbnail)
                                .load(bytes)
                                .placeholder(R.drawable.ic_file)
                                .error(R.drawable.ic_file)
                                .into(imgThumbnail)
                        } catch (e: Exception) {
                            imgThumbnail.setImageResource(R.drawable.ic_file)
                        }
                    } else {
                        imgThumbnail.setImageResource(R.drawable.ic_file)
                    }
                }
            }
            isVideo -> {
                if (item.isMe) {
                    Glide.with(imgThumbnail)
                        .load(item.file.uri)
                        .placeholder(R.drawable.ic_video)
                        .error(R.drawable.ic_video)
                        .into(imgThumbnail)
                } else {
                    if (item.status == TransferStatus1.COMPLETED && item.savedPath != null) {
                        Glide.with(imgThumbnail)
                            .load(File(item.savedPath))
                            .placeholder(R.drawable.ic_video)
                            .error(R.drawable.ic_video)
                            .into(imgThumbnail)
                    } else {
                        imgThumbnail.setImageResource(R.drawable.ic_video)
                    }
                }
            }
            isAudio -> {
                imgThumbnail.setImageResource(R.drawable.ic_audio)
            }
            isApk -> {
                val apkPath = if (item.isMe) item.file.uri.path else item.savedPath
                if (apkPath != null && File(apkPath).exists()) {
                    val icon = ApkFileScanner.loadIcon(context, apkPath)
                    if (icon != null) {
                        imgThumbnail.setImageDrawable(icon)
                    } else {
                        imgThumbnail.setImageResource(R.drawable.ic_app)
                    }
                } else {
                    imgThumbnail.setImageResource(R.drawable.ic_app)
                }
            }
            else -> {
                val iconRes = when (ext) {
                    "pdf" -> R.drawable.ic_pdf
                    "xls", "xlsx", "csv" -> R.drawable.ic_excel
                    "doc", "docx" -> R.drawable.ic_doc
                    "ppt", "pptx" -> R.drawable.ic_ppt
                    "txt" -> R.drawable.ic_txt
                    "zip", "rar", "7z" -> R.drawable.ic_zip
                    else -> R.drawable.ic_file
                }
                imgThumbnail.setImageResource(iconRes)
            }
        }
    }
}
