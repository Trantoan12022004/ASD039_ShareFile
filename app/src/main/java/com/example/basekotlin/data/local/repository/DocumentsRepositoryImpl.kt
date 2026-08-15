package com.example.basekotlin.data.local.repository

import android.content.Context
import com.example.basekotlin.data.local.documentstore.DocumentFileScanner
import com.example.basekotlin.data.local.documentstore.RecentDocumentsStore
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.util.SharedPreUtils
import java.io.File

class DocumentsRepositoryImpl(private val appContext: Context) : DocumentsRepository {

    init {
        SharedPreUtils.init(appContext)
    }

    override suspend fun fetchAllDocuments(): List<DocumentInfo> {
        return DocumentFileScanner.scanAllDocuments()
    }

    // Ghép danh sách path đã lưu trong RecentDocumentsStore với dữ liệu file thật,
    // bỏ qua entry mà file đã bị xoá khỏi máy
    override suspend fun fetchRecentDocuments(): List<DocumentInfo> {
        val recentEntries = RecentDocumentsStore.getAll()
        val result = mutableListOf<DocumentInfo>()
        for (entry in recentEntries) {
            val file = File(entry.filePath)
            if (file.exists()) {
                val documentInfo = DocumentFileScanner.buildDocumentInfo(file, entry.openedAtMillis)
                result.add(documentInfo)
            }
        }
        return result
    }

    override fun markDocumentOpened(filePath: String) {
        RecentDocumentsStore.markOpened(filePath)
    }
}