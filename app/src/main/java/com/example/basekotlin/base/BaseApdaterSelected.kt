package com.example.basekotlin.base

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseApdaterSelected<T, VB : ViewBinding> :
    RecyclerView.Adapter<BaseApdaterSelected<T, VB>.ViewHolder>() {

    companion object {
        const val PAYLOAD_SELECTION = "PAYLOAD_SELECTION"
    }

    // 1. Giữ nguyên các hàm cốt lõi của BaseAdapter
    abstract fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): VB

    abstract fun addListData(newList: MutableList<T>)

    abstract fun setData(binding: VB, item: T, layoutPosition: Int)

    open fun onCLick(binding: VB, item: T, layoutPosition: Int) {
    }

    /**
     * HÀM MỚI: Chỉ xử lý riêng Checkbox / RadioButton / Trạng thái chọn.
     * Khi click chọn/bỏ chọn, CHỈ hàm này được gọi lại (không reload ảnh hay text).
     */
    open fun setSelection(binding: VB, item: T, layoutPosition: Int) {
    }

    val listData: MutableList<T> = mutableListOf()
    var context: Context? = null

    /**
     * Định danh duy nhất cho item (ví dụ: apkPath, filePath, id...).
     * Mặc định là chính item đó.
     */
    open fun getItemKey(item: T): Any = item as Any

    /**
     * Tập các Key đang chọn (từ ViewModel hoặc Fragment).
     * Khi gán tập mới, Base tự so khớp O(1) và CHỈ notify đúng các item thay đổi qua PAYLOAD_SELECTION.
     */
    var selectedKeys: Set<Any> = emptySet()
        set(value) {
            val oldSet = field
            field = value
            listData.forEachIndexed { index, item ->
                val key = getItemKey(item)
                val wasSelected = oldSet.contains(key)
                val isSelected = value.contains(key)
                if (wasSelected != isSelected) {
                    notifyItemChanged(index, PAYLOAD_SELECTION)
                }
            }
        }

    /**
     * Cập nhật riêng trạng thái chọn của 1 item duy nhất
     */
    fun notifyItemSelectionChanged(position: Int) {
        if (position in listData.indices) {
            notifyItemChanged(position, PAYLOAD_SELECTION)
        }
    }

    override fun getItemCount(): Int = listData.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding = setBinding(LayoutInflater.from(context), parent, viewType)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(listData[position])
    }

    /**
     * Bắt Payload: Khi chỉ thay đổi chọn, CHỈ gọi setSelection, KHÔNG gọi setData và onCLick
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            val item = listData.getOrNull(position) ?: return
            setSelection(holder.binding, item, holder.bindingAdapterPosition)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ViewHolder(val binding: VB) : BaseViewHolder<T>(binding) {

        override fun bindData(obj: T) {
            bindView(obj)
            setData(binding, obj, layoutPosition)
            setSelection(binding, obj, layoutPosition)
        }

        override fun bindView(obj: T) {
            onCLick(binding, obj, layoutPosition)
        }
    }
}
