package com.example.basekotlin.data.local.documentstore

import android.os.Environment
import com.example.basekotlin.model.DocumentInfo
import com.example.basekotlin.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DocumentFileScanner {

    // Map extension -> DocumentType, dùng để phân loại ngay khi quét
    private val PDF_EXTENSIONS = setOf("pdf")
    private val EXCEL_EXTENSIONS = setOf("xls", "xlsx", "xlsm", "csv")
    private val PPT_EXTENSIONS = setOf("ppt", "pptx", "pps", "ppsx")
    private val TXT_EXTENSIONS = setOf("txt")
    private val DOC_EXTENSIONS = setOf("doc", "docx")
    private val WPS_EXTENSIONS = setOf("wps", "wpt", "wpp", "wet")
    // Extension khác nhưng vẫn coi là "document" -> rơi vào tab Other
    private val OTHER_DOCUMENT_EXTENSIONS = setOf("rtf", "odt", "ods", "odp", "epub", "html", "htm", "xml", "json", "log")

    private val ALL_DOCUMENT_EXTENSIONS = PDF_EXTENSIONS + EXCEL_EXTENSIONS + PPT_EXTENSIONS +
            TXT_EXTENSIONS + DOC_EXTENSIONS + WPS_EXTENSIONS + OTHER_DOCUMENT_EXTENSIONS

    suspend fun scanAllDocuments(): List<DocumentInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<DocumentInfo>()
            val rootDir = Environment.getExternalStorageDirectory()

            val foundFiles = mutableListOf<File>()
            collectDocumentFilesRecursively(rootDir, foundFiles, depth = 0)

            for (file in foundFiles) {
                val documentInfo = buildDocumentInfo(file)
                result.add(documentInfo)
            }
            result
        }
    }

    private fun collectDocumentFilesRecursively(dir: File, output: MutableList<File>, depth: Int) {
        if (depth > MAX_SCAN_DEPTH) {
            return
        }
        val children = dir.listFiles()
        if (children == null) {
            return
        }
        for (child in children) {
            if (child.isDirectory) {
                if (SKIP_DIR_NAMES.contains(child.name) == false) {
                    collectDocumentFilesRecursively(child, output, depth + 1)
                }
            } else {
                val extension = child.extension.lowercase()
                if (ALL_DOCUMENT_EXTENSIONS.contains(extension)) {
                    output.add(child)
                }
            }
        }
    }

    private fun buildDocumentInfo(file: File): DocumentInfo {
        return buildDocumentInfo(file, lastOpenedMillis = 0L)
    }

    // Public để DocumentsRepositoryImpl có thể tái sử dụng khi build DocumentInfo
// cho danh sách Recent (cần gắn thêm thời điểm mở gần nhất)
    fun buildDocumentInfo(file: File, lastOpenedMillis: Long): DocumentInfo {
        val extension = file.extension.lowercase()
        val documentType = resolveDocumentType(extension)
        return DocumentInfo(
            fileName = file.name,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            dateModifiedMillis = file.lastModified(),
            extension = extension,
            documentType = documentType,
            lastOpenedMillis = lastOpenedMillis
        )
    }

    private fun resolveDocumentType(extension: String): DocumentType {
        if (PDF_EXTENSIONS.contains(extension)) {
            return DocumentType.PDF
        }
        if (EXCEL_EXTENSIONS.contains(extension)) {
            return DocumentType.EXCEL
        }
        if (PPT_EXTENSIONS.contains(extension)) {
            return DocumentType.PPT
        }
        if (TXT_EXTENSIONS.contains(extension)) {
            return DocumentType.TXT
        }
        if (DOC_EXTENSIONS.contains(extension)) {
            return DocumentType.DOC
        }
        if (WPS_EXTENSIONS.contains(extension)) {
            return DocumentType.WPS
        }
        return DocumentType.OTHER
    }

    private const val MAX_SCAN_DEPTH = 12
    private val SKIP_DIR_NAMES = setOf("Android", ".thumbnails", ".trashed")
}