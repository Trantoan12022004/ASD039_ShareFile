package com.example.basekotlin.ui.files.photos.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentAllFolderPhotoBinding
import com.example.basekotlin.model.PhotoFolder
import com.example.basekotlin.ui.files.photos.PhotosActivity
import com.example.basekotlin.ui.files.photos.PhotosViewModel
import com.example.basekotlin.ui.files.photos.adapter.PhotoAdapter
import com.example.basekotlin.ui.files.photos.adapter.PhotoFolderAdapter
import kotlinx.coroutines.launch
import kotlin.getValue


class AllFolderPhotoFragment : BaseFragment<FragmentAllFolderPhotoBinding>() {
    private val viewModel: PhotosViewModel by activityViewModels()
    private val adapter = PhotoFolderAdapter()
    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllFolderPhotoBinding {
        return FragmentAllFolderPhotoBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvFolder.adapter = adapter
        // 2. Gán hàm kiểm tra trạng thái chọn folder cho adapter
        adapter.isFolderSelectedChecker = { folderPath ->
            viewModel.isFolderFullySelected(folderPath)
        }
    }

    override fun bindView() {
        super.bindView()
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAllPhotos()
        }

        // 1. Callback click mở xem folder
        adapter.onClick = { folder ->
            openFolderDetail(folder)
        }

        // 2. Callback nhấn giữ folder để vào chế độ chọn (chọn toàn bộ ảnh trong folder)
        adapter.onLongClick = { folder ->
            viewModel.enterFolderSelectionMode(folder.folderPath)
        }

        // 3. Callback toggle chọn / bỏ chọn folder
        adapter.onSelectToggle = { folder ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleFolderSelection(folder.folderPath)
            } else {
                viewModel.enterFolderSelectionMode(folder.folderPath)
            }
        }


        // Lắng nghe các Flow từ ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Quan sát danh sách ảnh (sau khi tìm kiếm / tải lại)
                launch {
                    viewModel.foldersUi.collect { list ->
                        adapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }

//                 2. Quan sát chế độ Selection Mode
                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        adapter.isSelectionMode = isSelectionMode
                        adapter.notifyDataSetChanged()
                    }
                }

//                 3. Quan sát danh sách ảnh đã chọn
                launch {
                    viewModel.selectedPhotoPaths.collect { selectedPaths ->
                        adapter.selectedPhotoPaths = selectedPaths
                        adapter.notifyDataSetChanged()
                    }
                }

                // 4. Quan sát trạng thái Loading
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

    // Cập nhật trạng thái rỗng / hiển thị RecyclerView
    private fun updateEmptyState() {
        val isLoading = viewModel.isLoading.value
        if (isLoading) {
            binding.allEmpty.gone()
            binding.rvFolder.gone()
            return
        }

        val isEmpty = adapter.listData.isEmpty()
        if (isEmpty) {
            binding.allEmpty.visible()
            binding.rvFolder.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvFolder.visible()
        }
    }

    // Mở màn hình chi tiết danh sách ảnh trong folder
    private fun openFolderDetail(folder: PhotoFolder) {
        viewModel.setCurrentFolder(folder.folderPath)
        // 2. Gọi hàm điều hướng trên PhotosActivity
        val currentActivity = requireActivity()
        if (currentActivity is PhotosActivity) {
            currentActivity.openFolderPhotos(folder.folderName)
        }
    }
}