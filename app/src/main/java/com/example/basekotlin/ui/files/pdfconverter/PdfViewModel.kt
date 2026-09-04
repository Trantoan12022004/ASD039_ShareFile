package com.example.basekotlin.ui.files.pdfconverter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.data.local.repository.docs.DocumentsRepository
import com.example.basekotlin.data.local.repository.docs.DocumentsRepositoryImpl
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.model.DocumentType
import com.example.basekotlin.util.ImageToPdfConverter
import com.example.basekotlin.util.PdfToImageConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentsRepository

    init {
        val appContext = application.applicationContext
        repository = DocumentsRepositoryImpl(appContext)
    }

    // ================= NGUỒN DỮ LIỆU GỐC =================
    private val _allDocuments = MutableStateFlow<List<DocumentInfo>>(emptyList())
    val allDocuments: StateFlow<List<DocumentInfo>> = _allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingAll = MutableStateFlow(false)
    val isLoadingAll: StateFlow<Boolean> = _isLoadingAll

    private var allDocumentsLoadedOnce = false

    // Tải dữ liệu lần đầu nếu chưa tải
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

    // Làm mới lại dữ liệu khi người dùng chủ động refresh
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

    val pdfDocuments: StateFlow<List<DocumentInfo>> = allDocuments
        .map { documents -> filterDocumentsByType(documents, DocumentType.PDF) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // ================= SELECTION MODE =================
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private val _selectedPdfPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPdfPaths: StateFlow<Set<String>> = _selectedPdfPaths

    fun togglePdfSelection(filePath: String) {
        val currentSet = _selectedPdfPaths.value.toMutableSet()
        if (currentSet.contains(filePath)) {
            currentSet.remove(filePath)
        } else {
            currentSet.add(filePath)
        }
        _selectedPdfPaths.value = currentSet
    }

    // ================= CONVERTER MODE =================

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting

    private val _convertedImageCount = MutableStateFlow<Int?>(null)
    val convertedImageCount: StateFlow<Int?> = _convertedImageCount

    fun clearPdfSelection() {
        _selectedPdfPaths.value = emptySet()
    }

    fun resetConvertResult() {
        _convertedImageCount.value = null
    }
    fun convertSelectedPdfs() {
        val selected = _selectedPdfPaths.value
        if (selected.isEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isConverting.value = true
            try {
                val resultPaths = PdfToImageConverter.convertPdfListToImages(
                    getApplication<Application>().applicationContext,
                    selected
                )
                _convertedImageCount.value = resultPaths.size
                if (resultPaths.isNotEmpty()) {
                    clearPdfSelection()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _convertedImageCount.value = 0
            } finally {
                _isConverting.value = false
            }
        }
    }

    private val _convertedPdfPath = MutableStateFlow<String?>(null)
    val convertedPdfPath: StateFlow<String?> = _convertedPdfPath

    fun resetConvertedPdfPath() {
        _convertedPdfPath.value = null
    }

    fun convertSelectedImagesToPdf(imagePaths: List<String>) {
        if (imagePaths.isEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isConverting.value = true
            try {
                val resultPath = ImageToPdfConverter.convertImagesToPdf(
                    getApplication<Application>().applicationContext,
                    imagePaths
                )
                _convertedPdfPath.value = resultPath
            } catch (e: Exception) {
                e.printStackTrace()
                _convertedPdfPath.value = null
            } finally {
                _isConverting.value = false
            }
        }
    }
}
