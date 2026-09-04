package com.example.basekotlin.ui.storage.adapter

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemDocCardBinding
import com.example.basekotlin.databinding.ItemDocCardExpandBinding
import com.example.basekotlin.model.StorageItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageAdapter : BaseAdapter<StorageItem, ViewBinding>() {

    companion object {
        const val VIEW_TYPE_LINEAR = 0
        const val VIEW_TYPE_GRID = 1
    }

    // Biến lưu trạng thái hiển thị Grid hay Linear
    var isGridMode: Boolean = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    // Callback khi người dùng click vào item (mở thư mục hoặc mở file)
    var onItemClick: ((StorageItem) -> Unit)? = null

    // Callback khi người dùng click nút More (3 chấm)
    var onMoreClick: ((StorageItem, View) -> Unit)? = null

    // Callback khi người dùng toggle chọn item trong selection mode
    var onSelectToggle: ((StorageItem) -> Unit)? = null

    // Trạng thái selection mode (set từ bên ngoài khi observe ViewModel)
    var isSelectionMode: Boolean = false

    // Tập các path đang được chọn (set từ bên ngoài khi observe ViewModel)
    var selectedPaths: Set<String> = emptySet()

    // Class gom các View chung giữa ItemDocCardBinding và ItemDocCardExpandBinding
    private class ViewHolderViews(
        val root: View,
        val imgThumbnail: ImageView,
        val tvFileName: TextView,
        val tvFileInfo: TextView,
        val btnMore: ImageView,
        val imgCheckbox: ImageView
    )

    override fun getItemViewType(position: Int): Int {
        val viewType: Int
        if (isGridMode) {
            viewType = VIEW_TYPE_GRID
        } else {
            viewType = VIEW_TYPE_LINEAR
        }
        return viewType
    }

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ViewBinding {
        val binding: ViewBinding
        if (viewType == VIEW_TYPE_GRID) {
            // Khi ở chế độ Grid: sử dụng item_doc_card_expand.xml
            binding = ItemDocCardExpandBinding.inflate(inflater, parent, false)
        } else {
            // Khi ở chế độ Linear: sử dụng item_doc_card.xml
            binding = ItemDocCardBinding.inflate(inflater, parent, false)
        }
        return binding
    }

    override fun addListData(newList: MutableList<StorageItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    // Trích xuất các View tương ứng từ ViewBinding
    private fun extractViews(binding: ViewBinding): ViewHolderViews {
        val views: ViewHolderViews
        if (binding is ItemDocCardExpandBinding) {
            views = ViewHolderViews(
                root = binding.root,
                imgThumbnail = binding.imgThumbnail,
                tvFileName = binding.tvFileName,
                tvFileInfo = binding.tvFileInfo,
                btnMore = binding.btnMore,
                imgCheckbox = binding.imgCheckbox
            )
        } else {
            val linearBinding = binding as ItemDocCardBinding
            views = ViewHolderViews(
                root = linearBinding.root,
                imgThumbnail = linearBinding.imgThumbnail,
                tvFileName = linearBinding.tvFileName,
                tvFileInfo = linearBinding.tvFileInfo,
                btnMore = linearBinding.btnMore,
                imgCheckbox = linearBinding.imgCheckbox
            )
        }
        return views
    }

    @SuppressLint("SetTextI18n")
    override fun setData(
        binding: ViewBinding,
        item: StorageItem,
        layoutPosition: Int
    ) {
        val views = extractViews(binding)

        // Hiển thị tên
        views.tvFileName.text = item.name

        if (item.isDirectory) {
            // Icon thư mục
            views.imgThumbnail.setImageResource(R.drawable.ic_folder)

            // Hiển thị số lượng item con
            val context = views.root.context
            if (item.itemCount <= 1) {
                views.tvFileInfo.text = context.getString(R.string.folder_item_count_single)
            } else {
                views.tvFileInfo.text = context.getString(R.string.folder_item_count, item.itemCount)
            }
        } else {
            val context = views.root.context
            val filePath = item.path
            if (!filePath.isNullOrEmpty()) {
                val file = File(filePath)
                if (file.exists() && file.length() > 0) {
                    // Load thumbnail bằng Glide với File object
                    val iconRes = getFileIconByExtension(item.extension)
                    views.imgThumbnail.setImageResource(iconRes)
                    Glide.with(context)
                        .load(file)
                        .placeholder(iconRes)
                        .error(iconRes)
                        .centerCrop()
                        .into(views.imgThumbnail)
                } else {
                    // Icon theo đuôi file
                    val iconRes = getFileIconByExtension(item.extension)
                    views.imgThumbnail.setImageResource(iconRes)
                }
            }

            // Hiển thị dung lượng và ngày chỉnh sửa
            val readableSize = Formatter.formatShortFileSize(context, item.sizeBytes)
            if (item.dateModifiedMillis > 0L) {
                val formattedDate = SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.getDefault()
                ).format(Date(item.dateModifiedMillis))
                views.tvFileInfo.text = "$readableSize • $formattedDate"
            } else {
                views.tvFileInfo.text = readableSize
            }
        }

        // Cập nhật UI selection mode (checkbox, more button)
        bindSelectionUi(views, item)
    }

    override fun onCLick(
        binding: ViewBinding,
        item: StorageItem,
        layoutPosition: Int
    ) {
        super.onCLick(binding, item, layoutPosition)
        val views = extractViews(binding)

        // Click thường: nếu đang selection mode thì toggle chọn, còn lại mở item
        views.root.setOnClickListener {
            if (isSelectionMode) {
                onSelectToggle?.invoke(item)
            } else {
                onItemClick?.invoke(item)
            }
        }

        // Long press: bật selection mode và chọn item này
        views.root.setOnLongClickListener {
            if (!isSelectionMode) {
                onSelectToggle?.invoke(item)
            }
            true
        }

        // Nút more (3 chấm)
        views.btnMore.tap {
            onMoreClick?.invoke(item, views.btnMore)
        }

        // Click vào checkbox cũng toggle
        views.imgCheckbox.tap {
            onSelectToggle?.invoke(item)
        }
    }

    // Cập nhật hiển thị checkbox và nút more theo trạng thái selection
    private fun bindSelectionUi(views: ViewHolderViews, item: StorageItem) {
        if (isSelectionMode) {
            // Selection mode: hiện checkbox, ẩn more
            views.imgCheckbox.visible()
            views.btnMore.gone()

            // Đánh dấu checkbox đã chọn hay chưa
            val isChecked = selectedPaths.contains(item.path)
            if (isChecked) {
                views.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
            } else {
                views.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
            }
        } else {
            // Chế độ thường: ẩn checkbox, hiện more
            views.imgCheckbox.gone()
            views.btnMore.visible()
        }
    }

    // Trả về icon tương ứng theo đuôi mở rộng của file
    private fun getFileIconByExtension(extension: String): Int {
        val ext = extension.lowercase()
        val iconRes: Int
        if (ext == "pdf") {
            iconRes = R.drawable.ic_pdf
        } else if (ext == "doc" || ext == "docx") {
            iconRes = R.drawable.ic_doc
        } else if (ext == "xls" || ext == "xlsx") {
            iconRes = R.drawable.ic_excel
        } else if (ext == "ppt" || ext == "pptx") {
            iconRes = R.drawable.ic_ppt
        } else if (ext == "txt") {
            iconRes = R.drawable.ic_txt
        } else if (ext == "zip" || ext == "rar" || ext == "7z" || ext == "tar" || ext == "gz" || ext == "bz2") {
            iconRes = R.drawable.ic_zip
        } else if (ext == "mp3" || ext == "wav" || ext == "m4a" || ext == "flac" || ext == "aac") {
            iconRes = R.drawable.ic_audio
        } else if (ext == "mp4" || ext == "mkv" || ext == "avi" || ext == "mov") {
            iconRes = R.drawable.ic_video
        } else if (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "gif" || ext == "webp") {
            iconRes = R.drawable.ic_file
        } else {
            iconRes = R.drawable.ic_file
        }
        return iconRes
    }
}
