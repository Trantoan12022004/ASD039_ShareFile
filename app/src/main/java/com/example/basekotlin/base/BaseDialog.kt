package com.example.basekotlin.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.viewbinding.ViewBinding
import com.example.basekotlin.R
import com.example.basekotlin.util.SystemUtil

abstract class BaseDialog<VB : ViewBinding>(
    context: Context,
    private val canAble: Boolean,
) :
    Dialog(context, R.style.BaseDialog) {

    // binding chỉ có giá trị thật sau khi onCreate() chạy (tức sau khi show() được gọi)
    lateinit var binding: VB
        private set

    protected abstract fun setBinding(): VB

    init {
        // Các thao tác không phụ thuộc vào property của lớp con thì để ở init cũng an toàn
        SystemUtil.setLocale(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lúc này constructor của lớp con (SelectPlaylistDialog, TextInputDialog...) đã chạy xong,
        // property truyền qua constructor (playlists, title, message...) đã có giá trị thật.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = setBinding()
        setContentView(binding.root)
        setCancelable(canAble)
        initView()
        bindView()
    }

    open fun initView() {
    }

    open fun bindView() {
    }
}