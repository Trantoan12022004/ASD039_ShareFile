package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentSendBinding
import com.example.basekotlin.ui.transfer.model.AppSubTab
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.AppsAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileGroupAdapter
import kotlinx.coroutines.launch

class VideosFragment : BaseFragment<FragmentSendBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private val adapter = FileGroupAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendBinding {
        return FragmentSendBinding.inflate(inflater!!, container, false)
    }

    override fun getData() {
        viewModel.loadVideos()
    }

    override fun initView() {
        binding.rvSends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSends.adapter = adapter
        binding.layoutHeader.gone()
        // Bấm item: Chỉ gọi 1 lần duy nhất để ViewModel xử lý
        adapter.onItemClick  = { item ->
            viewModel.toggleFileSelection(item)
        }
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Lắng nghe danh sách Video theo nhóm
                launch {
                    viewModel.displayVideoGroups.collect { videos ->
                        adapter.addListData(videos.toMutableList())
                        // Cập nhật số lượng video trong ngoặc: Videos (8)

                        updateEmptyState(viewModel.isLoadingVideos.value)
                    }
                }
                // 2. Lắng nghe trạng thái loading
                launch {
                    viewModel.isLoadingVideos .collect { isLoading ->
                        handleLoading(isLoading)
                    }
                }
                // 3. Đồng bộ Checkbox: BaseApdaterSelected tự diff O(1) chỉ cập nhật riêng checkbox của video vừa click
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
