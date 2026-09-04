package com.example.basekotlin.ui.files.zips.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentZipsBinding
import com.example.basekotlin.databinding.PopupMoreZipBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.model.ZipInfo
import com.example.basekotlin.ui.files.zips.ZipsViewModel
import com.example.basekotlin.ui.files.zips.adapter.ZippedAdapter
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch
import java.io.File


class ZippedFragment : BaseFragment<FragmentZipsBinding>() {

    private val viewModel: ZipsViewModel by activityViewModels()
    private val adapter = ZippedAdapter()
    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentZipsBinding {
        return FragmentZipsBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvZips.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAllZips()
        }
    }

    override fun bindView() {

        adapter.onSelectToggle = { zip ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleZipSelection(zip.filePath)
            } else {
                viewModel.enterSelectionMode(zip.filePath)
            }
        }

        adapter.onMoreClick = { zip, anchor ->
            showMoreMenu(zip, anchor)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.progressLoading.visible()
                            binding.layoutContent.gone()
                            binding.allEmpty.gone()
                        } else {
                            binding.progressLoading.gone()
                            binding.swipeRefresh.isRefreshing = false
                            updateEmptyState()
                        }
                    }
                }
                launch {
                    viewModel.allZips.collect { list ->
                        adapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }

                // Quan sát trạng thái chế độ chọn
                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        adapter.isSelectionMode = isSelectionMode
                        adapter.notifyDataSetChanged()
                    }
                }

                // Quan sát danh sách các file được chọn
                launch {
                    viewModel.selectedZipsPaths.collect { selectedPaths ->
                        adapter.selectedZips = selectedPaths
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun updateEmptyState() {
        val isLoading = viewModel.isLoading.value
        if (isLoading) {
            binding.allEmpty.gone()
            binding.layoutContent.gone()
            return
        }
        if (adapter.listData.isEmpty()) {
            binding.allEmpty.visible()
            binding.layoutContent.gone()
        } else {
            binding.allEmpty.gone()
            binding.layoutContent.visible()
        }
    }

    private fun showMoreMenu(zip: ZipInfo, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor,
            inflateBinding = {inflater -> PopupMoreZipBinding.inflate(inflater)}
        ){  popupBinding, popupWindow ->
            popupBinding.tvExtract.tap {
                popupWindow.dismiss()
                handleExtractZip(zip)

            }
            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeleteSelectedZipsDialog(listOf(zip))
            }

        }
    }

    private fun showDeleteSelectedZipsDialog(zipList: List<ZipInfo>) {
        val message: String
        if (zipList.size == 1) {
            message = getString(R.string.delete_zip_desc, zipList[0].fileName)
        } else {
            message = getString(R.string.delete_zips_desc, zipList.size)
        }

        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDeleteSelectedZips(zipList)
        }.show()
    }

    private fun performDeleteSelectedZips(zipList: List<ZipInfo>) {
        var successCount = 0
        var failCount = 0

        for (zip in zipList) {
            val file = File(zip.filePath)
            val deleted = file.delete()
            if (deleted) {
                successCount = successCount + 1
            } else {
                failCount = failCount + 1
            }
        }

        if (failCount == 0) {
            Toast.makeText(requireContext(), getString(R.string.delete_zip_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.delete_zip_failed), Toast.LENGTH_SHORT).show()
        }

        // Thoát chế độ chọn và làm mới danh sách
        viewModel.exitSelectionMode()
        viewModel.refreshAllZips()
    }

    private fun handleExtractZip(zip: ZipInfo) {
        Toast.makeText(requireContext(), getString(R.string.extracting), Toast.LENGTH_SHORT).show()

        viewModel.extractZip(zip.filePath) { extractedFolder ->
            if (extractedFolder != null) {
                val successMessage = getString(R.string.extract_success, extractedFolder.name)
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.extract_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (adapter.listData.isEmpty()) {
            viewModel.loadData()
        }
    }
}