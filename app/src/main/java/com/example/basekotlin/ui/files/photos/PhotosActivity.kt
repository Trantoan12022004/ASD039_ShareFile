package com.example.basekotlin.ui.files.photos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.example.basekotlin.databinding.ActivityPhotosBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.InformationPhotoDialog
import com.example.basekotlin.dialog.common.SelectMore1Dialog
import com.example.basekotlin.dialog.common.SelectMoreDialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.DeleteResult
import com.example.basekotlin.model.MusicSelectionTarget
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.model.PhotoInfo
import com.example.basekotlin.model.RenameResult
import com.example.basekotlin.ui.files.music.ringtone.RingtoneActivity
import com.example.basekotlin.ui.files.pdfconverter.PdfConverterActivity
import com.example.basekotlin.ui.files.pdfconverter.PdfViewModel
import com.example.basekotlin.ui.files.photos.fragment.AllPhotosFragment
import com.example.basekotlin.util.ImageToPdfConverter
import com.example.basekotlin.util.Utils
import com.example.basekotlin.util.reduceDragSensitivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File

class PhotosActivity : BaseActivity<ActivityPhotosBinding>(ActivityPhotosBinding::inflate) {
    private lateinit var pagerAdapter: PhotosPagerAdapter

    private val viewModel: PhotosViewModel by viewModels()
    private val pdfViewModel: PdfViewModel by viewModels()
    private var isSearchMode = false
    private var isFolderDetailMode = false

    override fun initView() {
        binding.layoutToolbar.tvTitle.text = getString(R.string.photos)
        pagerAdapter = PhotosPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.reduceDragSensitivity(multiplier = 3)
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

        bindSelectionActions()

        binding.layoutToolbar.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.updateSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        observeSelectMode()
    }
    private fun updateSelectionCount() {
        val count = viewModel.selectedPhotoPaths.value.size
        binding.layoutToolbar.tvCountSong.text = count.toString()
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
                    viewModel.selectedPhotoPaths.collect { selectedPaths ->
                        updateSelectionCount()
                    }
                }

