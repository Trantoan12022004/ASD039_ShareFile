package com.example.basekotlin.ui.clone_phone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentConnectBinding
import com.example.basekotlin.ui.clone_phone.model.CategoryState
import com.example.basekotlin.ui.clone_phone.model.CategoryTransferStatus
import com.example.basekotlin.ui.clone_phone.model.CloneCategory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectFragment : BaseFragment<FragmentConnectBinding>() {

    private val viewModel: ClonePhoneViewModel by activityViewModels()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentConnectBinding {
        return FragmentConnectBinding.inflate(inflater ?: LayoutInflater.from(context), container, false)
    }

    override fun bindView() {
        super.bindView()

        // Nút hủy truyền file / hoàn tất
        binding.btnCancel.tap {
            val act = activity as? ClonePhoneActivity
            if (viewModel.overallState.value.isDone) {
                // Đã clone xong -> Reset trạng thái và quay về MainFragment
                viewModel.resetState(requireContext())
                act?.navigateToPage(ClonePhoneActivity.PAGE_MAIN)
            } else {
                if (act != null) {
                    act.showCancelConfirmDialog {
                        viewModel.cancelClone(requireContext())
                        act.finishThisActivity()
                    }
                } else {
                    viewModel.cancelClone(requireContext())
                    activity?.finish()
                }
            }
        }

        observeData()
    }

    private fun observeData() {
        // Quan sát tiến trình tổng thể
        lifecycleScope.launch {
            viewModel.overallState.collectLatest { overall ->
                if (overall.isDone) {
                    // Khi truyền xong
                    binding.ivStatus.setImageResource(R.drawable.ic_coplete)
                    binding.tvStatus.text = getString(R.string.done)
                    binding.tvStatusDesc.text = getString(R.string.all_selected_data_transferred)
                    binding.progressBarOverall.progress = 100
                    binding.tvProgressPercent.text = "100%"
                    binding.btnCancel.text = getString(R.string.done)
                } else {
                    // Đang trong quá trình truyền

                    Glide.with(requireContext())
                        .load(R.drawable.ic_clone)
                        .placeholder(R.drawable.ic_clone)
                        .error(R.drawable.ic_clone)
                        .into(binding.ivStatus)
//                    binding.ivStatus.setImageResource(R.drawable.ic_connect)
                    binding.tvStatus.text = getString(R.string.connecting)
                    binding.tvStatusDesc.text = getString(R.string.please_keep_both_devices_connect_to_the_same_wifi)
                    binding.progressBarOverall.progress = overall.overallPercent
                    binding.tvProgressPercent.text = "${overall.overallPercent}%"
                    binding.btnCancel.text = getString(R.string.cancel)
                }
            }
        }

        // Quan sát trạng thái từng danh mục
        lifecycleScope.launch {
            viewModel.categoryStates.collectLatest { statesMap ->
                // Danh bạ (Contacts)
                handleCategoryView(
                    category = CloneCategory.CONTACTS,
                    layout = binding.layoutContacts,
                    tvStatus = binding.tvStatusContacts,
                    progressBar = binding.progressContacts,
                    state = statesMap[CloneCategory.CONTACTS]
                )

                // Ảnh (Photos)
                handleCategoryView(
                    category = CloneCategory.PHOTOS,
                    layout = binding.layoutPhotos,
                    tvStatus = binding.tvStatusPhotos,
                    progressBar = binding.progressPhotos,
                    state = statesMap[CloneCategory.PHOTOS]
                )

                // Video (Videos)
                handleCategoryView(
                    category = CloneCategory.VIDEOS,
                    layout = binding.layoutVideos,
                    tvStatus = binding.tvStatusVideos,
                    progressBar = binding.progressVideos,
                    state = statesMap[CloneCategory.VIDEOS]
                )

                // Nhạc (Music / Audios)
                handleCategoryView(
                    category = CloneCategory.AUDIOS,
                    layout = binding.layoutMusic,
                    tvStatus = binding.tvStatusMusic,
                    progressBar = binding.progressMusic,
                    state = statesMap[CloneCategory.AUDIOS]
                )

                // Tài liệu (Documents)
                handleCategoryView(
                    category = CloneCategory.DOCUMENTS,
                    layout = binding.layoutDocuments,
                    tvStatus = binding.tvStatusDocuments,
                    progressBar = binding.progressDocuments,
                    state = statesMap[CloneCategory.DOCUMENTS]
                )

                // Ứng dụng (Apps)
                handleCategoryView(
                    category = CloneCategory.APPS,
                    layout = binding.layoutApps,
                    tvStatus = binding.tvStatusApps,
                    progressBar = binding.progressApps,
                    state = statesMap[CloneCategory.APPS]
                )
            }
        }
    }

    private fun handleCategoryView(
        category: CloneCategory,
        layout: android.view.View,
        tvStatus: TextView,
        progressBar: ProgressBar,
        state: CategoryState?
    ) {
        val isSelected = state?.isSelected == true || viewModel.selectedCategories.value.contains(category)

        // Nếu mục nào không được chọn ở màn trước thì bị ẩn
        if (!isSelected) {
            layout.gone()
            return
        }

        layout.visible()
        val context = requireContext()

        when (state?.status ?: CategoryTransferStatus.WAITING) {
            CategoryTransferStatus.WAITING -> {
                // Đang chờ đến lượt: text màu black, progress = 0
                tvStatus.text = getString(R.string.status_waiting)
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
                progressBar.progress = 0
            }

            CategoryTransferStatus.PROGRESS -> {
                // Đang truyền: hiện % realtime, màu primary_35, progress realtime
                tvStatus.text = "${state?.progressPercent ?: 0}%"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_35))
                progressBar.progress = state?.progressPercent ?: 0
            }

            CategoryTransferStatus.COMPLETE -> {
                // Hoàn thành: text Complete màu primary_35, progress = 100
                tvStatus.text = getString(R.string.status_complete)
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_35))
                progressBar.progress = 100
            }

            CategoryTransferStatus.FAIL -> {
                // Bị lỗi: text Fail màu error, progress = 0
                tvStatus.text = getString(R.string.status_fail)
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error))
                progressBar.progress = 0
            }
        }
    }
}