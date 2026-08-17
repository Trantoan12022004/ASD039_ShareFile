package com.example.basekotlin.ui.files.photos.adapter

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.basekotlin.R
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemHeaderDateBinding
import com.example.basekotlin.databinding.ItemPhotoBinding
import com.example.basekotlin.model.PhotoInfo

// Model đại diện cho từng loại phần tử trong danh sách ảnh
sealed class PhotoListItem {
    data class Header(val dateString: String, val count: Int) : PhotoListItem()
    data class Photo(val photoInfo: PhotoInfo) : PhotoListItem()
}

class PhotoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_PHOTO = 1
    }

    // Danh sách dữ liệu hiển thị
    val listData = mutableListOf<PhotoListItem>()

    // Callback khi click mở xem ảnh ở chế độ bình thường
    var onItemClick: ((PhotoInfo) -> Unit)? = null

    // Callback khi nhấn giữ item để kích hoạt chế độ chọn
    var onItemLongClick: ((PhotoInfo) -> Unit)? = null

    // Callback khi toggle chọn/bỏ chọn ảnh
    var onSelectToggle: ((PhotoInfo) -> Unit)? = null

    // Trạng thái đang chọn nhiều ảnh hay không
    var isSelectionMode: Boolean = false

    // Danh sách đường dẫn (filePath) các ảnh đang được chọn
    var selectedPhotos: Set<String> = emptySet()

    fun submitList(newList: List<PhotoListItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = listData[position]
        if (item is PhotoListItem.Header) {
            return TYPE_HEADER
        } else {
            return TYPE_PHOTO
        }
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == TYPE_HEADER) {
            val binding = ItemHeaderDateBinding.inflate(inflater, parent, false)
            return HeaderViewHolder(binding)
        } else {
            val binding = ItemPhotoBinding.inflate(inflater, parent, false)
            return PhotoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = listData[position]
        if (holder is HeaderViewHolder && item is PhotoListItem.Header) {
            holder.bind(item)
        } else if (holder is PhotoViewHolder && item is PhotoListItem.Photo) {
            holder.bind(item.photoInfo)
        }
    }

    // ViewHolder cho phần Header ngày (ví dụ: "Jan 26, 2026 (5)")
    inner class HeaderViewHolder(private val binding: ItemHeaderDateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhotoListItem.Header) {
            val fullText = item.dateString + " (" + item.count + ")"
            val spannable = SpannableStringBuilder(fullText)

            // Đổi màu xanh lá cho phần đếm số lượng (count)
            val startIndex = item.dateString.length + 1
            val endIndex = fullText.length
            val colorGreen = ContextCompat.getColor(binding.root.context, R.color.primary_40)
            spannable.setSpan(
                ForegroundColorSpan(colorGreen),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            binding.tvHeaderDate.text = spannable
        }
    }

    // ViewHolder cho từng ảnh hiển thị trong Grid
    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photoInfo: PhotoInfo) {
            val context = binding.root.context

            // 1. Load ảnh thumbnail qua Glide
            Glide.with(context)
                .asBitmap()
                .load(photoInfo.contentUri)
                .centerCrop()
                .into(binding.imgThumbnail)

            // 2. Hiển thị checkbox khi ở chế độ Selection Mode
            bindSelectionUi(binding, photoInfo)

            // 3. Xử lý click mở ảnh hoặc chọn ảnh
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    val selectListener = onSelectToggle
                    if (selectListener != null) {
                        selectListener(photoInfo)
                    }
                } else {
                    val clickListener = onItemClick
                    if (clickListener != null) {
                        clickListener(photoInfo)
                    }
                }
            }

            // 4. Click checkbox để chọn/bỏ chọn
            binding.ivCheckBox.tap {
                val selectListener = onSelectToggle
                if (selectListener != null) {
                    selectListener(photoInfo)
                }
            }

            // 5. Long click để kích hoạt chế độ chọn
            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    val selectListener = onSelectToggle
                    if (selectListener != null) {
                        selectListener(photoInfo)
                    }
                }
                true
            }

            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    val longClickListener = onItemLongClick
                    if (longClickListener != null) {
                        longClickListener(photoInfo)
                    } else {
                        val selectListener = onSelectToggle
                        if (selectListener != null) {
                            selectListener(photoInfo)
                        }
                    }
                }
                true
            }
        }

        private fun bindSelectionUi(binding: ItemPhotoBinding, item: PhotoInfo) {
            if (isSelectionMode) {
                binding.ivCheckBox.visible()
                val isChecked = selectedPhotos.contains(item.filePath)
                if (isChecked) {
                    binding.ivCheckBox.setImageResource(R.drawable.check_box)
                } else {
                    binding.ivCheckBox.setImageResource(R.drawable.check_box_outline_blank)
                }
            } else {
                binding.ivCheckBox.gone()
            }
        }
    }
}
