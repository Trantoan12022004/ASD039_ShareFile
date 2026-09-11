package com.example.basekotlin.ui.cleanfile.bigfiles

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
import com.example.basekotlin.databinding.ActivityBigFilesBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import com.example.basekotlin.ui.cleanfile.bigfiles.adapter.BigFilesAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class BigFilesActivity : BaseActivity<ActivityBigFilesBinding>(ActivityBigFilesBinding::inflate) {

    private val viewModel: BigFilesViewModel by viewModels()
    private val adapter by lazy { BigFilesAdapter() }

    override fun initView() {
        binding.rvBigFiles.layoutManager = LinearLayoutManager(this)
        binding.rvBigFiles.adapter = adapter

        adapter.onSelectToggle = {
            updateSelectionUi()
        }

        setupTabLayout()
    }

    override fun bindView() {
        binding.btnBack.tap {
            finishThisActivity()
        }

        binding.layoutSelectAll.tap {
            val allSelected = adapter.selectedPaths.size == adapter.listData.size && adapter.listData.isNotEmpty()
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
                viewModel.filteredFiles.collect { files ->
                    adapter.addListData(files.toMutableList())
                    adapter.selectedPaths.clear()
                    updateSelectionUi()

                    val totalBytes = files.sumOf { it.sizeBytes }
                    val totalSizeStr = CleanFileUtils.formatFileSize(totalBytes)
                    binding.tvTotalInfo.text = "Big Files (${files.size}) - $totalSizeStr"

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

    private fun setupTabLayout() {
        val filterTabs = listOf(
            getString(R.string.filter_all) to BigFilesViewModel.FILTER_ALL,
            getString(R.string.filter_photo) to BigFilesViewModel.FILTER_PHOTO,
            getString(R.string.filter_video) to BigFilesViewModel.FILTER_VIDEO,
            getString(R.string.filter_music) to BigFilesViewModel.FILTER_MUSIC,
            getString(R.string.filter_other) to BigFilesViewModel.FILTER_OTHER
        )

        binding.tabLayout.removeAllTabs()
        for ((title, filterType) in filterTabs) {
            val tab = binding.tabLayout.newTab().setText(title)
            tab.tag = filterType
            binding.tabLayout.addTab(tab)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filterType = tab?.tag as? Int ?: BigFilesViewModel.FILTER_ALL
                viewModel.setFilter(filterType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
        viewModel.loadBigFiles()
    }
}
