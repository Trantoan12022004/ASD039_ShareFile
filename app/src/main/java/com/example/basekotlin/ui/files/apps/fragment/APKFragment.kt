package com.example.basekotlin.ui.files.apps.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
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
import com.example.basekotlin.databinding.FragmentAppsBinding
import com.example.basekotlin.databinding.PopupMoreAppBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.InformationAppDialog
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.files.apps.AppsViewModel
import com.example.basekotlin.ui.files.apps.adapter.ApkCardAdapter
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch
import java.io.File

class APKFragment : BaseFragment<FragmentAppsBinding>() {

    private val viewModel: AppsViewModel by activityViewModels()
    private val notInstalledAdapter = ApkCardAdapter()
    private val deletableAdapter = ApkCardAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAppsBinding {
        return FragmentAppsBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvNotInstalledApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotInstalledApps.adapter = notInstalledAdapter
        binding.rvDeletableApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeletableApps.adapter = deletableAdapter
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshApkFiles()
        }
    }

    override fun bindView() {

        notInstalledAdapter.onSelectToggle = { appInfo ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleApkSelection(appInfo.apkFilePath)
            } else {
                viewModel.enterApkSelectionMode(appInfo.apkFilePath)
            }
        }

        deletableAdapter.onSelectToggle = { appInfo ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleApkSelection(appInfo.apkFilePath)
            } else {
                viewModel.enterApkSelectionMode(appInfo.apkFilePath)
            }
        }

        notInstalledAdapter.onInstallClick = { appInfo ->
            requestInstallApk(appInfo)
        }

        deletableAdapter.onMoreClick = { appInfo, anchor ->
            showMoreMenu(appInfo, anchor)
        }

        notInstalledAdapter.onMoreClick = { appInfo, anchor ->
            showMoreMenu(appInfo, anchor)
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notInstalledApkFiles.collect { apkList ->
                        notInstalledAdapter.addListData(apkList.toMutableList())
                        if (apkList.isEmpty()) {
                            binding.viewTop.gone()
                        } else {
                            binding.viewTop.visible()
                        }
                        binding.tvCountNotInstalled.text = apkList.size.toString()
                        binding.tvSizeNotInstalled.text = Formatter.formatShortFileSize(
                            requireContext(),
                            sumApkSize(apkList)
                        )
                        updateEmptyState()
                    }
                }

                launch {
                    viewModel.deletableApkFiles.collect { apkList ->
                        deletableAdapter.addListData(apkList.toMutableList())
                        if (apkList.isEmpty()) {
                            binding.viewTop1.gone()
                        } else {
                            binding.viewTop1.visible()
                        }
                        binding.tvCountDeletable.text = apkList.size.toString()
                        binding.tvSizeDeletable.text = Formatter.formatShortFileSize(
                            requireContext(),
                            sumApkSize(apkList)
                        )
                        updateEmptyState()
                    }
                }
                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        notInstalledAdapter.isSelectionMode = isSelectionMode
                        deletableAdapter.isSelectionMode = isSelectionMode
                        notInstalledAdapter.notifyDataSetChanged()
                        deletableAdapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.selectedApkFilePaths.collect { selectedPaths ->
                        notInstalledAdapter.selectedApks = selectedPaths
                        deletableAdapter.selectedApks = selectedPaths
                        notInstalledAdapter.notifyDataSetChanged()
                        deletableAdapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.isLoadingApk.collect { isLoading ->
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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Kiểm tra quyền mỗi khi quay lại tab, vì user có thể vừa cấp quyền ở màn Settings
        if (hasManageStoragePermission()) {
            if (deletableAdapter.listData.isEmpty() && notInstalledAdapter.listData.isEmpty()) {
                viewModel.loadApkFilesIfNeeded()
            }
        } else {
            requestManageStoragePermission()
        }
    }

    private fun updateEmptyState() {

        val isLoading = viewModel.isLoadingApk.value
        if (isLoading) {
            binding.allEmpty.gone()
            binding.layoutContent.gone()
            return
        }
        val notInstalledEmpty = notInstalledAdapter.listData.isEmpty()
        val deletableEmpty = deletableAdapter.listData.isEmpty()
        if (notInstalledEmpty && deletableEmpty) {
            binding.allEmpty.visible()
            binding.layoutContent.gone()
        } else {
            binding.allEmpty.gone()
            binding.layoutContent.visible()
        }
    }

    private fun hasManageStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        return true
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:" + requireContext().packageName)
            startActivity(intent)
        }
    }

    // Mở màn cài đặt hệ thống cho file .apk thông qua FileProvider (bắt buộc từ Android 7+)
    private fun requestInstallApk(appInfo: AppInfo) {
        val apkFile = File(appInfo.apkFilePath)
        if (apkFile.exists() == false) {
            Toast.makeText(requireContext(), getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val apkUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW)
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(installIntent)
    }

    private fun showMoreMenu(appInfo: AppInfo, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { inflater -> PopupMoreAppBinding.inflate(inflater) }
        ) { popupBinding, popupWindow ->
            popupBinding.tvShare.tap {
                popupWindow.dismiss()
                shareApk(appInfo)
            }

            popupBinding.tvInfo.tap {
                popupWindow.dismiss()
                showInformationAppDialog(appInfo)
            }

            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeleteConfirmDialog(appInfo)
            }
        }
    }


    private fun showInformationAppDialog(appInfo: AppInfo) {
        InformationAppDialog(requireContext(), appInfo).show()
    }
    private fun shareApk(appInfo: AppInfo) {
        val apkFile = File(appInfo.apkFilePath)
        val apkUri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/vnd.android.package-archive"
        intent.putExtra(Intent.EXTRA_STREAM, apkUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }


    private fun showDeleteConfirmDialog(appInfo: AppInfo) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete_apk),
            message = getString(R.string.delete_apk_desc, appInfo.appName),
            positiveText = getString(R.string.delete)
        ) {
            performDelete(appInfo)
        }.show()
    }

    private fun performDelete(appInfo: AppInfo) {
        val apkFile = File(appInfo.apkFilePath)
        val deleted = apkFile.delete()
        if (deleted) {
            Toast.makeText(requireContext(), getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
            viewModel.refreshApkFiles()
        } else {
            Toast.makeText(requireContext(), getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sumApkSize(apkList: List<AppInfo>): Long {
        var total = 0L
        for (apk in apkList) {
            total = total + apk.sizeBytes
        }
        return total
    }
}