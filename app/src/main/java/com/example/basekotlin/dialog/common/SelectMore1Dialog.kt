package com.example.basekotlin.dialog.common

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.PopupSelectionMore1Binding
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.PhotoInfo

class SelectMore1Dialog(
    context: Context,
    private val selectedPhotos: List<PhotoInfo>,
    private val isFolderTab: Boolean = false,
    private val onRename: (PhotoInfo) -> Unit,
    private val onConvertPdf: (List<PhotoInfo>) -> Unit,
    private val onInformation: (PhotoInfo) -> Unit,
    private val onMoveSafeBox: (PhotoInfo) -> Unit,
) : BaseDialog<PopupSelectionMore1Binding>(context, true) {
    override fun setBinding(): PopupSelectionMore1Binding {
        return PopupSelectionMore1Binding.inflate(layoutInflater)
    }

    override fun initView() {
        val win = window
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // Nếu ở tab Folder: ẩn chức năng Đổi tên và Thông tin chi tiết cùng divider tương ứng
        if (isFolderTab) {
            binding.tvRename.gone()
            binding.dividerRename.gone()
            binding.dividerInformation.gone()
            binding.tvInformation.gone()
        } else {
            binding.tvRename.visible()
            binding.dividerRename.visible()
            binding.dividerInformation.visible()
            binding.tvInformation.visible()
        }
    }

    override fun bindView() {
        // 1. Xử lý Đổi tên (Chỉ cho phép khi chọn đúng 1 ảnh)
        binding.tvRename.tap {
            if (selectedPhotos.size == 1) {
                val photo = selectedPhotos[0]
                onRename(photo)
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.select_only_one_item_to_rename),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 2. Xử lý Chuyển ảnh sang PDF
        binding.tvConvertPdf.tap {
            if (selectedPhotos.isNotEmpty()) {
                onConvertPdf(selectedPhotos)
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // 3. Xử lý Xem thông tin chi tiết (Chỉ cho phép khi chọn đúng 1 ảnh)
        binding.tvInformation.tap {
            if (selectedPhotos.size == 1) {
                val photo = selectedPhotos[0]
                onInformation(photo)
                dismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
