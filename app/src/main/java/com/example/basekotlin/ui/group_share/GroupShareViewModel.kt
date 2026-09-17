package com.example.basekotlin.ui.group_share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.service.group.GroupShareService
import com.example.basekotlin.ui.group_share.model.GroupMemberModel
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.model.OverallStats
import com.example.basekotlin.ui.transfer.model.ProgressItem
import com.example.basekotlin.ui.transfer.model.TransferFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupShareViewModel : ViewModel() {

    private var groupService: GroupShareService? = null
    private var isBound = false

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _hostIp = MutableStateFlow("")
    val hostIp: StateFlow<String> = _hostIp.asStateFlow()

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private val _connectionInfo = MutableStateFlow<ConnectionInfo?>(null)
    val connectionInfo: StateFlow<ConnectionInfo?> = _connectionInfo.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMemberModel>>(emptyList())
    val members: StateFlow<List<GroupMemberModel>> = _members.asStateFlow()

    private val _memberCount = MutableStateFlow(1)
    val memberCount: StateFlow<Int> = _memberCount.asStateFlow()

    private val _items = MutableStateFlow<List<ProgressItem>>(emptyList())
    val items: StateFlow<List<ProgressItem>> = _items.asStateFlow()

    private val _stats = MutableStateFlow(OverallStats())
    val stats: StateFlow<OverallStats> = _stats.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GroupShareService.ServiceBinder
            val s = binder.getService()
            groupService = s
            isBound = true

            viewModelScope.launch { s.isOnline.collect { _isOnline.value = it } }
            viewModelScope.launch { s.groupName.collect { _groupName.value = it } }
            viewModelScope.launch { s.hostIp.collect { _hostIp.value = it } }
            viewModelScope.launch { s.qrBitmap.collect { _qrBitmap.value = it } }
            viewModelScope.launch { s.connectionInfo.collect { _connectionInfo.value = it } }
            viewModelScope.launch { s.members.collect { _members.value = it } }
            viewModelScope.launch { s.memberCount.collect { _memberCount.value = it } }
            viewModelScope.launch { s.items.collect { _items.value = it } }
            viewModelScope.launch { s.stats.collect { _stats.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            groupService = null
            isBound = false
        }
    }

    fun initSession(context: Context, isHost: Boolean, hostIp: String?, hostPort: Int = 8888) {
        if (isHost) {
            GroupShareService.startHost(context, hostPort)
        } else if (!hostIp.isNullOrEmpty()) {
            GroupShareService.startClient(context, hostIp, hostPort)
        }

        val intent = Intent(context, GroupShareService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun sendTextMessage(text: String) {
        groupService?.sendChatMessage(text)
    }

    fun sendFiles(files: List<TransferFile>) {
        groupService?.addFilesToSend(files)
    }

    fun cancelFile(fileId: String) {
        groupService?.cancelFile(fileId)
    }

    fun retryFile(fileId: String) {
        groupService?.retryFile(fileId)
    }

    fun unbindService(context: Context) {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
    }
}
