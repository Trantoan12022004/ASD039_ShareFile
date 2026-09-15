package com.example.basekotlin.ui.transfer.received

import android.Manifest
import android.content.Context
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

    // 1. Launcher xin quyền Vị trí & Thiết bị lân cận
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            checkGpsAndStart()
        } else {
            Toast.makeText(this, getString(R.string.permission_location_required_receive), Toast.LENGTH_LONG).show()
            binding.pbQrLoading.gone()
            binding.tvIpAddress.text = getString(R.string.location_permission_needed)
        }
    }

    override fun initView() {
        binding.tvDeviceName.text = NetworkUtils.getDeviceName()

        // 2. Kiểm tra quyền trước khi khởi chạy
        checkPermissionsAndStart()
    }

    override fun bindView() {
        binding.btnBack.tap {
            onBack()
        }
    }

    private fun checkPermissionsAndStart() {
        val neededPermissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val allGranted = neededPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            checkGpsAndStart()
        } else {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private fun checkGpsAndStart() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled) {
            Toast.makeText(this, getString(R.string.prompt_turn_on_gps), Toast.LENGTH_SHORT).show()
        }

        setupReceiver()
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

        // 2. Tạo QR Code bất đồng bộ
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
        hotspotManager?.stopHotspot()
        // SỬA LỖI: Hủy TransferService để đóng ServerSocket và giải phóng port khi thoát màn hình
        TransferService.cancel(this)
        super.onDestroy()
    }
}
