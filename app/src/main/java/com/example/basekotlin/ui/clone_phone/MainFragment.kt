package com.example.basekotlin.ui.clone_phone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.FragmentMainBinding
import com.example.basekotlin.ui.transfer.scanner.QrScannerActivity

class MainFragment : BaseFragment<FragmentMainBinding>() {

    private val viewModel: ClonePhoneViewModel by activityViewModels()

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater ?: LayoutInflater.from(context), container, false)
    }

    override fun bindView() {
        super.bindView()

        // Thiết bị MỚI: Mở QrScannerActivity để quét mã QR từ máy cũ
        binding.btnNew.tap {
            val act = activity as? ClonePhoneActivity ?: return@tap
            val intent = Intent(requireContext(), QrScannerActivity::class.java).apply {
                putExtra("EXTRA_IS_JOIN_GROUP", true)
            }
            act.scanQrLauncher.launch(intent)
        }

        // Thiết bị CŨ: Chuyển sang màn hình chọn dữ liệu cần clone
        binding.btnOld.tap {
            val act = activity as? ClonePhoneActivity ?: return@tap
            act.navigateToPage(ClonePhoneActivity.PAGE_SELECT)
        }
    }
}