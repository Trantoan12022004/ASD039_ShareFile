package com.example.basekotlin.ui.language

import android.view.Gravity
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.setTextColor
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityLanguageStartBinding
import com.example.basekotlin.ui.intro.IntroActivity
import com.example.basekotlin.ui.language.adapter.LanguageAdapter
import com.example.basekotlin.util.InsertListManager
import com.example.basekotlin.util.SystemUtil

class LanguageStartActivity :
    BaseActivity<ActivityLanguageStartBinding>(ActivityLanguageStartBinding::inflate) {

    private var codeLang: String? = null
    private var toast: Toast? = null

    override fun getData() {
        super.getData()

        codeLang = SystemUtil.getPreLanguage(this)
    }

    override fun initView() {
        binding.viewTop.apply {
            tvToolBar.text = getString(R.string.language)
//            setTextColor(tvToolBar, "#FFFFF")
            tvToolBar.gravity = Gravity.START
            ivRight.visible()
        }


        binding.rcvLangStart.apply {
            adapter = LanguageAdapter {
                codeLang = it
            }.apply {
                addListData(InsertListManager.getListLanguage(this@LanguageStartActivity))
                if (codeLang != ""){
                    setCheck(codeLang)
                }
            }
        }
    }

    override fun bindView() {

        binding.viewTop.ivRight.tap {
            SystemUtil.saveLocale(this, codeLang)

            if (codeLang != "") {
                SystemUtil.saveLocale(this, codeLang)
                startNextActivity()
            } else {
                if (toast != null) toast!!.cancel()
                toast = Toast.makeText(
                    this, getString(R.string.please_choose_a_language), Toast.LENGTH_SHORT
                )
                toast!!.show()
            }
        }
    }

    private fun startNextActivity() {
        startNextActivity(IntroActivity::class.java, null)
        finish()
    }

    override fun onBack() {
        finishAffinity()
    }
}
