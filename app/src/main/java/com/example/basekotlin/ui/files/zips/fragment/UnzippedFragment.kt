package com.example.basekotlin.ui.files.zips.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentZipsBinding
import com.example.basekotlin.model.UnzippedItem
import com.example.basekotlin.ui.files.zips.UnzippedActivity
import com.example.basekotlin.ui.files.zips.ZipsViewModel
import com.example.basekotlin.ui.files.zips.adapter.UnzippedAdapter
import kotlinx.coroutines.launch
import java.io.File

class UnzippedFragment : BaseFragment<FragmentZipsBinding>() {

    private val viewModel: ZipsViewModel by activityViewModels()
    private val adapter = UnzippedAdapter()
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentZipsBinding {
        return FragmentZipsBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvZips.adapter = adapter

        // Thiết lập text cho trạng thái rỗng
        binding.tvEmptyTitle.text = getString(R.string.unzipped_empty_title)
        binding.tvEmptyMessage.text = getString(R.string.unzipped_empty_desc)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCurrentFolder()
        }

        setupBackPressHandling()
    }

    override fun bindView() {
        adapter.onItemClick = { item ->
            if (item.isDirectory) {
                // 1. Nếu là Folder: mở folder đó bằng UnzippedActivity
                val bundle = Bundle()
                bundle.putString("EXTRA_FOLDER_PATH", item.path)
                bundle.putString("EXTRA_FOLDER_NAME", item.name)
                // 3. Chuyển sang màn hình PhotoDetailActivity
                startNextActivity(UnzippedActivity::class.java, bundle)
            } else {
                // 2. Nếu là File: mở file đó bằng app tương ứng
                openExtractedFile(File(item.path), item.extension)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát trạng thái tải dữ liệu
                launch {
                    viewModel.isLoading.collect { isLoading ->
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

                // Quan sát danh sách item
                launch {
                    viewModel.unzippedItems.collect { list ->
                        adapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }
            }
        }
    }

    // Xử lý bắt sự kiện nút Back khi đang ở trong folder con
    private fun setupBackPressHandling() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handled = viewModel.navigateUp()
                if (!handled) {
                    // Đã ở thư mục gốc, nhường quyền back cho Activity
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    // Mở file bằng Intent thông qua FileProvider
    private fun openExtractedFile(file: File, extension: String) {
        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )

        val mimeType = getMimeType(extension)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Thử lại với MIME type tổng quát
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(genericIntent)
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMimeType(extension: String): String {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        if (mime != null) {
            return mime
        }
        return "*/*"
    }

    private fun updateEmptyState() {
        val isLoading = viewModel.isLoading.value
        if (isLoading) {
            binding.allEmpty.gone()
            binding.layoutContent.gone()
            return
        }
        if (adapter.listData.isEmpty()) {
            binding.allEmpty.visible()
            binding.layoutContent.gone()
        } else {
            binding.allEmpty.gone()
            binding.layoutContent.visible()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.currentFolder.value == null) {
            viewModel.loadFolder()
        }
    }
}
