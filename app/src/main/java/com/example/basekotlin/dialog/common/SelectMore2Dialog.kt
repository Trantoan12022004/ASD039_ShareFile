package com.example.basekotlin.dialog.common

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.PopupSelectionMore1Binding
import com.example.basekotlin.databinding.PopupSelectionMore2Binding
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.PhotoInfo

class SelectMore2Dialog(
    context: Context,
    private val photo: PhotoInfo,
    private val onSend: (PhotoInfo) -> Unit,
    private val onShare: (PhotoInfo) -> Unit,
    private val onDelete: (PhotoInfo) -> Unit,
    private val onRename: (PhotoInfo) -> Unit,
    private val onConvertPdf: (PhotoInfo) -> Unit,
    private val onMoveSafeBox: (PhotoInfo) -> Unit,
    private val onInformation: (PhotoInfo) -> Unit,
) : BaseDialog<PopupSelectionMore2Binding>(context, true) {
    override fun setBinding(): PopupSelectionMore2Binding {
        return PopupSelectionMore2Binding.inflate(layoutInflater)
    }

    override fun initView() {
        val win = window
        if (win != null) {
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }

    }

    override fun bindView() {
        // 1. Nút Gửi (Send)
        binding.btnSend.tap {
            onSend(photo)
            dismiss()
        }
        // 2. Nút Chia sẻ (Share)
        binding.btnShare.tap {
            onShare(photo)
            dismiss()
        }
        // 3. Nút Xoá (Delete)
        binding.btnDeleteSelected.tap {
            onDelete(photo)
            dismiss()
        }
        // 4. Đổi tên ảnh (Rename)
        binding.tvRename.tap {
            onRename(photo)
            dismiss()
        }
        // 5. Chuyển ảnh sang PDF
        binding.tvConvertPdf.tap {
            onConvertPdf(photo)
            dismiss()
        }
        // 6. Chuyển vào SafeBox
        binding.tvAddToPlaylist.tap {
            onMoveSafeBox(photo)
            dismiss()
        }
        // 7. Xem thông tin chi tiết (Information)
        binding.tvInformation.tap {
            onInformation(photo)
            dismiss()
        }
    }
}
