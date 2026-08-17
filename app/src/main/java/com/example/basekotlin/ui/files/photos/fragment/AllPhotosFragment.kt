package com.example.basekotlin.ui.files.photos.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentAllPhotosBinding
import com.example.basekotlin.model.PhotoInfo
import com.example.basekotlin.ui.files.photos.PhotosViewModel
import com.example.basekotlin.ui.files.photos.adapter.PhotoAdapter
import com.example.basekotlin.ui.files.photos.adapter.PhotoListItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AllPhotosFragment : BaseFragment<FragmentAllPhotosBinding>() {

    private val viewModel: PhotosViewModel by activityViewModels()
    private val adapter = PhotoAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllPhotosBinding {
        return FragmentAllPhotosBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        // 1. Cấu hình GridLayoutManager 3 cột
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)

        // 2. Thiết lập Header chiếm full 3 cột, Photo item chiếm 1 cột
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = adapter.getItemViewType(position)
                if (viewType == PhotoAdapter.TYPE_HEADER) {
                    return 3
                } else {
                    return 1
                }
            }
        }

        binding.rvPhotos.layoutManager = gridLayoutManager
        binding.rvPhotos.adapter = adapter

        // 3. Xử lý sự kiện kéo làm mới danh sách
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAllPhotos()
        }
    }

    override fun bindView() {
        // Callback mở xem chi tiết ảnh
        adapter.onItemClick = { photoInfo ->
            openPhoto(photoInfo)
        }

        // Callback nhấn giữ item để chuyển sang select mode
        adapter.onItemLongClick = { photoInfo ->
            viewModel.enterSelectionMode(photoInfo.filePath)
        }
        // Callback toggle chọn ảnh
        adapter.onSelectToggle = { photoInfo ->
            if (viewModel.isSelectionMode.value) {
                viewModel.togglePhotoSelection(photoInfo.filePath)
            } else {
                viewModel.enterSelectionMode(photoInfo.filePath)
            }
        }

        // Lắng nghe các Flow từ ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Quan sát danh sách ảnh (sau khi tìm kiếm / tải lại)
                launch {
                    viewModel.allPhotosUi.collect { photoList ->
                        val groupedItems = groupPhotosByDate(photoList)
                        adapter.submitList(groupedItems)
                        updateEmptyState()
                    }
                }

                // 2. Quan sát chế độ Selection Mode
                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        adapter.isSelectionMode = isSelectionMode
                        adapter.notifyDataSetChanged()
                    }
                }

                // 3. Quan sát danh sách ảnh đã chọn
                launch {
                    viewModel.selectedPhotoPaths.collect { selectedPaths ->
                        adapter.selectedPhotos = selectedPaths
                        adapter.notifyDataSetChanged()
                    }
                }

                // 4. Quan sát trạng thái Loading
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.progressLoading.visible()
                            binding.rvPhotos.gone()
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
        if (hasManageStoragePermission()) {
            if (adapter.listData.isEmpty()) {
                viewModel.refreshAllPhotos()
            }
        } else {
            requestManageStoragePermission()
        }
    }

    // Hàm gom nhóm danh sách ảnh theo từng ngày
    private fun groupPhotosByDate(photos: List<PhotoInfo>): List<PhotoListItem> {
        val result = mutableListOf<PhotoListItem>()
        if (photos.isEmpty()) {
            return result
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        val groupedMap = LinkedHashMap<String, MutableList<PhotoInfo>>()

        // Phân loại ảnh theo chuỗi ngày
        for (photo in photos) {
            val timeMillis = if (photo.dateAddedSeconds > 0L) {
                photo.dateAddedSeconds * 1000L
            } else {
                photo.dateModifiedSeconds * 1000L
            }
            val dateKey = dateFormat.format(Date(timeMillis))
            var groupList = groupedMap[dateKey]
            if (groupList == null) {
                groupList = mutableListOf()
                groupedMap[dateKey] = groupList
            }
            groupList.add(photo)
        }

        // Tạo danh sách phẳng gồm Header và các Photo
        for ((dateKey, photoGroup) in groupedMap) {
            val headerItem = PhotoListItem.Header(
                dateString = dateKey,
                count = photoGroup.size
            )
            result.add(headerItem)

            for (photo in photoGroup) {
                val photoItem = PhotoListItem.Photo(photo)
                result.add(photoItem)
            }
        }

        return result
    }

    // Cập nhật trạng thái rỗng / hiển thị RecyclerView
    private fun updateEmptyState() {
        val isLoading = viewModel.isLoading.value
        if (isLoading) {
            binding.allEmpty.gone()
            binding.rvPhotos.gone()
            return
        }

        val isEmpty = adapter.listData.isEmpty()
        if (isEmpty) {
            binding.allEmpty.visible()
            binding.rvPhotos.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvPhotos.visible()
        }
    }

    // Mở xem ảnh qua Intent hệ thống
    private fun openPhoto(photo: PhotoInfo) {
        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = if (photo.mimeType.isNotEmpty()) {
            photo.mimeType
        } else {
            "image/*"
        }
        intent.setDataAndType(photo.contentUri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No application found to view this image", Toast.LENGTH_SHORT).show()
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
}
