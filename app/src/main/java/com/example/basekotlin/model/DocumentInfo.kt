package com.example.basekotlin.model

// Phân loại theo định dạng để lọc vào từng tab, tương tự AppSourceType
enum class DocumentType {
    PDF,
    EXCEL,
    PPT,
    TXT,
    DOC,
    WPS,
    OTHER,
}

// domain model cho 1 file document trên máy
data class DocumentInfo(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
    val extension: String,
    val documentType: DocumentType,
    val lastOpenedMillis: Long = 0L
)