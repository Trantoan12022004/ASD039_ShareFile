package com.example.basekotlin.ui.clone_phone

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.FragmentSelectDataBinding
import com.example.basekotlin.ui.clone_phone.model.CloneCategory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectDataFragment : BaseFragment<FragmentSelectDataBinding>() {

    private val viewModel: ClonePhoneViewModel by activityViewModels()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSelectDataBinding {
        return FragmentSelectDataBinding.inflate(inflater ?: LayoutInflater.from(context), container, false)
    }

    override fun initView() {
        super.initView()
        // Tải số lượng file cho từng danh mục
        viewModel.loadCategoryCounts()
    }

    override fun bindView() {
        super.bindView()

        // Click từng danh mục để chọn / bỏ chọn
        binding.btnContacts.tap { viewModel.toggleCategory(CloneCategory.CONTACTS) }
        binding.btnPhotos.tap { viewModel.toggleCategory(CloneCategory.PHOTOS) }
        binding.btnVideos.tap { viewModel.toggleCategory(CloneCategory.VIDEOS) }
        binding.btnAudios.tap { viewModel.toggleCategory(CloneCategory.AUDIOS) }
        binding.btnDocuments.tap { viewModel.toggleCategory(CloneCategory.DOCUMENTS) }
        binding.btnApps.tap { viewModel.toggleCategory(CloneCategory.APPS) }

        // Nút chọn tất cả / bỏ chọn tất cả
        binding.btnSelectAll.tap {
            viewModel.toggleSelectAll()
        }

        // Nút Tạo QR Code
        binding.btnCreateQr.tap {
            if (viewModel.selectedCategories.value.isNotEmpty()) {
                val act = activity as? ClonePhoneActivity ?: return@tap
                viewModel.startHost(requireContext())
                act.navigateToPage(ClonePhoneActivity.PAGE_QR)
            }
        }

        observeData()
    }

    private fun observeData() {
        // Quan sát số lượng file từng mục
        lifecycleScope.launch {
            viewModel.categoryCounts.collectLatest { counts ->
                binding.tvContactCount.text = getString(R.string.folder_item_count, counts[CloneCategory.CONTACTS] ?: 0)
                binding.tvPhotoCount.text = getString(R.string.folder_item_count, counts[CloneCategory.PHOTOS] ?: 0)
                binding.tvVideoCount.text = getString(R.string.folder_item_count, counts[CloneCategory.VIDEOS] ?: 0)
                binding.tvAudioCount.text = getString(R.string.folder_item_count, counts[CloneCategory.AUDIOS] ?: 0)
                binding.tvDocumentCount.text = getString(R.string.folder_item_count, counts[CloneCategory.DOCUMENTS] ?: 0)
                binding.tvAppsCount.text = getString(R.string.folder_item_count, counts[CloneCategory.APPS] ?: 0)
            }
        }

        // Quan sát các danh mục đang được chọn
        lifecycleScope.launch {
            viewModel.selectedCategories.collectLatest { selected ->
                val allSelected = selected.size == CloneCategory.values().size
                binding.btnSelectAll.text = getString(if (allSelected) R.string.deselect_all else R.string.select_all)

                // Cập nhật checkbox
                updateCheckbox(binding.cbContacts, selected.contains(CloneCategory.CONTACTS))
                updateCheckbox(binding.cbPhotos, selected.contains(CloneCategory.PHOTOS))
                updateCheckbox(binding.cbVideos, selected.contains(CloneCategory.VIDEOS))
                updateCheckbox(binding.cbAudios, selected.contains(CloneCategory.AUDIOS))
                updateCheckbox(binding.cbDocuments, selected.contains(CloneCategory.DOCUMENTS))
                updateCheckbox(binding.cbApps, selected.contains(CloneCategory.APPS))

                // Cập nhật trạng thái nút Tạo mã QR
                val hasSelection = selected.isNotEmpty()
                binding.btnCreateQr.isEnabled = hasSelection

                if (hasSelection) {
                    binding.btnCreateQr.setBackgroundResource(R.drawable.bg_btn_create)
                    val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
                    binding.ivCreateQr.imageTintList = ColorStateList.valueOf(whiteColor)
                    binding.tvCreateQr.setTextColor(whiteColor)
                } else {
                    binding.btnCreateQr.setBackgroundResource(R.drawable.bg_btn_create_1)
                    val disableColor = ContextCompat.getColor(requireContext(), R.color.white_disable)
                    binding.ivCreateQr.imageTintList = ColorStateList.valueOf(disableColor)
                    binding.tvCreateQr.setTextColor(disableColor)
                }
            }
        }
    }

    private fun updateCheckbox(iv: android.widget.ImageView, isChecked: Boolean) {
        iv.setImageResource(
            if (isChecked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
        )
    }
}