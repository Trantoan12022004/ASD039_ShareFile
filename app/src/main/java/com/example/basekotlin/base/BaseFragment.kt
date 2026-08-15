package com.example.basekotlin.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.basekotlin.R
import com.example.basekotlin.util.SystemUtil

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    protected lateinit var binding: VB

    abstract fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?,
    ): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = setBinding(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemUtil.setLocale(context)

        getData()
        initView()
        bindView()
    }

    open fun getData() {

    }

    open fun initView() {

    }

    open fun bindView() {

    }


    fun startNextActivity(actClass: Class<*>, bundle: Bundle?) {
        var bundle = bundle
        val intent = Intent(requireActivity(), actClass)
        if (bundle == null) {
            bundle = Bundle()
        }
        intent.putExtras(bundle)
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.in_right, R.anim.out_left)
    }

}
