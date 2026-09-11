package com.example.basekotlin.ui.cleanfile.apps

import androidx.activity.viewModels
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityAppsCleanupBinding
import com.google.android.material.tabs.TabLayoutMediator

class AppsCleanupActivity : BaseActivity<ActivityAppsCleanupBinding>(ActivityAppsCleanupBinding::inflate) {

    private val viewModel: AppsCleanupViewModel by viewModels()

    override fun initView() {
        val pagerAdapter = AppsCleanupPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.installed_apps)
                else -> getString(R.string.apk_package)
            }
        }.attach()

    }

    override fun bindView() {
        binding.btnBack.tap {
            finishThisActivity()
        }
    }
}
