package com.example.basekotlin.ui.safebox.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.R
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepository
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepositoryImpl
import com.example.basekotlin.data.local.safebox.SafeBoxFileType
import com.example.basekotlin.ui.safebox.model.SafeBoxCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SafeBoxHomeViewModel(application: Application): AndroidViewModel(application) {

    private val repository: SafeBoxRepository = SafeBoxRepositoryImpl(application)


    // Danh sách 5 danh mục kết hợp luồng đếm file theo thời gian thực từ Room DB
    val categories: StateFlow<List<SafeBoxCategory>> = combine(
        repository.observeCountByType(SafeBoxFileType.PICTURES),
        repository.observeCountByType(SafeBoxFileType.VIDEOS),
        repository.observeCountByType(SafeBoxFileType.AUDIO),
        repository.observeCountByType(SafeBoxFileType.DOCUMENTS),
        repository.observeCountByType(SafeBoxFileType.OTHERS)
    ) { picCount, vidCount, audioCount, docCount, otherCount ->
        listOf(
            SafeBoxCategory(SafeBoxFileType.PICTURES, R.string.pictures, R.drawable.ic_photo_expand, picCount),
            SafeBoxCategory(SafeBoxFileType.VIDEOS, R.string.videos, R.drawable.ic_video_expand, vidCount),
            SafeBoxCategory(SafeBoxFileType.AUDIO, R.string.audio, R.drawable.ic_audio_expand, audioCount),
            SafeBoxCategory(SafeBoxFileType.DOCUMENTS, R.string.documents, R.drawable.ic_document_expand, docCount),
            SafeBoxCategory(SafeBoxFileType.OTHERS, R.string.others, R.drawable.ic_other_expand, otherCount)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = createDefaultCategories()
    )

    private fun createDefaultCategories(): List<SafeBoxCategory> {
        return listOf(
            SafeBoxCategory(SafeBoxFileType.PICTURES, R.string.pictures, R.drawable.ic_cat_photos, 0),
            SafeBoxCategory(SafeBoxFileType.VIDEOS, R.string.videos, R.drawable.ic_cat_videos, 0),
            SafeBoxCategory(SafeBoxFileType.AUDIO, R.string.audio, R.drawable.ic_cat_music, 0),
            SafeBoxCategory(SafeBoxFileType.DOCUMENTS, R.string.documents, R.drawable.ic_cat_document, 0),
            SafeBoxCategory(SafeBoxFileType.OTHERS, R.string.others, R.drawable.ic_file, 0)
        )
    }
}