package com.example.basekotlin.ui.files.photos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemPhotoFolderBinding
import com.example.basekotlin.model.PhotoFolder


class PhotoFolderAdapter : BaseAdapter<PhotoFolder, ItemPhotoFolderBinding>() {
    // Callback khi click vào folder ở chế độ xem thông thường
    var onClick: ((PhotoFolder) -> Unit) ? = null
    // Callback khi nhấn giữ (long click) vào folder để kích hoạt chế độ chọn
    var onLongClick: ((PhotoFolder) -> Unit)? = null
    // Callback khi toggle chọn/bỏ chọn folder
    var onSelectToggle: ((PhotoFolder) -> Unit)? = null
    // Callback kiểm tra xem folder có được chọn đầy đủ hay không
    var isFolderSelectedChecker: ((String) -> Boolean)? = null
    // Trạng thái đang ở Selection Mode hay không
    var isSelectionMode: Boolean = false
    // Danh sách đường dẫn ảnh đang được chọn
    var selectedPhotoPaths: Set<String> = emptySet()

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemPhotoFolderBinding {
        return ItemPhotoFolderBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<PhotoFolder>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemPhotoFolderBinding,
        item: PhotoFolder,
        layoutPosition: Int
    ) {
        val context = binding.root.context
        // 1. Hiển thị tên folder
        binding.tvFolderName.text = item.folderName
        // 2. Hiển thị số lượng file trong folder
        val countText = item.photoCount.toString() + " " + context.getString(R.string.files1)
        binding.tvCount.text = countText
        // 3. Load ảnh cover của folder bằng Glide
        if (item.coverPhotoUri != null) {
            Glide.with(context)
                .asBitmap()
                .load(item.coverPhotoUri)
                .centerCrop()
                .placeholder(R.drawable.ic_no_folder)
                .error(R.drawable.ic_no_folder)
                .into(binding.imgThumbnail)
        } else {
            binding.imgThumbnail.setImageResource(R.drawable.ic_no_folder)
        }
        // 4. Hiển thị Checkbox khi ở Selection Mode
        if (isSelectionMode) {
            binding.ivCheckBox.visible()
            val checker = isFolderSelectedChecker
            val isChecked = if (checker != null) {
                checker(item.folderPath)
            } else {
                false
            }
            if (isChecked) {
                binding.ivCheckBox.setImageResource(R.drawable.check_box)
            } else {
                binding.ivCheckBox.setImageResource(R.drawable.check_box_outline_blank)
            }
        } else {
            binding.ivCheckBox.gone()
        }
    }

    override fun onCLick(
        binding: ItemPhotoFolderBinding,
        item: PhotoFolder,
        layoutPosition: Int
    ) {
        super.onCLick(binding, item, layoutPosition)
        // 1. Xử lý click vào item
        binding.root.setOnClickListener {
            if (isSelectionMode) {
                val toggleListener = onSelectToggle
                if (toggleListener != null) {
                    toggleListener(item)
                }
            } else {
                val clickListener = onClick
                if (clickListener != null) {
                    clickListener(item)
                }
            }
        }
        // 2. Xử lý click trực tiếp vào checkbox
        binding.ivCheckBox.tap {
            val toggleListener = onSelectToggle
            if (toggleListener != null) {
                toggleListener(item)
            }
        }
        // 3. Xử lý long click vào item để kích hoạt chế độ chọn
        binding.root.setOnLongClickListener {
            if (isSelectionMode == false) {
                val longClickListener = onLongClick
                if (longClickListener != null) {
                    longClickListener(item)
                } else {
                    val toggleListener = onSelectToggle
                    if (toggleListener != null) {
                        toggleListener(item)
                    }
                }
            }
            true
        }
    }
}