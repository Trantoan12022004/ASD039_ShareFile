package com.example.basekotlin.ui.safebox.model

import com.example.basekotlin.data.local.safebox.SafeBoxFileType

data class SafeBoxCategory(
    val type: SafeBoxFileType,
    val titleRes: Int,
    val iconRes: Int,
    val count: Int = 0
)
