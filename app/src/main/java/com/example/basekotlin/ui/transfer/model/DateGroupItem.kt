package com.example.basekotlin.ui.transfer.model

data class DateGroupItem<T>(
    val dateTitle: String,
    val count: Int,
    val items: List<T>
)
