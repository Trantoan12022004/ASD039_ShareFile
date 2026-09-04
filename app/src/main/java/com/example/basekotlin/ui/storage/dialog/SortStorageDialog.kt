package com.example.basekotlin.ui.storage.dialog

import android.content.Context
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogSortStorageBinding
import com.example.basekotlin.model.StorageSortOption

class SortStorageDialog(
    context: Context,
    private val currentOption: StorageSortOption,
    private val onSortSelected: (StorageSortOption) -> Unit
): BaseDialog<DialogSortStorageBinding>(context, true) {
    override fun setBinding(): DialogSortStorageBinding {
        return DialogSortStorageBinding.inflate(layoutInflater)
    }

    override fun initView() {
        updateSelection(currentOption)
    }

    override fun bindView() {
        binding.optionNameAZ.tap {
            onSortSelected(StorageSortOption.NAME_A_Z)
            dismiss()
        }
        binding.optionNameZA.tap {
            onSortSelected(StorageSortOption.NAME_Z_A)
            dismiss()
        }
        // Sắp xếp Ngày mới nhất
        binding.optionDateNewest.tap {
            onSortSelected(StorageSortOption.DATE_NEWEST)
            dismiss()
        }
        // Sắp xếp Ngày cũ nhất
        binding.optionDateOldest.tap {
            onSortSelected(StorageSortOption.DATE_OLDEST)
            dismiss()
        }
        // Sắp xếp Kích thước lớn -> bé
        binding.optionSizeBigSmall.tap {
            onSortSelected(StorageSortOption.SIZE_BIG_SMALL)
            dismiss()
        }
        // Sắp xếp Kích thước bé -> lớn
        binding.optionSizeSmallBig.tap {
            onSortSelected(StorageSortOption.SIZE_SMALL_BIG)
            dismiss()
        }
    }
    private fun updateSelection(option: StorageSortOption) {
        // Đặt tất cả icon về trạng thái uncheck
        binding.icNameAZ.setImageResource(R.drawable.ic_radio_uncheck)
        binding.icNameZA.setImageResource(R.drawable.ic_radio_uncheck)
        binding.icDateNewest.setImageResource(R.drawable.ic_radio_uncheck)
        binding.icDateOldest.setImageResource(R.drawable.ic_radio_uncheck)
        binding.icSizeBigSmall.setImageResource(R.drawable.ic_radio_uncheck)
        binding.icSizeSmallBig.setImageResource(R.drawable.ic_radio_uncheck)
        // Đánh dấu icon tương ứng với lựa chọn hiện tại
        when (option) {
            StorageSortOption.NAME_A_Z -> {
                binding.icNameAZ.setImageResource(R.drawable.ic_radio_check)
            }
            StorageSortOption.NAME_Z_A -> {
                binding.icNameZA.setImageResource(R.drawable.ic_radio_check)
            }
            StorageSortOption.DATE_NEWEST -> {
                binding.icDateNewest.setImageResource(R.drawable.ic_radio_check)
            }
            StorageSortOption.DATE_OLDEST -> {
                binding.icDateOldest.setImageResource(R.drawable.ic_radio_check)
            }
            StorageSortOption.SIZE_BIG_SMALL -> {
                binding.icSizeBigSmall.setImageResource(R.drawable.ic_radio_check)
            }
            StorageSortOption.SIZE_SMALL_BIG -> {
                binding.icSizeSmallBig.setImageResource(R.drawable.ic_radio_check)
            }
        }
    }
}