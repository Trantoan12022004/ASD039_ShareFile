package com.example.basekotlin.ui.files.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityDocumentsBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.util.Utils
import com.example.basekotlin.util.reduceDragSensitivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

class DocumentsActivity : BaseActivity<ActivityDocumentsBinding>(ActivityDocumentsBinding::inflate) {

    private lateinit var pagerAdapter: DocumentsPagerAdapter
    private val viewModel: DocumentsViewModel by viewModels()
    private var isSearchMode = false

    override fun initView() {
        binding.layoutToolbar.tvTitle.text = getString(R.string.documents)
        pagerAdapter = DocumentsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.reduceDragSensitivity(multiplier = 4)

        val tabTitles = arrayOf(
            getString(R.string.recent),
            getString(R.string.all),
            getString(R.string.pdf),
            getString(R.string.excel),
            getString(R.string.ppt),
            getString(R.string.txt),
            getString(R.string.doc),
            getString(R.string.wps),
            getString(R.string.other),
        )

        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager,
        ) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        val initialTab = intent.getIntExtra(EXTRA_TAB_INDEX, 0)
        if (initialTab in tabTitles.indices) {
            binding.viewPager.setCurrentItem(initialTab, false)
        }
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        binding.layoutToolbar.btnSelect.tap {
            viewModel.enterSelectionMode()
        }
        binding.layoutToolbar.btnSearch.tap {
            openSearch()
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupSelectionActions()
        observeSelectMode()
    }

    private fun setupSelectionActions() {
        val actions = binding.layoutSelectionApps

        actions.btnDeselectAll.tap {
            if (viewModel.selectedDocumentPaths.value.isNotEmpty()) {
                viewModel.clearDocumentSelection()
            } else {
                val currentDocs = getCurrentTabDocuments()
                if (currentDocs.isNotEmpty()) {
                    viewModel.selectAllDocuments(currentDocs.map { it.filePath })
                } else {
                    val allDocs = viewModel.allDocumentsSearch.value
                    viewModel.selectAllDocuments(allDocs.map { it.filePath })
                }
            }
        }

        actions.btnDelete.tap {
            val selectedDocs = getSelectedDocuments()
            if (selectedDocs.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showDeleteSelectedDocumentsDialog(selectedDocs)
            }
        }

        actions.btnSend.tap {
            val selectedDocs = getSelectedDocuments()
            if (selectedDocs.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                shareSelectedDocuments(selectedDocs)
            }
        }
    }

    private fun observeSelectMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSelectionMode.collect { isSelecting ->
                        if (isSelecting) {
                            binding.layoutToolbar.tvTitle.gone()
                            binding.layoutToolbar.tvTitle1.visible()
                            binding.layoutSelectionApps.root.visible()
                        } else {
                            binding.layoutToolbar.tvTitle.visible()
                            binding.layoutToolbar.tvTitle1.gone()
                            binding.layoutSelectionApps.root.gone()
                        }
                    }
                }
                launch {
                    viewModel.selectedDocumentPaths.collect { selectedPaths ->
                        updateSelectionCount()
                        if (selectedPaths.isNotEmpty()) {
                            binding.layoutSelectionApps.tvDeselectAll.text = getString(R.string.deselect_all)
                        } else {
                            binding.layoutSelectionApps.tvDeselectAll.text = getString(R.string.select_all)
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentTabDocuments(): List<DocumentInfo> {
        return when (binding.viewPager.currentItem) {
            0 -> viewModel.recentDocumentsSearch.value
            1 -> viewModel.allDocumentsSearch.value
            2 -> viewModel.pdfDocumentsSearch.value
            3 -> viewModel.excelDocumentsSearch.value
            4 -> viewModel.pptDocumentsSearch.value
            5 -> viewModel.txtDocumentsSearch.value
            6 -> viewModel.docDocumentsSearch.value
            7 -> viewModel.wpsDocumentsSearch.value
            else -> viewModel.otherDocumentsSearch.value
        }
    }

    private fun showDeleteSelectedDocumentsDialog(docList: List<DocumentInfo>) {
        val message: String = if (docList.size == 1) {
            getString(R.string.delete_song_desc, docList[0].fileName)
        } else {
            getString(R.string.delete_songs_desc, docList.size)
        }

        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDeleteSelectedDocuments(docList)
        }.show()
    }

    private fun performDeleteSelectedDocuments(docList: List<DocumentInfo>) {
        var successCount = 0
        var failCount = 0

        for (doc in docList) {
            val file = File(doc.filePath)
            val deleted = file.delete()
            if (deleted) {
                successCount++
            } else {
                failCount++
            }
        }

        if (failCount == 0) {
            Toast.makeText(this, getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
        }

        viewModel.exitSelectionMode()
        viewModel.refreshAllDocuments()
        viewModel.loadRecentDocuments()
    }

    private fun shareSelectedDocuments(docList: List<DocumentInfo>) {
        val uris = ArrayList<Uri>()
        for (doc in docList) {
            val file = File(doc.filePath)
            if (!file.exists()) {
                continue
            }
            val uri = FileProvider.getUriForFile(
                this,
                packageName + ".provider",
                file
            )
            uris.add(uri)
        }

        if (uris.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_select_at_least_one_item),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent: Intent
        if (uris.size == 1) {
            intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "*/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.send)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.send), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedDocuments(): List<DocumentInfo> {
        val selectedPaths = viewModel.selectedDocumentPaths.value
        val result = mutableListOf<DocumentInfo>()

        val allDocs = viewModel.allDocuments.value
        val addedPaths = mutableSetOf<String>()
        for (doc in allDocs) {
            if (selectedPaths.contains(doc.filePath)) {
                result.add(doc)
                addedPaths.add(doc.filePath)
            }
        }

        for (path in selectedPaths) {
            if (!addedPaths.contains(path)) {
                val file = File(path)
                if (file.exists()) {
                    result.add(
                        DocumentInfo(
                            fileName = file.name,
                            filePath = file.absolutePath,
                            sizeBytes = file.length(),
                            dateModifiedMillis = file.lastModified(),
                            extension = file.extension.lowercase(),
                            documentType = com.example.basekotlin.model.DocumentType.OTHER
                        )
                    )
                }
            }
        }

        return result
    }

    private fun updateSelectionCount() {
        val count = viewModel.selectedDocumentPaths.value.size
        binding.layoutToolbar.tvCountSong.text = count.toString()
    }

    private fun openSearch() {
        if (isSearchMode) {
            return
        }
        isSearchMode = true

        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        }

        binding.tabLayout.gone()
        binding.layoutSearch.visible()
        binding.edtSearch.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        if (!isSearchMode) {
            return
        }
        isSearchMode = false

        viewModel.updateSearchQuery("")
        binding.edtSearch.setText("")
        Utils.hideKeyboard(this)

        binding.layoutSearch.gone()
        binding.tabLayout.visible()
    }

    override fun onBack() {
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        } else if (isSearchMode) {
            closeSearch()
        } else {
            finish()
        }
    }

    companion object {
        const val EXTRA_TAB_INDEX = "TAB_INDEX"
    }
}