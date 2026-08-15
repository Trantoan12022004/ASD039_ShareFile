package com.example.basekotlin.ui.welcome

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityWelcomeBinding

class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate) {

    override fun initView() {
        // Có thể thêm hiệu ứng animation cho icon kim cương ở đây nếu muốn
    }

    override fun bindView() {
        binding.tvContinue.tap {
            finishThisActivity()
        }
    }

    override fun onBack() {
        finishAffinity()
    }
}