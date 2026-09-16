package com.example.basekotlin.ui.transfer.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityPreparationBinding

class PreparationActivity : BaseActivity<ActivityPreparationBinding>(ActivityPreparationBinding::inflate) {

    // Launcher xin quyền vị trí runtime
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkAndRefreshState()
    }

    override fun initView() {
        checkAndRefreshState()
    }

    override fun bindView() {
        binding.btnBackPrep.tap {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.btnOpenWlan.tap {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startActivity(Intent(Settings.Panel.ACTION_WIFI))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            } else {
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
                binding.root.postDelayed({ checkAndRefreshState() }, 600)
            }
        }

        binding.btnOpenGps.tap {
            if (!isLocationGranted()) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                // Đã có quyền runtime nhưng tắt GPS máy -> mở cài đặt Location
                try {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        binding.btnNext.tap {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRefreshState()
    }

    private fun checkAndRefreshState() {
        val wifiOn = isWifiEnabled()
        val locationGranted = isLocationGranted()

        // 1. Kiểm tra trạng thái Wi-Fi
        if (wifiOn) {
            binding.btnOpenWlan.gone()
            binding.ivWlanDone.visible()
        } else {
            binding.btnOpenWlan.visible()
            binding.ivWlanDone.gone()
        }

        // 2. Kiểm tra trạng thái Vị trí
        if (locationGranted) {
            binding.btnOpenGps.gone()
            binding.ivGpsDone.visible()
        } else {
            binding.btnOpenGps.visible()
            binding.ivGpsDone.gone()
        }

        // 3. Nếu cả 2 đã mở thành công -> bỏ disable, đổi bg thành bg_btn_create
        val allReady = wifiOn && locationGranted
        binding.btnNext.isEnabled = allReady
        if (allReady) {
            binding.btnNext.setBackgroundResource(R.drawable.bg_btn_create)
            binding.btnNext.setTextColor(getColor(R.color.white))
        } else {
            binding.btnNext.setBackgroundResource(R.drawable.bg_btn_create_1)
            binding.btnNext.setTextColor(getColor(R.color.white_disable))
        }
    }

    private fun isWifiEnabled(): Boolean {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    private fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
