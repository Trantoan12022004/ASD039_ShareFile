package com.example.basekotlin.ui.cleanfile.photocleanup

import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityMediaCleanupBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.ui.cleanfile.SectionCleanupAdapter
import kotlinx.coroutines.launch

class PhotoCleanupActivity : BaseActivity<ActivityMediaCleanupBinding>(ActivityMediaCleanupBinding::inflate) {

    private val viewModel: PhotoCleanupViewModel by viewModels()
    private val adapter by lazy { SectionCleanupAdapter() }

    override fun initView() {
        binding.tvTitle.text = getString(R.string.photo_cleanup)
        binding.tvEmptyTitle.text = getString(R.string.no_photo_cleanup)
        binding.tvEmptyDesc.text = getString(R.string.no_photo_cleanup_desc)

        binding.rvMediaCleanup.layoutManager = LinearLayoutManager(this)
        binding.rvMediaCleanup.adapter = adapter

        adapter.onSelectToggle = {
            updateSelectionUi()
        }

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.search(text?.toString() ?: "")
        }
    }

    override fun bindView() {
        binding.btnBack.tap {
            finishThisActivity()
        }

        binding.layoutSelectAll.tap {
            val totalCount = adapter.getAllItemCount()
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
                viewModel.filteredGroups.collect { groups ->
                    adapter.setGroups(groups)
                    adapter.selectedPaths.clear()
                    updateSelectionUi()

                    val hasItems = groups.any { it.items.isNotEmpty() }
                    if (!hasItems && !viewModel.isLoading.value) {
                        binding.layoutEmpty.visible()
                    } else {
                        binding.layoutEmpty.gone()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    if (loading) {
                        binding.progressBar.visible()
                        binding.layoutEmpty.gone()
                    }else binding.progressBar.gone()
                }
            }
        }
    }

    private fun updateSelectionUi() {
        val selectedCount = adapter.selectedPaths.size
        val totalCount = adapter.getAllItemCount()
        val allSelected = selectedCount == totalCount && totalCount > 0

        binding.selectAll.text = getString(
            if (allSelected) R.string.deselect_all else R.string.select_all
        )

        if (selectedCount > 0) {
            binding.btnDelete.setBackgroundResource(R.drawable.bg_btn_delete)
            binding.btnDelete.text = getString(R.string.delete_with_count, selectedCount)
        } else {
            binding.btnDelete.setBackgroundResource(R.drawable.bg_btn_delete1)
            binding.btnDelete.text = getString(R.string.delete_with_count, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPhotos()
    }
}
