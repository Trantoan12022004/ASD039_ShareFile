package com.example.basekotlin.ui.files.music.ringtone

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.R
import com.example.basekotlin.data.local.music.MusicDatabase
import com.example.basekotlin.data.local.repository.MusicRepository
import com.example.basekotlin.data.local.repository.MusicRepositoryImpl
import com.example.basekotlin.model.MusicTrack
import com.example.basekotlin.util.AudioTrimmer
import com.example.basekotlin.util.RingtoneHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RingtoneViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentTrack: MusicTrack? = null
    private var currentStartMs: Long = 0L
    private var currentEndMs: Long = 0L
    private var isSaving: Boolean = false
    private var previewRequest: PreviewRequest? = null
    private var errorMessage: String? = null
    private var successMessage: String? = null

    init {
        val appContext = application.applicationContext
        val database = MusicDatabase.getInstance(appContext)
        repository = MusicRepositoryImpl(appContext, database)
    }

    class PreviewRequest(
        val startMs: Long,
        val endMs: Long
    )

    class UiState {
        var track: MusicTrack? = null
        var startMs: Long = 0L
        var endMs: Long = 0L
        var isSaving: Boolean = false
        var previewRequest: PreviewRequest? = null
        var errorMessage: String? = null
        var successMessage: String? = null
    }

    fun loadTrack(trackId: Long) {
        viewModelScope.launch {
            val track = repository.getTrackById(trackId)
            if (track == null) {
                currentTrack = null
                errorMessage = getApplication<Application>().getString(R.string.no_song_available)
                successMessage = null
                publishUiState()
                return@launch
            }

            val defaultEndMs = minOf(track.durationMs, DEFAULT_CLIP_END_MS)
            currentTrack = track
            currentStartMs = 0L
            currentEndMs = defaultEndMs
            errorMessage = null
            successMessage = null
            publishUiState()
        }
    }

    fun updateSelection(startMs: Long, endMs: Long) {
        val track = currentTrack
        if (track == null) {
            return
        }

        var newStartMs = startMs
        var newEndMs = endMs

        if (newEndMs - newStartMs < MIN_CLIP_DURATION_MS) {
            newEndMs = minOf(newStartMs + MIN_CLIP_DURATION_MS, track.durationMs)
            if (newEndMs - newStartMs < MIN_CLIP_DURATION_MS) {
                newStartMs = maxOf(0L, newEndMs - MIN_CLIP_DURATION_MS)
            }
        }

        if (newEndMs > track.durationMs) {
            newEndMs = track.durationMs
        }

        currentStartMs = newStartMs
        currentEndMs = newEndMs
        publishUiState()
    }

    fun previewSelection() {
        if (currentTrack == null) {
            return
        }

        previewRequest = PreviewRequest(
            startMs = currentStartMs,
            endMs = currentEndMs
        )
        publishUiState()
    }

    fun onPreviewHandled() {
        previewRequest = null
        publishUiState()
    }

    fun saveRingtone() {
        val track = currentTrack
        if (track == null) {
            return
        }

        val startMs = currentStartMs
        val endMs = currentEndMs

        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            successMessage = null
            publishUiState()

            val resultUri = withContext(Dispatchers.IO) {
                exportRingtone(track, startMs, endMs)
            }
            Log.d("DEBUG_RINGTONE", resultUri.toString())

            isSaving = false

            if (resultUri == null) {
                errorMessage = getApplication<Application>().getString(R.string.ringtone_save_failed)
                successMessage = null
            } else {
                errorMessage = null
                successMessage = getApplication<Application>().getString(R.string.ringtone_save_success)
            }

            publishUiState()
        }
    }

    fun onErrorHandled() {
        errorMessage = null
        publishUiState()
    }

    fun onSuccessHandled() {
        successMessage = null
        publishUiState()
    }

    private fun publishUiState() {
        val state = UiState()
        state.track = currentTrack
        state.startMs = currentStartMs
        state.endMs = currentEndMs
        state.isSaving = isSaving
        state.previewRequest = previewRequest
        state.errorMessage = errorMessage
        state.successMessage = successMessage
        _uiState.value = state
    }

    private suspend fun exportRingtone(
        track: MusicTrack,
        startMs: Long,
        endMs: Long
    ): Uri? {
        val context = getApplication<Application>().applicationContext
        val cacheFile = File(
            context.cacheDir,
            "ringtone_${track.id}_${System.currentTimeMillis()}.m4a"
        )

        val trimmed = AudioTrimmer.trim(
            context = context,
            inputUri = track.contentUri,
            startMs = startMs,
            endMs = endMs,
            outputFile = cacheFile
        )
        Log.d("DEBUG_RINGTONE", "trimmed=$trimmed, cacheSize=${cacheFile.length()}, path=${cacheFile.absolutePath}")

        if (!trimmed || cacheFile.length() == 0L) {
            Log.e("DEBUG_RINGTONE", "AudioTrimmer failed")
            return null
        }

        val displayName = sanitizeFileName(track.title)
        val uri = RingtoneHelper.saveToMediaStore(context, cacheFile, displayName)
        Log.d("DEBUG_RINGTONE", "mediaStoreUri=$uri")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }

        return uri
    }

    private fun sanitizeFileName(title: String): String {
        var result = title.trim()
        if (result.isEmpty()) {
            result = "ringtone"
        }
        result = result.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return result
    }

    companion object {
        private const val MIN_CLIP_DURATION_MS = 5_000L
        private const val DEFAULT_CLIP_END_MS = 30_000L
    }
}
