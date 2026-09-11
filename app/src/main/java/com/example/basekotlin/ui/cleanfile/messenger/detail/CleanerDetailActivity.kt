package com.example.basekotlin.ui.cleanfile.messenger.detail

import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityCleanerDetailBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import com.example.basekotlin.ui.cleanfile.messenger.detail.adapter.CleanerDetailAdapter
import kotlinx.coroutines.launch

class CleanerDetailActivity : BaseActivity<ActivityCleanerDetailBinding>(ActivityCleanerDetailBinding::inflate) {

    companion object {
        const val EXTRA_MESSENGER_TYPE = "extra_messenger_type"
        const val EXTRA_CATEGORY_ID = "extra_category_id"
        const val EXTRA_CATEGORY_NAME = "extra_category_name"
    }

    private val viewModel: CleanerDetailViewModel by viewModels()
    private val adapter by lazy { CleanerDetailAdapter() }

    private var messengerType = ""
    private var categoryId = ""
    private var categoryName = ""

    override fun getData() {
        messengerType = intent.getStringExtra(EXTRA_MESSENGER_TYPE) ?: ""
        categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID) ?: ""
        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
    }

    override fun initView() {
        binding.tvTitle.text = categoryName

        binding.rvDetailFiles.layoutManager = LinearLayoutManager(this)
        binding.rvDetailFiles.adapter = adapter

        adapter.onSelectToggle = {
            updateSelectionUi()
        }
    }

    override fun bindView() {
        binding.btnClose.tap {
            finishThisActivity()
        }

        binding.layoutSelectAll.tap {
            val totalCount = adapter.listData.size
            val allSelected = adapter.selectedPaths.size == totalCount && totalCount > 0
            adapter.selectAll(!allSelected)
            updateSelectionUi()
        }

        binding.btnDelete.tap {
            val selectedCount = adapter.selectedPaths.size
            if (selectedCount > 0) {
                ConfirmActionDialog(
                    context = this,
                    title = getString(R.string.delete_selected_files),
                    message = getString(R.string.delete_selected_files_desc, selectedCount),
                    positiveText = getString(R.string.delete)
                ) {
                    val pathsToDelete = adapter.selectedPaths.toList()
                    viewModel.deleteFiles(pathsToDelete) { success ->
                        adapter.selectedPaths.clear()
                        updateSelectionUi()
                        val msg = if (success) getString(R.string.delete_files_success) else getString(R.string.delete_files_failed)
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }.show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.files.collect { files ->
                    adapter.addListData(files.toMutableList())
                    adapter.selectedPaths.clear()
                    updateSelectionUi()

                    val totalBytes = files.sumOf { it.sizeBytes }
                    val totalSizeStr = CleanFileUtils.formatFileSize(totalBytes)
                    binding.tvTotalInfo.text = "$categoryName (${files.size}) - $totalSizeStr"

                    if (files.isEmpty() && !viewModel.isLoading.value) {
                        binding.layoutEmpty.visible()
                        binding.tvTotalInfo.gone()
                    } else {
                        binding.layoutEmpty.gone()
                        binding.tvTotalInfo.visible()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    if (loading) binding.progressBar.visible() else binding.progressBar.gone()
                }
            }
        }
    }

    private fun updateSelectionUi() {
        val selectedCount = adapter.selectedPaths.size
        val totalCount = adapter.listData.size
        val allSelected = selectedCount == totalCount && totalCount > 0

        binding.selectAll.text = getString(
            if (allSelected) R.string.deselect_all else R.string.select_all
        )

        if (selectedCount > 0) {
            binding.layoutBottomDelete.visible()
            binding.btnDelete.text = getString(R.string.delete_with_count, selectedCount)
        } else {
            binding.layoutBottomDelete.gone()
        }
    }

    override fun onResume() {
        super.onResume()
        if (messengerType.isNotEmpty() && categoryId.isNotEmpty()) {
            viewModel.loadFiles(messengerType, categoryId)
        }
    }
}
