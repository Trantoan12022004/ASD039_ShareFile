package com.example.basekotlin.ui.files.video.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentAllVideoBinding
import com.example.basekotlin.ui.files.video.VideoDetailActivity
import com.example.basekotlin.ui.files.video.VideosViewModel
import com.example.basekotlin.ui.files.video.adapter.VideoAdapter
import com.example.basekotlin.ui.files.video.model.VideoInfo
import kotlinx.coroutines.launch

class FolderVideoDetailFragment : BaseFragment<FragmentAllVideoBinding>() {

    private val viewModel: VideosViewModel by activityViewModels()
    private val adapter = VideoAdapter()
    private var folderName: String = ""
    private var folderPath: String = ""

    companion object {
        private const val ARG_FOLDER_NAME = "ARG_FOLDER_NAME"
        private const val ARG_FOLDER_PATH = "ARG_FOLDER_PATH"

        fun newInstance(folderName: String, folderPath: String = ""): FolderVideoDetailFragment {
            val args = Bundle().apply {
                putString(ARG_FOLDER_NAME, folderName)
                putString(ARG_FOLDER_PATH, folderPath)
            }
            return FolderVideoDetailFragment().apply {
                arguments = args
            }
        }
    }

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllVideoBinding {
        return FragmentAllVideoBinding.inflate(inflater!!, container, false)
    }

    override fun getData() {
        super.getData()
        folderName = arguments?.getString(ARG_FOLDER_NAME) ?: ""
        folderPath = arguments?.getString(ARG_FOLDER_PATH) ?: ""
        if (folderPath.isNotEmpty()) {
            viewModel.setCurrentFolder(folderPath)
        }
    }

    override fun initView() {
        // Cấu hình Grid 3 cột hiển thị video
        binding.rvVideos.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvVideos.adapter = adapter

        // Kéo xuống để tải lại dữ liệu
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAllVideos()
        }
    }

    override fun bindView() {
        // 1. Mở xem video chi tiết
        adapter.onItemClick = { videoInfo ->
            openVideoDetail(videoInfo)
        }

        // 2. Nhấn giữ để kích hoạt chế độ chọn video
        adapter.onItemLongClick = { video ->
            viewModel.enterSelectionMode(video.filePath)
        }

        // 3. Chọn hoặc bỏ chọn video
        adapter.onSelectToggle = { video ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleVideoSelection(video.filePath)
            } else {
                viewModel.enterSelectionMode(video.filePath)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Quan sát danh sách video trong folder hiện tại
                launch {
                    viewModel.videosInCurrentFolderUi.collect { list ->
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

                // 3. Quan sát danh sách đường dẫn video đang được chọn
                launch {
                    viewModel.selectedVideoPaths.collect { selectedPaths ->
                        adapter.selectedVideos = selectedPaths
                        adapter.notifyDataSetChanged()
                    }
                }

                // 4. Quan sát trạng thái tải dữ liệu
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.progressLoading.visible()
                            binding.rvVideos.gone()
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
            binding.rvVideos.gone()
            return
        }
        if (adapter.listData.isEmpty()) {
            binding.allEmpty.visible()
            binding.rvVideos.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvVideos.visible()
        }
    }

    @OptIn(UnstableApi::class)
    private fun openVideoDetail(videoInfo: VideoInfo) {
        val currentFolderList = viewModel.videosInCurrentFolderUi.value
        val allVideosList = viewModel.allVideosUi.value
        var targetPosition = allVideosList.indexOfFirst { it.filePath == videoInfo.filePath }
        if (targetPosition == -1) {
            targetPosition = 0
        }

        // Phát trực tiếp vào PiP nếu đang hoạt động
        if (VideoDetailActivity.playInPipIfActive(targetPosition, currentFolderList)) {
            return
        }

        val bundle = Bundle().apply {
            putInt("EXTRA_CURRENT_POSITION", targetPosition)
        }
        startNextActivity(VideoDetailActivity::class.java, bundle)
    }

    override fun onResume() {
        super.onResume()
        if (adapter.listData.isEmpty()) {
            viewModel.refreshAllVideos()
        }
    }
}