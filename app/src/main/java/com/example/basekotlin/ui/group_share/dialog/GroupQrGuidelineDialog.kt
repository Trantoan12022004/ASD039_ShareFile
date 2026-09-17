package com.example.basekotlin.ui.group_share.dialog

import android.content.Context
import android.graphics.Bitmap
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.DialogGroupQrGuidelineBinding

class GroupQrGuidelineDialog(
    context: Context,
    private val qrBitmap: Bitmap,
    private val ipAddress: String,
    private val isHotspotMode: Boolean = false
) : BaseDialog<DialogGroupQrGuidelineBinding>(context, true) {

    override fun setBinding(): DialogGroupQrGuidelineBinding {
        return DialogGroupQrGuidelineBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.ivQrCode.setImageBitmap(qrBitmap)

        if (isHotspotMode) {
            // Chế độ Hotspot: Bỏ dòng IP, chỉ quét mã QR
            binding.layoutIp.gone()
        } else {
            // Chế độ Wi-Fi chung: Hiển thị IP
            binding.layoutIp.visible()
            binding.tvIpAddress.text = ipAddress
        }
    }

    override fun bindView() {
        binding.btnClose.tap {
            dismiss()
        }
    }
}