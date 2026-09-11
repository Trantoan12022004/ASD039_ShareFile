package com.example.basekotlin.ui.cleanfile.messenger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemCleanerCategoryBinding
import com.example.basekotlin.model.MessengerCategory
import com.example.basekotlin.ui.cleanfile.CleanFileUtils

class CleanerCategoryAdapter : BaseAdapter<MessengerCategory, ItemCleanerCategoryBinding>() {

    var onCategoryClick: ((MessengerCategory) -> Unit)? = null
    var onCleanJunkClick: ((MessengerCategory) -> Unit)? = null

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemCleanerCategoryBinding {
        return ItemCleanerCategoryBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<MessengerCategory>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemCleanerCategoryBinding,
        item: MessengerCategory,
        layoutPosition: Int
    ) {
        binding.imgCategoryIcon.setImageResource(item.iconRes)
        binding.tvCategoryTitle.text = item.name
        binding.tvCategoryDesc.text = item.description
        binding.tvCategorySize.text = CleanFileUtils.formatFileSize(item.sizeBytes)

        if (item.isJunk) {
            binding.imgArrow.gone()
            if (item.hasJunk) {
                binding.btnCleanJunk.visible()
            } else {
                binding.btnCleanJunk.gone()
            }
        } else {
            binding.btnCleanJunk.gone()
            binding.imgArrow.visible()
        }
    }

    override fun onCLick(
        binding: ItemCleanerCategoryBinding,
        item: MessengerCategory,
        layoutPosition: Int
    ) {
        binding.root.tap {
            if (!item.isJunk) {
                onCategoryClick?.invoke(item)
            }
        }

        binding.btnCleanJunk.tap {
            onCleanJunkClick?.invoke(item)
        }
    }
}
