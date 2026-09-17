package com.example.basekotlin.ui.group_share.dialog

import android.content.Context
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogJoinGroupOptionBinding
import com.example.basekotlin.util.transfer.NetworkUtils

class JoinGroupOptionDialog(
    context: Context,
    private val onScanQrClick: () -> Unit,
    private val onEnterIpClick: () -> Unit
) : BaseDialog<DialogJoinGroupOptionBinding>(context, true) {

    override fun setBinding(): DialogJoinGroupOptionBinding {
        return DialogJoinGroupOptionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        if (!NetworkUtils.isWifiConnected(context)) {
            binding.btnEnterIp.gone()
        }
    }

    override fun bindView() {
        binding.btnScanQr.tap {
            dismiss()
            onScanQrClick()
        }
        binding.btnEnterIp.tap {
            dismiss()
            onEnterIpClick()
        }
        binding.btnCancel.tap {
            dismiss()
        }
    }
}