package com.example.basekotlin.ui.storage.dialog

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailInformationBinding
import com.example.basekotlin.model.StorageItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class InformationStorageDialog(
    context: Context,
    private val item: StorageItem
) : BaseDialog<DialogDetailInformationBinding>(context, true) {
    override fun setBinding(): DialogDetailInformationBinding {
        return DialogDetailInformationBinding.inflate(layoutInflater)
    }
    override fun initView() {
        super.initView()
        // Ẩn dòng thông tin ca sĩ (chỉ dùng cho bài hát)
        binding.llArtist.gone()
        // Gán tên file hoặc thư mục
        binding.tvLabelName.text = context.getString(R.string.label_name)
        binding.tvValueName.text = item.name
        // Gán đường dẫn file hoặc thư mục
        binding.tvLabelPath.text = context.getString(R.string.label_path)
        binding.tvValuePath.text = item.path
        // Gán dung lượng (nếu là thư mục thì hiển thị số lượng item con)
        binding.tvLabelSize.text = context.getString(R.string.label_size)
        if (item.isDirectory) {
            if (item.itemCount == 1) {
                binding.tvValueSize.text = context.getString(R.string.folder_item_count_single)
            } else {
                binding.tvValueSize.text = context.getString(R.string.folder_item_count, item.itemCount)
            }
        } else {
            binding.tvValueSize.text = formatFileSize(item.sizeBytes)
        }
        // Gán ngày chỉnh sửa
        binding.tvLabelDate.text = context.getString(R.string.label_date)
        binding.tvValueDate.text = formatDate(item.dateModifiedMillis)
    }
    override fun bindView() {
        super.bindView()
        // Nhấn nút Got It để đóng dialog
        binding.btnGotIt.tap {
            dismiss()
        }
    }
    private fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        if (sizeBytes >= gb) {
            val value = sizeBytes / gb
            return String.format(Locale.getDefault(), "%.1f GB", value)
        }
        if (sizeBytes >= mb) {
            val value = sizeBytes / mb
            return String.format(Locale.getDefault(), "%.1f MB", value)
        }
        if (sizeBytes >= kb) {
            val value = sizeBytes / kb
            return String.format(Locale.getDefault(), "%.1f KB", value)
        }
        return sizeBytes.toString() + " B"
    }
    private fun formatDate(dateMillis: Long): String {
        if (dateMillis <= 0L) {
            return "-"
        }
        val date = Date(dateMillis)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
}