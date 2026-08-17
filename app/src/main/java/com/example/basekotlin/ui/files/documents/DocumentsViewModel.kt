package com.example.basekotlin.ui.files.documents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.docs.DocumentsRepository
import com.example.basekotlin.data.local.repository.docs.DocumentsRepositoryImpl
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.model.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentsRepository

    init {
        val appContext = application.applicationContext
        repository = DocumentsRepositoryImpl(appContext)
    }
    // ================= NGUỒN DỮ LIỆU GỐC (lấy khi vào activity) =================
    private val _allDocuments = MutableStateFlow<List<DocumentInfo>>(emptyList())
    val allDocuments: StateFlow<List<DocumentInfo>> = _allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _isLoadingAll = MutableStateFlow(false)
    val isLoadingAll: StateFlow<Boolean> = _isLoadingAll
    private var allDocumentsLoadedOnce = false
    fun loadAllDocumentsIfNeeded() {
        if (allDocumentsLoadedOnce) {
            return
        }
        viewModelScope.launch {
            _isLoadingAll.value = true
            try {
                val documents = repository.fetchAllDocuments()
                _allDocuments.value = documents
                allDocumentsLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingAll.value = false
            }
        }
    }
    fun refreshAllDocuments() {
        viewModelScope.launch {
            _isLoadingAll.value = true
            try {
                val documents = repository.fetchAllDocuments()
                _allDocuments.value = documents
                allDocumentsLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingAll.value = false
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

//    TAB All

    private val searchedAllDocuments: StateFlow<List<DocumentInfo>> = combine(
        allDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedAllDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // ================= CÁC TAB PHÂN LOẠI THEO ĐỊNH DẠNG =================
    // Đều lấy từ allDocuments, không quét lại ổ đĩa
    val pdfDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.PDF) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedPdfDocuments: StateFlow<List<DocumentInfo>> = combine(
        pdfDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pdfDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedPdfDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excelDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.EXCEL) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedExcelDocuments: StateFlow<List<DocumentInfo>> = combine(
        excelDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val excelDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedExcelDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pptDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.PPT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedPptDocuments: StateFlow<List<DocumentInfo>> = combine(
        pptDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pptDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedPptDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val txtDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.TXT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedTxtDocuments: StateFlow<List<DocumentInfo>> = combine(
        txtDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val txtDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedTxtDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val docDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.DOC) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedDocDocuments: StateFlow<List<DocumentInfo>> = combine(
        docDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val docDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedDocDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wpsDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.WPS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedWpsDocuments: StateFlow<List<DocumentInfo>> = combine(
        wpsDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wpsDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedWpsDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val otherDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.OTHER) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchedOtherDocuments: StateFlow<List<DocumentInfo>> = combine(
        otherDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val otherDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedOtherDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ================= TAB RECENT (nguồn dữ liệu riêng) =================
    private val _recentDocuments = MutableStateFlow<List<DocumentInfo>>(emptyList())
    private val _isLoadingRecent = MutableStateFlow(false)
    val isLoadingRecent: StateFlow<Boolean> = _isLoadingRecent

    private val searchedRecentDocuments: StateFlow<List<DocumentInfo>> = combine(
        _recentDocuments,
        _searchQuery
    ) { documents, query ->
        filterDocumentsBySearchQuery(documents, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocumentsSearch: StateFlow<List<DocumentInfo>> = searchedRecentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadRecentDocuments() {
        viewModelScope.launch {
            _isLoadingRecent.value = true
            try {
                val documents = repository.fetchRecentDocuments()
                _recentDocuments.value = documents
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingRecent.value = false
            }
        }
    }

    // Gọi hàm này ngay khi user bấm mở 1 document (ACTION_VIEW), rồi load lại tab Recent
    fun markDocumentOpened(filePath: String) {
        repository.markDocumentOpened(filePath)
        loadRecentDocuments()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ================= SELECTION MODE =================
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedDocumentPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedDocumentPaths: StateFlow<Set<String>> = _selectedDocumentPaths

    fun enterSelectionMode(filePath: String? = null) {
        _isSelectionMode.value = true
        if (filePath != null) {
            _selectedDocumentPaths.value = setOf(filePath)
        } else {
            _selectedDocumentPaths.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedDocumentPaths.value = emptySet()
    }

    fun toggleDocumentSelection(filePath: String) {
        val currentSet = _selectedDocumentPaths.value.toMutableSet()
        if (currentSet.contains(filePath)) {
            currentSet.remove(filePath)
        } else {
            currentSet.add(filePath)
        }
        _selectedDocumentPaths.value = currentSet
    }

    fun selectAllDocuments(filePaths: List<String>) {
        _selectedDocumentPaths.value = filePaths.toSet()
    }

    fun clearDocumentSelection() {
        _selectedDocumentPaths.value = emptySet()
    }

    // ================= FILTER HELPERS =================
    private fun filterDocumentsByType(
        documents: List<DocumentInfo>,
        documentType: DocumentType
    ): List<DocumentInfo> {
        val result = mutableListOf<DocumentInfo>()
        for (document in documents) {
            if (document.documentType == documentType) {
                result.add(document)
            }
        }
        return result
    }

    private fun filterDocumentsBySearchQuery(
        documents: List<DocumentInfo>,
        query: String
    ): List<DocumentInfo> {
        if (query.isBlank()) {
            return documents
        }
        val filtered = mutableListOf<DocumentInfo>()
        for (document in documents) {
            if (document.fileName.contains(query, ignoreCase = true)) {
                filtered.add(document)
            }
        }
        return filtered
    }
}