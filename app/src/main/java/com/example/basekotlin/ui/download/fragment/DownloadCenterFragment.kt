package com.example.basekotlin.ui.download.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.databinding.FragmentDownloadCenterBinding
import com.example.basekotlin.databinding.PopupMoreDocBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.ui.download.DownloadCenterViewmodel
import com.example.basekotlin.ui.download.adpater.DownloadAdapter
import com.example.basekotlin.ui.download.dialog.InformationDownloadDialog
import com.example.basekotlin.ui.download.model.DownloadItem
import com.example.basekotlin.ui.download.model.DownloadType
import com.example.basekotlin.ui.storage.dialog.RenameDialog
import com.example.basekotlin.util.PopupMenuUtils
import com.example.basekotlin.util.SafeBoxHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadCenterFragment : BaseFragment<FragmentDownloadCenterBinding>() {

    private val viewModel: DownloadCenterViewmodel by activityViewModels()
    private val adapter = DownloadAdapter()
    private var tabPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabPosition = arguments?.getInt(ARG_TAB_POSITION, 0) ?: 0
    }

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentDownloadCenterBinding {
        return FragmentDownloadCenterBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvDownloads.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDownloads.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDownloads()
        }
    }

    override fun bindView() {
        adapter.onItemClick = { item ->
            openFile(item)
        }

        adapter.onSelectToggle = { item ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleItemSelection(item.path)
            } else {
                viewModel.enterSelectionMode(item.path)
            }
        }

        adapter.onMoreClick = { item, anchor ->
            showMoreMenu(item, anchor)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát dữ liệu của tab hiện tại
                launch {
                    val targetFlow = getTargetFlow()
                    targetFlow.collect { list ->
                        adapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }

                // Quan sát trạng thái loading
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

                // Quan sát chế độ chọn nhiều
                launch {
                    viewModel.isSelectionMode.collect { isSelecting ->
                        adapter.isSelectionMode = isSelecting
                        adapter.notifyDataSetChanged()
                    }
                }

                // Quan sát danh sách các file được chọn
                launch {
                    viewModel.selectedPaths.collect { selectedPaths ->
                        adapter.selectedPaths = selectedPaths
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (adapter.listData.isEmpty()) {
            viewModel.loadDownloads()
        }
    }

    private fun getTargetFlow(): StateFlow<List<DownloadItem>> {
        return when (tabPosition) {
            1 -> viewModel.allDownloads
            2 -> viewModel.videoDownloads
            3 -> viewModel.photoDownloads
            4 -> viewModel.musicDownloads
            5 -> viewModel.appDownloads
            else -> viewModel.allDownloads
        }
    }

    private fun updateEmptyState() {
        if (viewModel.isLoading.value) {
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

    private fun openFile(item: DownloadItem) {
        val file = File(item.path)
        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (item.mimeType.isNotEmpty()) item.mimeType else "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.no_app_to_open_file), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMoreMenu(item: DownloadItem, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { inflater -> PopupMoreDocBinding.inflate(inflater) }
        ) { popupBinding, popupWindow ->
            popupBinding.tvSelect.tap {
                popupWindow.dismiss()
                viewModel.enterSelectionMode(item.path)
            }

            popupBinding.tvShare.tap {
                popupWindow.dismiss()
                shareFile(item)
            }

            popupBinding.tvSend.tap {
                popupWindow.dismiss()
                shareFile(item)
            }

            popupBinding.tvMoveToSafebox.tap {
                popupWindow.dismiss()
                moveToSafeBox(item)
            }

            popupBinding.tvRename.tap {
                popupWindow.dismiss()
                showRenameDialog(item)
            }

            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                ConfirmActionDialog(
                    context = requireContext(),
                    title = getString(R.string.delete_file),
                    message = getString(R.string.delete_file_desc, item.name),
                    positiveText = getString(R.string.delete),
                    onConfirm = {
                        deleteDownloadItem(item)
                    }
                ).show()
            }

            popupBinding.tvInfo.tap {
                popupWindow.dismiss()
                showInfoDialog(item)
            }
        }
    }

    private fun showRenameDialog(item: DownloadItem) {
        val currentFile = File(item.path)
        val baseName = currentFile.nameWithoutExtension
        val extension = currentFile.extension

        RenameDialog(
            context = requireContext(),
            initText = baseName,
            validate = { enteredName ->
                val newFileName = if (extension.isNotEmpty()) "$enteredName.$extension" else enteredName
                val targetFile = File(currentFile.parentFile, newFileName)
                if (targetFile.exists() && targetFile.absolutePath != currentFile.absolutePath) {
                    getString(R.string.text_input_failed1)
                } else {
                    null
                }
            },
            onConfirm = { newBaseName ->
                viewModel.renameFile(item, newBaseName) { success ->
                    val msg = if (success) R.string.rename_song_success else R.string.rename_song_failed
                    Toast.makeText(requireContext(), getString(msg), Toast.LENGTH_SHORT).show()
                }
            }
        ).show()
    }

    private fun showInfoDialog(item: DownloadItem) {
        InformationDownloadDialog(requireContext(), item).show()
    }

    private fun moveToSafeBox(item: DownloadItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fileType = when (item.type) {
                DownloadType.VIDEOS -> SafeBoxFileType.VIDEOS
                DownloadType.PHOTOS -> SafeBoxFileType.PICTURES
                DownloadType.MUSIC -> SafeBoxFileType.AUDIO
                DownloadType.APPS -> SafeBoxFileType.OTHERS
                DownloadType.ALL -> {
                    val ext = item.extension.lowercase()
                    when {
                        ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") -> SafeBoxFileType.PICTURES
                        ext in listOf("mp4", "mkv", "avi", "3gp", "mov", "flv") -> SafeBoxFileType.VIDEOS
                        ext in listOf("mp3", "wav", "m4a", "aac", "flac", "ogg") -> SafeBoxFileType.AUDIO
                        ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> SafeBoxFileType.DOCUMENTS
                        else -> SafeBoxFileType.OTHERS
                    }
                }
            }
            val success = SafeBoxHelper.moveToSafeBox(requireContext(), item.path, fileType)
            if (success) {
                Toast.makeText(requireContext(), getString(R.string.safe_box_move_success), Toast.LENGTH_SHORT).show()
                viewModel.loadDownloads()
            } else {
                Toast.makeText(requireContext(), getString(R.string.safe_box_move_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDownloadItem(item: DownloadItem) {
        viewModel.deleteFile(item) { success ->
            val msg = if (success) R.string.delete_song_success else R.string.delete_song_failed
            Toast.makeText(requireContext(), getString(msg), Toast.LENGTH_SHORT).show()
        }
    }
    private fun shareFile(item: DownloadItem) {
        val file = File(item.path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.mimeType.isNotEmpty()) item.mimeType else "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    companion object {
        private const val ARG_TAB_POSITION = "arg_tab_position"

        @JvmStatic
        fun newInstance(position: Int) = DownloadCenterFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TAB_POSITION, position)
            }
        }
    }
}
