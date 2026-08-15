package com.example.basekotlin.ui.language

import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityLanguageBinding
import com.example.basekotlin.ui.language.adapter.LanguageAdapter
import com.example.basekotlin.ui.main.MainActivity
import com.example.basekotlin.util.InsertListManager
import com.example.basekotlin.util.SystemUtil

class LanguageActivity : BaseActivity<ActivityLanguageBinding>(ActivityLanguageBinding::inflate) {

    private var codeLang: String? = null

    override fun initView() {
        codeLang = SystemUtil.getPreLanguage(this)
        binding.viewTop.tvToolBar.text = getString(R.string.language)

        binding.rcvLang.apply {
            adapter = LanguageAdapter { codeLang = it }.apply {
                addListData(InsertListManager.getListLanguage(this@LanguageActivity))
                setCheck(codeLang)
            }

        }
    }

    override fun bindView() {
        binding.viewTop.ivLeft.visible()
        binding.viewTop.ivLeft.tap { onBack() }
        binding.viewTop.ivRight1.gone()
        binding.viewTop.ivRight.setImageResource(R.drawable.ic_check)
        binding.viewTop.ivRight.tap {
            SystemUtil.saveLocale(this, codeLang)

            onNextActivity()
        }
    }

    private fun onNextActivity() {
        startNextActivity(MainActivity::class.java, null)
        finishAffinity()
    }

}
