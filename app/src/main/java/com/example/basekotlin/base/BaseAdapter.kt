package com.example.basekotlin.base

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T, VB : ViewBinding> :
    RecyclerView.Adapter<BaseAdapter<T, VB>.ViewHolder>() {

    private var binding: VB? = null

    abstract fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): VB

    abstract fun addListData(newList: MutableList<T>)

    abstract fun setData(binding: VB, item: T, layoutPosition: Int)

    val listData: MutableList<T> = mutableListOf()
    var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        binding = setBinding(LayoutInflater.from(context), parent, viewType)
        return ViewHolder(binding!!)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(listData[position])
    }

    override fun getItemCount(): Int = listData.size

    inner class ViewHolder(val binding: VB) : BaseViewHolder<T>(binding) {

        override fun bindData(obj: T) {
            bindView(obj)
            setData(binding, obj, layoutPosition)
        }

        override fun bindView(obj: T) {
            onCLick(binding, obj, layoutPosition)
        }

    }

    open fun onCLick(binding: VB, item: T, layoutPosition: Int) {

    }
}