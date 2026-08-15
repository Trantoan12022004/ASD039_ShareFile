package com.example.basekotlin.ui.files.documents.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import com.example.basekotlin.databinding.FragmentAllDocumentsBinding
import com.example.basekotlin.databinding.PopupMoreDocBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.InformationDocumentDialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.ui.files.documents.DocumentsViewModel
import com.example.basekotlin.ui.files.documents.adapter.DocCardAdapter
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class AllDocumentsFragment : BaseFragment<FragmentAllDocumentsBinding>() {

    private val viewModel: DocumentsViewModel by activityViewModels()
    private val adapter = DocCardAdapter()
    private var tabPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabPosition = arguments?.getInt(ARG_TAB_POSITION, 0) ?: 0
    }

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentAllDocumentsBinding {
        return FragmentAllDocumentsBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            if (tabPosition == 0) {
                viewModel.loadRecentDocuments()
            } else {
                viewModel.refreshAllDocuments()
            }
        }
    }

    override fun bindView() {
        adapter.onItemClick = { doc ->
            openDocumentFile(doc)
        }

        adapter.onSelectToggle = { doc ->
            if (viewModel.isSelectionMode.value) {
                viewModel.toggleDocumentSelection(doc.filePath)
            } else {
                viewModel.enterSelectionMode(doc.filePath)
            }
        }

        adapter.onMoreClick = { doc, anchor ->
            showMoreMenu(doc, anchor)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val targetFlow = getTargetDocumentFlow()
                    targetFlow.collect { docList ->
                        adapter.addListData(docList.toMutableList())
                        updateEmptyState()
                    }
                }

                launch {
                    val loadingFlow = if (tabPosition == 0) viewModel.isLoadingRecent else viewModel.isLoadingAll
                    loadingFlow.collect { isLoading ->
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

                launch {
                    viewModel.isSelectionMode.collect { isSelectionMode ->
                        adapter.isSelectionMode = isSelectionMode
                        adapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.selectedDocumentPaths.collect { selectedPaths ->
                        adapter.selectedDocs = selectedPaths
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasManageStoragePermission()) {
            if (adapter.listData.isEmpty()) {
                if (tabPosition == 0) {
                    viewModel.loadRecentDocuments()
                } else {
                    viewModel.loadAllDocumentsIfNeeded()
                }
            }
        } else {
            requestManageStoragePermission()
        }
    }

    private fun getTargetDocumentFlow(): StateFlow<List<DocumentInfo>> {
        return when (tabPosition) {
            0 -> viewModel.recentDocumentsSearch
            1 -> viewModel.allDocumentsSearch
            2 -> viewModel.pdfDocumentsSearch
            3 -> viewModel.excelDocumentsSearch
            4 -> viewModel.pptDocumentsSearch
            5 -> viewModel.txtDocumentsSearch
            6 -> viewModel.docDocumentsSearch
            7 -> viewModel.wpsDocumentsSearch
            else -> viewModel.otherDocumentsSearch
        }
    }

    private fun updateEmptyState() {
        val isLoading = if (tabPosition == 0) viewModel.isLoadingRecent.value else viewModel.isLoadingAll.value
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

    private fun openDocumentFile(doc: DocumentInfo) {
        val file = File(doc.filePath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.markDocumentOpened(doc.filePath)

        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )

        val mimeType = getMimeType(doc.extension)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(genericIntent)
            } catch (e2: Exception) {
                Toast.makeText(requireContext(), "No application found to open this file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMoreMenu(doc: DocumentInfo, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { inflater -> PopupMoreDocBinding.inflate(inflater) }
        ) { popupBinding, popupWindow ->
            popupBinding.tvSelect.tap {
                popupWindow.dismiss()
                viewModel.enterSelectionMode(doc.filePath)
            }

            popupBinding.tvShare.tap {
                popupWindow.dismiss()
                shareDocument(doc)
            }

            popupBinding.tvSend.tap {
                popupWindow.dismiss()
                shareDocument(doc)
            }

            popupBinding.tvMoveToSafebox.tap {
                popupWindow.dismiss()
                Toast.makeText(requireContext(), getString(R.string.move_to_safebox), Toast.LENGTH_SHORT).show()
            }

            popupBinding.tvRename.tap {
                popupWindow.dismiss()
                showRenameDialog(doc)
            }

            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeleteConfirmDialog(doc)
            }

            popupBinding.tvInfo.tap {
                popupWindow.dismiss()
                showInformationDocDialog(doc)
            }
        }
    }

    private fun shareDocument(doc: DocumentInfo) {
        val file = File(doc.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(doc.extension)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.share), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameDialog(doc: DocumentInfo) {
        val currentFile = File(doc.filePath)
        val baseName = currentFile.nameWithoutExtension
        val extension = currentFile.extension

        TextInputDialog(
            context = requireContext(),
            title = getString(R.string.rename),
            hint = getString(R.string.rename),
            initialText = baseName,
            positiveText = getString(R.string.rename),
            validate = { enteredName ->
                val newFileName = if (extension.isNotEmpty()) "$enteredName.$extension" else enteredName
                val targetFile = File(currentFile.parentFile, newFileName)
                if (targetFile.exists() && targetFile.absolutePath != currentFile.absolutePath) {
                    getString(R.string.text_input_failed1)
                } else {
                    null
                }
            }
        ) { newBaseName ->
            performRename(doc, newBaseName)
        }.show()
    }

    private fun performRename(doc: DocumentInfo, newBaseName: String) {
        val currentFile = File(doc.filePath)
        val extension = currentFile.extension
        val newFileName = if (extension.isNotEmpty()) "$newBaseName.$extension" else newBaseName
        val targetFile = File(currentFile.parentFile, newFileName)

        if (currentFile.renameTo(targetFile)) {
            Toast.makeText(requireContext(), getString(R.string.rename_song_success), Toast.LENGTH_SHORT).show()
            if (tabPosition == 0) {
                viewModel.loadRecentDocuments()
            } else {
                viewModel.refreshAllDocuments()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmDialog(doc: DocumentInfo) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete),
            message = getString(R.string.delete_song_desc, doc.fileName),
            positiveText = getString(R.string.delete)
        ) {
            performDelete(doc)
        }.show()
    }

    private fun performDelete(doc: DocumentInfo) {
        val file = File(doc.filePath)
        val deleted = file.delete()
        if (deleted) {
            Toast.makeText(requireContext(), getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
            if (tabPosition == 0) {
                viewModel.loadRecentDocuments()
            } else {
                viewModel.refreshAllDocuments()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInformationDocDialog(doc: DocumentInfo) {
        InformationDocumentDialog(requireContext(), doc).show()
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xlsm" -> "application/vnd.ms-excel.sheet.macroEnabled.12"
            "csv" -> "text/comma-separated-values"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "pps" -> "application/vnd.ms-powerpoint"
            "ppsx" -> "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
            "txt", "log" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "wps" -> "application/vnd.ms-works"
            "wpt" -> "application/vnd.ms-works"
            "html", "htm" -> "text/html"
            "xml" -> "text/xml"
            "json" -> "application/json"
            "epub" -> "application/epub+zip"
            "rtf" -> "application/rtf"
            else -> "*/*"
        }
    }

    companion object {
        private const val ARG_TAB_POSITION = "arg_tab_position"

        fun newInstance(tabPosition: Int): AllDocumentsFragment {
            val fragment = AllDocumentsFragment()
            val bundle = Bundle().apply {
                putInt(ARG_TAB_POSITION, tabPosition)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}