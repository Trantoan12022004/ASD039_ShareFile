package com.example.basekotlin.ui.transfer.send.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ItemFileGroupBinding
import com.example.basekotlin.ui.transfer.model.ItemGroup
import com.example.basekotlin.ui.transfer.model.TransferableItem

class FileGroupAdapter : BaseAdapter<ItemGroup<TransferableItem>, ItemFileGroupBinding>() {
    var onItemClick: ((TransferableItem) -> Unit)? = null
    private var parentRecyclerView: RecyclerView? = null
    private val sharedPool = RecyclerView.RecycledViewPool()

    // Lưu danh sách tiêu đề các nhóm đang bị thu gọn/ẩn
    private val collapsedGroupKeys = mutableSetOf<String>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        parentRecyclerView = null
    }

    /**
     * KHÔNG gọi notifyDataSetChanged() của cha.
     * Duyệt qua các child RecyclerView đang hiển thị để cập nhật selectedKeys.
     * FileItemAdapter (BaseApdaterSelected) sẽ tự diff O(1) và chỉ cập nhật riêng checkbox qua PAYLOAD_SELECTION.
     */
    var selectedKeys: Set<Any> = emptySet()
        set(value) {
            field = value
            parentRecyclerView?.let { rv ->
                for (i in 0 until rv.childCount) {
                    val childView = rv.getChildAt(i)
                    val groupRv = childView.findViewById<RecyclerView>(R.id.rvGroupFiles)
                    (groupRv?.adapter as? FileItemAdapter)?.selectedKeys = value
                }
            }
        }

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemFileGroupBinding {
        return ItemFileGroupBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<ItemGroup<TransferableItem>>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemFileGroupBinding, item: ItemGroup<TransferableItem>, layoutPosition: Int) {
        binding.tvTitle.text = item.title
        binding.tvCount.text = item.count.toString()

        // 1. Hiển thị trạng thái đóng/mở hiện tại
        val isCollapsed = collapsedGroupKeys.contains(item.title)
        if (isCollapsed) {
            binding.cardFiles.gone()
            binding.imgArrow.rotation = 180f
        } else {
            binding.cardFiles.visible()
            binding.imgArrow.rotation = 0f
        }

        // 2. Click Header để thu gọn / mở rộng danh sách item
        binding.layoutHeader.setOnClickListener {
            val willCollapse = !collapsedGroupKeys.contains(item.title)
            if (willCollapse) {
                collapsedGroupKeys.add(item.title)
                binding.cardFiles.gone()
                binding.imgArrow.animate().rotation(180f).setDuration(200).start()
            } else {
                collapsedGroupKeys.remove(item.title)
                binding.cardFiles.visible()
                binding.imgArrow.animate().rotation(0f).setDuration(200).start()
            }
        }

        // 3. Quản lý child adapter
        val childAdapter = (binding.rvGroupFiles.adapter as? FileItemAdapter) ?: FileItemAdapter().also { newAdapter ->
            binding.rvGroupFiles.setRecycledViewPool(sharedPool)
            binding.rvGroupFiles.adapter = newAdapter
        }

        childAdapter.onClick = { fileItem ->
            onItemClick?.invoke(fileItem)
        }
        childAdapter.addListData(item.items.toMutableList())
        childAdapter.selectedKeys = selectedKeys
    }
}