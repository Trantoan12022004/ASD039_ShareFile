package com.example.basekotlin.ui.transfer.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityQrScannerBinding
import com.example.basekotlin.service.transfer.TransferService
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.QrCodeHelper
import com.example.basekotlin.util.transfer.WifiHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import com.example.basekotlin.ui.transfer.progress.ProgressActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class QrScannerActivity : BaseActivity<ActivityQrScannerBinding>(ActivityQrScannerBinding::inflate) {

    companion object {
        private const val TAG = "DEBUG_SEND_FILE"
    }

    private var filesToSend: ArrayList<TransferFile> = arrayListOf()
    private var cameraControl: CameraControl? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private var wifiHelper: WifiHelper? = null

    // SỬA LỖI: AtomicBoolean khóa scan tức thì, chỉ xử lý đúng 1 QR duy nhất
    private val isScanningLocked = AtomicBoolean(false)
    private var isTransferStarted = false

    private var lanDiscoveryHelper: com.example.basekotlin.util.transfer.LanDiscoveryHelper? = null
    private lateinit var deviceAdapter: DeviceAdapter
    private val foundDevices = mutableListOf<com.example.basekotlin.ui.transfer.model.DeviceInfo>()

    private val prepLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK || (isWifiEnabled() && isLocationGranted())) {
            // Đã cấp quyền và bật Wi-Fi xong -> Khởi động Camera
            checkCameraAndStart()
        } else {
            // Người dùng back ra mà chưa cấp đủ -> Thoát về SendFileActivity
            finish()
        }
    }


    // 1. Tối ưu: Khởi tạo 1 scanner duy nhất, chuyên quét QR Code
    private val barcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.error_permission_required, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun getData() {
        if (intent.getBooleanExtra("EXTRA_IS_JOIN_GROUP", false)) {
            return
        }
        @Suppress("DEPRECATION")
        filesToSend = intent.getParcelableArrayListExtra("EXTRA_FILES") ?: arrayListOf()
        if (filesToSend.isEmpty()) {
            finish()
            return
        }
    }

    override fun initView() {
        if (intent.getBooleanExtra("EXTRA_IS_JOIN_GROUP", false)) {
            binding.layoutToolbar.tvFileCount.text = getString(R.string.join_group)
        } else {
            binding.layoutToolbar.tvFileCount.text = "${filesToSend.size} File(s)"
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        wifiHelper = WifiHelper(this)

        setupDeviceRecyclerView()
        startLanDeviceDiscovery()

        // Kiểm tra 2 điều kiện: 1 là quyền vị trí, 2 là đã mở wifi hay chưa
        if (!isLocationGranted() || !isWifiEnabled()) {
            prepLauncher.launch(Intent(this, PreparationActivity::class.java))
        } else {
            checkCameraAndStart()
        }
    }

    override fun bindView() {
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
    }

    private fun setupDeviceRecyclerView() {
        deviceAdapter = DeviceAdapter { selectedDevice ->
            connectToDiscoveredDevice(selectedDevice)
        }
        binding.rvDevices.adapter = deviceAdapter

        binding.btnSendAll.tap {
            if (foundDevices.isNotEmpty()) {
                connectToDiscoveredDevice(foundDevices.first())
            }
        }
    }

    private fun startLanDeviceDiscovery() {
        // Trạng thái ban đầu khi đang tìm kiếm
        binding.etSearch.visible()
        binding.pbSearching.visible()
        binding.layoutDeviceCount.gone()
        binding.rvDevices.gone()

        lanDiscoveryHelper = com.example.basekotlin.util.transfer.LanDiscoveryHelper(this)
        lanDiscoveryHelper?.startDiscovery { device ->
            runOnUiThread {
                val cleanIp = com.example.basekotlin.util.transfer.LanDiscoveryHelper.normalizeIp(device.ipAddress)
                val cleanDevice = device.copy(ipAddress = cleanIp)

                // Kiểm tra trùng lặp theo cả tên thiết bị hoặc IP
                val isDuplicate = foundDevices.any { existing ->
                    existing.name.equals(cleanDevice.name, ignoreCase = true) || existing.ipAddress == cleanDevice.ipAddress
                }

                if (!isDuplicate) {
                    foundDevices.add(cleanDevice)

                    // Ẩn tiêu đề "Search for devices" (et_search) và vòng xoay loading khi đã tra xong / có thiết bị
                    binding.etSearch.gone()
                    binding.pbSearching.gone()

                    // Hiển thị cụm đếm số thiết bị và danh sách kết quả
                    binding.layoutDeviceCount.visible()
                    binding.rvDevices.visible()
                    binding.tvCount.text = "${foundDevices.size}"
                    deviceAdapter.addListData(foundDevices)
                }
            }
        }
    }

    private fun connectToDiscoveredDevice(device: com.example.basekotlin.ui.transfer.model.DeviceInfo) {
        if (isTransferStarted) return
        isScanningLocked.set(true)
        imageAnalyzer?.clearAnalyzer()
        lanDiscoveryHelper?.stopDiscovery()

        vibratePhone()

        val connectionInfo = ConnectionInfo(
            ssid = "",
            password = "",
            ipAddress = device.ipAddress,
            port = device.port,
            deviceName = device.name
        )

        binding.layoutConnecting.visible()
        binding.tvConnecting.text = getString(
            R.string.connection_connecting, device.name
        )
        connectToReceiver(connectionInfo)
    }

    private fun isWifiEnabled(): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }
    private fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun checkCameraAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                val camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                cameraControl = camera?.cameraControl
            } catch (e: Exception) {
                Log.e(TAG, "[SENDER] Lỗi bind Camera", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        // Nếu đã khóa scan thì đóng frame ngay lập tức
        if (isScanningLocked.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage, imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (isScanningLocked.get()) return@addOnSuccessListener
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    val connectionInfo = QrCodeHelper.parseQrContent(rawValue)
                    if (connectionInfo != null) {
                        // Khóa nguyên tử: Chỉ có đúng 1 frame được lọt qua!
                        if (isScanningLocked.compareAndSet(false, true)) {
                            Log.d(TAG, "[SENDER] [QR] Quét thấy mã QR hợp lệ: $rawValue")
                            onQrCodeDetected(connectionInfo)
                            break
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[SENDER] [QR] Lỗi quét barcode", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun onQrCodeDetected(connectionInfo: ConnectionInfo) {
        vibratePhone()

        val isJoinGroup = intent.getBooleanExtra("EXTRA_IS_JOIN_GROUP", false)
        val currentWifi = NetworkUtils.getConnectedWifiName(this)
        val isAlreadySameWifi = connectionInfo.password.isEmpty() ||
                (NetworkUtils.isWifiConnected(this) && currentWifi.equals(connectionInfo.ssid, ignoreCase = true))

        if (isJoinGroup) {
            runOnUiThread {
                imageAnalyzer?.clearAnalyzer()
                if (isAlreadySameWifi) {
                    // Đã cùng mạng Wi-Fi
                    finishWithJoinResult(connectionInfo.ipAddress, connectionInfo.port, connectionInfo.deviceName)
                } else {
                    // Host đang phát Hotspot -> Kết nối vào Hotspot trước khi vào nhóm
                    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (wifiManager?.isWifiEnabled == false) {
                        binding.layoutConnecting.gone()
                        Toast.makeText(this, getString(R.string.prompt_turn_on_wifi_to_connect), Toast.LENGTH_LONG).show()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try { startActivity(Intent(Settings.Panel.ACTION_WIFI)) } catch (_: Exception) {}
                        }
                        binding.root.postDelayed({ resetScanning() }, 2500)
                        return@runOnUiThread
                    }

                    binding.layoutConnecting.visible()
                    binding.tvConnecting.text = getString(R.string.connection_connecting, connectionInfo.deviceName)

                    wifiHelper?.connectToWifi(
                        ssid = connectionInfo.ssid,
                        password = connectionInfo.password,
                        listener = object : WifiHelper.WifiConnectionListener {
                            override fun onConnected(network: android.net.Network) {
                                Log.d(TAG, "[MEMBER] Đã kết nối vào Hotspot của Host: ${connectionInfo.ssid}")
                                isTransferStarted = true
                                val actualIp = if (connectionInfo.password.isNotEmpty()) {
                                    WifiHelper.activeGatewayIp ?: connectionInfo.ipAddress
                                } else {
                                    connectionInfo.ipAddress
                                }
                                runOnUiThread {
                                    binding.layoutConnecting.gone()
                                    finishWithJoinResult(actualIp, connectionInfo.port, connectionInfo.deviceName)
                                }
                            }

                            override fun onDisconnected() {
                                Log.d(TAG, "[MEMBER] Mất kết nối tới Hotspot")
                                runOnUiThread {
                                    binding.layoutConnecting.gone()
                                    Toast.makeText(this@QrScannerActivity, R.string.connection_failed_message, Toast.LENGTH_SHORT).show()
                                    binding.root.postDelayed({ resetScanning() }, 1500)
                                }
                            }

                            override fun onFailed() {
                                Log.e(TAG, "[MEMBER] Kết nối Hotspot thất bại")
                                runOnUiThread {
                                    binding.layoutConnecting.gone()
                                    Toast.makeText(this@QrScannerActivity, R.string.connection_failed, Toast.LENGTH_SHORT).show()
                                    binding.root.postDelayed({ resetScanning() }, 1500)
                                }
                            }
                        }
                    )
                }
            }
            return
        }

        runOnUiThread {
            // Dừng analyzer để tránh hao pin, CPU và ngăn quét lặp
            imageAnalyzer?.clearAnalyzer()
            Toast.makeText(this, R.string.qr_scan_success, Toast.LENGTH_SHORT).show()
            binding.layoutConnecting.visible()
            binding.tvConnecting.text = getString(
                R.string.connection_connecting, connectionInfo.deviceName
            )
            connectToReceiver(connectionInfo)
        }
    }

    private fun finishWithJoinResult(ip: String, port: Int, deviceName: String) {
        val resultIntent = Intent().apply {
            putExtra("EXTRA_HOST_IP", ip)
            putExtra("EXTRA_HOST_PORT", port)
            putExtra("EXTRA_DEVICE_NAME", deviceName)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun vibratePhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    private fun connectToReceiver(connectionInfo: ConnectionInfo) {
        val currentWifi = NetworkUtils.getConnectedWifiName(this)

        // Kiểm tra xem 2 máy có đang ở cùng 1 mạng WiFi không (password rỗng hoặc trùng tên WiFi)
        val isAlreadySameWifi = connectionInfo.password.isEmpty() ||
                (NetworkUtils.isWifiConnected(this) && currentWifi.equals(connectionInfo.ssid, ignoreCase = true))

        if (isAlreadySameWifi) {
            // Trường hợp 1: Cùng chung mạng Wi-Fi -> Bắn file trực tiếp qua IP!
            Log.d(TAG, "[SENDER] Cùng mạng Wi-Fi, bắt đầu gửi trực tiếp tới ${connectionInfo.ipAddress}:${connectionInfo.port}")
            startTransferService(connectionInfo)
        } else {
            // Trường hợp 2: Máy nhận phát Hotspot -> Kiểm tra và kết nối vào Hotspot bằng WifiHelper
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) {
                // SỬA LỖI NHÁY MÀN HÌNH: Ẩn ngay layout connecting và return, KHÔNG gọi connectToWifi khi Wi-Fi đang tắt
                binding.layoutConnecting.gone()
                Toast.makeText(this, getString(R.string.prompt_turn_on_wifi_to_connect), Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        startActivity(Intent(Settings.Panel.ACTION_WIFI))
                    } catch (e: Exception) {
                        Log.e(TAG, "[SENDER] Không thể mở Wi-Fi Settings Panel", e)
                    }
                }
                // Trì hoãn 2.5 giây trước khi mở lại scan để tránh giật nháy màn hình
                binding.root.postDelayed({
                    isScanningLocked.set(false)
                    imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }, 2500)
                return
            }

            Log.d(TAG, "[SENDER] Đang kết nối tới Hotspot của Receiver -> SSID: ${connectionInfo.ssid}")
            wifiHelper?.connectToWifi(
                ssid = connectionInfo.ssid,
                password = connectionInfo.password,
                listener = object : WifiHelper.WifiConnectionListener {
                    override fun onConnected(network: android.net.Network) {
                        Log.d(TAG, "[SENDER] Đã kết nối vào Hotspot: ${connectionInfo.ssid} -> Khởi chạy TransferService gửi tới ${connectionInfo.ipAddress}:${connectionInfo.port}")
                        runOnUiThread {
                            startTransferService(connectionInfo)
                        }
                    }

                    override fun onDisconnected() {
                        Log.d(TAG, "[SENDER] Mất kết nối tới Hotspot")
                        runOnUiThread {
                            binding.layoutConnecting.gone()
                            Toast.makeText(this@QrScannerActivity, R.string.connection_failed_message, Toast.LENGTH_SHORT).show()
                            binding.root.postDelayed({ resetScanning() }, 1500)
                        }
                    }

                    override fun onFailed() {
                        Log.e(TAG, "[SENDER] Kết nối tới Hotspot thất bại")
                        runOnUiThread {
                            binding.layoutConnecting.gone()
                            Toast.makeText(this@QrScannerActivity, R.string.connection_failed, Toast.LENGTH_SHORT).show()
                            // Trì hoãn 1.5 giây để tránh lặp quét liên tục
                            binding.root.postDelayed({ resetScanning() }, 1500)
                        }
                    }
                }
            )
        }
    }

    private fun resetScanning() {
        isScanningLocked.set(false)
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            processImage(imageProxy)
        }
    }

    private fun startTransferService(connectionInfo: ConnectionInfo) {
        // Guard: chỉ cho phép gọi startSend đúng 1 lần duy nhất
        if (isTransferStarted) return
        isTransferStarted = true
        lanDiscoveryHelper?.release()

        // Ưu tiên gateway IP phát hiện từ DHCP khi kết nối Hotspot
        val actualIp = if (connectionInfo.password.isNotEmpty()) {
            WifiHelper.activeGatewayIp ?: connectionInfo.ipAddress
        } else {
            connectionInfo.ipAddress
        }
        if (connectionInfo.password.isNotEmpty() && WifiHelper.activeGatewayIp != null && WifiHelper.activeGatewayIp != connectionInfo.ipAddress) {
            Log.d(TAG, "[SENDER] Override IP từ QR (${connectionInfo.ipAddress}) -> Gateway DHCP ($actualIp)")
        }

        TransferService.startSender(
            context = this,
            ip = actualIp,
            port = connectionInfo.port,
            peerName = connectionInfo.deviceName,
            files = filesToSend,
            network = WifiHelper.activeWifiNetwork
        )
        // Ẩn loading và đóng màn hình quét ngay khi chuyển sang ProgressActivity
        binding.layoutConnecting.gone()
        startNextActivity(ProgressActivity::class.java, null)
        finishThisActivity()
    }

    override fun onBack() {
        Log.d(TAG, "[SENDER] 🛑 Người dùng bấm Back trên QrScannerActivity -> Ngắt kết nối ngay lập tức")
        cancelConnectingAndDisconnect()
        super.onBack()
    }

    private fun cancelConnectingAndDisconnect() {
        lanDiscoveryHelper?.release()
        binding.layoutConnecting.gone()
        wifiHelper?.disconnectWifi()
        WifiHelper.disconnectActiveWifi(this)
        TransferService.disconnect(this)
        isTransferStarted = false
    }

    override fun onDestroy() {
        lanDiscoveryHelper?.release()
        cameraExecutor.shutdown()
        // Ngắt kết nối Wi-Fi nếu chưa bắt đầu gửi file
        if (!isTransferStarted) {
            wifiHelper?.disconnectWifi()
        }
        barcodeScanner.close()
        super.onDestroy()
    }
}
