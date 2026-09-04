package com.example.basekotlin.data.local.repository.zips

import android.content.Context
import android.os.Environment
import com.example.basekotlin.data.local.zipstore.ZipFileScanner
import com.example.basekotlin.model.UnzippedItem
import com.example.basekotlin.model.ZipInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ZipsRepositoryImpl(private val context: Context) : ZipsRepository {
    override suspend fun fetchAllZipFiles(): List<ZipInfo> {
        return ZipFileScanner.scanAllZipFiles()
    }

    override fun getExtractRootDir(): File {
        val root = Environment.getExternalStorageDirectory()
        val extractDir = File(root, "ShareFile/Zip/Extract")
        if (!extractDir.exists()) {
            extractDir.mkdirs()
        }
        return extractDir
    }

    override suspend fun fetchItemsInDirectory(directory: File): List<UnzippedItem> {
        return withContext(Dispatchers.IO) {
            ZipFileScanner.getItemsInDirectory(directory)
        }
    }

}
