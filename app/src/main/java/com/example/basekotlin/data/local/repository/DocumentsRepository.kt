package com.example.basekotlin.data.local.repository

import com.example.basekotlin.model.DocumentInfo

interface DocumentsRepository {
    suspend fun fetchAllDocuments(): List<DocumentInfo>
    suspend fun fetchRecentDocuments(): List<DocumentInfo>
    fun markDocumentOpened(filePath: String)
}