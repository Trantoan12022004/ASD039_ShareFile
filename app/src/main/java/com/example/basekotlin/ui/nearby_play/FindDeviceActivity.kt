package com.example.basekotlin.ui.nearby_play

import android.app.Activity
import android.content.Intent
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityFindDeviceBinding
import com.example.basekotlin.ui.nearby_play.adapter.NearbySongsAdapter
import com.example.basekotlin.ui.nearby_play.model.NearbySongItem
import com.example.basekotlin.ui.nearby_play.protocol.NearbyPlayProtocol
import com.example.basekotlin.ui.transfer.model.DeviceInfo
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.scanner.DeviceAdapter
import com.example.basekotlin.ui.transfer.scanner.QrScannerActivity
import com.example.basekotlin.ui.transfer.send.SendFileActivity
import com.example.basekotlin.ui.transfer.send.SendTabPagerAdapter
import com.example.basekotlin.util.transfer.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class FindDeviceActivity : BaseActivity<ActivityFindDeviceBinding>(ActivityFindDeviceBinding::inflate) {

    private val discoveredDevices = mutableListOf<DeviceInfo>()
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var songsAdapter: NearbySongsAdapter
    private val selectedSongs = mutableListOf<NearbySongItem>()

    private var socket: Socket? = null
    private var dataInput: DataInputStream? = null
    private var dataOutput: DataOutputStream? = null
    private var receiverDeviceName = ""

    private var udpDiscoveryJob: Job? = null
    private var isPlayingState = false
    private var isUserTrackingSeekBar = false

    // Launcher chọn file nhạc từ SendFileActivity
    private val pickMusicLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val files: ArrayList<TransferFile>? = result.data?.getParcelableArrayListExtra(SendFileActivity.EXTRA_SELECTED_FILES)
            if (!files.isNullOrEmpty()) {
                handleSelectedMusic(files)
            }
        }
    }

    // Launcher quét QR
    private val qrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val ip = result.data?.getStringExtra("RESULT_IP") ?: ""
            val port = result.data?.getIntExtra("RESULT_PORT", 8890) ?: 8890
            val name = result.data?.getStringExtra("RESULT_DEVICE_NAME") ?: ip
            connectToSpeaker(ip, port, name)
        }
    }

    override fun initView() {
        super.initView()
        binding.viewTop.btnBack.tap { finish() }
        binding.layoutPlaying.gone()
        binding.tvDeviceCount.text = "0"

        setupAdapters()
        startDiscovery()
    }

    override fun bindView() {
        super.bindView()

        // Kết nối qua IP nhập trong edtSearch
        binding.btnConnect.tap {
            val ip = binding.edtSearch.text.toString().trim()
            if (ip.isNotEmpty()) {
                connectToSpeaker(ip, 8890, ip)
            } else {
                Toast.makeText(this, getString(R.string.enter_ip), Toast.LENGTH_SHORT).show()
            }
        }

        // Chuyển sang màn quét QR
        binding.btnQr.tap {
            val intent = Intent(this, QrScannerActivity::class.java).apply {
                putExtra("EXTRA_IS_JOIN_GROUP", true)
            }
            qrLauncher.launch(intent)
        }

        // Nút Play / Pause trên layout_playing
        binding.btnPlayPause.tap {
            if (isPlayingState) {
                sendControlCommand(NearbyPlayProtocol.CMD_PAUSE)
            } else {
                sendControlCommand(NearbyPlayProtocol.CMD_PLAY)
            }
        }

        // Nút chuyển bài kế tiếp
        binding.btnNext.tap {
            sendControlCommand(NearbyPlayProtocol.CMD_NEXT)
        }

        // Nút lùi bài trước
        binding.btnPrevious.tap {
            sendControlCommand(NearbyPlayProtocol.CMD_PREVIOUS)
        }

        // Nút Quit trên layout_playing: ngắt kết nối
        binding.btnQuitPlaying.tap {
            disconnectFromSpeaker()
        }

        // SeekBar tua nhạc
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                isUserTrackingSeekBar = true
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                isUserTrackingSeekBar = false
                val targetMs = sb?.progress?.toLong() ?: 0L
                lifecycleScope.launch(Dispatchers.IO) {
                    dataOutput?.let {
                        NearbyPlayProtocol.sendMessage(it, NearbyPlayProtocol.CMD_SEEK, targetMs.toString())
                    }
                }
            }
        })
    }

    private fun setupAdapters() {
        // rvDevices: hiển thị những thiết bị có thể kết nối
        deviceAdapter = DeviceAdapter { device ->
            connectToSpeaker(device.ipAddress, device.port, device.name)
        }
        binding.rvDevices.adapter = deviceAdapter

        // rv_songs: hiển thị danh sách bài hát đã chọn
        songsAdapter = NearbySongsAdapter { index, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                dataOutput?.let {
                    NearbyPlayProtocol.sendMessage(it, NearbyPlayProtocol.CMD_SELECT_TRACK, index.toString())
                }
            }
        }
        binding.rvSongs.adapter = songsAdapter
    }

    private fun startDiscovery() {
        binding.pbSearching.visible()
        binding.rvDevices.gone()

        udpDiscoveryJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val datagramSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(NearbyPlayProtocol.DISCOVERY_PORT))
                }
                val buffer = ByteArray(1024)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    datagramSocket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()

                    if (msg.startsWith(NearbyPlayProtocol.BEACON_PREFIX)) {
                        val parts = msg.split("|")
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val ip = parts[2]
                            val port = parts[3].toIntOrNull() ?: 8890

                            withContext(Dispatchers.Main) {
                                if (discoveredDevices.none { it.ipAddress == ip }) {
                                    discoveredDevices.add(DeviceInfo(name = name, ipAddress = ip, port = port))
                                    deviceAdapter.addListData(discoveredDevices)
                                    binding.tvDeviceCount.text = discoveredDevices.size.toString()
                                    binding.pbSearching.gone()
                                    binding.rvDevices.visible()
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun connectToSpeaker(ip: String, port: Int, name: String) {
        receiverDeviceName = name
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(ip, port), 5000)
                socket = newSocket
                dataInput = DataInputStream(newSocket.getInputStream())
                dataOutput = DataOutputStream(newSocket.getOutputStream())

                // Gửi gói Handshake
                NearbyPlayProtocol.sendMessage(
                    dataOutput!!,
                    NearbyPlayProtocol.MSG_HANDSHAKE,
                    Gson().toJson(NearbyPlayProtocol.HandshakeData(NetworkUtils.getDeviceName()))
                )

                val (ackType, _) = NearbyPlayProtocol.readMessage(dataInput!!)
                if (ackType == NearbyPlayProtocol.MSG_ACK_HANDSHAKE) {
                    withContext(Dispatchers.Main) {
                        listenSpeakerStatus()
                        // Chuyển đến màn SendFileActivity với tab music để chọn file
                        val intent = Intent(this@FindDeviceActivity, SendFileActivity::class.java).apply {
                            putExtra(SendFileActivity.EXTRA_IS_PICK_MODE, true)
                            putExtra(SendFileActivity.EXTRA_INITIAL_TAB, SendTabPagerAdapter.TAB_MUSIC)
                        }
                        pickMusicLauncher.launch(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FindDeviceActivity, getString(R.string.cannot_connect), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleSelectedMusic(files: ArrayList<TransferFile>) {
        selectedSongs.clear()
        files.forEachIndexed { index, file ->
            selectedSongs.add(
                NearbySongItem(
                    id = index.toString(),
                    title = file.name,
                    sizeBytes = file.size,
                    uriString = file.uri.toString(),
                    isPlaying = (index == 0)
                )
            )
        }

        // Hiện layout_playing, ẩn rvDevices
        binding.rvDevices.gone()
        binding.pbSearching.gone()
        binding.layoutPlaying.visible()
        binding.tvPlayDevice.text = getString(R.string.playing_on, receiverDeviceName)
        binding.tvSongName.text = selectedSongs.firstOrNull()?.title ?: ""

        songsAdapter.addListData(selectedSongs)

        // Bắt đầu truyền dữ liệu file nhạc sang Loa
        lifecycleScope.launch(Dispatchers.IO) {
            sendSongsToSpeaker(files)
        }
    }

    private suspend fun sendSongsToSpeaker(files: ArrayList<TransferFile>) {
        val out = dataOutput ?: return
        files.forEachIndexed { index, transferFile ->
            val uri = transferFile.uri
            val inputStream = contentResolver.openInputStream(uri) ?: return@forEachIndexed
            val fileSize = transferFile.size

            val header = NearbyPlayProtocol.SongHeaderData(index, transferFile.name, fileSize)
            NearbyPlayProtocol.sendMessage(out, NearbyPlayProtocol.MSG_SONG_HEADER, Gson().toJson(header))

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                NearbyPlayProtocol.sendChunk(out, buffer, 0, bytesRead)
            }
            inputStream.close()
        }
    }

    private fun listenSpeakerStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val input = dataInput ?: return@launch
            try {
                while (isActive) {
                    val (type, bytes) = NearbyPlayProtocol.readMessage(input)
                    when (type) {
                        NearbyPlayProtocol.STATUS_SYNC -> {
                            val status = Gson().fromJson(
                                NearbyPlayProtocol.parseJson(bytes),
                                NearbyPlayProtocol.PlaybackStatusData::class.java
                            )
                            withContext(Dispatchers.Main) {
                                updatePlaybackUI(status)
                            }
                        }

                        NearbyPlayProtocol.CMD_DISCONNECT -> {
                            withContext(Dispatchers.Main) {
                                onSpeakerDisconnected()
                            }
                            break
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    onSpeakerDisconnected()
                }
            }
        }
    }

    private fun updatePlaybackUI(status: NearbyPlayProtocol.PlaybackStatusData) {
        isPlayingState = status.isPlaying
        if (status.currentIndex in selectedSongs.indices) {
            binding.tvSongName.text = selectedSongs[status.currentIndex].title
            songsAdapter.updatePlayingIndex(status.currentIndex)
        }

        binding.btnPlayPause.setImageResource(if (status.isPlaying) R.drawable.btn_pause2 else R.drawable.btn_play2)

        if (!isUserTrackingSeekBar && status.durationMs > 0) {
            binding.seekBar.max = status.durationMs.toInt()
            binding.seekBar.progress = status.currentPositionMs.toInt()
            binding.tvCurrentTime.text = formatTime(status.currentPositionMs)
            binding.tvTotalTime.text = formatTime(status.durationMs)
        }
    }

    private fun sendControlCommand(cmd: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            dataOutput?.let {
                NearbyPlayProtocol.sendMessage(it, cmd, "{}")
            }
        }
    }

    private fun disconnectFromSpeaker() {
        lifecycleScope.launch(Dispatchers.IO) {
            dataOutput?.let {
                try {
                    NearbyPlayProtocol.sendMessage(it, NearbyPlayProtocol.CMD_DISCONNECT, "{}")
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                onSpeakerDisconnected()
            }
        }
    }

    private fun onSpeakerDisconnected() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null

        // Thông báo ngắt kết nối
        Toast.makeText(this, getString(R.string.device_disconnected), Toast.LENGTH_SHORT).show()

        // Số thiết bị về 0, ẩn layout_playing, hiện lại rvDevices
        binding.tvDeviceCount.text = "0"
        binding.layoutPlaying.gone()
        binding.rvDevices.visible()
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        udpDiscoveryJob?.cancel()
        disconnectFromSpeaker()
        super.onDestroy()
    }
}