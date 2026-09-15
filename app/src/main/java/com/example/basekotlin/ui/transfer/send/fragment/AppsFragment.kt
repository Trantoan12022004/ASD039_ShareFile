package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentSendBinding
import com.example.basekotlin.ui.transfer.model.AppSubTab
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.AppsAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileGroupAdapter
import kotlinx.coroutines.launch

class AppsFragment : BaseFragment<FragmentSendBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private val adapter = AppsAdapter()


    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendBinding {
        return FragmentSendBinding.inflate(inflater!!, container, false)
    }

    override fun getData() {
        viewModel.loadApps()
    }

    override fun initView() {
        binding.rvSends.adapter = adapter

        // Bấm item: Chỉ gọi 1 lần duy nhất để ViewModel xử lý
        adapter.onClick = { item ->
            viewModel.toggleFileSelection(item)
        }
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Lắng nghe danh sách App theo sub-tab (Installed vs Not Installed)
                launch {
                    viewModel.displayAppsList.collect { apps ->
                        adapter.addListData(apps.toMutableList())
                        // Cập nhật số lượng app trong ngoặc: Apps (8)
                        binding.tvCount.text = apps.size.toString()
                        updateEmptyState(viewModel.isLoadingApps .value)
                    }
                }
                // 2. Lắng nghe trạng thái loading
                launch {
                    viewModel.isLoadingApps .collect { isLoading ->
                        handleLoading(isLoading)
                    }
                }
                // 3. Đồng bộ Checkbox: BaseApdaterSelected tự diff O(1) chỉ cập nhật riêng checkbox của app vừa click
                launch {
                    viewModel.selectedFiles.collect { files ->
                        val selectedIds = files.map { it.id }.toSet()
                        adapter.selectedKeys = selectedIds
                    }
                }
            }
        }
    }

    private fun handleLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressLoading.visible()
            binding.rvSends.gone()
            binding.allEmpty.gone()
        } else {
            binding.progressLoading.gone()
            updateEmptyState(false)
        }
    }

    private fun updateEmptyState(isLoading: Boolean) {
        if (isLoading) {
            binding.allEmpty.gone()
            binding.rvSends.gone()
            return
        }
        val isEmpty = adapter.listData.isEmpty()
        if (isEmpty) {
            binding.allEmpty.visible()
            binding.rvSends.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvSends.visible()
        }
    }
}
