package com.example.basekotlin.ui.transfer.scanner

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.basekotlin.base.BaseAdapter
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ItemDeviceBinding
import com.example.basekotlin.ui.transfer.model.DeviceInfo

/**
 * Adapter hiển thị danh sách thiết bị nhận tìm thấy trong mạng nội bộ
 */
class DeviceAdapter(
    private val onSendClick: (DeviceInfo) -> Unit
) : BaseAdapter<DeviceInfo, ItemDeviceBinding>() {

    override fun setBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemDeviceBinding {
        return ItemDeviceBinding.inflate(inflater, parent, false)
    }

    override fun addListData(newList: MutableList<DeviceInfo>) {
        listData.clear()
        listData.addAll(newList)
        notifyDataSetChanged()
    }

    override fun setData(binding: ItemDeviceBinding, item: DeviceInfo, layoutPosition: Int) {
        binding.tvName.text = item.name
        binding.tvInfo.text = "${item.ipAddress}:${item.port}"
        binding.btnInstall.gone()

        binding.btnSend.tap {
            onSendClick(item)
        }
        binding.root.tap {
            onSendClick(item)
        }
    }
}
