package com.example.basekotlin.ui.safebox.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.databinding.ActivitySafeBoxHomeBinding
import com.example.basekotlin.ui.safebox.adapter.SafeBoxCategoryAdapter
import kotlinx.coroutines.launch
import kotlin.jvm.java

class SafeBoxHomeActivity : BaseActivity<ActivitySafeBoxHomeBinding>(ActivitySafeBoxHomeBinding::inflate) {

    private val viewModel: SafeBoxHomeViewModel by viewModels()
    private val categoryAdapter = SafeBoxCategoryAdapter()

    override fun initView() {
        // Cấu hình Toolbar
        binding.viewTop.btnBack.tap {
            onBack()
        }
        binding.rvCategories.adapter = categoryAdapter
    }

    override fun bindView() {
        // Xử lý khi click vào từng danh mục (Pictures, Videos...)
        categoryAdapter.onCategoryClick = { category ->
            openFileList(category.type)
        }

        binding.layoutOther.tap {
            openFileOtherList()
        }



        // Lắng nghe cập nhật số lượng danh mục từ ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    categoryAdapter.addListData(categories.toMutableList())
                    val countOther = categories[4].count

                    val countText = if (countOther == 1) {
                        binding.tvCategoryCount.text = getString(R.string.category_file_single_count)
                    } else {
                        binding.tvCategoryCount.text = getString(R.string.category_files_count, countOther)
                    }
                }
            }
        }
    }

    private fun openFileList(type: SafeBoxFileType) {
        val bundle = Bundle().apply {
            putString("EXTRA_FILE_TYPE", type.name)
        }
        startNextActivity(SafeBoxFileListActivity::class.java, bundle)
    }

    private fun openFileOtherList() {
        val bundle = Bundle().apply {
            putString("EXTRA_FILE_TYPE", "OTHERS")
        }
        startNextActivity(SafeBoxFileListActivity::class.java, bundle)
    }


    private fun onFabClicked() {
        // TODO: Mở bộ chọn file để import vào SafeBox ở Layer 4 / Layer 5
    }
}
