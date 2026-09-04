package com.example.basekotlin.ui.files.pdfconverter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityPdfConverterBinding
import com.example.basekotlin.ui.files.documents.DocumentsActivity.Companion.EXTRA_TAB_INDEX
import com.example.basekotlin.ui.files.documents.DocumentsPagerAdapter
import com.example.basekotlin.ui.files.documents.DocumentsViewModel
import com.example.basekotlin.ui.files.pdfconverter.fragment.ImgToPdfFragment
import com.example.basekotlin.ui.files.photos.PhotosViewModel
import com.example.basekotlin.ui.files.photos.fragment.AllPhotosFragment
import com.example.basekotlin.util.reduceDragSensitivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlin.getValue

class PdfConverterActivity : BaseActivity<ActivityPdfConverterBinding>(ActivityPdfConverterBinding::inflate) {

    companion object {
        const val TAB_PDF_TO_IMAGE = 0
        const val TAB_IMAGE_TO_PDF = 1
    }

    private val viewModel1: PhotosViewModel by viewModels()
    private lateinit var pagerAdapter: PdfPagerAdapter
    private var isFolderDetailMode = false

    override fun initView() {
        pagerAdapter = PdfPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        // Giảm độ nhạy vuốt ngang của ViewPager2 (nhân hệ số 4 hoặc 5)
        binding.viewPager.reduceDragSensitivity(multiplier = 4)

        val tabTitles = arrayOf(
            getString(R.string.pdf_to_image),
            getString(R.string.image_to_pdf),
        )

        TabLayoutMediator(
            binding.layoutToolbar.tabLayout,
            binding.viewPager,
        ) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Mặc định ban đầu luôn ẩn layoutBack
        binding.layoutToolbar.layoutBack.gone()
    }

    override fun bindView() {
        super.bindView()
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        binding.layoutToolbar.layoutBack.tap {
            closeFolderPhotos()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == TAB_PDF_TO_IMAGE) {
                    // Tab PDF to Image: luôn ẩn layoutBack
                    if (isFolderDetailMode) {
                        closeFolderPhotos()
                    } else {
                        binding.layoutToolbar.layoutBack.gone()
                    }
                }
            }
        })
    }

    // Mở màn hình danh sách ảnh của 1 folder
    fun openFolderPhotos(folderName: String) {
        isFolderDetailMode = true
        // 1. Cập nhật Toolbar: Ẩn TabLayout, hiển thị tvTitle bằng tên folder
        binding.layoutToolbar.tvTitle.visible()
        // Tạm khóa vuốt ngang khi đang xem chi tiết folder
        binding.viewPager.isUserInputEnabled = false
        binding.layoutToolbar.tvTitle.text = folderName
        binding.layoutToolbar.layoutBack.visible()
        viewModel1.enterSelectionMode()
    }

    private fun closeFolderPhotos() {
        isFolderDetailMode = false
        viewModel1.setCurrentFolder("")
        // 1. Khôi phục Toolbar
        binding.layoutToolbar.tvTitle.visible()
        binding.layoutToolbar.layoutBack.gone()
        // Mở lại tính năng vuốt chuyển tab
        binding.viewPager.isUserInputEnabled = true
        viewModel1.exitSelectionMode()
        val fragment = supportFragmentManager.findFragmentByTag("f1")
        if (fragment is ImgToPdfFragment) {
            fragment.closeFolderDetail()
        }
    }

    override fun onBack() {
        if (isFolderDetailMode) {
            // Nếu đang mở xem ảnh trong folder thì quay lại danh sách folder
            closeFolderPhotos()
        } else {
            // Màn hình chính bình thường -> thoát Activity
            finish()
        }
    }
}