package com.example.basekotlin.ui.group_share

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.databinding.ActivityGroupShareBinding

class GroupShareActivity : BaseActivity<ActivityGroupShareBinding>(ActivityGroupShareBinding::inflate) {


    override fun initView() {
        Glide.with(this)
            .load(R.drawable.ic_group)
            .placeholder(R.drawable.ic_group)
            .error(R.drawable.ic_group)
            .into(binding.ivGroup)
    }
}