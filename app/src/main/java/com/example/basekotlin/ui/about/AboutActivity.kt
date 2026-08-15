package com.example.basekotlin.ui.about

import android.annotation.SuppressLint
import android.view.View
import com.example.basekotlin.BuildConfig
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.inVisible
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityAboutBinding
import com.example.basekotlin.ui.policy.PolicyActivity

class AboutActivity : BaseActivity<ActivityAboutBinding>(ActivityAboutBinding::inflate) {

    @SuppressLint("SetTextI18n")
    override fun initView() {
        binding.viewTop.tvToolBar.text = getString(R.string.about)
        binding.viewTop.ivRight.inVisible()
        binding.viewTop.ivRight1.inVisible()
        binding.viewTop.ivLeft.visible()

        binding.tvVersion.text = getString(R.string.version) + " ${BuildConfig.VERSION_NAME}"
    }

    override fun bindView() {
        binding.viewTop.ivLeft.tap { onBack() }

        binding.tvPolicy.tap { startNextActivity(PolicyActivity::class.java, null) }
    }

}