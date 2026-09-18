package com.example.basekotlin.ui.clone_phone

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.service.clone.ClonePhoneService
import com.example.basekotlin.ui.clone_phone.model.*
import com.example.basekotlin.ui.transfer.data.SendFileRepository
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClonePhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SendFileRepository(application)
    private var cloneService: ClonePhoneService? = null
    private var isBound = false

    // Danh sách các danh mục người dùng đã chọn
    val selectedCategories = MutableStateFlow<Set<CloneCategory>>(emptySet())

    // Số lượng file trong từng danh mục trên máy cũ
    val categoryCounts = MutableStateFlow<Map<CloneCategory, Int>>(emptyMap())

    // Trạng thái từ Service
    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private val _hostIp = MutableStateFlow("")
    val hostIp: StateFlow<String> = _hostIp.asStateFlow()

    private val _connectionInfo = MutableStateFlow<ConnectionInfo?>(null)
    val connectionInfo: StateFlow<ConnectionInfo?> = _connectionInfo.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _overallState = MutableStateFlow(CloneOverallState())
    val overallState: StateFlow<CloneOverallState> = _overallState.asStateFlow()

    private val _categoryStates = MutableStateFlow<Map<CloneCategory, CategoryState>>(emptyMap())
    val categoryStates: StateFlow<Map<CloneCategory, CategoryState>> = _categoryStates.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ClonePhoneService.ServiceBinder
            val s = binder.getService()
            cloneService = s
            isBound = true

            viewModelScope.launch { s.qrBitmap.collect { _qrBitmap.value = it } }
            viewModelScope.launch { s.hostIp.collect { _hostIp.value = it } }
            viewModelScope.launch { s.connectionInfo.collect { _connectionInfo.value = it } }
            viewModelScope.launch { s.isConnected.collect { _isConnected.value = it } }
            viewModelScope.launch { s.overallState.collect { _overallState.value = it } }
            viewModelScope.launch { s.categoryStates.collect { _categoryStates.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cloneService = null
            isBound = false
        }
    }

    fun initService(context: Context) {
        val intent = Intent(context, ClonePhoneService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Tải nhanh số lượng các item của 6 danh mục trên máy cũ bằng MediaStore / PackageManager
     */
    fun loadCategoryCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            val counts = mutableMapOf<CloneCategory, Int>()
            counts[CloneCategory.CONTACTS] = repository.queryContacts().size
            counts[CloneCategory.PHOTOS] = repository.queryPhotos().size
            counts[CloneCategory.VIDEOS] = repository.queryVideos().size
            counts[CloneCategory.AUDIOS] = repository.queryMusic().size
            counts[CloneCategory.DOCUMENTS] = repository.queryDocuments().size
            counts[CloneCategory.APPS] = repository.queryInstalledApps().size + repository.queryNotInstalledApks().size
            categoryCounts.value = counts
        }
    }

    /**
     * Chọn hoặc bỏ chọn một danh mục
     */
    fun toggleCategory(category: CloneCategory) {
        val current = selectedCategories.value.toMutableSet()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        selectedCategories.value = current
    }

    /**
     * Chọn tất cả hoặc bỏ chọn tất cả các danh mục
     */
    fun toggleSelectAll() {
        if (selectedCategories.value.size == CloneCategory.values().size) {
            selectedCategories.value = emptySet()
        } else {
            selectedCategories.value = CloneCategory.values().toSet()
        }
    }

    /**
     * Máy cũ: Bắt đầu phát QR và mở ServerSocket
     */
    fun startHost(context: Context) {
        cloneService?.setSelectedCategories(selectedCategories.value)
        ClonePhoneService.startHost(context, 8889)
    }

    /**
     * Máy mới: Bắt đầu kết nối Socket tới Máy cũ
     */
    fun startClient(context: Context, hostIp: String, port: Int = 8889) {
        ClonePhoneService.startClient(context, hostIp, port)
    }

    /**
     * Hủy kết nối & dừng truyền
     */
    fun cancelClone(context: Context) {
        ClonePhoneService.disconnect(context)
    }

    /**
     * Đưa toàn bộ trạng thái về ban đầu khi kết thúc phiên clone để quay lại MainFragment
     */
    fun resetState(context: Context) {
        ClonePhoneService.disconnect(context)
        selectedCategories.value = emptySet()
        _isConnected.value = false
        _overallState.value = CloneOverallState()
        _categoryStates.value = emptyMap()
        _qrBitmap.value = null
        _hostIp.value = ""
        _connectionInfo.value = null
    }

    fun unbindService(context: Context) {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindService(getApplication())
    }
}
