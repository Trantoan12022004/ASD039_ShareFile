package com.example.basekotlin.ui.group_share

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityGroupShareBinding
import com.example.basekotlin.ui.group_share.dialog.EnterIpAddressDialog
import com.example.basekotlin.ui.group_share.dialog.JoinGroupOptionDialog
import com.example.basekotlin.ui.transfer.scanner.PreparationActivity
import com.example.basekotlin.ui.transfer.scanner.QrScannerActivity
import com.example.basekotlin.util.transfer.NetworkUtils

class GroupShareActivity : BaseActivity<ActivityGroupShareBinding>(ActivityGroupShareBinding::inflate) {

    private val prepLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startGroupShareDetail(isHost = true)
        }
    }

    private val scanQrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val hostIp = result.data?.getStringExtra("EXTRA_HOST_IP")
            val hostPort = result.data?.getIntExtra("EXTRA_HOST_PORT", 8888) ?: 8888
            if (!hostIp.isNullOrEmpty()) {
                startGroupShareDetail(isHost = false, hostIp = hostIp, hostPort = hostPort)
            }
        }
    }

    private fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun initView() {
        Glide.with(this)
            .load(R.drawable.ic_group)
            .placeholder(R.drawable.ic_group)
            .error(R.drawable.ic_group)
            .into(binding.ivGroup)
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            finishThisActivity()
        }

        // Tạo nhóm -> Thiết bị làm HOST
        binding.btnCreateGroup.tap {
            if (!NetworkUtils.isWifiConnected(this) && !isLocationGranted()) {
                prepLauncher.launch(Intent(this, PreparationActivity::class.java))
            } else {
                startGroupShareDetail(isHost = true)
            }
        }

        // Tham gia nhóm
        binding.btnJoinGroup.tap {
            if (!NetworkUtils.isWifiConnected(this)) {
                val intent = Intent(this, QrScannerActivity::class.java).apply {
                    putExtra("EXTRA_IS_JOIN_GROUP", true)
                }
                scanQrLauncher.launch(intent)
            } else {
                showJoinOptionDialog()
            }
        }
    }

    private fun showJoinOptionDialog() {
        JoinGroupOptionDialog(
            context = this,
            onScanQrClick = {
                val intent = Intent(this, QrScannerActivity::class.java).apply {
                    putExtra("EXTRA_IS_JOIN_GROUP", true)
                }
                scanQrLauncher.launch(intent)
            },
            onEnterIpClick = {
                showEnterIpDialog()
            }
        ).show()
    }

    private fun showEnterIpDialog() {
        EnterIpAddressDialog(this) { ip, port ->
            startGroupShareDetail(isHost = false, hostIp = ip, hostPort = port)
        }.show()
    }

    private fun startGroupShareDetail(isHost: Boolean, hostIp: String? = null, hostPort: Int = 8888) {
        val port = if (isHost) 8888 else hostPort
        val intent = Intent(this, GroupShareDetailActivity::class.java).apply {
            putExtra(GroupShareDetailActivity.EXTRA_IS_HOST, isHost)
            putExtra(GroupShareDetailActivity.EXTRA_HOST_IP, hostIp)
            putExtra(GroupShareDetailActivity.EXTRA_HOST_PORT, port)
        }
        startActivity(intent)
    }
}
