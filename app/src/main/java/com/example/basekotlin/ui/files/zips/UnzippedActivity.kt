package com.example.basekotlin.ui.files.zips

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityUnzippedBinding
import com.example.basekotlin.ui.files.photos.PhotoDetailActivity
import com.example.basekotlin.ui.files.zips.adapter.UnzippedAdapter
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

class UnzippedActivity : BaseActivity<ActivityUnzippedBinding>(ActivityUnzippedBinding::inflate) {
    private val viewModel: ZipsViewModel by viewModels()
    private val adapter = UnzippedAdapter()
    private var targetFolderPath: String? = null
    private var targetFolderName: String? = null
    override fun getData() {
        super.getData()
        // Lấy đường dẫn và tên folder truyền qua Intent
        targetFolderPath = intent.getStringExtra("EXTRA_FOLDER_PATH")
        targetFolderName = intent.getStringExtra("EXTRA_FOLDER_NAME")
    }

    override fun initView() {
        binding.layoutToolbar.btnSelect.gone()
        binding.layoutToolbar.tabLayout.gone()
        binding.rvZips.adapter = adapter
        // Thiết lập text cho trạng thái rỗng
        binding.tvEmptyTitle.text = getString(R.string.unzipped_empty_title)
        binding.tvEmptyMessage.text = getString(R.string.unzipped_empty_desc)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCurrentFolder()
        }

        // Hiển thị tên folder lên toolbar
        val folderName = targetFolderName
        if (folderName != null && folderName.isNotBlank()) {
            binding.layoutToolbar.tvTitle.text = folderName
        } else {
            binding.layoutToolbar.tvTitle.text = getString(R.string.unzipped)
        }
        // Xử lý nút Back trên toolbar
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
    }

    override fun bindView() {
        adapter.onItemClick = { item ->
            if (item.isDirectory) {
                // 1. Nếu là Folder con bên trong: tiếp tục mở bằng UnzippedActivity
                val bundle = Bundle()
                bundle.putString("EXTRA_FOLDER_PATH", item.path)
                bundle.putString("EXTRA_FOLDER_NAME", item.name)
                // 3. Chuyển sang màn hình PhotoDetailActivity
                startNextActivity(UnzippedActivity::class.java, bundle)
            } else {
                // 2. Nếu là File: mở file bằng app tương ứng
                openExtractedFile(File(item.path), item.extension)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Lắng nghe trạng thái loading
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
                // Lắng nghe danh sách item
                launch {
                    viewModel.unzippedItems.collect { list ->
                        adapter.addListData(list.toMutableList())
                        updateEmptyState()
                    }
                }
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

    private fun openExtractedFile(file: File, extension: String) {
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            this,
            this.packageName + ".provider",
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
                Toast.makeText(this, getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show()
            }
        }
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

    // Tải dữ liệu thư mục mục tiêu
    private fun loadFolderData() {
        val path = targetFolderPath
        if (path != null) {
            val folder = File(path)
            if (folder.exists() && folder.isDirectory) {
                viewModel.loadFolder(folder)
            } else {
                viewModel.loadFolder()
            }
        } else {
            viewModel.loadFolder()
        }
    }
    override fun onResume() {
        super.onResume()
        loadFolderData()
    }
}