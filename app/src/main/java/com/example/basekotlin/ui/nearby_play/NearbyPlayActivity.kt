package com.example.basekotlin.ui.nearby_play

import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityNearbyPlayBinding

class NearbyPlayActivity : BaseActivity<ActivityNearbyPlayBinding>(ActivityNearbyPlayBinding::inflate) {

    override fun initView() {
        super.initView()
        binding.viewTop.btnBack.tap {
            finish()
        }
    }

    override fun bindView() {
        super.bindView()
        // Nhấn vào đây: thiết bị đóng vai trò là thiết bị chứa dữ liệu nhạc
        binding.btnNearbyPlay.tap {
            startNextActivity(FindDeviceActivity::class.java, null)
        }

        // Nhấn vào đây: thiết bị đóng vai trò là thiết bị phát nhạc (Loa)
        binding.btnReceiveMusic.tap {
            startNextActivity(ReceiverActivity::class.java, null)
        }
    }
}