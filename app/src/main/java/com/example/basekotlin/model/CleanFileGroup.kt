package com.example.basekotlin.model

// Model đại diện cho một nhóm file (nhóm theo folder hoặc duplicate group)
data class CleanFileGroup(
    val groupName: String,
    var totalSizeBytes: Long,
    val items: MutableList<CleanFileItem>,
    var isExpanded: Boolean = true
)
