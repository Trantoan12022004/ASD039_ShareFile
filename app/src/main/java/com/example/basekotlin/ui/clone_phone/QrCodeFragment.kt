package com.example.basekotlin.ui.clone_phone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.FragmentQrCodeBinding
import com.example.basekotlin.util.transfer.NetworkUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QrCodeFragment : BaseFragment<FragmentQrCodeBinding>() {

    private val viewModel: ClonePhoneViewModel by activityViewModels()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentQrCodeBinding {
        return FragmentQrCodeBinding.inflate(inflater ?: LayoutInflater.from(context), container, false)
    }

    override fun bindView() {
        super.bindView()
        observeData()
    }

    private fun observeData() {
        // Quan sát bitmap mã QR
        lifecycleScope.launch {
            viewModel.qrBitmap.collectLatest { bitmap ->
                if (bitmap != null) {
                    binding.ivQr.setImageBitmap(bitmap)
                }
            }
        }

        // Quan sát thông tin kết nối để ẩn/hiện IP
        lifecycleScope.launch {
            viewModel.connectionInfo.collectLatest { info ->
                val ip = viewModel.hostIp.value
                val isHotspot = info?.password?.isNotEmpty() == true || !NetworkUtils.isWifiConnected(requireContext())

                if (isHotspot) {
                    // Khi truyền bằng Hotspot: ẩn layout IP không cho kết nối bằng IP
                    binding.layoutIp.gone()
                } else {
                    // Khi cùng mạng Wi-Fi: hiển thị IP của máy cũ
                    binding.layoutIp.visible()
                    binding.tvIpAddress.text = ip
                }
            }
        }
    }
}