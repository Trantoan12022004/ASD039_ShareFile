package com.example.basekotlin.ui.storage.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.DialogRenameBinding
import com.example.basekotlin.util.FileNameCheck

class RenameDialog(
    context: Context,
    private val initText: String,
    private val validate: ((String) -> String?)? = null,
    private val onConfirm: (String) -> Unit
) : BaseDialog<DialogRenameBinding>(context, true) {

    override fun setBinding(): DialogRenameBinding {
        return DialogRenameBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // Gán tên ban đầu vào EditText
        binding.edtInput.setText(initText)
        // Đặt con trỏ chuột ở cuối chuỗi văn bản
        binding.edtInput.setSelection(initText.length)

        hideError()
        setupSaveButton(isEnabled = false)

    }

    override fun bindView() {
        // Nút Cancel: Đóng dialog
        binding.btnCancel.tap {
            dismiss()
        }
        // Lắng nghe thay đổi nội dung nhập
        binding.edtInput.doAfterTextChanged { editableText ->
            val inputText = editableText?.toString()?.trim().orEmpty()
            handleTextChange(inputText)
        }
        // Nút Save: Xác nhận đổi tên
        binding.btnCreate.tap {
            val newName = binding.edtInput.text.toString().trim()
            val errorMessage = validateFileName(newName)
            if (errorMessage != null) {
                showError(errorMessage)
                setupSaveButton(isEnabled = false)
            } else {
                dismiss()
                onConfirm(newName)
            }
        }
    }
    /**
     * Xử lý khi text thay đổi: kiểm tra rỗng, trùng tên cũ và tính hợp lệ của tên
     */
    private fun handleTextChange(inputText: String) {
        when {
            // Trường hợp rỗng hoặc trùng khớp hoàn toàn với tên ban đầu -> Không cho lưu, ẩn lỗi
            inputText.isEmpty() || inputText == initText.trim() -> {
                hideError()
                setupSaveButton(isEnabled = false)
            }
            else -> {
                val errorMessage = validateFileName(inputText)
                if (errorMessage != null) {
                    showError(errorMessage)
                    setupSaveButton(isEnabled = false)
                } else {
                    hideError()
                    setupSaveButton(isEnabled = true)
                }
            }
        }
    }
    /**
     * Kiểm tra tính hợp lệ của tên thông qua FileNameCheck và custom validator
     */
    private fun validateFileName(name: String): String? =
        FileNameCheck.validateFileName(
            context = context,
            name = name,
            customValidator = validate
        )
    private fun showError(message: String) {
        binding.tvErrorInput.text = message
        binding.tvErrorInput.visible()
    }
    private fun hideError() {
        binding.tvErrorInput.gone()
    }
    /**
     * Cập nhật màu sắc và trạng thái click của nút Save
     */
    private fun setupSaveButton(isEnabled: Boolean) {
        binding.btnCreate.isEnabled = isEnabled
        val bgRes = if (isEnabled) R.drawable.bg_btn_create else R.drawable.bg_btn_create_1
        val textColorRes = if (isEnabled) R.color.white else R.color.white_disable
        binding.btnCreate.setBackgroundResource(bgRes)
        binding.btnCreate.setTextColor(ContextCompat.getColor(context, textColorRes))
    }
}
