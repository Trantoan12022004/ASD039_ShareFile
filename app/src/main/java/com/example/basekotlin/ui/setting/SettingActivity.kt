package com.example.basekotlin.ui.setting

import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivitySettingsBinding
import com.example.basekotlin.ui.about.AboutActivity
import com.example.basekotlin.ui.language.LanguageActivity
import com.example.basekotlin.util.InsertListManager
import com.example.basekotlin.util.SharedPreUtils
import com.example.basekotlin.util.SystemUtil
import com.example.basekotlin.util.feedbackApp
import com.example.basekotlin.util.rateApp
import com.example.basekotlin.util.shareApp

class SettingActivity : BaseActivity<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {


    override fun getData() {
        val codeLang = SystemUtil.getPreLanguage(this)
        binding.tvLang.text = InsertListManager.getListLanguage(this@SettingActivity).find { it.code == codeLang }?.name ?: ""

        if (SharedPreUtils.getInstance().isRated(this)) {
            binding.btnRateUs.gone()
        }
    }

    override fun initView() {
        binding.viewTop.tvToolBar.text = getString(R.string.settings)
        // binding.viewTop.tvToolBar.isAllCaps = true
        binding.viewTop.ivRight.gone()
        binding.viewTop.ivRight1.gone()
        binding.viewTop.ivLeft.visible()

    }

    override fun bindView() {

        binding.btnLanguage.tap { startNextActivity(LanguageActivity::class.java, null) }

        binding.btnShare.tap { shareApp() }

        binding.btnRateUs.tap { rateApp(binding.btnRateUs) }
        binding.viewTop.ivLeft.tap{
            onBack()
        }

        binding.btnAbout.tap { startNextActivity(AboutActivity::class.java, null) }
    }


}