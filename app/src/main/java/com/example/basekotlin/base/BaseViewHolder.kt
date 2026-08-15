package com.example.basekotlin.base

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseViewHolder<T>(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

    abstract fun bindData(obj: T)

    open fun bindView(obj: T) {

    }

}