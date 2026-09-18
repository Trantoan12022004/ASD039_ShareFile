package com.example.basekotlin.ui.clone_phone

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityClonePhoneBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClonePhoneActivity : BaseActivity<ActivityClonePhoneBinding>(ActivityClonePhoneBinding::inflate) {

    companion object {
        const val PAGE_MAIN = 0
        const val PAGE_SELECT = 1
        const val PAGE_QR = 2
        const val PAGE_CONNECT = 3
    }

    val viewModel: ClonePhoneViewModel by viewModels()

    // Launcher quét mã QR cho Máy Mới
    val scanQrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val hostIp = result.data?.getStringExtra("EXTRA_HOST_IP")
            val hostPort = result.data?.getIntExtra("EXTRA_HOST_PORT", 8889) ?: 8889
            if (!hostIp.isNullOrEmpty()) {
                // Máy mới kết nối tới Máy cũ
                viewModel.startClient(this, hostIp, hostPort)
                navigateToPage(PAGE_CONNECT)
            }
        }
    }

    override fun initView() {
        super.initView()
        viewModel.initService(this)

        setupViewPager()
    }

    private fun setupViewPager() {
        // Vô hiệu hóa vuốt thủ công để điều hướng tuần tự
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    PAGE_MAIN -> MainFragment()
                    PAGE_SELECT -> SelectDataFragment()
                    PAGE_QR -> QrCodeFragment()
                    PAGE_CONNECT -> ConnectFragment()
                    else -> MainFragment()
                }
            }
        }
        updateDots(PAGE_MAIN)
    }

    override fun bindView() {
        super.bindView()

        // Nút Back trên Toolbar
        binding.viewTop.btnBack.tap {
            handleBackNavigation()
        }

        // Tự động chuyển sang ConnectFragment khi 2 máy kết nối socket thành công
        lifecycleScope.launch {
            viewModel.isConnected.collectLatest { connected ->
                if (connected && binding.viewPager.currentItem != PAGE_CONNECT) {
                    navigateToPage(PAGE_CONNECT)
                }
            }
        }
    }

    fun navigateToPage(pageIndex: Int) {
        binding.viewPager.setCurrentItem(pageIndex, false)
        updateDots(pageIndex)
    }

    private fun updateDots(pageIndex: Int) {
        binding.viewTop.dot1.setImageResource(if (pageIndex == PAGE_MAIN) R.drawable.active_dot else R.drawable.inactive_dot)
        binding.viewTop.dot2.setImageResource(if (pageIndex == PAGE_SELECT) R.drawable.active_dot else R.drawable.inactive_dot)
        binding.viewTop.dot3.setImageResource(if (pageIndex == PAGE_QR) R.drawable.active_dot else R.drawable.inactive_dot)
        binding.viewTop.dot4.setImageResource(if (pageIndex == PAGE_CONNECT) R.drawable.active_dot else R.drawable.inactive_dot)
    }

    fun showCancelConfirmDialog(onConfirm: () -> Unit) {
        com.example.basekotlin.dialog.common.ConfirmActionDialog(
            context = this,
            title = getString(R.string.dialog_cancel_clone_title),
            message = getString(R.string.dialog_cancel_clone_msg),
            positiveText = getString(R.string.stop),
            onConfirm = onConfirm
        ).show()
    }

    override fun onBack() {
        handleBackNavigation()
    }

    private fun handleBackNavigation() {
        when (binding.viewPager.currentItem) {
            PAGE_MAIN -> finishThisActivity()
            PAGE_SELECT -> navigateToPage(PAGE_MAIN)
            PAGE_QR -> {
                showCancelConfirmDialog {
                    viewModel.cancelClone(this)
                    navigateToPage(PAGE_SELECT)
                }
            }
            PAGE_CONNECT -> {
                if (viewModel.overallState.value.isDone) {
                    viewModel.resetState(this)
                    navigateToPage(PAGE_MAIN)
                } else {
                    showCancelConfirmDialog {
                        viewModel.cancelClone(this)
                        finishThisActivity()
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackNavigation()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService(this)
    }
}