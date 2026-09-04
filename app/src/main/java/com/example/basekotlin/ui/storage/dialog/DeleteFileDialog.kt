package com.example.basekotlin.ui.storage.dialog

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDeleteBinding
import com.example.basekotlin.model.StorageItem

class DeleteFileDialog(
    context: Context,
    private val item: StorageItem,
    private val onConfirm: () -> Unit
) : BaseDialog<DialogDeleteBinding>(context, true) {
    override fun setBinding(): DialogDeleteBinding {
        return DialogDeleteBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvTitleConfirm.text = context.getString(R.string.delete_file)
        binding.tvMessageConfirm.text = context.getString(R.string.delete_file_desc, item.name)
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