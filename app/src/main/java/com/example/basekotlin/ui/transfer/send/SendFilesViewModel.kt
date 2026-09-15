package com.example.basekotlin.ui.transfer.send

import android.app.Application
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.ui.transfer.data.SendFileRepository
import com.example.basekotlin.ui.transfer.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SendFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SendFileRepository(application)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)


    // ===== SUB-TAB CHO CÁC MÀN HÌNH =====
    val recentSubTab = MutableStateFlow(RecentSubTab.SEND)
    val videoSubTab = MutableStateFlow(VideoSubTab.RECENT)
    val photoSubTab = MutableStateFlow(PhotoSubTab.RECENT)
    val appsSubTab = MutableStateFlow(AppsSubTab.INSTALLED)

    // ===== QUẢN LÝ SELECTION (SINGLE SOURCE OF TRUTH) =====
    private val _selectedFiles = MutableStateFlow<List<TransferableItem>>(emptyList())
    val selectedFiles: StateFlow<List<TransferableItem>> = _selectedFiles.asStateFlow()
    val totalSizeBytes: StateFlow<String> = _selectedFiles
        .map { list ->
            val totalBytes = list.sumOf { it.sizeBytes }
            Formatter.formatShortFileSize(getApplication(), totalBytes)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "0 B"
        )


    val isBottomExpanded = MutableStateFlow(false)

    fun toggleFileSelection(item: TransferableItem) {
        val current = _selectedFiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current.removeAt(index) else current.add(item)
        _selectedFiles.value = current
    }

    fun removeSelectedFile(item: TransferableItem) {
        val current = _selectedFiles.value.toMutableList()
        current.removeAll { it.id == item.id }
        _selectedFiles.value = current
    }

    fun clearSelection() {
        _selectedFiles.value = emptyList()
        isBottomExpanded.value = false
    }

    // Text đếm dung lượng Bottom Bar: "2 of 5,5MB"
    val bottomSizeSummary: StateFlow<String> = _selectedFiles.map { list ->
        val totalBytes = list.sumOf { it.sizeBytes }
        val sizeFormatted = Formatter.formatShortFileSize(getApplication(), totalBytes)
        "${list.size} of $sizeFormatted"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0 of 0 B")

    // ===== DỮ LIỆU GỐC & TRẠNG THÁI LOADING CHO TỪNG TAB (CÓ CACHE) =====

    // 1. VIDEO
    private val _rawVideos = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingVideos = MutableStateFlow(false)
    fun loadVideos() {
        if (_rawVideos.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingVideos.value = true
            _rawVideos.value = repository.queryVideos()
            isLoadingVideos.value = false
        }
    }

    // 2. PHOTO
    private val _rawPhotos = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingPhotos = MutableStateFlow(false)
    fun loadPhotos() {
        if (_rawPhotos.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingPhotos.value = true
            _rawPhotos.value = repository.queryPhotos()
            isLoadingPhotos.value = false
        }
    }

    // 3. FILES (DOCUMENTS)
    private val _rawDocs = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingDocs = MutableStateFlow(false)
    fun loadDocuments() {
        if (_rawDocs.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingDocs.value = true
            _rawDocs.value = repository.queryDocuments()
            isLoadingDocs.value = false
        }
    }

    // 4. MUSIC
    private val _rawMusic = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingMusic = MutableStateFlow(false)
    fun loadMusic() {
        if (_rawMusic.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingMusic.value = true
            _rawMusic.value = repository.queryMusic()
            isLoadingMusic.value = false
        }
    }

    // 5. CONTACTS
    private val _rawContacts = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingContacts = MutableStateFlow(false)
    fun loadContacts(forceReload: Boolean = false) {
        if (!forceReload && _rawContacts.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingContacts.value = true
            _rawContacts.value = repository.queryContacts()
            isLoadingContacts.value = false
        }
    }

    // 6. APPS
    private val _rawInstalledApps = MutableStateFlow<List<TransferableItem>>(emptyList())
    private val _rawNotInstalledApps = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingApps = MutableStateFlow(false)
    fun loadApps() {
        if (_rawInstalledApps.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingApps.value = true
            _rawInstalledApps.value = repository.queryInstalledApps()
            _rawNotInstalledApps.value = repository.queryNotInstalledApks()
            isLoadingApps.value = false
        }
    }

    // 7. RECENT
    private val _rawRecentSent = MutableStateFlow<List<TransferableItem>>(emptyList())
    private val _rawRecentReceived = MutableStateFlow<List<TransferableItem>>(emptyList())
    val isLoadingRecent = MutableStateFlow(false)
    fun loadRecent() {
        if (_rawRecentSent.value.isNotEmpty() || _rawRecentReceived.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingRecent.value = true
            _rawRecentSent.value = repository.queryRecentSent()
            _rawRecentReceived.value = repository.queryRecentReceived()
            isLoadingRecent.value = false
        }
    }

    // ===== UDF STREAMS GOM NHÓM CHO VIEW quan sát =====

    val displayVideoGroups: StateFlow<List<ItemGroup<TransferableItem>>> = combine(
        videoSubTab, _rawVideos
    ) { subTab, videos ->
        when (subTab) {
            VideoSubTab.RECENT -> groupByDate(videos)
            VideoSubTab.FOLDERS -> groupByFolder(videos)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayPhotoGroups: StateFlow<List<ItemGroup<TransferableItem>>> = combine(
        photoSubTab, _rawPhotos
    ) { subTab, photos ->
        when (subTab) {
            PhotoSubTab.RECENT -> groupByDate(photos)
            PhotoSubTab.FOLDERS -> groupByFolder(photos)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayContactGroups: StateFlow<List<ItemGroup<TransferableItem>>> = _rawContacts.map { contacts ->
        contacts.groupBy { it.groupKey }
            .map { (char, list) -> ItemGroup(title = char, count = list.size, items = list) }
            .sortedBy { it.title }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayDocumentGroups: StateFlow<List<ItemGroup<TransferableItem>>> = _rawDocs.map { docs ->
        docs.groupBy { it.groupKey }
            .map { (type, list) -> ItemGroup(title = type, count = list.size, items = list) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayMusicGroups: StateFlow<List<ItemGroup<TransferableItem>>> = _rawMusic.map { songs ->
        songs.groupBy { it.groupKey }
            .map { (char, list) -> ItemGroup(title = char, count = list.size, items = list) }
            .sortedBy { it.title }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayRecentGroups: StateFlow<List<ItemGroup<TransferableItem>>> = combine(
        recentSubTab, _rawRecentSent, _rawRecentReceived
    ) { subTab, sent, received ->
        val list = if (subTab == RecentSubTab.SEND) sent else received
        groupByDate(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayAppsList: StateFlow<List<TransferableItem>> = combine(
        appsSubTab, _rawInstalledApps, _rawNotInstalledApps
    ) { subTab, installed, notInstalled ->
        if (subTab == AppsSubTab.INSTALLED) installed else notInstalled
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun groupByDate(items: List<TransferableItem>): List<ItemGroup<TransferableItem>> {
        return items.groupBy { dateFormat.format(Date(it.dateModifiedMillis)) }
            .map { (dateStr, list) -> ItemGroup(title = dateStr, count = list.size, items = list) }
    }

    private fun groupByFolder(items: List<TransferableItem>): List<ItemGroup<TransferableItem>> {
        return items.groupBy { it.groupKey }
            .map { (folder, list) -> ItemGroup(title = folder, count = list.size, items = list) }
            .sortedBy { it.title.lowercase() }
    }
}
