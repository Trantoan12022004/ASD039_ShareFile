package com.example.basekotlin.ui.download.safebox

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import com.example.basekotlin.databinding.FragmentDownloadSafeBoxBinding
import com.example.basekotlin.databinding.PopupMoreSafeboxBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.ui.download.DownloadCenterViewmodel
import com.example.basekotlin.ui.safebox.adapter.SafeBoxFileAdapter
import com.example.basekotlin.ui.safebox.lock.PatternLockView
import com.example.basekotlin.ui.safebox.lock.SafeBoxLockActivity
import com.example.basekotlin.util.PopupMenuUtils
import kotlinx.coroutines.launch

class DownloadSafeBoxFragment : BaseFragment<FragmentDownloadSafeBoxBinding>() {

    private val viewModel: DownloadSafeBoxViewModel by viewModels()
    private val downloadCenterViewModel: DownloadCenterViewmodel by activityViewModels()
    private val adapter = SafeBoxFileAdapter()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentDownloadSafeBoxBinding {
        return FragmentDownloadSafeBoxBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        binding.rvSafeFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSafeFiles.adapter = adapter

        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {
            override fun onPatternCompleted(pattern: List<Int>) {
                viewModel.onPatternCompleted(pattern)
            }
        })
    }

    override fun bindView() {
        // Nút bấm tạo mật khẩu khi chưa có pass
        binding.btnCreatePassword.tap {
            val intent = Intent(requireContext(), SafeBoxLockActivity::class.java).apply {
                putExtra(SafeBoxLockActivity.EXTRA_FINISH_ON_SETUP, true)
            }
            startActivity(intent)
        }

        binding.btnRedraw.tap {
            viewModel.onRedrawClicked()
        }

        adapter.onMoreClick = { file, anchorView ->
            showMorePopup(file, anchorView)
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPasswordState()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.lockState.collect { state ->
                        when (state) {
                            DownloadSafeBoxLockState.NOT_SET -> {
                                binding.layoutNotSet.visible()
                                binding.layoutLocked.gone()
                                binding.layoutContent.gone()
                            }
                            DownloadSafeBoxLockState.LOCKED -> {
                                binding.layoutNotSet.gone()
                                binding.layoutLocked.visible()
                                binding.layoutContent.gone()
                            }
                            DownloadSafeBoxLockState.UNLOCKED -> {
                                binding.layoutNotSet.gone()
                                binding.layoutLocked.gone()
                                binding.layoutContent.visible()
                            }
                        }
                    }
                }

                launch {
                    viewModel.lockUiState.collect { uiState ->
                        binding.tvLockDesc.setText(uiState.hintResId)
                        if (uiState.isError) {
                            binding.tvLockDesc.setTextColor("#EF4444".toColorInt())
                        } else {
                            binding.tvLockDesc.setTextColor(Color.BLACK)
                        }
                        binding.patternLockView.setPatternState(uiState.patternViewState)
                        binding.patternLockView.setInputEnabled(uiState.isInputEnabled)
                        if (uiState.showRedrawButton) binding.btnRedraw.visible() else binding.btnRedraw.gone()
                    }
                }

                launch {
                    viewModel.clearPatternEvent.collect {
                        binding.patternLockView.clearPattern()
                    }
                }

                launch {
                    viewModel.downloadFiles.collect { files ->
                        adapter.addListData(files.toMutableList())
                        if (files.isEmpty()) {
                            binding.layoutEmpty.visible()
                            binding.rvSafeFiles.gone()
                        } else {
                            binding.layoutEmpty.gone()
                            binding.rvSafeFiles.visible()
                        }
                    }
                }
            }
        }
    }

    private fun showMorePopup(file: SafeBoxFile, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor = anchor,
            inflateBinding = { PopupMoreSafeboxBinding.inflate(it) },
            widthRatio = 0.55f
        ) { popupBinding, popupWindow ->
            popupBinding.tvRestore.tap {
                popupWindow.dismiss()
                confirmRestore(file)
            }
            popupBinding.tvDeletePermanently.tap {
                popupWindow.dismiss()
                confirmDelete(file)
            }
        }
    }

    private fun confirmRestore(file: SafeBoxFile) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.restore_file_safebox_title),
            message = getString(R.string.restore_file_safebox_desc, file.fileName),
            positiveText = getString(R.string.restore)
        ) {
            viewModel.restoreFile(file) { success ->
                val msg = if (success) R.string.restore_success else R.string.restore_failed
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                if (success) {
                    downloadCenterViewModel.loadDownloads()
                }
            }
        }.show()
    }

    private fun confirmDelete(file: SafeBoxFile) {
        ConfirmActionDialog(
            context = requireContext(),
            title = getString(R.string.delete_file_safebox_title),
            message = getString(R.string.delete_file_safebox_desc, file.fileName),
            positiveText = getString(R.string.delete)
        ) {
            viewModel.deletePermanently(file) { success ->
                val msg = if (success) R.string.delete_permanently_success else R.string.delete_permanently_failed
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }.show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = DownloadSafeBoxFragment()
    }
}