                launch {
                    pdfViewModel.isConverting.collect { isConverting ->
                        if (isConverting) {
                            Toast.makeText(
                                this@PhotosActivity,
                                getString(R.string.converting_pdf),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                launch {
                    pdfViewModel.convertedPdfPath.collect { path ->
                        if (path != null) {
                            val message = getString(R.string.convert_pdf_success1)
                            Toast.makeText(this@PhotosActivity, message, Toast.LENGTH_SHORT).show()
                            viewModel  .clearPhotoSelection()
                            pdfViewModel.resetConvertedPdfPath()
                        }
                    }
                }
            }
        }
    }

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
                    // Chặn toàn bộ tương tác chạm vào từng tab
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


    // Mở màn hình danh sách ảnh của 1 folder
    fun openFolderPhotos(folderName: String) {
        isFolderDetailMode = true
        // 1. Cập nhật Toolbar: Ẩn TabLayout, hiển thị tvTitle bằng tên folder
//        binding.layoutToolbar.tabLayout.gone()
        setTabLayoutClickable(false)
        binding.layoutToolbar.tvTitle.visible()
        binding.layoutToolbar.tvTitle.text = folderName
        // 2. Hiển thị AllPhotosFragment với TYPE_FOLDER
        val folderFragment = AllPhotosFragment.newInstance(AllPhotosFragment.TYPE_FOLDER)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, folderFragment)
            .commit()
        // 3. Ẩn ViewPager, hiển thị Container
        binding.viewPager.gone()
        binding.fragmentContainer.visible()
    }
    // Đóng màn hình danh sách ảnh của folder và quay lại tab Folders
    private fun closeFolderPhotos() {
        isFolderDetailMode = false
        viewModel.setCurrentFolder("")
        // 1. Khôi phục Toolbar
        binding.layoutToolbar.tvTitle.visible()
        binding.layoutToolbar.tvTitle.text = getString(R.string.photos)
        setTabLayoutClickable(true)
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

    private fun openSearch() {
        if (isSearchMode) {
            return
        }
        isSearchMode = true

        if (viewModel.isSelectionMode.value) {
            viewModel.exitSelectionMode()
        }

        binding.layoutToolbar.tabLayout.gone()
        binding.layoutToolbar.layoutSearch.visible()
        binding.layoutToolbar.edtSearch.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.layoutToolbar.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        if (isSearchMode == false) {
            return
        }
        isSearchMode = false

        viewModel.updateSearchQuery("")
        binding.layoutToolbar.edtSearch.setText("")
        Utils.hideKeyboard(this)

        binding.layoutToolbar.layoutSearch.gone()
        binding.layoutToolbar.tabLayout.visible()
        if (isFolderDetailMode) {
            setTabLayoutClickable(false)
        }
    }
    private fun bindSelectionActions() {
        val actions = binding.layoutSelectionActions
        // Xoá các ảnh đã chọn
        actions.btnDeleteSelected.tap {
            val selected = getSelectedPhotos()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                showDeleteConfirmDialog(selected)
            }
        }
        // Chia sẻ các ảnh đã chọn
        actions.btnShare.tap {
            val selected = getSelectedPhotos()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                sharePhotos(selected)
            }
        }
        // Gửi các ảnh đã chọn
        actions.btnSend.tap {
            val selected = getSelectedPhotos()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            } else {
                sharePhotos(selected)
            }
        }
        // Mở popup More
        actions.btnMoreSelected.tap {
            showSelectionMoreMenu()
        }
    }
    // Lấy danh sách PhotoInfo ứng với các đường dẫn đang được chọn
    private fun getSelectedPhotos(): List<PhotoInfo> {
        val selectedPaths = viewModel.selectedPhotoPaths.value
        val allPhotos = viewModel.allPhotosUi.value
        val result = mutableListOf<PhotoInfo>()
        for (photo in allPhotos) {
            if (selectedPaths.contains(photo.filePath)) {
                result.add(photo)
            }
        }
        return result
    }
    private fun showSelectionMoreMenu() {
        val selectedPhotos = getSelectedPhotos()
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra xem có đang ở tab Folder hay không
        val isFolderTab = binding.viewPager.currentItem == 1 && !isFolderDetailMode

        SelectMore1Dialog(
            context = this,
            selectedPhotos = selectedPhotos,
            isFolderTab = isFolderTab,
            onRename = { photo ->
                showRenameDialog(photo)
            },
            onConvertPdf = { photos ->
                openPdfConverter(photos)
            },
            onInformation = { photo ->
                showPhotoInformationDialog(photo)
            },
            onMoveSafeBox = { photo ->
                moveToSafeBox(photo)
            }
        ).show()
    }
    // 1. Đổi tên ảnh
    private fun showRenameDialog(photo: PhotoInfo) {
        val file = File(photo.filePath)
        val nameWithoutExt = file.nameWithoutExtension
        val extension = file.extension
        TextInputDialog(
            context = this,
            title = getString(R.string.rename),
            hint = getString(R.string.rename),
            initialText = nameWithoutExt,
            positiveText = getString(R.string.rename)
        ) { newName ->
            val newFileName = if (extension.isNotEmpty()) {
                "$newName.$extension"
            } else {
                newName
            }
            val targetFile = File(file.parentFile, newFileName)
            if (file.renameTo(targetFile)) {
                Toast.makeText(this, getString(R.string.rename_song_success), Toast.LENGTH_SHORT).show()
                viewModel.refreshAllPhotos()
                viewModel.exitSelectionMode()
            } else {
                Toast.makeText(this, getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
            }
        }.show()
    }
    // 2. Chuyển sang PDF Converter Activity
    private fun openPdfConverter(photos: List<PhotoInfo>) {
        val filePaths = ArrayList<String>()
        for (p in photos) {
            filePaths.add(p.filePath)
        }
        pdfViewModel.convertSelectedImagesToPdf(filePaths)
    }
    // 3. Hiển thị Dialog thông tin ảnh
    private fun showPhotoInformationDialog(photo: PhotoInfo) {
        InformationPhotoDialog(this, photo).show()
    }
    // 4. Di chuyển vào Safe Box
    private fun moveToSafeBox(photo: PhotoInfo) {
        // TODO: Implement logic to move photo to safe box
    }
    // 4. Chia sẻ ảnh
    private fun sharePhotos(photos: List<PhotoInfo>) {
        val uris = ArrayList<Uri>()
        for (photo in photos) {
            val file = File(photo.filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    this,
                    packageName + ".provider",
                    file
                )
                uris.add(uri)
            }
        }
        if (uris.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent()
        if (uris.size == 1) {
            intent.action = Intent.ACTION_SEND
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.action = Intent.ACTION_SEND_MULTIPLE
            intent.type = "image/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show()
        }
    }
    // 5. Xác nhận và Xoá ảnh
    private fun showDeleteConfirmDialog(photos: List<PhotoInfo>) {
        val message = if (photos.size == 1) {
            getString(R.string.delete_song_desc, photos[0].displayName)
        } else {
            getString(R.string.delete_songs_desc, photos.size)
        }
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDeletePhotos(photos)
        }.show()
    }
    private fun performDeletePhotos(photos: List<PhotoInfo>) {
        var successCount = 0
        var failCount = 0
        for (photo in photos) {
            val file = File(photo.filePath)
            val deleted = file.delete()
            if (deleted) {
                successCount = successCount + 1
            } else {
                failCount = failCount + 1
            }
        }
        if (failCount == 0) {
            Toast.makeText(this, getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
        }
        viewModel.exitSelectionMode()
        viewModel.refreshAllPhotos()
    }

    // 3. Xử lý logic Back toàn diện (Nút Back Toolbar & Phím Back thiết bị)
    override fun onBack() {
        if (viewModel.isSelectionMode.value) {
            // Nếu đang chọn nhiều ảnh -> thoát chế độ chọn
            viewModel.exitSelectionMode()
        } else if (isSearchMode) {
            // Nếu đang tìm kiếm -> đóng tìm kiếm
            closeSearch()
        } else if (isFolderDetailMode) {
            // NẾU ĐANG Ở TYPE FOLDER -> Quay về AllFolderPhotoFragment (Tab Folders)
            closeFolderPhotos()
        } else {
            // Màn hình bình thường -> thoát Activity
            finish()
        }
    }

}