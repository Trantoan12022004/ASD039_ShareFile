package com.example.basekotlin.ui.transfer.send.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseApdaterSelected
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.databinding.ItemAppBinding
import com.example.basekotlin.ui.transfer.model.TransferableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppsAdapter : BaseApdaterSelected<TransferableItem, ItemAppBinding>() {

    var onClick: ((TransferableItem) -> Unit)? = null

    // Tăng dung lượng Cache lên 100 icon để cuộn 60fps mượt mà
    private val iconCache = LruCache<String, Drawable>(100)
    private val adapterScope = CoroutineScope(Dispatchers.Main)

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemAppBinding {
        return ItemAppBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<TransferableItem>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemKey(item: TransferableItem): Any = item.id

    // 1. Nạp tên và tải icon tương ứng
    override fun setData(binding: ItemAppBinding, item: TransferableItem, layoutPosition: Int) {
        binding.tvTitle.text = item.displayName
        loadAppIconAsync(binding, item)
    }

    // 2. Cập nhật Checkbox qua Payload O(1)
    override fun setSelection(binding: ItemAppBinding, item: TransferableItem, layoutPosition: Int) {
        val isSelected = selectedKeys.contains(item.id)
        if (isSelected) {
            binding.ivCheckBox.visible()
            binding.ivCheckBox.setImageResource(R.drawable.radio_button_checked)
        } else {
            binding.ivCheckBox.gone()
        }
    }

    override fun onCLick(binding: ItemAppBinding, item: TransferableItem, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)
        binding.root.tap {
            onClick?.invoke(item)
        }
    }

    /**
     * Tải Icon thông minh:
     * - Nếu là file APK chưa cài đặt (kết thúc bằng .apk hoặc file tồn tại trên máy) -> giải mã từ file APK archive
     * - Nếu là App đã cài đặt -> đọc qua Package Manager bằng packageName
     */
    private fun loadAppIconAsync(binding: ItemAppBinding, item: TransferableItem) {
        val cached = iconCache.get(item.id)
        if (cached != null) {
            binding.imgThumbnail.setImageDrawable(cached)
            return
        }

        binding.imgThumbnail.setImageResource(R.drawable.ic_app)
        binding.imgThumbnail.tag = item.id

        adapterScope.launch {
            val icon = withContext(Dispatchers.IO) {
                val context = binding.root.context
                val isApkFile = item.id.endsWith(".apk", ignoreCase = true) || File(item.id).exists()

                if (isApkFile) {
                    // Giải mã icon từ file APK chưa cài đặt
                    ApkFileScanner.loadIcon(context, item.id)
                } else {
                    // Lấy icon của app đã cài đặt
                    try {
                        context.packageManager.getApplicationIcon(item.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (icon != null) {
                iconCache.put(item.id, icon)
                // Đảm bảo viewholder chưa bị recycle khi cuộn nhanh
                if (binding.imgThumbnail.tag == item.id) {
                    binding.imgThumbnail.setImageDrawable(icon)
                }
            }
        }
    }
}
