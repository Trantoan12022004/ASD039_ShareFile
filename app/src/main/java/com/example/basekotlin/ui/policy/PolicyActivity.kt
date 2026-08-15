package com.example.basekotlin.ui.policy

import android.annotation.SuppressLint
import android.view.View
import com.example.basekotlin.R
import com.example.basekotlin.ads.IsNetWork
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.inVisible
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityPolicyBinding
import com.example.basekotlin.util.SettingManager

class PolicyActivity : BaseActivity<ActivityPolicyBinding>(ActivityPolicyBinding::inflate) {

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        binding.viewTop.tvToolBar.text = getString(R.string.privacy_policy)
        binding.viewTop.ivRight.inVisible()
        binding.viewTop.ivRight1.inVisible()
        binding.viewTop.ivLeft.visible()

        if (SettingManager.linkPolicy != "" && IsNetWork.haveNetworkConnection(this)) {
            binding.webView.visible()
            binding.lnNoInternet.gone()

            binding.webView.settings.javaScriptEnabled = true
            binding.webView.loadUrl(SettingManager.linkPolicy)
        } else {
            binding.webView.gone()
            binding.lnNoInternet.visible()
        }
    }

    override fun bindView() {
        binding.viewTop.ivLeft.tap { onBack() }
    }

}