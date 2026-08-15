package com.example.basekotlin.ui.files.apps.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.basekotlin.databinding.FragmentApps1Binding
import com.example.basekotlin.databinding.PopupMoreAppBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.InformationAppDialog
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.files.apps.AppsViewModel
import com.example.basekotlin.ui.files.apps.adapter.InstalledCardAdapter
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

class InstalledFragment : BaseFragment<FragmentApps1Binding>() {
    private val viewModel: AppsViewModel by activityViewModels()
    private val adapter = InstalledCardAdapter()

    private var pendingUninstallPackage: String? = null
    // Khai báo ở field, không khai báo trong hàm
    private val uninstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val packageName = pendingUninstallPackage
        pendingUninstallPackage = null
        if (packageName == null) {
            return@registerForActivityResult
        }
        val stillInstalled = isPackageInstalled(packageName)
        if (stillInstalled == false) {
            viewModel.refreshInstalledApps()
        }
    }
    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentApps1Binding {
        return FragmentApps1Binding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshInstalledApps()
        }
    }

    override fun bindView() {
        adapter.onUninstallClick = { appInfo ->
            requestUninstallApk(appInfo)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.installedAppsSearch.collect { apkList ->
                        adapter.addListData(apkList.toMutableList())
                        updateEmptyState()
                    }
                }
                launch {
                    viewModel.isLoadingInstalled.collect { isLoading ->
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
            if (adapter.listData.isEmpty()) {
                Log.d("APP_LOAD", "InstalledFragment calling refreshAll()")

                viewModel.loadInstalledAppsIfNeeded()
            }
        } else {
            requestManageStoragePermission()
        }
    }
    private fun updateEmptyState() {
        // Đang loading thì không tính là empty, tránh nhấp nháy "không có app" trước khi data về
        val isLoading = viewModel.isLoadingInstalled.value
        if (isLoading) {
            binding.allEmpty.gone()
            binding.layoutContent.gone()
            return
        }

        val notInstalledEmpty = adapter.listData.isEmpty()
        if (notInstalledEmpty) {
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
    private fun requestUninstallApk(appInfo: AppInfo) {
        if (appInfo.isSystemApp) {
            Toast.makeText(
                requireContext(),
                getString(R.string.cannot_uninstall_system_app),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        pendingUninstallPackage = appInfo.packageName

        val packageUri = Uri.parse("package:" + appInfo.packageName)
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        uninstallLauncher.launch(uninstallIntent)
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        try {
            requireContext().packageManager.getPackageInfo(packageName, 0)
            return true
        } catch (error: PackageManager.NameNotFoundException) {
            return false
        }
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
            viewModel.refreshInstalledApps()
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