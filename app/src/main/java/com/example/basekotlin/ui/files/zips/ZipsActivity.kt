package com.example.basekotlin.ui.files.zips

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityZipsBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.model.ZipInfo
import com.example.basekotlin.util.reduceDragSensitivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

class ZipsActivity : BaseActivity<ActivityZipsBinding>(ActivityZipsBinding::inflate) {

    private val viewModel: ZipsViewModel by viewModels()
    private lateinit var pagerAdapter: ZipsPagerAdapter

    override fun initView() {
        pagerAdapter = ZipsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        // Giảm độ nhạy vuốt ngang của ViewPager2 (nhân hệ số 4 hoặc 5)
        binding.viewPager.reduceDragSensitivity(multiplier = 4)

        val tabTitles = arrayOf(
            getString(R.string.zipped),
            getString(R.string.unzipped),
        )

        TabLayoutMediator(
            binding.layoutToolbar.tabLayout,
            binding.viewPager,
        ) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()


        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isUnzippedTab = position == 1
                if (isUnzippedTab) {
                    // Nếu là tab Unzipped: ẩn nút btnSelect
                    binding.layoutToolbar.btnSelect.gone()
                    // Thoát chế độ chọn nếu đang mở ở tab Zipped
                    val isSelecting = viewModel.isSelectionMode.value
                    if (isSelecting) {
                        viewModel.exitSelectionMode()
                    }
                } else {
                    // Nếu là tab Zipped: hiển thị nút btnSelect
                    binding.layoutToolbar.btnSelect.visible()
                }
            }
        })
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        binding.layoutToolbar.btnSelect.tap {
            viewModel.enterSelectionMode()
        }

        setupSelectionActions()
        observeSelectMode()
    }

    private fun observeSelectMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Lắng nghe trạng thái bật/tắt chế độ chọn
                launch {
                    viewModel.isSelectionMode.collect { isSelecting ->
                        if (isSelecting) {
                            binding.layoutToolbar.tvTitle.gone()
                            binding.layoutToolbar.tvTitle1.visible()
                            binding.layoutSelectionActions.root.visible()
                        } else {
                            binding.layoutToolbar.tvTitle.visible()
                            binding.layoutToolbar.tvTitle1.gone()
                            binding.layoutSelectionActions.root.gone()
                        }
                    }
                }
                // Lắng nghe danh sách file zip được chọn để cập nhật giao diện
                launch {
                    viewModel.selectedZipsPaths.collect { selectedPaths ->
                        updateSelectionCount()
                        if (selectedPaths.isNotEmpty()) {
                            binding.layoutSelectionActions.tvDeselectAll.text = getString(R.string.deselect_all)
                        } else {
                            binding.layoutSelectionActions.tvDeselectAll.text = getString(R.string.select_all)
                        }
                    }
                }
            }
        }
    }

    private fun updateSelectionCount() {
        val count = viewModel.selectedZipsPaths.value.size
        binding.layoutToolbar.tvCountSong.text = count.toString()
    }

    private fun setupSelectionActions() {
        val actions = binding.layoutSelectionActions

        // Xử lý nút Chọn tất cả / Bỏ chọn tất cả
        actions.btnDeselectAll.tap {
            if (viewModel.selectedZipsPaths.value.isNotEmpty()) {
                viewModel.clearAllZips()
            } else {
                viewModel.selectAllZips()
            }
        }

        // Xử lý nút Xoá các file zip đã chọn
        actions.btnDelete.tap {
            val selectedZips = getSelectedZips()
            if (selectedZips.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showDeleteSelectedZipsDialog(selectedZips)
            }
        }

        // Xử lý nút Gửi / Chia sẻ các file zip đã chọn
        actions.btnSend.tap {
            val selectedZips = getSelectedZips()
            if (selectedZips.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_item),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                shareSelectedZips(selectedZips)
            }
        }
    }

    // Hiển thị dialog xác nhận xoá các file zip đã chọn
    private fun showDeleteSelectedZipsDialog(zipList: List<ZipInfo>) {
        val message: String
        if (zipList.size == 1) {
            message = getString(R.string.delete_zip_desc, zipList[0].fileName)
        } else {
            message = getString(R.string.delete_zips_desc, zipList.size)
        }

        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDeleteSelectedZips(zipList)
        }.show()
    }

    // Thực hiện xoá file trên bộ nhớ thiết bị
    private fun performDeleteSelectedZips(zipList: List<ZipInfo>) {
        var successCount = 0
        var failCount = 0

        for (zip in zipList) {
            val file = File(zip.filePath)
            val deleted = file.delete()
            if (deleted) {
                successCount = successCount + 1
            } else {
                failCount = failCount + 1
            }
        }

        if (failCount == 0) {
            Toast.makeText(this, getString(R.string.delete_zip_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.delete_zip_failed), Toast.LENGTH_SHORT).show()
        }

        // Thoát chế độ chọn và làm mới danh sách
        viewModel.exitSelectionMode()
        viewModel.refreshAllZips()
    }

    // Chia sẻ các file zip qua Intent
    private fun shareSelectedZips(zipList: List<ZipInfo>) {
        val uris = ArrayList<Uri>()
        for (zip in zipList) {
            val file = File(zip.filePath)
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

    // Lấy danh sách ZipInfo tương ứng với các đường dẫn đang được chọn
    private fun getSelectedZips(): List<ZipInfo> {
        val selectedPaths = viewModel.selectedZipsPaths.value
        val result = mutableListOf<ZipInfo>()
        val allZips = viewModel.allZips.value
        val addedPaths = mutableSetOf<String>()

        for (zip in allZips) {
            if (selectedPaths.contains(zip.filePath)) {
                result.add(zip)
                addedPaths.add(zip.filePath)
            }
        }

        for (path in selectedPaths) {
            if (!addedPaths.contains(path)) {
                val file = File(path)
                if (file.exists()) {
                    result.add(
                        ZipInfo(
                            fileName = file.name,
                            filePath = file.absolutePath,
                            sizeBytes = file.length(),
                            dateModifiedMillis = file.lastModified(),
                            extension = file.extension.lowercase()
                        )
                    )
                }
            }
        }

        return result
    }

    override fun onBack() {
        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        } else {
            finishThisActivity()
        }
    }
}
