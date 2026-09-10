package com.example.basekotlin.ui.safebox.home

import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import com.example.basekotlin.databinding.ActivitySafeboxFileListBinding
import com.example.basekotlin.databinding.PopupMoreSafeboxBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.ui.safebox.adapter.SafeBoxFileAdapter
import com.example.basekotlin.ui.safebox.dialog.AddSafeboxDialog
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class SafeBoxFileListActivity :
    BaseActivity<ActivitySafeboxFileListBinding>(ActivitySafeboxFileListBinding::inflate) {

    private val viewModel: SafeBoxFileListViewModel by viewModels()
    private val adapter = SafeBoxFileAdapter()

    override fun getData() {
        super.getData()
        val typeString = intent.getStringExtra("EXTRA_FILE_TYPE") ?: SafeBoxFileType.PICTURES.name
        val fileType = runCatching { SafeBoxFileType.valueOf(typeString) }.getOrDefault(SafeBoxFileType.PICTURES)
        viewModel.initType(fileType)
    }

    override fun initView() {
        binding.rvFiles.adapter = adapter

        // Đặt tiêu đề theo category
        val titleRes = when (viewModel.currentType.value) {
            SafeBoxFileType.PICTURES -> R.string.pictures
            SafeBoxFileType.VIDEOS -> R.string.videos
            SafeBoxFileType.AUDIO -> R.string.audio
            SafeBoxFileType.DOCUMENTS -> R.string.documents
            SafeBoxFileType.OTHERS -> R.string.others
        }
        binding.viewTop.tvTitle.setText(titleRes)



        binding.viewTop.btnBack.tap { onBack() }
    }

    override fun bindView() {

        // Adapter Events
        adapter.onItemClick = { file ->
            // Mở xem file chi tiết
        }
        adapter.onMoreClick = { file, anchorView ->
            showMorePopup(file, anchorView)
        }

        binding.fabAdd.tap {
            showAddSafeboxDialog()
        }


        observeData()
    }

    private fun showAddSafeboxDialog() {
        lifecycleScope.launch {
            val currentType = viewModel.currentType.value
            val candidates = viewModel.getCandidateFiles(currentType)
            if (candidates.isEmpty()) {
                Toast.makeText(this@SafeBoxFileListActivity, getString(R.string.no_file), Toast.LENGTH_SHORT).show()
                return@launch
            }
            AddSafeboxDialog(
                context = this@SafeBoxFileListActivity,
                fileType = currentType,
                candidateFiles = candidates
            ) { selectedFiles ->
                viewModel.addSelectedFilesToSafeBox(selectedFiles, currentType) { count ->
                    val msg =
                        if (count > 0) getString(R.string.safe_box_move_success) else getString(R.string.safe_box_move_failed)
                    Toast.makeText(this@SafeBoxFileListActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }.show()
        }
    }


    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.files.collect { files ->
                        adapter.addListData(files.toMutableList())
                        if (files.isEmpty()) {
                            binding.layoutEmpty.visible()
                            binding.rvFiles.gone()
                        } else {
                            binding.layoutEmpty.gone()
                            binding.rvFiles.visible()
                        }
                    }
                }
            }
        }
    }

    private fun showMorePopup(file: SafeBoxFile, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { PopupMoreSafeboxBinding.inflate(it) },
            widthRatio = 0.55f
        ) { popupBinding, popupWindow ->
            popupBinding.tvRestore.tap {
                popupWindow.dismiss()
                confirmRestoreSingle(file)
            }
            popupBinding.tvDeletePermanently.tap {
                popupWindow.dismiss()
                confirmDeleteSingle(file)
            }
        }
    }

    private fun confirmRestoreSingle(file: SafeBoxFile) {
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.restore_file_safebox_title),
            message = getString(R.string.restore_file_safebox_desc, file.fileName),
            positiveText = getString(R.string.restore)
        ) {
            viewModel.restoreFile(file) { success ->
                val msg = if (success) R.string.restore_success else R.string.restore_failed
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }.show()
    }

    private fun confirmDeleteSingle(file: SafeBoxFile) {
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete_file_safebox_title),
            message = getString(R.string.delete_file_safebox_desc, file.fileName),
            positiveText = getString(R.string.delete)
        ) {
            viewModel.deletePermanently(file) { success ->
                val msg = if (success) R.string.delete_permanently_success else R.string.delete_permanently_failed
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }.show()
    }
}
