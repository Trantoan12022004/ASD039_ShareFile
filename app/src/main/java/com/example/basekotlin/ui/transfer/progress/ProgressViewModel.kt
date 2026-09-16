package com.example.basekotlin.ui.transfer.progress

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.service.transfer.TransferService
import com.example.basekotlin.ui.transfer.model.OverallStats
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.launch

class ProgressViewModel : ViewModel() {

    private var transferService: TransferService? = null
    private var isBound = false

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _items = MutableStateFlow<List<ProgressItem>>(emptyList())
    val items: StateFlow<List<ProgressItem>> = _items.asStateFlow()

    private val _stats = MutableStateFlow(OverallStats())
    val stats: StateFlow<OverallStats> = _stats.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TransferService.ServiceBinder
            val s = binder.getService()
            transferService = s
            isBound = true

            // Lắng nghe StateFlows từ Service
            viewModelScope.launch { s.isPeerOnline.collect { _isOnline.value = it } }
            viewModelScope.launch { s.peerDeviceName.collect { _deviceName.value = it } }
            viewModelScope.launch { s.items.collect { _items.value = it } }
            viewModelScope.launch { s.stats.collect { _stats.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            transferService = null
            isBound = false
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, TransferService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    fun sendText(text: String) {
        transferService?.sendChatMessage(text)
    }

    fun addFiles(files: List<TransferFile>) {
        transferService?.addFilesToSend(files)
    }

    fun cancelFile(fileId: String) {
        transferService?.cancelFile(fileId)
    }

    fun retryFile(fileId: String) {
        transferService?.retryFile(fileId)
    }
}
