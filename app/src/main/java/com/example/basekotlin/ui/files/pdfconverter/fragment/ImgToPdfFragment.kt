package com.example.basekotlin.ui.files.pdfconverter.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentImgToPdfBinding
import com.example.basekotlin.model.PhotoFolder
import com.example.basekotlin.model.PhotoInfo
import com.example.basekotlin.ui.files.pdfconverter.PdfConverterActivity
import com.example.basekotlin.ui.files.pdfconverter.PdfViewModel
import com.example.basekotlin.ui.files.photos.PhotosActivity
import com.example.basekotlin.ui.files.photos.PhotosViewModel
import com.example.basekotlin.ui.files.photos.adapter.PhotoAdapter
import com.example.basekotlin.ui.files.photos.adapter.PhotoFolderAdapter
import com.example.basekotlin.ui.files.photos.adapter.PhotoListItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue

class ImgToPdfFragment : BaseFragment<FragmentImgToPdfBinding>() {
    private val viewModel: PhotosViewModel by activityViewModels()
    private val pdfViewModel: PdfViewModel by activityViewModels()
    private val folderAdapter = PhotoFolderAdapter()
    private val photoAdapter = PhotoAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentImgToPdfBinding {
        return FragmentImgToPdfBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvFolder.adapter = folderAdapter

        // 1. Cấu hình GridLayoutManager 3 cột
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)

        // 2. Thiết lập Header chiếm full 3 cột, Photo item chiếm 1 cột
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = photoAdapter.getItemViewType(position)
                if (viewType == PhotoAdapter.TYPE_HEADER) {
                    return 3
                } else {
                    return 1
                }
            }
        }

        binding.rvPhotos.layoutManager = gridLayoutManager
        binding.rvPhotos.adapter = photoAdapter
    }

    override fun bindView() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAllPhotos()
        }

        folderAdapter.onClick = { folder ->
            openFolderDetail(folder)
        }

        photoAdapter.onSelectToggle = { photoInfo ->
            viewModel.togglePhotoSelection(photoInfo.filePath)
        }
        photoAdapter.onItemClick = { photoInfo ->
            val isSelecting = viewModel.isSelectionMode.value
            if (isSelecting) {
                viewModel.togglePhotoSelection(photoInfo.filePath)
            } else {
                viewModel.enterSelectionMode(photoInfo.filePath)
            }
        }

        binding.layoutConvert.tap {
            val selectedPhotos = viewModel.selectedPhotoPaths.value
            if (selectedPhotos.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val photoList = selectedPhotos.toList()
                pdfViewModel.convertSelectedImagesToPdf(photoList)
            }
        }

        // Lắng nghe các Flow từ ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Quan sát danh sách ảnh (sau khi tìm kiếm / tải lại)
                launch {
                    viewModel.foldersUi.collect { list ->
                        folderAdapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }

                launch {
                    viewModel.photosInCurrentFolderUi.collect { list ->
                        val groupedItems = groupPhotosByDate(list)
                        photoAdapter.submitList(groupedItems)
                        updateEmptyState()
                    }
                }

                // 4.3. Quan sát chế độ Selection Mode
                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        photoAdapter.isSelectionMode = isSelectionMode
                        photoAdapter.notifyDataSetChanged()
                    }
                }
                // 4.4. Quan sát danh sách ảnh đã chọn
                launch {
                    viewModel.selectedPhotoPaths.collect { selectedPaths ->
                        photoAdapter.selectedPhotos = selectedPaths
                        photoAdapter.notifyDataSetChanged()
                    }
                }
                // Quan sát danh sách ảnh đã chọn & cập nhật nút Convert
                launch {
                    viewModel  .selectedPhotoPaths.collect { selectedPaths ->
                        photoAdapter.selectedPhotos = selectedPaths
                        photoAdapter.notifyDataSetChanged()
                        val count = selectedPaths.size
                        binding.tvCount.text = count.toString()
                        if (count > 0) {
                            binding.layoutConvert.setBackgroundResource(R.drawable.bg_btn_create)
                        } else {
                            binding.layoutConvert.setBackgroundResource(R.drawable.bg_btn_create_1)
                        }
                    }
                }
                // Quan sát trạng thái Loading
                launch {
                    viewModel  .isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.progressLoading.visible()
                            binding.rvFolder.gone()
                            binding.rvPhotos.gone()
                            binding.allEmpty.gone()
                        } else {
                            binding.progressLoading.gone()
                            binding.swipeRefresh.isRefreshing = false
                            updateEmptyState()
                        }
                    }
                }
                // Quan sát trạng thái đang Convert của PdfViewModel
                launch {
                    pdfViewModel.isConverting.collect { isConverting ->
                        if (isConverting) {
                            binding.layoutConvert.isEnabled = false
                            binding.tvConvert.text = getString(R.string.converting_pdf)
                        } else {
                            binding.layoutConvert.isEnabled = true
                            binding.tvConvert.text = getString(R.string.convert)
                        }
                    }
                }
                // Quan sát kết quả Convert Ảnh sang PDF
                launch {
                    pdfViewModel.convertedPdfPath.collect { path ->
                        if (path != null) {
                            val message = getString(R.string.convert_pdf_success, 1)
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            viewModel  .clearPhotoSelection()
                            pdfViewModel.resetConvertedPdfPath()
                        }
                    }
                }
            }
        }


    }

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
            binding.rvFolder.gone()
            return
        }

        val isEmpty = folderAdapter.listData.isEmpty()
        if (isEmpty) {
            binding.allEmpty.visible()
            binding.rvFolder.gone()
        } else {
            binding.allEmpty.gone()
            binding.rvFolder.visible()
        }
    }

    private fun openFolderDetail(folder: PhotoFolder) {
        viewModel.setCurrentFolder(folder.folderPath)
        // 2. Gọi hàm điều hướng trên PhotosActivity
        val currentActivity = requireActivity()
        if (currentActivity is PdfConverterActivity) {
            currentActivity.openFolderPhotos(folder.folderName)
        }
        binding.rvFolder.gone()
        binding.rvPhotos.visible()
    }

    fun closeFolderDetail() {
        binding.rvPhotos.gone()
        binding.rvFolder.visible()
        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        if (folderAdapter.listData.isEmpty()) {
            viewModel.refreshAllPhotos()
        }
    }
}