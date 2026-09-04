package com.example.basekotlin.ui.files.pdfconverter.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.basekotlin.databinding.FragmentPdfToImgBinding
import com.example.basekotlin.ui.files.pdfconverter.PdfViewModel
import com.example.basekotlin.ui.files.pdfconverter.adapter.PdfAdapter
import kotlinx.coroutines.launch
import kotlin.getValue


class PdfToImgFragment : BaseFragment<FragmentPdfToImgBinding>() {
    private val viewModel: PdfViewModel by activityViewModels()
    private val adapter = PdfAdapter()
    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentPdfToImgBinding {
        return FragmentPdfToImgBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvPdfs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPdfs.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
                viewModel.refreshAllDocuments()
        }
    }

    override fun bindView() {
        super.bindView()
        adapter.onItemClick = { pdf ->
                viewModel.togglePdfSelection(pdf.filePath)
        }

        // Xử lý bấm nút Convert PDF -> Image
        binding.layoutConvert.tap {
            val selectedPaths = viewModel.selectedPdfPaths.value
            if (selectedPaths.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.convertSelectedPdfs()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoadingAll.collect { isLoading ->
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
                    viewModel.pdfDocuments.collect { pdfList ->
                        adapter.addListData(pdfList.toMutableList())
                        updateEmptyState()
                    }
                }
                launch {
                    viewModel.selectedPdfPaths.collect { selectedPaths ->
                        adapter.selectedDocs = selectedPaths
                        adapter.notifyDataSetChanged()
                        val count = selectedPaths.size
                        binding.tvCount.text = count.toString()
                        if (count > 0) {
                            binding.layoutConvert.setBackgroundResource(R.drawable.bg_btn_create)
                        } else {
                            binding.layoutConvert.setBackgroundResource(R.drawable.bg_btn_create_1)
                        }
                    }
                }

                // Quan sát trạng thái Convert
                launch {
                    viewModel.isConverting.collect { isConverting ->
                        if (isConverting) {
                            binding.layoutConvert.isEnabled = false
                            binding.tvConvert.text = getString(R.string.converting_pdf)
                        } else {
                            binding.layoutConvert.isEnabled = true
                            binding.tvConvert.text = getString(R.string.convert)
                        }
                    }
                }

                // Quan sát kết quả Convert PDF
                launch {
                    viewModel.convertedImageCount.collect { count ->
                        if (count != null) {
                            if (count > 0) {
                                val message = getString(R.string.convert_pdf_success, count)
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.convert_pdf_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            viewModel.resetConvertResult()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState() {
        val isLoading = viewModel.isLoadingAll.value
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
        if (adapter.listData.isEmpty()) {
            viewModel.loadAllDocumentsIfNeeded()
        }
    }
}
