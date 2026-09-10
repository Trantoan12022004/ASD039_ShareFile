package com.example.basekotlin.ui.safebox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemSafeboxCategoryBinding
import com.example.basekotlin.ui.safebox.model.SafeBoxCategory

class SafeBoxCategoryAdapter : BaseAdapter<SafeBoxCategory, ItemSafeboxCategoryBinding>() {
    var onCategoryClick: ((SafeBoxCategory) -> Unit)? = null

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemSafeboxCategoryBinding {
        return ItemSafeboxCategoryBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<SafeBoxCategory>) {
        this.listData.clear()
        listData.addAll(newList.take(4))
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemSafeboxCategoryBinding,
        item: SafeBoxCategory,
        layoutPosition: Int
    ) {
        val context = binding.root.context
        binding.imgCategoryIcon.setImageResource(item.iconRes)
        binding.tvCategoryTitle.setText(item.titleRes)

        val countText = if (item.count == 1) {
            context.getString(R.string.category_file_single_count)
        } else {
            context.getString(R.string.category_files_count, item.count)
        }
        binding.tvCategoryCount.text = countText
    }

    override fun onCLick(
        binding: ItemSafeboxCategoryBinding,
        item: SafeBoxCategory,
        layoutPosition: Int
    ) {
        binding.root.tap {
            onCategoryClick?.invoke(item)
        }
    }
}