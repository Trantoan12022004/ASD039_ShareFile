package com.example.basekotlin.data.local.documentstore

import com.example.basekotlin.util.SharedPreUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Lưu danh sách đường dẫn file đã mở kèm thời điểm mở, dùng cho tab Recent
object RecentDocumentsStore {

    private const val KEY = "recent_documents_paths"
    private const val MAX_RECENT_COUNT = 30

    private val gson = Gson()

    data class RecentEntry(val filePath: String, val openedAtMillis: Long)

    fun getAll(): MutableList<RecentEntry> {
        val json = SharedPreUtils.getInstance().getString(KEY, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<RecentEntry>>() {}.type
        val list: MutableList<RecentEntry>? = gson.fromJson(json, type)
        if (list == null) {
            return mutableListOf()
        }
        return list
    }

    fun markOpened(filePath: String) {
        val currentList = getAll()
        currentList.removeAll { entry -> entry.filePath == filePath }
        currentList.add(0, RecentEntry(filePath, System.currentTimeMillis()))
        while (currentList.size > MAX_RECENT_COUNT) {
            currentList.removeAt(currentList.lastIndex)
        }
        save(currentList)
    }

    private fun save(list: List<RecentEntry>) {
        SharedPreUtils.getInstance().setString(KEY, gson.toJson(list))
    }
}