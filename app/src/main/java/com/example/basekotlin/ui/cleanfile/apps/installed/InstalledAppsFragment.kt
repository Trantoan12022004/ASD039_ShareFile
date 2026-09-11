package com.example.basekotlin.ui.cleanfile.apps.installed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentInstalledAppsBinding
import com.example.basekotlin.databinding.PopupInstalledAppBinding
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import com.example.basekotlin.ui.cleanfile.apps.AppsCleanupViewModel
import com.example.basekotlin.ui.cleanfile.apps.dialog.UninstallAppDialog
import com.example.basekotlin.ui.cleanfile.apps.installed.adapter.InstalledAppsAdapter
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class InstalledAppsFragment : BaseFragment<FragmentInstalledAppsBinding>() {

    private val viewModel: AppsCleanupViewModel by activityViewModels()
    private val adapter by lazy { InstalledAppsAdapter() }

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentInstalledAppsBinding {
        return FragmentInstalledAppsBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvInstalledApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInstalledApps.adapter = adapter

        adapter.onMoreClick = { appInfo, anchorView ->
            PopupMenuUtils.showAnchoredMenu(
                anchor = anchorView,
                inflateBinding = PopupInstalledAppBinding::inflate,
                widthRatio = 0.45f
            ) { popupBinding, popupWindow ->
                popupBinding.tvOpen.tap {
                    popupWindow.dismiss()
                    val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(requireContext(), "Cannot open app", Toast.LENGTH_SHORT).show()
                    }
                }

                popupBinding.tvUninstall.tap {
                    popupWindow.dismiss()
                    UninstallAppDialog(requireContext(), appInfo) {
                        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                            data = Uri.parse("package:${appInfo.packageName}")
                            putExtra(Intent.EXTRA_RETURN_RESULT, true)
                        }
                        startActivity(intent)
                    }.show()
                }
            }
        }
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.installedApps.collect { apps ->
                    adapter.addListData(apps.toMutableList())

                    val totalSize = apps.sumOf { it.sizeBytes }
                    binding.tvTotalSize.text = CleanFileUtils.formatFileSize(totalSize)
                        if (apps.isEmpty()) {
                            binding.layoutEmpty.visible()
                        } else {
                            binding.layoutEmpty.gone()
                        }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingInstalled.collect { loading ->
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

    override fun onResume() {
        super.onResume()
        viewModel.loadInstalledApps()
    }
}
