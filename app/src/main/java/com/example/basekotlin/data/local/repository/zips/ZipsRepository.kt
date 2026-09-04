package com.example.basekotlin.data.local.repository.zips

import com.example.basekotlin.model.UnzippedItem
import com.example.basekotlin.model.ZipInfo
import java.io.File

interface ZipsRepository {
    suspend fun fetchAllZipFiles(): List<ZipInfo>
    fun getExtractRootDir(): File
    suspend fun fetchItemsInDirectory(directory: File): List<UnzippedItem>
}
