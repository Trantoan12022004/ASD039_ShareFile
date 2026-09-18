package com.example.basekotlin.ui.clone_phone.model

import java.io.Serializable

/**
 * Các danh mục dữ liệu hỗ trợ trong tính năng Clone Phone
 */
enum class CloneCategory : Serializable {
    CONTACTS,
    PHOTOS,
    VIDEOS,
    AUDIOS,
    DOCUMENTS,
    APPS
}

/**
 * Trạng thái truyền tải của từng danh mục
 */
enum class CategoryTransferStatus : Serializable {
    WAITING,
    PROGRESS,
    COMPLETE,
    FAIL
}

/**
 * Dữ liệu tiến trình của từng danh mục
 */
data class CategoryState(
    val category: CloneCategory,
    val isSelected: Boolean = false,
    val status: CategoryTransferStatus = CategoryTransferStatus.WAITING,
    val progressPercent: Int = 0,
    val itemCount: Int = 0,
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L
) : Serializable

/**
 * Dữ liệu tổng thể của quá trình Clone Phone
 */
data class CloneOverallState(
    val isTransferring: Boolean = false,
    val isDone: Boolean = false,
    val overallPercent: Int = 0,
    val totalTransferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isOldDevice: Boolean = false,
    val currentCategory: CloneCategory? = null
) : Serializable
