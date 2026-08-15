package com.example.basekotlin.ui.files.apps.adapter

import android.graphics.drawable.Drawable
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.data.local.appstore.InstalledAppSource
import com.example.basekotlin.databinding.ItemAppCard1Binding
import com.example.basekotlin.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledCardAdapter : BaseAdapter<AppInfo, ItemAppCard1Binding>() {

    var onUninstallClick: ((AppInfo) -> Unit)? = null
    private val adapterScope = CoroutineScope(Dispatchers.Main)
    private val iconJobByPosition = mutableMapOf<Int, Job>()

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemAppCard1Binding {
        return ItemAppCard1Binding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<AppInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemAppCard1Binding, item: AppInfo, layoutPosition: Int) {

        binding.tvFileName.text = item.appName

        val readableSize = Formatter.formatShortFileSize(binding.root.context, item.sizeBytes)
        val residualDays = countResidualDays(item.firstInstallTimeMillis)
        val infoText = binding.root.context.getString(
            R.string.apk_residual_for_days,
            readableSize,
            residualDays
        )
        binding.tvFileInfo.text = infoText

        // Chỉ set placeholder nếu chưa có icon (tránh nháy khi bind lại)
        if (binding.imgThumbnail.drawable == null) {
            binding.imgThumbnail.setImageResource(R.drawable.ic_audio)
        }
        loadIconAsync(binding, item, layoutPosition)
    }

    override fun onCLick(binding: ItemAppCard1Binding, item: AppInfo, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.btnUninstall.tap {
            val listener = onUninstallClick
            if (listener != null) {
                listener(item)
            }
        }
    }

    private fun countResidualDays(fileTimeMillis: Long): Int {
        if (fileTimeMillis <= 0L) {
            return 0
        }

        val nowMillis = System.currentTimeMillis()
        var ageMillis = nowMillis - fileTimeMillis
        if (ageMillis < 0L) {
            ageMillis = 0L
        }

        val oneDayMillis = 24L * 60L * 60L * 1000L
        val days = ageMillis / oneDayMillis
        return days.toInt()
    }
    private val iconCache = mutableMapOf<String, Drawable>()

    private fun loadIconAsync(binding: ItemAppCard1Binding, item: AppInfo, layoutPosition: Int) {
        val cachedIcon = iconCache[item.packageName]
        if (cachedIcon != null) {
            binding.imgThumbnail.setImageDrawable(cachedIcon)
            return
        }
        val newJob = adapterScope.launch {
            val icon = withContext(Dispatchers.IO) {
                InstalledAppSource.loadIcon(binding.root.context, item.packageName) // dùng packageManager.getApplicationIcon, có cache sẵn của hệ thống, nhanh hơn getPackageArchiveInfo
            }
            if (icon != null) {
                iconCache[item.packageName] = icon
            }
            val currentItem = listData.getOrNull(layoutPosition)
            if (currentItem != null && currentItem.apkFilePath == item.apkFilePath && icon != null) {
                binding.imgThumbnail.setImageDrawable(icon)
            }
        }
        iconJobByPosition[layoutPosition] = newJob
    }
}