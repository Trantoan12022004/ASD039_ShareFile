package com.example.basekotlin.ui.cleanfile.apps.dialog

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogUninstallAppBinding
import com.example.basekotlin.model.AppInfo

class UninstallAppDialog(
    context: Context,
    private val appInfo: AppInfo,
    private val onConfirm: () -> Unit
) : BaseDialog<DialogUninstallAppBinding>(context, true) {

    override fun setBinding(): DialogUninstallAppBinding {
        return DialogUninstallAppBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvTitleConfirm.text = context.getString(R.string.uninstall_app_title)
        binding.tvMessageConfirm.text = context.getString(R.string.uninstall_app_desc, appInfo.appName)
    }

    override fun bindView() {
        binding.btnCancelConfirm.tap {
            dismiss()
        }
        binding.btnPositiveConfirm.tap {
            dismiss()
            onConfirm()
        }
    }
}
