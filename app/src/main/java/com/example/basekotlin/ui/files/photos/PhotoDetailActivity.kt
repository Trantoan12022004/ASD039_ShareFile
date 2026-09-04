package com.example.basekotlin.ui.files.photos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityPhotoDetailBinding
import com.example.basekotlin.databinding.ActivityPhotosBinding
import com.example.basekotlin.dialog.common.ConfirmActionDialog
import com.example.basekotlin.dialog.common.InformationPhotoDialog
import com.example.basekotlin.dialog.common.SelectMore2Dialog
import com.example.basekotlin.dialog.common.TextInputDialog
import com.example.basekotlin.model.PhotoInfo
import com.example.basekotlin.ui.files.pdfconverter.PdfConverterActivity
import com.example.basekotlin.ui.files.pdfconverter.PdfViewModel
import com.example.basekotlin.ui.files.photos.adapter.PhotoDetailAdapter
import kotlinx.coroutines.launch
import java.io.File

class PhotoDetailActivity : BaseActivity<ActivityPhotoDetailBinding>(ActivityPhotoDetailBinding::inflate) {

    private val viewModel: PhotosViewModel by viewModels()
    private val pdfViewModel: PdfViewModel by viewModels()

    private val photoDetailAdapter = PhotoDetailAdapter()
    private var currentPosition: Int = 0
    private var isFirstLoad: Boolean = true

    override fun getData() {
        super.getData()
        val bundle = intent.extras
        if (bundle != null) {
            currentPosition = bundle.getInt("EXTRA_CURRENT_POSITION", 0)
        }
    }

    override fun initView() {
        binding.viewPagerPhotos.adapter = photoDetailAdapter
    }

    override fun bindView() {
        super.bindView()

        binding.btnBack.tap{
            onBack()
        }

        binding.btnMore.tap{
            showMoreMenu()
        }

        binding.viewPagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updateHeaderTitle(position)
            }
        })

        // 4. Quan sát danh sách ảnh từ ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allPhotosUi.collect { photos ->
                        if (photos.isNotEmpty()) {
                            photoDetailAdapter.addListData(photos.toMutableList())
                            // Lần đầu tải: di chuyển đến đúng ảnh được click ban đầu
                            if (isFirstLoad) {
                                isFirstLoad = false
                                if (currentPosition >= 0 && currentPosition < photos.size) {
                                    binding.viewPagerPhotos.setCurrentItem(currentPosition, false)
                                    updateHeaderTitle(currentPosition)
                                }
                            }
                        }
                    }
                }

                launch {
                    pdfViewModel.isConverting.collect { isConverting ->
                        if (isConverting) {
                            Toast.makeText(
                                this@PhotoDetailActivity,
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
                            Toast.makeText(this@PhotoDetailActivity, message, Toast.LENGTH_SHORT).show()
                            viewModel  .clearPhotoSelection()
                            pdfViewModel.resetConvertedPdfPath()
                        }
                    }
                }

            }
        }
    }

    // Cập nhật tên ảnh đang hiển thị
    private fun updateHeaderTitle(position: Int) {
        val list = photoDetailAdapter.listData
        if (position >= 0 && position < list.size) {
            val photo = list[position]
            binding.tvName.text = photo.displayName
        }
    }
    // Hiển thị menu chức năng mở rộng cho ảnh hiện tại
    // Hiển thị Popup More 2
    private fun showMoreMenu() {
        val list = photoDetailAdapter.listData
        if (currentPosition < 0 || currentPosition >= list.size) {
            return
        }
        val currentPhoto = list[currentPosition]
        SelectMore2Dialog(
            context = this,
            photo = currentPhoto,
            onSend = { photo ->
                shareSinglePhoto(photo)
            },
            onShare = { photo ->
                shareSinglePhoto(photo)
            },
            onDelete = { photo ->
                showDeleteConfirmDialog(photo)
            },
            onRename = { photo ->
                showRenameDialog(photo)
            },
            onConvertPdf = { photo ->
                openPdfConverter(photo)
            },
            onMoveSafeBox = { photo ->
                moveToSafeBox(photo)
            },
            onInformation = { photo ->
                showPhotoInformationDialog(photo)
            }
        ).show()
    }
    // 1. Chia sẻ / Gửi ảnh
    private fun shareSinglePhoto(photo: PhotoInfo) {
        val file = File(photo.filePath)
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_item), Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            this,
            packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show()
        }
    }
    // 2. Xác nhận và Xoá ảnh
    private fun showDeleteConfirmDialog(photo: PhotoInfo) {
        val message = getString(R.string.delete_song_desc, photo.displayName)
        ConfirmActionDialog(
            context = this,
            title = getString(R.string.delete),
            message = message,
            positiveText = getString(R.string.delete)
        ) {
            performDeletePhoto(photo)
        }.show()
    }
    private fun performDeletePhoto(photo: PhotoInfo) {
        val file = File(photo.filePath)
        val isDeleted = file.delete()
        if (isDeleted) {
            Toast.makeText(this, getString(R.string.delete_song_success), Toast.LENGTH_SHORT).show()
            viewModel.refreshAllPhotos()
        } else {
            Toast.makeText(this, getString(R.string.delete_song_failed), Toast.LENGTH_SHORT).show()
        }
    }
    // 3. Đổi tên file ảnh
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
            val isSuccess = file.renameTo(targetFile)
            if (isSuccess) {
                Toast.makeText(this, getString(R.string.rename_song_success), Toast.LENGTH_SHORT).show()
                binding.tvName.text = newFileName
                viewModel.refreshAllPhotos()
            } else {
                Toast.makeText(this, getString(R.string.rename_song_failed), Toast.LENGTH_SHORT).show()
            }
        }.show()
    }
    // 4. Chuyển sang PDF Converter
    private fun openPdfConverter(photo: PhotoInfo) {
        val filePaths = arrayListOf(photo.filePath)

        pdfViewModel.convertSelectedImagesToPdf(filePaths)
    }
    // 5. Hiển thị thông tin chi tiết ảnh
    private fun showPhotoInformationDialog(photo: PhotoInfo) {
        InformationPhotoDialog(this, photo).show()
    }
    // 6. Chuyển vào SafeBox
    private fun moveToSafeBox(photo: PhotoInfo) {
        // TODO: Xử lý chuyển ảnh vào Safe Box khi có tính năng Safe Box
    }
}