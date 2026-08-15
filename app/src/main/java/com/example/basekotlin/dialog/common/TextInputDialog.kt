package com.example.basekotlin.dialog.common

import android.view.View
import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogTextInputBinding

// Dialog nhập text dùng chung: Create Playlist, Rename Playlist, Rename Song (Phần 5)...
class TextInputDialog(
    context: Context,
    private val title: String,
    private val hint: String,
    private val initialText: String = "",
    private val positiveText: String,
    private val validate: (String) -> String? = { null },
    private val onConfirm: (String) -> Unit
) : BaseDialog<DialogTextInputBinding>(context, true) {

    override fun setBinding(): DialogTextInputBinding {
        return DialogTextInputBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvTitleInput.text = title
        binding.edtInput.hint = hint
        binding.edtInput.setText(initialText)
        binding.btnCreate.text = positiveText
        binding.edtInput.setSelection(initialText.length)
    }

    override fun bindView() {
        binding.btnCancel.tap {
            dismiss()
        }

        binding.btnCreate.tap {
            val enteredText = binding.edtInput.text.toString().trim()
            if (enteredText.isEmpty()) {
                binding.tvErrorInput.text = context.getString(R.string.text_input_failed)
                binding.tvErrorInput.visibility = View.VISIBLE
            } else {
                val errorMessage = validate(enteredText)
                if (errorMessage != null) {
                    binding.tvErrorInput.text = errorMessage
                    binding.tvErrorInput.visibility = View.VISIBLE
                } else {
                    binding.tvErrorInput.visibility = View.GONE
                    dismiss()
                    onConfirm(enteredText)
                }
            }
        }
    }
    }