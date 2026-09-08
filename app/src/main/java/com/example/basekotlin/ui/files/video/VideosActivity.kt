package com.example.basekotlin.ui.files.video

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityVideosBinding
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.StorageItem
import com.example.basekotlin.ui.files.video.dialog.DeleteVideoDialog
import com.example.basekotlin.ui.files.video.dialog.InformationVideoDialog
import com.example.basekotlin.ui.files.video.dialog.RenameVideoDialog
import com.example.basekotlin.ui.files.video.dialog.VideoMoreDialog
import com.example.basekotlin.ui.files.video.fragment.FolderVideoDetailFragment
import com.example.basekotlin.ui.files.video.model.VideoInfo
import com.example.basekotlin.ui.storage.dialog.RenameDialog
import com.example.basekotlin.util.Utils
import com.example.basekotlin.util.VideoToAudioConverter
import com.example.basekotlin.util.reduceDragSensitivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File

class VideosActivity : BaseActivity<ActivityVideosBinding>(ActivityVideosBinding::inflate) {
    private lateinit var pagerAdapter: VideosPagerAdapter
    private val viewModel: VideosViewModel by viewModels()
    private var isSearchMode = false
    private var isFolderDetailMode = false

    override fun initView() {
        super.initView()
        // Cài đặt tiêu đề ban đầu cho toolbar
        binding.layoutToolbar.tvTitle.text = getString(R.string.videos)
        // Khởi tạo ViewPager2 và TabLayout
        pagerAdapter = VideosPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.reduceDragSensitivity(3)

        val tabTitles = arrayOf(
            getString(R.string.all),
            getString(R.string.folders),
            getString(R.string.receive),
        )
        TabLayoutMediator(
            binding.layoutToolbar.tabLayout,
            binding.viewPager,
        ) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

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

        // Thiết lập sự kiện click cho Bottom Action Bar
        bindSelectionActions()
        // Lắng nghe các trạng thái chọn file
        observeSelectMode()

        // Lắng nghe thay đổi text tìm kiếm
        binding.layoutToolbar.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeSelectMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                launch {
                    viewModel.selectedVideoPaths.collect {
                        updateSelectionCount()
                    }
                }
            }
        }
    }

    // Mở màn hình danh sách video của 1 folder
    fun openFolderVideos(folderName: String, folderPath: String = "") {
        isFolderDetailMode = true
        // 1. Cập nhật Toolbar: Ẩn TabLayout, hiển thị tvTitle bằng tên folder
        binding.layoutToolbar.tabLayout.gone()
        binding.layoutToolbar.tvTitle.visible()
        binding.layoutToolbar.tvTitle.text = folderName

        // 2. Hiển thị FolderVideoDetailFragment vào fragmentContainer
        val folderFragment = FolderVideoDetailFragment.newInstance(folderName, folderPath)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, folderFragment)
            .commit()

        // 3. Ẩn ViewPager, hiển thị Container
        binding.viewPager.gone()
        binding.fragmentContainer.visible()
    }

    // Đóng màn hình danh sách video của folder và quay lại tab Folders
    private fun closeFolderVideos() {
        isFolderDetailMode = false
        viewModel.setCurrentFolder("")

        // 1. Khôi phục Toolbar
        binding.layoutToolbar.tvTitle.visible()
        binding.layoutToolbar.tvTitle.text = getString(R.string.videos)
        binding.layoutToolbar.tabLayout.visible()

        // 2. Gỡ bỏ Fragment và hiển thị lại ViewPager
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(currentFragment)
                .commit()
        }
        binding.fragmentContainer.gone()
        binding.viewPager.visible()
    }

    // Bật/tắt khả năng bấm của TabLayout khi ở chế độ xem chi tiết thư mục
    @SuppressLint("ClickableViewAccessibility")
    private fun setTabLayoutClickable(clickable: Boolean) {
        val tabLayout = binding.layoutToolbar.tabLayout
        tabLayout.isEnabled = clickable
        val tabStrip = tabLayout.getChildAt(0) as? ViewGroup
        tabStrip?.let { strip ->
            strip.isEnabled = clickable
            for (i in 0 until strip.childCount) {
                val tabView = strip.getChildAt(i)
                tabView.isEnabled = clickable
                tabView.isClickable = clickable
                if (!clickable) {
                    tabView.setOnTouchListener { _, _ -> true }
                } else {
                    tabView.setOnTouchListener(null)
                }
            }
        }
        if (!clickable) {
            tabLayout.setOnTouchListener { _, _ -> true }
        } else {
            tabLayout.setOnTouchListener(null)
        }
    }



    private fun bindSelectionActions() {
        val actions = binding.layoutSelectionActions
//        Xóa các video đã chọn
        actions.btnDeleteSelected.tap {
            val selected = getSelectedVideos()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                showDeleteConfirmDialog(selected)
            }
        }

        // Chia sẻ các video đã chọn
        actions.btnShare.tap {
            val selected = getSelectedVideos()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                shareVideos(selected)
            }
        }

        // Mở popup Menu More
        actions.btnMoreSelected.tap {
            showSelectionMoreMenu()
        }
    }

    // Hiển thị Popup More cho selection mode video
    private fun showSelectionMoreMenu() {
        val selectedVideos = getSelectedVideos()
        if (selectedVideos.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            return
        }
        val isFolderTab = binding.viewPager.currentItem == 1 && !isFolderDetailMode
        VideoMoreDialog(
            context = this,
            selectedVideos = selectedVideos,
            isFolderTab = isFolderTab,
            onRename = { video ->
                showRenameDialog(video)
            },
            onToMp3 = { video ->
                showToMp3Dialog(video)
            },
            onInformation = { video ->
                showVideoInformationDialog(video)
            },
            onMoveSafeBox = { video ->
//                moveToSafeBox(video)
            }
        ).show()
    }

    private fun showRenameDialog(video: VideoInfo){
        val targetFile = File(video.filePath)
        val initName: String = targetFile.nameWithoutExtension
        val dialog = RenameDialog(
            context = this,
            initText = initName,
            validate = { enteredName ->
                // Xác định tên file/folder đích sau khi đổi
                val candidateName: String
                val fileExtension: String = targetFile.extension
                if (fileExtension.isNotEmpty()) {
                    candidateName = "$enteredName.$fileExtension"
                } else {
                    candidateName = enteredName
                }
                // Kiểm tra xem tên mới đã tồn tại trong thư mục cha hay chưa
                val parentDir: File? = targetFile.parentFile
                if (parentDir != null) {
                    val destinationFile = File(parentDir, candidateName)
                    val isSameFile: Boolean =
                        destinationFile.absolutePath == targetFile.absolutePath
                    if (destinationFile.exists() && !isSameFile) {
                        // Trả về thông báo tên đã tồn tại
                        getString(R.string.text_input_failed1)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        ) { enteredNewName ->
            // Ghép lại phần mở rộng nếu là file
            val finalNewName: String
            val fileExtension: String = targetFile.extension
            if (fileExtension.isNotEmpty()) {
                finalNewName = "$enteredNewName.$fileExtension"
            } else {
                finalNewName = enteredNewName
            }
            val targetFile = File(targetFile.parentFile, finalNewName)
            // Gọi ViewModel với đối tượng targetFile kiểu File và tên mới
            if (targetFile.renameTo(targetFile)) {
                Toast.makeText(this, getString(R.string.rename_video_success), Toast.LENGTH_SHORT).show()
                viewModel.refreshAllVideos()
                viewModel.exitSelectionMode()
            } else {
                Toast.makeText(this, getString(R.string.rename_video_failed), Toast.LENGTH_SHORT).show()
            }
        }
        // Hiển thị dialog
        dialog.show()
    }

    private fun showToMp3Dialog(video: VideoInfo) {
        val file = File(video.filePath)
        val defaultMp3Name = "${file.nameWithoutExtension}.mp3"
        TextInputDialog(
            context = this,
            title = getString(R.string.to_mp3),
            hint = getString(R.string.enter_file_name),
            initialText = defaultMp3Name,
            positiveText = getString(R.string.convert)
        ) { chosenFileName ->
            convertVideoToMp3(video, chosenFileName)
        }.show()
    }

    // Thực hiện convert video sang MP3 bằng FFmpeg ở background
    private fun convertVideoToMp3(video: VideoInfo, outputFileName: String) {
        Toast.makeText(this, getString(R.string.converting_to_mp3), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val convertedFile = VideoToAudioConverter.convertVideoToMp3(
                context = this@VideosActivity,
                videoPath = video.filePath,
                outputFileName = outputFileName
            )
            if (convertedFile != null) {
                Toast.makeText(this@VideosActivity, getString(R.string.convert_to_mp3_success), Toast.LENGTH_SHORT).show()
                viewModel.exitSelectionMode()
            } else {
                Toast.makeText(this@VideosActivity, getString(R.string.convert_to_mp3_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showVideoInformationDialog(video: VideoInfo) {
        InformationVideoDialog(this, video).show()
    }

    private fun shareVideos(videos: List<VideoInfo>) {
        val uris = ArrayList<Uri>()
        for (video in videos) {
            val file = File(video.filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.provider",
                    file
                )
                uris.add(uri)
            }
        }
        if (uris.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmDialog(videos: List<VideoInfo>) {

        DeleteVideoDialog(
            context = this,
            selectedVideos = getSelectedVideos()
        ) {
            performDeleteVideos(videos)
        }.show()
    }
    private fun performDeleteVideos(videos: List<VideoInfo>) {
        var successCount = 0
        var failCount = 0
        for (video in videos) {
            val file = File(video.filePath)
            if (file.delete()) {
                successCount++
            } else {
                failCount++
            }
        }
        if (failCount == 0) {
            Toast.makeText(this, getString(R.string.delete_video_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.delete_video_failed), Toast.LENGTH_SHORT).show()
        }
        viewModel.exitSelectionMode()
        viewModel.refreshAllVideos()
    }

    // Lấy danh sách VideoInfo tương ứng với các đường dẫn đang được chọn
    private fun getSelectedVideos(): List<VideoInfo> {
        val selectedPaths = viewModel.selectedVideoPaths.value
        val allVideos = viewModel.allVideosUi.value
        val result = mutableListOf<VideoInfo>()
        for (video in allVideos) {
            if (selectedPaths.contains(video.filePath)) {
                result.add(video)
            }
        }
        return result
    }

    private fun openSearch() {
        if (isSearchMode) return

        isSearchMode = true
        binding.layoutToolbar.tabLayout.gone()
        binding.layoutToolbar.layoutSearch.visible()
        binding.layoutToolbar.edtSearch.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.layoutToolbar.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        if (!isSearchMode) return
        isSearchMode = false
        viewModel.updateSearchQuery("")
        binding.layoutToolbar.edtSearch.setText("")
        Utils.hideKeyboard(this)
        binding.layoutToolbar.layoutSearch.gone()
        if (!isFolderDetailMode) {
            binding.layoutToolbar.tabLayout.visible()
        }
    }

    private fun updateSelectionCount(){
        val count = viewModel.selectedVideoPaths.value.size
        binding.layoutToolbar.tvCountSong.text = count.toString()
    }

    override fun onBack() {
        when {
            viewModel.isSelectionMode.value -> {
                // Thoát Selection Mode nếu đang chọn
                viewModel.exitSelectionMode()
            }
            isSearchMode -> {
                // Đóng tìm kiếm nếu đang mở
                closeSearch()
            }
            isFolderDetailMode -> {
                // Đóng folder detail, trở về tab Folders
                closeFolderVideos()
            }
            else -> {
                // Thoát Activity
                finishThisActivity()
            }
        }
    }
}