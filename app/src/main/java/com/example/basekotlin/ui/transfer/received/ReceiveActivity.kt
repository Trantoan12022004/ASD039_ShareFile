package com.example.basekotlin.ui.transfer.received

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityReceiveBinding
import com.example.basekotlin.service.transfer.HotspotManager
import com.example.basekotlin.service.transfer.TransferService
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.progress.ProgressActivity
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.QrCodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiveActivity : BaseActivity<ActivityReceiveBinding>(ActivityReceiveBinding::inflate) {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"
    }

    private var port = 8888
    private var hotspotManager: HotspotManager? = null
    private var lanDiscoveryHelper: com.example.basekotlin.util.transfer.LanDiscoveryHelper? = null



    override fun initView() {
        binding.tvDeviceName.text = NetworkUtils.getDeviceName()

        setupReceiver()
    }

    private var isNavigatingToProgress = false

    override fun bindView() {
        binding.btnBack.tap {
            onBack()
        }

        // Lắng nghe khi client kết nối vào server -> Tự động chuyển sang ProgressActivity
        lifecycleScope.launch {
            TransferService.isConnected.collect { connected ->
                if (connected) {
                    isNavigatingToProgress = true
                    val intent = Intent(this@ReceiveActivity, ProgressActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }





    private fun setupReceiver() {
        binding.pbQrLoading.visible()
        binding.ivQrCode.gone()

        // SỬA LỖI EADDRINUSE: Lấy port trống khả dụng từ hệ điều hành
        port = NetworkUtils.getAvailablePort()

        val isConnectedWifi = NetworkUtils.isWifiConnected(this)
        android.util.Log.d(TAG, "[RECEIVER] setupReceiver -> isConnectedWifi = $isConnectedWifi | Port = $port")

        if (isConnectedWifi) {
            // TRƯỜNG HỢP 1: CÓ WIFI SẴN
            val wifiName = NetworkUtils.getConnectedWifiName(this)
            val localIp = NetworkUtils.getWifiIpAddress()
            binding.tvWifiNetwork.text = wifiName
            binding.tvIpAddress.text = localIp

            android.util.Log.d(TAG, "[RECEIVER] Chế độ chung Wi-Fi: $wifiName | IP: $localIp | Port: $port")

            startServerAndGenerateQr(
                ConnectionInfo(
                    ssid = wifiName,
                    password = "",
                    ipAddress = localIp,
                    port = port,
                    deviceName = NetworkUtils.getDeviceName()
                )
            )
        } else {
            // TRƯỜNG HỢP 2: NO WIFI -> BẬT HOTSPOT CỤC BỘ
            binding.tvWifiNetwork.text = "---"
            android.util.Log.d(TAG, "[RECEIVER] Chế độ NO WIFI -> Bắt đầu khởi tạo HotspotManager với Port = $port")
            hotspotManager = HotspotManager(this)
            hotspotManager?.startHotspot(
                port = port,
                listener = object : HotspotManager.HotspotListener {
                    override fun onHotspotStarted(connectionInfo: ConnectionInfo) {
                        runOnUiThread {
                            binding.tvIpAddress.text = connectionInfo.ipAddress
                            android.util.Log.d(TAG, "[RECEIVER] Hotspot đã sẵn sàng -> Sinh mã QR với thông tin: ${connectionInfo.ssid} - ${connectionInfo.ipAddress}:${connectionInfo.port}")
                            startServerAndGenerateQr(connectionInfo)
                        }
                    }

                    override fun onHotspotFailed(errorCode: Int) {
                        runOnUiThread {
                            binding.pbQrLoading.gone()
                            binding.tvIpAddress.text = getString(R.string.error_hotspot_failed_code, errorCode)
                            android.util.Log.e(TAG, "[RECEIVER] Lỗi bật Hotspot: $errorCode")
                            Toast.makeText(this@ReceiveActivity, getString(R.string.error_hotspot_failed_code, errorCode), Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onHotspotStopped() {
                        android.util.Log.d(TAG, "[RECEIVER] Hotspot đã dừng")
                    }
                }
            )
        }
    }

    private fun startServerAndGenerateQr(info: ConnectionInfo) {
        // 1. Khởi chạy Server Socket nhận file ở background
        android.util.Log.d(TAG, "[RECEIVER] Khởi động TransferService.startReceive(port = ${info.port})")
        TransferService.startReceive(this, port = info.port)

        // 2. Bắt đầu phát beacon tìm kiếm thiết bị trên mạng LAN
        lanDiscoveryHelper = com.example.basekotlin.util.transfer.LanDiscoveryHelper(this)
        lanDiscoveryHelper?.startBroadcasting(
            deviceName = info.deviceName,
            ipAddress = info.ipAddress,
            port = info.port
        )

        // 3. Tạo QR Code bất đồng bộ
        lifecycleScope.launch(Dispatchers.IO) {
            val qrBitmap = QrCodeHelper.generateQrBitmap(info, size = 600)
            withContext(Dispatchers.Main) {
                binding.pbQrLoading.gone()
                binding.ivQrCode.visible()
                binding.ivQrCode.setImageBitmap(qrBitmap)
                android.util.Log.d(TAG, "[RECEIVER] Đã hiển thị QR Code lên màn hình thành công")
            }
        }
    }

    override fun onDestroy() {
        lanDiscoveryHelper?.release()
        if (!isNavigatingToProgress) {
            hotspotManager?.stopHotspot()
            TransferService.cancel(this)
        }
        super.onDestroy()
    }
}
