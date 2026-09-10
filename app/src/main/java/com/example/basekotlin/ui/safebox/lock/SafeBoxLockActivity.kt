package com.example.basekotlin.ui.safebox.lock

import android.content.Intent
import android.graphics.Color
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivitySafeBoxLockBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.example.basekotlin.ui.safebox.home.SafeBoxHomeActivity

class SafeBoxLockActivity : BaseActivity<ActivitySafeBoxLockBinding>(ActivitySafeBoxLockBinding::inflate) {

    companion object {
        const val EXTRA_FINISH_ON_SETUP = "EXTRA_FINISH_ON_SETUP"
    }
    private val viewModel: SafeBoxLockViewModel by viewModels()

    override fun initView() {
        // Cấu hình Toolbar
        // Lắng nghe sự kiện vẽ pattern từ Custom View
        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {
            override fun onPatternCompleted(pattern: List<Int>) {
                viewModel.onPatternCompleted(pattern)
            }
        })
    }

    override fun bindView() {
        // Nút Back trên Toolbar
        binding.viewTop.btnBack.tap {
            onBack()
        }

        // Nút Redraw
        binding.btnRedraw.tap {
            binding.patternLockView.clearPattern()
            viewModel.onRedrawClicked()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát thay đổi UI State
                launch {
                    viewModel.uiState.collect { state ->
                        // Cập nhật text hướng dẫn
                        binding.tvDesc.setText(state.hintResId)
                        if (state.isError) {
                            binding.tvDesc.setTextColor("#EF4444".toColorInt())
                        } else {
                            binding.tvDesc.setTextColor(Color.BLACK)
                        }

                        if (state.mode == LockMode.SETUP_NEW){
                            binding.tvTitle.setText(R.string.create_pattern)
                            binding.tvDesc.setText(R.string.pattern_too_short)
                        } else if (state.mode == LockMode.CONFIRM_NEW){
                            binding.tvTitle.setText(R.string.confirm_lock_pattern)
                        } else {
                            binding.tvTitle.setText(R.string.draw_pattern_to_lock)
                        }

                        // Cập nhật trạng thái hiển thị của PatternLockView
                        binding.patternLockView.setPatternState(state.patternViewState)
                        binding.patternLockView.setInputEnabled(state.isInputEnabled)

                        // Ẩn/Hiện nút Redraw
                        if (state.showRedrawButton) {
                            binding.btnRedraw.visible()
                        } else {
                            binding.btnRedraw.gone()
                        }
                    }
                }

                // Lắng nghe sự kiện chuyển màn hình khi mở khóa thành công
                launch {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is LockNavigationEvent.NavigateToHome -> {
                                openSafeBoxHome()
                            }
                            is LockNavigationEvent.ClearPattern -> {
                                binding.patternLockView.clearPattern()
                            }
                        }
                    }
                }

            }
        }
    }

    private fun openSafeBoxHome() {
        if (intent.getBooleanExtra(EXTRA_FINISH_ON_SETUP, false)) {
            finishThisActivity()
            return
        }
        val intent = Intent(this, SafeBoxHomeActivity::class.java)
        startActivity(intent)
        finishThisActivity()
    }

}
