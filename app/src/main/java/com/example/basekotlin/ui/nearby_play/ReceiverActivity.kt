package com.example.basekotlin.ui.nearby_play

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityReceiverBinding
import com.example.basekotlin.service.transfer.HotspotManager
import com.example.basekotlin.ui.nearby_play.protocol.NearbyPlayProtocol
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.util.transfer.NetworkUtils
import com.example.basekotlin.util.transfer.QrCodeHelper
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ReceiverActivity : BaseActivity<ActivityReceiverBinding>(ActivityReceiverBinding::inflate) {

    private var serverPort = 8890
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var dataInput: DataInputStream? = null
    private var dataOutput: DataOutputStream? = null

    private var hotspotManager: HotspotManager? = null
    private var udpBroadcastJob: Job? = null
    private var syncJob: Job? = null

    private var exoPlayer: ExoPlayer? = null
    private val cachedFiles = mutableListOf<File>()
    private var currentSongIndex = 0

    override fun initView() {
        super.initView()
        binding.tvDeviceName.text = NetworkUtils.getDeviceName()
        binding.layoutPlay.gone()
        binding.tvWaiting.visible()

        binding.viewTop.btnBack.tap { onStopReceiver() }
        binding.btnStop.tap { onStopReceiver() }

        initPlayer()
        setupConnection()
    }

    override fun bindView() {
        super.bindView()

        // Nút Play / Pause linh hoạt giữa ic_play1 và ic_pause
        binding.ivPlayPause.tap {
            togglePlayPause()
        }

        // Nút Quit ngắt kết nối
        binding.ivQuit.tap {
            disconnectFromSender()
        }
    }

    private fun initPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            // Phát hết 1 bài tự động chuyển bài kế tiếp, hết list thì dừng
                            playNextSong()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayPauseIcon(isPlaying)
                        sendSyncStatus()
                    }
                })
            }
    }

    private fun setupConnection() {
        serverPort = NetworkUtils.getAvailablePort()
        val isWifi = NetworkUtils.isWifiConnected(this)

        if (isWifi) {
            // Trường hợp chung Wi-Fi: hiện IP, sinh mã QR, phát beacon
            val wifiName = NetworkUtils.getConnectedWifiName(this)
            val localIp = NetworkUtils.getWifiIpAddress()
            binding.layoutIp.visible()
            binding.tvIpAddress.text = localIp

            startServer()
            startUdpBroadcast(localIp, serverPort)
            generateQr(
                ConnectionInfo(
                    ssid = wifiName,
                    password = "",
                    ipAddress = localIp,
                    port = serverPort,
                    deviceName = NetworkUtils.getDeviceName()
                )
            )
        } else {
            // Trường hợp Hotspot: ẩn IP
            binding.layoutIp.gone()
            hotspotManager = HotspotManager(this)
            hotspotManager?.startHotspot(serverPort, object : HotspotManager.HotspotListener {
                override fun onHotspotStarted(connectionInfo: ConnectionInfo) {
                    runOnUiThread {
                        startServer()
                        generateQr(connectionInfo)
                    }
                }

                override fun onHotspotFailed(errorCode: Int) {
                    runOnUiThread {
                        Toast.makeText(this@ReceiverActivity, getString(R.string.cannot_connect), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onHotspotStopped() {}
            })
        }
    }

    private fun generateQr(info: ConnectionInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            val qrBmp = QrCodeHelper.generateQrBitmap(info, 500)
            withContext(Dispatchers.Main) {
                binding.ivQrCode.setImageBitmap(qrBmp)
            }
        }
    }

    private fun startUdpBroadcast(ip: String, port: Int) {
        udpBroadcastJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(NearbyPlayProtocol.DISCOVERY_PORT))
                }
                val beacon = "${NearbyPlayProtocol.BEACON_PREFIX}|${NetworkUtils.getDeviceName()}|$ip|$port"
                val sendData = beacon.toByteArray(Charsets.UTF_8)
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(sendData, sendData.size, broadcastAddr, NearbyPlayProtocol.DISCOVERY_PORT)

                while (isActive) {
                    socket.send(packet)
                    delay(1200)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(serverPort)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    clientSocket = socket
                    dataInput = DataInputStream(socket.getInputStream())
                    dataOutput = DataOutputStream(socket.getOutputStream())

                    listenClientMessages()
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun listenClientMessages() {
        val input = dataInput ?: return
        try {
            while (coroutineContext.isActive) {
                val (type, bytes) = NearbyPlayProtocol.readMessage(input)
                when (type) {
                    NearbyPlayProtocol.MSG_HANDSHAKE -> {
                        val handshake = Gson().fromJson(
                            NearbyPlayProtocol.parseJson(bytes),
                            NearbyPlayProtocol.HandshakeData::class.java
                        )
                        withContext(Dispatchers.Main) {
                            binding.tvWaiting.gone()
                            binding.layoutPlay.visible()
                            binding.tvDevice.text = handshake.deviceName
                        }
                        dataOutput?.let {
                            NearbyPlayProtocol.sendMessage(
                                it,
                                NearbyPlayProtocol.MSG_ACK_HANDSHAKE,
                                Gson().toJson(NearbyPlayProtocol.HandshakeData(NetworkUtils.getDeviceName()))
                            )
                        }
                    }

                    NearbyPlayProtocol.MSG_SONG_HEADER -> {
                        val header = Gson().fromJson(
                            NearbyPlayProtocol.parseJson(bytes),
                            NearbyPlayProtocol.SongHeaderData::class.java
                        )
                        receiveSongFile(header)
                    }

                    NearbyPlayProtocol.CMD_PLAY -> {
                        withContext(Dispatchers.Main) {
                            exoPlayer?.play()
                        }
                    }

                    NearbyPlayProtocol.CMD_PAUSE -> {
                        withContext(Dispatchers.Main) {
                            exoPlayer?.pause()
                        }
                    }

                    NearbyPlayProtocol.CMD_SEEK -> {
                        val pos = NearbyPlayProtocol.parseJson(bytes).toLongOrNull() ?: 0L
                        withContext(Dispatchers.Main) {
                            exoPlayer?.seekTo(pos)
                        }
                    }

                    NearbyPlayProtocol.CMD_SELECT_TRACK -> {
                        val index = NearbyPlayProtocol.parseJson(bytes).toIntOrNull() ?: 0
                        withContext(Dispatchers.Main) {
                            playSongAtIndex(index)
                        }
                    }

                    NearbyPlayProtocol.CMD_NEXT -> {
                        withContext(Dispatchers.Main) {
                            playNextSong()
                        }
                    }

                    NearbyPlayProtocol.CMD_PREVIOUS -> {
                        withContext(Dispatchers.Main) {
                            playPreviousSong()
                        }
                    }

                    NearbyPlayProtocol.CMD_DISCONNECT -> {
                        withContext(Dispatchers.Main) {
                            onSenderDisconnected()
                        }
                        break
                    }
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                onSenderDisconnected()
            }
        }
    }

    private suspend fun receiveSongFile(header: NearbyPlayProtocol.SongHeaderData) {
        val dir = File(cacheDir, "nearby_play").apply { mkdirs() }
        val targetFile = File(dir, "song_${header.index}.mp3")
        var remaining = header.fileSize

        val fos = FileOutputStream(targetFile)
        val input = dataInput ?: return

        while (remaining > 0) {
            val (msgType, chunk) = NearbyPlayProtocol.readMessage(input)
            if (msgType == NearbyPlayProtocol.MSG_SONG_CHUNK) {
                fos.write(chunk)
                remaining -= chunk.size
            }
        }
        fos.flush()
        fos.close()

        while (cachedFiles.size <= header.index) {
            cachedFiles.add(targetFile)
        }
        cachedFiles[header.index] = targetFile

        // Nếu là bài đầu tiên nhận được -> Bắt đầu phát ngay
        if (header.index == 0 && currentSongIndex == 0) {
            withContext(Dispatchers.Main) {
                playSongAtIndex(0)
                startSyncProgress()
            }
        }
    }

    private fun playSongAtIndex(index: Int) {
        if (index in cachedFiles.indices) {
            currentSongIndex = index
            val file = cachedFiles[index]
            exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exoPlayer?.prepare()
            exoPlayer?.play()
            updatePlayPauseIcon(true)
            sendSyncStatus()
        }
    }

    private fun playNextSong() {
        if (currentSongIndex + 1 < cachedFiles.size) {
            playSongAtIndex(currentSongIndex + 1)
        } else {
            // Hết danh sách thì dừng
            exoPlayer?.pause()
            updatePlayPauseIcon(false)
            sendSyncStatus()
        }
    }

    private fun playPreviousSong() {
        if (currentSongIndex > 0) {
            playSongAtIndex(currentSongIndex - 1)
        }
    }

    private fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play1)
    }

    private fun startSyncProgress() {
        syncJob?.cancel()
        syncJob = lifecycleScope.launch {
            while (isActive) {
                sendSyncStatus()
                delay(500)
            }
        }
    }

    private fun sendSyncStatus() {
        val player = exoPlayer ?: return
        val status = NearbyPlayProtocol.PlaybackStatusData(
            currentIndex = currentSongIndex,
            isPlaying = player.isPlaying,
            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L)
        )
        dataOutput?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    NearbyPlayProtocol.sendMessage(it, NearbyPlayProtocol.STATUS_SYNC, Gson().toJson(status))
                } catch (_: Exception) {}
            }
        }
    }

    private fun disconnectFromSender() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                dataOutput?.let {
                    NearbyPlayProtocol.sendMessage(it, NearbyPlayProtocol.CMD_DISCONNECT, "{}")
                }
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                onSenderDisconnected()
            }
        }
    }

    private fun onSenderDisconnected() {
        exoPlayer?.pause()
        syncJob?.cancel()
        binding.layoutPlay.gone()
        binding.tvWaiting.visible()
        updatePlayPauseIcon(false)
        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null
    }

    private fun onStopReceiver() {
        disconnectFromSender()
        udpBroadcastJob?.cancel()
        hotspotManager?.stopHotspot()
        try { serverSocket?.close() } catch (_: Exception) {}
        exoPlayer?.release()
        finish()
    }

    override fun onDestroy() {
        onStopReceiver()
        super.onDestroy()
    }
}