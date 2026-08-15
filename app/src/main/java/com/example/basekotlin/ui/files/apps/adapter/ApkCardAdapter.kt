package com.example.basekotlin.ui.files.apps.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.appstore.ApkFileScanner
import com.example.basekotlin.databinding.ItemAppCardBinding
import com.example.basekotlin.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApkCardAdapter : BaseAdapter<AppInfo, ItemAppCardBinding>() {

    var onInstallClick: ((AppInfo) -> Unit)? = null
    var onMoreClick: ((AppInfo, View) -> Unit)? = null

    var onSelectToggle: ((AppInfo) -> Unit)? = null
    var isSelectionMode: Boolean = false
    var selectedApks: Set<String> = emptySet()

    // Scope riêng để hủy job load icon khi item bị recycle, tránh set nhầm icon cho item khác
    private val adapterScope = CoroutineScope(Dispatchers.Main)
    private val iconJobByPosition = mutableMapOf<Int, Job>()

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemAppCardBinding {
        return ItemAppCardBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<AppInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemAppCardBinding, item: AppInfo, layoutPosition: Int) {
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
        bindSelectionUi(binding, item)
        // Item đã cài đặt trên máy -> ẩn nút Cài đặt, chỉ để nút More
        if (item.isCurrentlyInstalled) {
            binding.btnInstall.gone()
        } else {
            binding.btnInstall.visible()
        }

        if (isSelectionMode) {
            binding.imgCheckbox.visibility = View.VISIBLE
            binding.btnMore.visibility = View.GONE
        } else {
            binding.imgCheckbox.visibility = View.GONE
            binding.btnMore.visibility = View.VISIBLE
        }

        val isChecked = selectedApks.contains(item.apkFilePath)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    override fun onCLick(binding: ItemAppCardBinding, item: AppInfo, layoutPosition: Int) {
        super.onCLick(binding, item, layoutPosition)

        binding.btnInstall.tap {
            val listener = onInstallClick
            if (listener != null) {
                listener(item)
            }
        }

        binding.btnMore.tap {
            val listener = onMoreClick
            if (listener != null) {
                listener(item, binding.btnMore)
            }
        }

        binding.imgCheckbox.tap {
            val toggleListener = onSelectToggle
            if (toggleListener != null) {
                toggleListener(item)
            }
        }
        binding.root.setOnLongClickListener {
            if (isSelectionMode == false) {
                val toggleListener = onSelectToggle
                if (toggleListener != null) {
                    toggleListener(item)
                }
            }
            true
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

    private fun bindSelectionUi(binding: ItemAppCardBinding, item: AppInfo) {
        if (isSelectionMode) {
            binding.imgCheckbox.visible()
            binding.btnMore.gone()
            binding.btnInstall.gone()
        } else {
            binding.imgCheckbox.gone()
            binding.btnMore.visible()
            if (item.isCurrentlyInstalled) {
                binding.btnInstall.gone()
            } else {
                binding.btnInstall.visible()
            }
        }
        val isChecked = selectedApks.contains(item.apkFilePath)
        if (isChecked) {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_checked)
        } else {
            binding.imgCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked)
        }
    }

    private fun loadIconAsync(binding: ItemAppCardBinding, item: AppInfo, layoutPosition: Int) {
        val previousJob = iconJobByPosition[layoutPosition]
        if (previousJob != null) {
            previousJob.cancel()
        }

        val newJob = adapterScope.launch {
            val icon = withContext(Dispatchers.IO) {
                ApkFileScanner.loadIcon(binding.root.context, item.apkFilePath)
            }
            // Kiểm tra item ở vị trí này còn đúng file cũ không, tránh set nhầm do RecyclerView tái sử dụng view
            val currentItem = listData.getOrNull(layoutPosition)
            if (currentItem != null && currentItem.apkFilePath == item.apkFilePath && icon != null) {
                binding.imgThumbnail.setImageDrawable(icon)
            }
        }
        iconJobByPosition[layoutPosition] = newJob
    }
}