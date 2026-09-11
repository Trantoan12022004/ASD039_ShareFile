package com.example.basekotlin.ui.cleanfile.apps.apkpackage.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.databinding.ItemApkPackageBinding
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApkPackageAdapter : BaseAdapter<AppInfo, ItemApkPackageBinding>() {

    val selectedPaths = mutableSetOf<String>()
    var onSelectToggle: ((AppInfo) -> Unit)? = null
    var onMoreClick: ((AppInfo, View) -> Unit)? = null

    private val adapterScope = CoroutineScope(Dispatchers.Main)
    private val iconCache = mutableMapOf<String, Drawable>()
    private val iconJobByPosition = mutableMapOf<Int, Job>()

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemApkPackageBinding {
        return ItemApkPackageBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<AppInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemApkPackageBinding,
        item: AppInfo,
        layoutPosition: Int
    ) {
        binding.tvApkName.text = item.appName

        val sizeStr = CleanFileUtils.formatFileSize(item.sizeBytes)
        val versionStr = item.versionName.ifEmpty { item.versionCode.toString() }
        binding.tvApkInfo.text = "v$versionStr - $sizeStr"

        val isSelected = selectedPaths.contains(item.apkFilePath)
        binding.cbSelect.setImageResource(
            if (isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
        )

        binding.imgApkIcon.setImageResource(R.drawable.logo_app)
        loadIconAsync(binding, item, layoutPosition)
    }

    override fun onCLick(
        binding: ItemApkPackageBinding,
        item: AppInfo,
        layoutPosition: Int
    ) {
        binding.root.tap {
            toggleSelect(item)
        }

        binding.cbSelect.tap {
            toggleSelect(item)
        }

        binding.btnMore.tap {
            onMoreClick?.invoke(item, binding.btnMore)
        }
    }

    private fun toggleSelect(item: AppInfo) {
        if (selectedPaths.contains(item.apkFilePath)) {
            selectedPaths.remove(item.apkFilePath)
        } else {
            selectedPaths.add(item.apkFilePath)
        }
        notifyDataSetChanged()
        onSelectToggle?.invoke(item)
    }

    fun selectAll(selectAll: Boolean) {
        selectedPaths.clear()
        if (selectAll) {
            selectedPaths.addAll(listData.map { it.apkFilePath })
        }
        notifyDataSetChanged()
    }

    private fun loadIconAsync(
        binding: ItemApkPackageBinding,
        item: AppInfo,
        layoutPosition: Int
    ) {
        val cached = iconCache[item.apkFilePath]
        if (cached != null) {
            binding.imgApkIcon.setImageDrawable(cached)
            return
        }

        iconJobByPosition[layoutPosition]?.cancel()
        val job = adapterScope.launch {
            val icon = withContext(Dispatchers.IO) {
                ApkFileScanner.loadIcon(binding.root.context, item.apkFilePath)
            }
            if (icon != null) {
                iconCache[item.apkFilePath] = icon
                val currentItem = listData.getOrNull(layoutPosition)
                if (currentItem != null && currentItem.apkFilePath == item.apkFilePath) {
                    binding.imgApkIcon.setImageDrawable(icon)
                }
            }
        }
        iconJobByPosition[layoutPosition] = job
    }
}
