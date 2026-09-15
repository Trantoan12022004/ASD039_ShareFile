package com.example.basekotlin.ui.transfer.send

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivitySendBinding
import com.example.basekotlin.ui.transfer.model.*
import com.example.basekotlin.ui.transfer.scanner.QrScannerActivity
import com.example.basekotlin.ui.transfer.send.adapter.FileGroupAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileItem1Adapter
import com.example.basekotlin.util.reduceDragSensitivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class SendFileActivity : BaseActivity<ActivitySendBinding>(ActivitySendBinding::inflate) {

    private val viewModel: SendFilesViewModel by viewModels()
    private val adapter = FileItem1Adapter()
    private var isBottomListExpanded = false


    // 7 Tabs theo đúng thiết kế
    private val tabTitles by lazy {
        arrayOf(
            getString(R.string.send_tab_recent),
            getString(R.string.send_tab_contacts),
            getString(R.string.send_tab_file),
            getString(R.string.send_tab_video),
            getString(R.string.send_tab_apps),
            getString(R.string.send_tab_music),
            getString(R.string.send_tab_photo)
        )
    }

    override fun initView() {
        super.initView()
        setupViewPager()
        setupChips()
        binding.rvSends.adapter = adapter
        binding.rvSends.gone() // Mặc định ban đầu ẩn danh sách preview
        adapter.onClick  = { item ->
            viewModel.removeSelectedFile(item)
        }
        binding.btnClear.tap {
            viewModel.clearSelection()
        }
        binding.btnUpDown.tap {
            toggleExpandedList()
        }
        binding.btnSend.tap {
            handleSendFiles()
        }

    }
    private fun toggleExpandedList() {
        isBottomListExpanded = !isBottomListExpanded
        if (isBottomListExpanded) {
            binding.rvSends.visible()
            binding.btnUpDown.animate().rotation(180f).setDuration(200).start()
        } else {
            binding.rvSends.gone()
            binding.btnUpDown.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = SendTabPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.reduceDragSensitivity(5)

        TabLayoutMediator(binding.layoutToolbar.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Lắng nghe đổi Tab để cập nhật Sub-tab tương ứng
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateSubTabUI(position)
            }
        })
    }

    override fun bindView() {
        super.bindView()
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        observeSelectedFiles()
    }

    /**
     * Cập nhật tiêu đề và hiển thị của 2 Chip khi chuyển Tab chính
     */
    private fun updateSubTabUI(position: Int) {
        when (position) {
            SendTabPagerAdapter.TAB_RECENT -> {
                binding.layoutToolbar.layoutChip.visible()
                binding.layoutToolbar.btnChip1.text = getString(R.string.send)
                binding.layoutToolbar.btnChip2.text = getString(R.string.received)
                updateChipSelection(viewModel.recentSubTab.value == RecentSubTab.SEND)
            }
            SendTabPagerAdapter.TAB_VIDEO -> {
                binding.layoutToolbar.layoutChip.visible()
                binding.layoutToolbar.btnChip1.text = getString(R.string.send_tab_recent)
                binding.layoutToolbar.btnChip2.text = getString(R.string.folders)
                updateChipSelection(viewModel.videoSubTab.value == VideoSubTab.RECENT)
            }
            SendTabPagerAdapter.TAB_APPS -> {
                binding.layoutToolbar.layoutChip.visible()
                binding.layoutToolbar.btnChip1.text = getString(R.string.installed)
                binding.layoutToolbar.btnChip2.text = getString(R.string.not_installed)
                updateChipSelection(viewModel.appsSubTab.value == AppsSubTab.INSTALLED)
            }
            SendTabPagerAdapter.TAB_PHOTO -> {
                binding.layoutToolbar.layoutChip.visible()
                binding.layoutToolbar.btnChip1.text = getString(R.string.send_tab_recent)
                binding.layoutToolbar.btnChip2.text = getString(R.string.folders)
                updateChipSelection(viewModel.photoSubTab.value == PhotoSubTab.RECENT)
            }
            else -> {
                // Các Tab Contacts, Files, Music không có Sub-tab
                binding.layoutToolbar.layoutChip.gone()
            }
        }
    }

    /**
     * Xử lý sự kiện bấm 2 Chip
     */
    private fun setupChips() {
        // Bấm Chip 1 (Bên trái)
        binding.layoutToolbar.btnChip1.tap {
            when (binding.viewPager.currentItem) {
                SendTabPagerAdapter.TAB_RECENT -> {
                    viewModel.recentSubTab.value = RecentSubTab.SEND
                    updateChipSelection(true)
                }
                SendTabPagerAdapter.TAB_VIDEO -> {
                    viewModel.videoSubTab.value = VideoSubTab.RECENT
                    updateChipSelection(true)
                }
                SendTabPagerAdapter.TAB_APPS -> {
                    viewModel.appsSubTab.value = AppsSubTab.INSTALLED
                    updateChipSelection(true)
                }
                SendTabPagerAdapter.TAB_PHOTO -> {
                    viewModel.photoSubTab.value = PhotoSubTab.RECENT
                    updateChipSelection(true)
                }
            }
        }

        // Bấm Chip 2 (Bên phải)
        binding.layoutToolbar.btnChip2.tap {
            when (binding.viewPager.currentItem) {
                SendTabPagerAdapter.TAB_RECENT -> {
                    viewModel.recentSubTab.value = RecentSubTab.RECEIVED
                    updateChipSelection(false)
                }
                SendTabPagerAdapter.TAB_VIDEO -> {
                    viewModel.videoSubTab.value = VideoSubTab.FOLDERS
                    updateChipSelection(false)
                }
                SendTabPagerAdapter.TAB_APPS -> {
                    viewModel.appsSubTab.value = AppsSubTab.NOT_INSTALLED
                    updateChipSelection(false)
                }
                SendTabPagerAdapter.TAB_PHOTO -> {
                    viewModel.photoSubTab.value = PhotoSubTab.FOLDERS
                    updateChipSelection(false)
                }
            }
        }
    }

    /**
     * Đổi màu Chip: Nút được chọn màu xanh chữ trắng, nút chưa chọn màu xám nhạt chữ đen
     */
    private fun updateChipSelection(isChip1Selected: Boolean) {
        if (isChip1Selected) {
            binding.layoutToolbar.btnChip1.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.layoutToolbar.btnChip1.setTextColor(getColor(R.color.white))
            binding.layoutToolbar.btnChip2.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.layoutToolbar.btnChip2.setTextColor(getColor(R.color.black))
        } else {
            binding.layoutToolbar.btnChip2.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.layoutToolbar.btnChip2.setTextColor(getColor(R.color.white))
            binding.layoutToolbar.btnChip1.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.layoutToolbar.btnChip1.setTextColor(getColor(R.color.black))
        }
    }

    private fun observeSelectedFiles() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedFiles.collect { files ->
                        updateBottomBar(files.size)
                        adapter.addListData(files.toMutableList())
                    }
                }
                launch {
                    viewModel.totalSizeBytes.collect { size ->
                        binding.tvSize.text = size
                    }
                }

            }
        }
    }

    private fun updateBottomBar(selectedCount: Int) {
        if (selectedCount > 0) {
            // Có file được chọn
            binding.layoutSizeFile.visible()
            binding.tvCount1.text = selectedCount.toString()
            binding.tvSelectedCount.text = getString(R.string.send_files_selected, selectedCount)

            // Bật nút Send
            binding.btnSend.setBackgroundResource(R.drawable.bg_btn_create)
            binding.btnSend.setTextColor(getColor(R.color.white))
            binding.btnSend.isEnabled = true
        } else {
            // Không có file nào
            binding.layoutSizeFile.gone()
            binding.rvSends.gone()
            isBottomListExpanded = false
            binding.btnUpDown.rotation = 0f

            binding.tvSelectedCount.text = getString(R.string.send_files_selected, 0)

            // Tắt nút Send
            binding.btnSend.setBackgroundResource(R.drawable.bg_btn_create_1)
            binding.btnSend.setTextColor(getColor(R.color.white_disable))
            binding.btnSend.isEnabled = false
        }
    }

    private fun handleSendFiles() {
        val selected = viewModel.selectedFiles.value
        if (selected.isEmpty()) return
        // 1. Chuyển đổi sang ArrayList<TransferFile>
        val transferFiles = ArrayList(selected.map { item ->
            TransferFile(
                uri = item.uri,
                name = item.displayName,
                size = item.sizeBytes,
                mimeType = item.mimeType
            )
        })
        // 2. Chuyển sang màn hình quét mã QR
        val bundle = Bundle()
        bundle.putParcelableArrayList("EXTRA_FILES", transferFiles)
        startNextActivity(QrScannerActivity::class.java, bundle)
    }
}
