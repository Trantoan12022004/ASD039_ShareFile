package com.example.basekotlin.ui.cleanfile.apps.installed.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.tap
import com.example.basekotlin.data.local.appstore.InstalledAppSource
import com.example.basekotlin.databinding.ItemInstalledAppBinding
import com.example.basekotlin.model.AppInfo
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsAdapter : BaseAdapter<AppInfo, ItemInstalledAppBinding>() {

    var onMoreClick: ((AppInfo, View) -> Unit)? = null

    private val adapterScope = CoroutineScope(Dispatchers.Main)
    private val iconCache = mutableMapOf<String, Drawable>()
    private val iconJobByPosition = mutableMapOf<Int, Job>()

    override fun setBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemInstalledAppBinding {
        return ItemInstalledAppBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<AppInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(
        binding: ItemInstalledAppBinding,
        item: AppInfo,
        layoutPosition: Int
    ) {
        binding.tvAppName.text = item.appName

        val sizeStr = CleanFileUtils.formatFileSize(item.sizeBytes)
        val versionStr = item.versionName.ifEmpty { item.versionCode.toString() }
        binding.tvAppInfo.text = "v$versionStr - $sizeStr"

        binding.imgAppIcon.setImageResource(R.drawable.logo_app)
        loadIconAsync(binding, item, layoutPosition)
    }

    override fun onCLick(
        binding: ItemInstalledAppBinding,
        item: AppInfo,
        layoutPosition: Int
    ) {
        binding.btnMore.tap {
            onMoreClick?.invoke(item, binding.btnMore)
        }
    }

    private fun loadIconAsync(
        binding: ItemInstalledAppBinding,
        item: AppInfo,
        layoutPosition: Int
    ) {
        val cached = iconCache[item.packageName]
        if (cached != null) {
            binding.imgAppIcon.setImageDrawable(cached)
            return
        }

        iconJobByPosition[layoutPosition]?.cancel()
        val job = adapterScope.launch {
            val icon = withContext(Dispatchers.IO) {
                InstalledAppSource.loadIcon(binding.root.context, item.packageName)
            }
            if (icon != null) {
                iconCache[item.packageName] = icon
                val currentItem = listData.getOrNull(layoutPosition)
                if (currentItem != null && currentItem.packageName == item.packageName) {
                    binding.imgAppIcon.setImageDrawable(icon)
                }
            }
        }
        iconJobByPosition[layoutPosition] = job
    }
}
