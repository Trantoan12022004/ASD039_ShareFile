package com.example.basekotlin.ui.cleanfile.apps.dialog

import android.content.Context
import com.example.basekotlin.base.BaseDialog
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.DialogDetailInformationApkBinding
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.cleanfile.CleanFileUtils

class DetailInformationApkDialog(
    context: Context,
    private val appInfo: AppInfo
) : BaseDialog<DialogDetailInformationApkBinding>(context, true) {

    override fun setBinding(): DialogDetailInformationApkBinding {
        return DialogDetailInformationApkBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.tvValueName.text = appInfo.appName
        binding.tvValuePackage.text = appInfo.packageName
        binding.tvValueVersion.text = appInfo.versionName.ifEmpty { appInfo.versionCode.toString() }
        binding.tvValueSize.text = CleanFileUtils.formatFileSize(appInfo.sizeBytes)
        binding.tvValuePath.text = appInfo.apkFilePath
    }

    override fun bindView() {
        binding.btnGotIt.tap {
            dismiss()
        }
    }
}
