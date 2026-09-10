package com.example.basekotlin.ui.download

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.basekotlin.databinding.ActivityDownloadCenterBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.util.reduceDragSensitivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File

class DownloadCenterActivity : BaseActivity<ActivityDownloadCenterBinding>(ActivityDownloadCenterBinding::inflate) {

    private lateinit var pagerAdapter: DownloadCenterPagerAdapter
    private val viewModel: DownloadCenterViewmodel by viewModels()

    override fun initView() {
        binding.layoutToolbar.tvTitle.text = getString(R.string.title_download)
        // Ẩn nút Search trên Toolbar vì màn này không dùng tính năng tìm kiếm
        binding.layoutToolbar.btnSearch.gone()

        pagerAdapter = DownloadCenterPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.reduceDragSensitivity(multiplier = 4)

        val tabTitles = arrayOf(
            getString(R.string.safe_box),
            getString(R.string.all),
            getString(R.string.videos),
            getString(R.string.photos),
            getString(R.string.music),
            getString(R.string.apps),
        )

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Ẩn nút select ban đầu nếu đang ở tab Safe Box (tab 0)
        if (binding.viewPager.currentItem == 0) {
            binding.layoutToolbar.btnSelect.gone()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    binding.layoutToolbar.btnSelect.gone()
                    if (viewModel.isSelectionMode.value) {
                        viewModel.exitSelectionMode()
                    }
                } else {
                    if (!viewModel.isSelectionMode.value) {
                        binding.layoutToolbar.btnSelect.visible()
                    }
                }
            }
        })

        setupBackHandler()
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            handleBackAction()
        }

        binding.layoutToolbar.btnSelect.tap {
            if (binding.viewPager.currentItem != 0) {
                if (viewModel.isSelectionMode.value) {
                    viewModel.exitSelectionMode()
                } else {
                    viewModel.enterSelectionMode()
                }
            }
        }

        setupSelectionActions()
        observeSelectMode()
    }

    private fun setupSelectionActions() {
        val actions = binding.layoutSelectionApps

        actions.btnDeselectAll.tap {
            if (viewModel.selectedPaths.value.isNotEmpty()) {
                viewModel.clearSelection()
            } else {
                val currentTabFiles = getCurrentTabFiles()
                viewModel.selectAll(currentTabFiles.map { it.path })
            }
        }

        actions.btnDelete.tap {
            val selectedFiles = getSelectedFiles()
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                val message = if (selectedFiles.size == 1) {
                    getString(R.string.delete_file_desc, selectedFiles[0].name)
                } else {
                    getString(R.string.delete_songs_desc, selectedFiles.size)
                }
                ConfirmActionDialog(
                    context = this,
                    title = getString(R.string.delete_file),
                    message = message,
                    positiveText = getString(R.string.delete),
                    onConfirm = {
                        viewModel.deleteSelectedFiles { success ->
                            val msg = if (success) R.string.delete_song_success else R.string.delete_song_failed
                            Toast.makeText(this, getString(msg), Toast.LENGTH_SHORT).show()
                        }
                    }
                ).show()
            }
        }

        actions.btnSend.tap {
            val selectedFiles = getSelectedFiles()
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                shareSelectedFiles(selectedFiles)
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
                            binding.layoutToolbar.btnSelect.gone()
                        } else {
                            binding.layoutToolbar.tvTitle.visible()
                            binding.layoutToolbar.tvTitle1.gone()
                            binding.layoutSelectionApps.root.gone()
                            if (binding.viewPager.currentItem != 0) {
                                binding.layoutToolbar.btnSelect.visible()
                            }
                        }
                    }
                }

                launch {
                    viewModel.selectedPaths.collect { selectedPaths ->
                        val count = selectedPaths.size
                        binding.layoutToolbar.tvCountSong.text = " $count "
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

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
    }

    private fun handleBackAction() {
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        } else {
            finish()
        }
    }

    private fun getCurrentTabFiles() = when (binding.viewPager.currentItem) {
        1 -> viewModel.allDownloads.value
        2 -> viewModel.videoDownloads.value
        3 -> viewModel.photoDownloads.value
        4 -> viewModel.musicDownloads.value
        5 -> viewModel.appDownloads.value
        else -> emptyList()
    }

    private fun getSelectedFiles() = viewModel.allDownloads.value.filter {
        viewModel.selectedPaths.value.contains(it.path)
    }

    private fun shareSelectedFiles(files: List<com.example.basekotlin.ui.download.model.DownloadItem>) {
        val uris = ArrayList<Uri>()
        for (item in files) {
            val file = File(item.path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                uris.add(uri)
            }
        }
        if (uris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
}
