package com.example.basekotlin.ui.download.safebox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.R
import com.example.basekotlin.data.local.download.DownloadScanner
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepository
import com.example.basekotlin.data.local.repository.safebox.SafeBoxRepositoryImpl
import com.example.basekotlin.data.local.safebox.SafeBoxPreferences
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile
import com.example.basekotlin.ui.safebox.lock.PatternLockView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadSafeBoxLockState {
    NOT_SET,   // Chưa thiết lập mật khẩu
    LOCKED,    // Đã có mật khẩu, đang khóa
    UNLOCKED   // Đã mở khóa thành công
}

data class SafeBoxLockTabUiState(
    val hintResId: Int = R.string.draw_pattern_to_unlock,
    val isError: Boolean = false,
    val patternViewState: PatternLockView.PatternViewState = PatternLockView.PatternViewState.NORMAL,
    val isInputEnabled: Boolean = true,
    val showRedrawButton: Boolean = false
)

class DownloadSafeBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SafeBoxRepository = SafeBoxRepositoryImpl(application)
    private val preferences = SafeBoxPreferences.getInstance(application)

    private val _lockState = MutableStateFlow(DownloadSafeBoxLockState.LOCKED)
    val lockState: StateFlow<DownloadSafeBoxLockState> = _lockState.asStateFlow()

    private val _lockUiState = MutableStateFlow(SafeBoxLockTabUiState())
    val lockUiState: StateFlow<SafeBoxLockTabUiState> = _lockUiState.asStateFlow()

    private val _clearPatternEvent = MutableSharedFlow<Unit>()
    val clearPatternEvent: SharedFlow<Unit> = _clearPatternEvent.asSharedFlow()

    // Lọc danh sách file SafeBox thuộc thư mục Download
    val downloadFiles: StateFlow<List<SafeBoxFile>> = repository.observeAllFiles()
        .map { allFiles ->
            val rootDownload = DownloadScanner.getRootDownloadDir().absolutePath
            allFiles.filter { file ->
                file.originalPath.startsWith(rootDownload) || file.originalPath.contains("ShareFile/Download")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkPasswordState() {
        if (!preferences.isPatternSet()) {
            _lockState.value = DownloadSafeBoxLockState.NOT_SET
        } else if (_lockState.value != DownloadSafeBoxLockState.UNLOCKED) {
            _lockState.value = DownloadSafeBoxLockState.LOCKED
            _lockUiState.value = SafeBoxLockTabUiState()
        }
    }

    companion object {
        private const val MIN_PATTERN_LENGTH = 4
    }

    fun onPatternCompleted(pattern: List<Int>) {
        if (pattern.size < MIN_PATTERN_LENGTH) {
            _lockUiState.value = _lockUiState.value.copy(
                hintResId = R.string.pattern_too_short,
                isError = true,
                patternViewState = PatternLockView.PatternViewState.ERROR,
                isInputEnabled = false,
                showRedrawButton = true
            )
            viewModelScope.launch {
                delay(700)
                _clearPatternEvent.emit(Unit)
                _lockUiState.value = _lockUiState.value.copy(
                    patternViewState = PatternLockView.PatternViewState.NORMAL,
                    isInputEnabled = true
                )
            }
            return
        }

        val patternString = pattern.joinToString(",")
        val isCorrect = preferences.verifyPattern(patternString)

        if (isCorrect) {
            _lockUiState.value = _lockUiState.value.copy(
                patternViewState = PatternLockView.PatternViewState.SUCCESS,
                isError = false,
                isInputEnabled = false
            )
            viewModelScope.launch {
                delay(300)
                _lockState.value = DownloadSafeBoxLockState.UNLOCKED
            }
        } else {
            val failedCount = preferences.getFailedAttempts()
            val errorHint = if (failedCount >= 5) R.string.too_many_attempts else R.string.pattern_incorrect

            _lockUiState.value = _lockUiState.value.copy(
                hintResId = errorHint,
                isError = true,
                patternViewState = PatternLockView.PatternViewState.ERROR,
                isInputEnabled = false,
                showRedrawButton = true
            )
            viewModelScope.launch {
                delay(700)
                _clearPatternEvent.emit(Unit)
                _lockUiState.value = _lockUiState.value.copy(
                    patternViewState = PatternLockView.PatternViewState.NORMAL,
                    isInputEnabled = true
                )
            }
        }
    }

    fun onRedrawClicked() {
        _lockUiState.value = SafeBoxLockTabUiState()
        viewModelScope.launch {
            _clearPatternEvent.emit(Unit)
        }
    }

    fun restoreFile(file: SafeBoxFile, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreFile(file)
            onResult(result.isSuccess)
        }
    }

    fun deletePermanently(file: SafeBoxFile, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deletePermanently(file)
            onResult(result.isSuccess)
        }
    }
}
