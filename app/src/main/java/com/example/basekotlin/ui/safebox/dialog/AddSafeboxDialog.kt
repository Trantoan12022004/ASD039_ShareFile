package com.example.basekotlin.ui.safebox.dialog

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.databinding.DialogAddSafeboxBinding
import com.example.basekotlin.ui.safebox.adapter.AddSafeboxAdapter
import com.example.basekotlin.ui.safebox.model.SafeBoxCandidateItem

class AddSafeboxDialog(
    context: Context,
    private val fileType: SafeBoxFileType,
    private val candidateFiles: List<SafeBoxCandidateItem>,
    private val onAddConfirmed: (List<SafeBoxCandidateItem>) -> Unit
) : BaseDialog<DialogAddSafeboxBinding>(context, true) {

    private val adapter = AddSafeboxAdapter()

    override fun setBinding(): DialogAddSafeboxBinding {
        return DialogAddSafeboxBinding.inflate(layoutInflater)
    }

    override fun initView() {
        window?.let { win ->
            val displayMetrics = context.resources.displayMetrics
            val height = (displayMetrics.heightPixels * 0.5).toInt()
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
            win.setGravity(Gravity.BOTTOM)
            win.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // Đặt tiêu đề theo category
        val categoryNameRes = when (fileType) {
            SafeBoxFileType.PICTURES -> R.string.pictures
            SafeBoxFileType.VIDEOS -> R.string.videos
            SafeBoxFileType.AUDIO -> R.string.audio
            SafeBoxFileType.DOCUMENTS -> R.string.documents
            SafeBoxFileType.OTHERS -> R.string.others
        }

        binding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        binding.rvPlaylists.adapter = adapter
        adapter.addListData(candidateFiles.toMutableList())
    }

    override fun bindView() {
        binding.btnAdd.tap {
            val selected = adapter.selectedItems
            if (selected.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.please_select_one_item), Toast.LENGTH_SHORT).show()
                return@tap
            }
            dismiss()
            onAddConfirmed(selected)
        }
    }
}
