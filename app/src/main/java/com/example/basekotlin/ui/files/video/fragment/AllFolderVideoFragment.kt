package com.example.basekotlin.ui.files.video.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentAllFolderVideoBinding
import com.example.basekotlin.ui.files.video.VideosActivity
import com.example.basekotlin.ui.files.video.VideosViewModel
import com.example.basekotlin.ui.files.video.adapter.VideoFolderAdapter
import com.example.basekotlin.ui.files.video.model.VideoFolder
import kotlinx.coroutines.launch

class AllFolderVideoFragment : BaseFragment<FragmentAllFolderVideoBinding>() {

    private val viewModel: VideosViewModel by activityViewModels()
    private val adapter = VideoFolderAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllFolderVideoBinding {
        return FragmentAllFolderVideoBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        // Cấu hình Grid 2 cột hiển thị các thư mục video
        binding.rvFolder.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvFolder.adapter = adapter

        // Hàm kiểm tra folder có được chọn đầy đủ file hay chưa
        adapter.isFolderSelectedChecker = { folderPath ->
            viewModel.isFolderFullySelected(folderPath)
        }
    }

    override fun bindView() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAllVideos()
        }

        // 1. Click mở xem chi tiết các video trong folder
        adapter.onClick = { folder ->
            openFolderDetail(folder)
        }

        // 2. Nhấn giữ để kích hoạt chế độ chọn toàn bộ video trong folder
        adapter.onLongClick = { folder ->
            viewModel.enterFolderSelectionMode(folder.folderPath)
        }

        // 3. Toggle chọn / bỏ chọn thư mục
        adapter.onSelectToggle = { folder ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleFolderSelection(folder.folderPath)
            } else {
                viewModel.enterFolderSelectionMode(folder.folderPath)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Quan sát danh sách các folder
                launch {
                    viewModel.foldersUi.collect { list ->
                        adapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }

                // 2. Quan sát trạng thái Selection Mode
                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        adapter.isSelectionMode = isSelectionMode
                        adapter.notifyDataSetChanged()
                    }
                }

                // 3. Quan sát các đường dẫn video đã chọn để cập nhật checkbox folder
                launch {
                    viewModel.selectedVideoPaths.collect {
                        adapter.notifyDataSetChanged()
                    }
                }

                // 4. Quan sát trạng thái tải dữ liệu
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.progressLoading.visible()
                            binding.rvFolder.gone()
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

    private fun updateEmptyState() {
        if (viewModel.isLoading.value) {
            binding.allEmpty.gone()
            binding.rvFolder.gone()
            return
        }

        if (adapter.listData.isEmpty()) {
            binding.allEmpty.visible()
            binding.rvFolder.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvFolder.visible()
        }
    }

    // Điều hướng mở chi tiết folder trên VideosActivity
    private fun openFolderDetail(folder: VideoFolder) {
        viewModel.setCurrentFolder(folder.folderPath)
        val currentActivity = requireActivity()
        if (currentActivity is VideosActivity) {
            currentActivity.openFolderVideos(folder.folderName, folder.folderPath)
        }
    }
}
