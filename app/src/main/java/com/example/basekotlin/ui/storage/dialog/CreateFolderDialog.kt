package com.example.basekotlin.ui.storage.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.DialogCreateFolderBinding
import com.example.basekotlin.util.FileNameCheck

class CreateFolderDialog(
    context: Context,
    private val validate: ((String) -> String?)? = null,
    private val onConfirm: (String) -> Unit
) : BaseDialog<DialogCreateFolderBinding>(context, true) {

    override fun setBinding(): DialogCreateFolderBinding {
        return DialogCreateFolderBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // Trạng thái ban đầu: nút Create vô hiệu hóa, ẩn thông báo lỗi
        setupCreateButton(false)
        binding.tvErrorInput.gone()
    }

    override fun bindView() {
        // Nút Cancel: đóng dialog
        binding.btnCancel.tap {
            dismiss()
        }

        // Lắng nghe thay đổi nội dung trong ô nhập liệu
        binding.edtInput.doAfterTextChanged { editableText ->
            val inputText: String = editableText?.toString()?.trim() ?: ""

            // Khi ô nhập rỗng: disable nút Create và ẩn lỗi
            if (inputText.isEmpty()) {
                setupCreateButton(false)
                binding.tvErrorInput.gone()
            } else {
                // Kiểm tra tên folder thông qua FileNameUtils
                val errorMessage: String? = FileNameCheck.validateFolderName(
                    context = context,
                    name = inputText,
                    customValidator = validate
                )

                if (errorMessage != null) {
                    // Hiển thị lỗi và vô hiệu hóa nút Create
                    binding.tvErrorInput.text = errorMessage
                    binding.tvErrorInput.visible()
                    setupCreateButton(false)
                } else {
                    // Tên hợp lệ: ẩn lỗi và kích hoạt nút Create
                    binding.tvErrorInput.gone()
                    setupCreateButton(true)
                }
            }
        }

        // Nút Create: xác nhận tạo folder
        binding.btnCreate.tap {
            val folderName: String = binding.edtInput.text.toString().trim()
            val errorMessage: String? = FileNameCheck.validateFolderName(
                context = context,
                name = folderName,
                customValidator = validate
            )

            if (errorMessage != null) {
                // Hiển thị lỗi nếu không hợp lệ
                binding.tvErrorInput.text = errorMessage
                binding.tvErrorInput.visible()
            } else {
                // Đóng dialog và trả về tên thư mục hợp lệ
                dismiss()
                onConfirm(folderName)
            }
        }
    }

    // Đổi màu sắc và trạng thái click của nút Create
    private fun setupCreateButton(isEnabled: Boolean) {
        binding.btnCreate.isEnabled = isEnabled

        if (isEnabled) {
            binding.btnCreate.setBackgroundResource(R.drawable.bg_btn_create)
            binding.btnCreate.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.btnCreate.setBackgroundResource(R.drawable.bg_btn_create_1)
            binding.btnCreate.setTextColor(ContextCompat.getColor(context, R.color.white_disable))
        }
    }
}