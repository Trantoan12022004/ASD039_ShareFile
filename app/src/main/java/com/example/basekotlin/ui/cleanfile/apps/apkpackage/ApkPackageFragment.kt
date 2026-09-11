package com.example.basekotlin.ui.cleanfile.apps.apkpackage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentApkPackageBinding
import com.example.basekotlin.databinding.PopupApkPackageBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import com.example.basekotlin.ui.cleanfile.apps.AppsCleanupViewModel
import com.example.basekotlin.ui.cleanfile.apps.apkpackage.adapter.ApkPackageAdapter
import com.example.basekotlin.ui.cleanfile.apps.dialog.DetailInformationApkDialog
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch
import java.io.File

class ApkPackageFragment : BaseFragment<FragmentApkPackageBinding>() {

    private val viewModel: AppsCleanupViewModel by activityViewModels()
    private val adapter by lazy { ApkPackageAdapter() }

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentApkPackageBinding {
        return FragmentApkPackageBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvApkPackages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApkPackages.adapter = adapter

        adapter.onSelectToggle = {
            updateSelectionUi()
        }

        adapter.onMoreClick = { appInfo, anchorView ->
            PopupMenuUtils.showAnchoredMenu(
                anchor = anchorView,
                inflateBinding = PopupApkPackageBinding::inflate,
                widthRatio = 0.5f
            ) { popupBinding, popupWindow ->
                popupBinding.tvSend.tap {
                    popupWindow.dismiss()
                    shareApkFile(appInfo)
                }

                popupBinding.tvShare.tap {
                    popupWindow.dismiss()
                    shareApkFile(appInfo)
                }

                popupBinding.tvDelete.tap {
                    popupWindow.dismiss()
                    ConfirmActionDialog(
                        context = requireContext(),
                        title = getString(R.string.delete_apk_title),
                        message = getString(R.string.delete_apk_desc, appInfo.appName),
                        positiveText = getString(R.string.delete)
                    ) {
                        viewModel.deleteApks(listOf(appInfo.apkFilePath)) { success ->
                            if (success) {
                                Toast.makeText(requireContext(), getString(R.string.delete_files_success), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.show()
                }

                popupBinding.tvInfo.tap {
                    popupWindow.dismiss()
                    DetailInformationApkDialog(requireContext(), appInfo).show()
                }
            }
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
                    context = requireContext(),
                    title = getString(R.string.delete_selected_files),
                    message = getString(R.string.delete_selected_files_desc, selectedCount),
                    positiveText = getString(R.string.delete)
                ) {
                    val pathsToDelete = adapter.selectedPaths.toList()
                    viewModel.deleteApks(pathsToDelete) { success ->
                        adapter.selectedPaths.clear()
                        updateSelectionUi()
                        val msg = if (success) getString(R.string.delete_files_success) else getString(R.string.delete_files_failed)
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }.show()
            }
        }
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apkFiles.collect { apks ->
                    adapter.addListData(apks.toMutableList())
                    adapter.selectedPaths.clear()
                    updateSelectionUi()

                    val totalBytes = apks.sumOf { it.sizeBytes }
                    val totalSizeStr = CleanFileUtils.formatFileSize(totalBytes)
                    binding.tvTotalSize.text = totalSizeStr

                    if (apks.isEmpty()) {
                        binding.layoutEmpty.visible()
                    } else {
                        binding.layoutEmpty.gone()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingApk.collect { loading ->
                    if (loading) {
                        binding.progressBar.visible()
                        binding.layoutEmpty.gone()
                    } else {
                        binding.progressBar.gone()
                    }
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

    private fun shareApkFile(appInfo: AppInfo) {
        try {
            val file = File(appInfo.apkFilePath)
            if (!file.exists()) {
                Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApkFiles()
    }
}
