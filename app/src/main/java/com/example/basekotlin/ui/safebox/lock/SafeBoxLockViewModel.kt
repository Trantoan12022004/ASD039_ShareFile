package com.example.basekotlin.ui.safebox.lock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.R
import com.example.basekotlin.data.local.safebox.SafeBoxPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LockMode {
    SETUP_NEW,    // Đang vẽ pattern lần đầu để thiết lập
    CONFIRM_NEW,  // Đang vẽ lại để xác nhận khớp
    UNLOCK        // Đang vẽ để mở khóa vào Safe Box
}

data class LockUiState(
    val mode: LockMode = LockMode.SETUP_NEW,
    val titleResId: Int = R.string.safe_box,
    val hintResId: Int = R.string.draw_pattern_to_lock,
    val isError: Boolean = false,
    val showRedrawButton: Boolean = false,
    val patternViewState: PatternLockView.PatternViewState = PatternLockView.PatternViewState.NORMAL,
    val isInputEnabled: Boolean = true
)

sealed class LockNavigationEvent {
    object NavigateToHome : LockNavigationEvent()
    object ClearPattern : LockNavigationEvent() // Sự kiện yêu cầu View xóa pattern
}

class SafeBoxLockViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SafeBoxPreferences.getInstance(application)

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<LockNavigationEvent>()
    val navigationEvent: SharedFlow<LockNavigationEvent> = _navigationEvent.asSharedFlow()

    private var firstPatternDrawn: List<Int>? = null

    companion object {
        private const val MIN_PATTERN_LENGTH = 4
        private const val MAX_FAILED_ATTEMPTS = 5
    }

    init {
        initLockMode()
    }

    private fun initLockMode() {
        val isSet = preferences.isPatternSet()
        if (isSet) {
            _uiState.value = LockUiState(
                mode = LockMode.UNLOCK,
                hintResId = R.string.draw_pattern_to_unlock
            )
        } else {
            _uiState.value = LockUiState(
                mode = LockMode.SETUP_NEW,
                hintResId = R.string.draw_pattern_to_lock
            )
        }
    }

    fun onPatternCompleted(pattern: List<Int>) {
        when (_uiState.value.mode) {
            LockMode.SETUP_NEW -> handleSetupPattern(pattern)
            LockMode.CONFIRM_NEW -> handleConfirmPattern(pattern)
            LockMode.UNLOCK -> handleUnlockPattern(pattern)
        }
    }

    private fun handleSetupPattern(pattern: List<Int>) {
        if (pattern.size < MIN_PATTERN_LENGTH) {
            _uiState.value = _uiState.value.copy(
                hintResId = R.string.pattern_too_short,
                isError = true,
                patternViewState = PatternLockView.PatternViewState.ERROR,
                isInputEnabled = false,
                showRedrawButton = true
            )
            viewModelScope.launch {
                delay(700)
                _navigationEvent.emit(LockNavigationEvent.ClearPattern)
                _uiState.value = _uiState.value.copy(
                    patternViewState = PatternLockView.PatternViewState.NORMAL,
                    isInputEnabled = true
                )
            }
            return
        }
        // Lưu tạm hình lần 1 và delay chuyển sang bước confirm
        firstPatternDrawn = pattern
        _uiState.value = _uiState.value.copy(
            isInputEnabled = false
        )
        viewModelScope.launch {
            delay(300)
            _navigationEvent.emit(LockNavigationEvent.ClearPattern)
            _uiState.value = _uiState.value.copy(
                mode = LockMode.CONFIRM_NEW,
                hintResId = R.string.confirm_lock_pattern,
                isError = false,
                showRedrawButton = true,
                patternViewState = PatternLockView.PatternViewState.NORMAL,
                isInputEnabled = true
            )
        }
    }

    private fun handleConfirmPattern(pattern: List<Int>) {
        val first = firstPatternDrawn
        if (first == pattern) {
            val patternString = pattern.joinToString(separator = ",")
            preferences.savePattern(patternString)
            _uiState.value = _uiState.value.copy(
                patternViewState = PatternLockView.PatternViewState.SUCCESS,
                isError = false,
                isInputEnabled = false
            )
            viewModelScope.launch {
                delay(300)
                _navigationEvent.emit(LockNavigationEvent.NavigateToHome)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                hintResId = R.string.pattern_mismatch,
                isError = true,
                patternViewState = PatternLockView.PatternViewState.ERROR,
                isInputEnabled = false,
                showRedrawButton = true
            )
            viewModelScope.launch {
                delay(700)
                _navigationEvent.emit(LockNavigationEvent.ClearPattern)
                _uiState.value = _uiState.value.copy(
                    patternViewState = PatternLockView.PatternViewState.NORMAL,
                    isInputEnabled = true
                )
            }
        }
    }

    private fun handleUnlockPattern(pattern: List<Int>) {
        val patternString = pattern.joinToString(separator = ",")
        val isCorrect = preferences.verifyPattern(patternString)
        if (isCorrect) {
            _uiState.value = _uiState.value.copy(
                patternViewState = PatternLockView.PatternViewState.SUCCESS,
                isError = false,
                isInputEnabled = false
            )
            viewModelScope.launch {
                delay(300)
                _navigationEvent.emit(LockNavigationEvent.NavigateToHome)
            }
        } else {
            val failedCount = preferences.getFailedAttempts()
            val errorHint = if (failedCount >= MAX_FAILED_ATTEMPTS) {
                R.string.too_many_attempts
            } else {
                R.string.pattern_incorrect
            }
            // Hiển thị màu đỏ và khóa input tạm thời
            _uiState.value = _uiState.value.copy(
                hintResId = errorHint,
                isError = true,
                patternViewState = PatternLockView.PatternViewState.ERROR,
                isInputEnabled = false,
                showRedrawButton = true
            )
            // Tự động clear sau 700ms để người dùng có thể vẽ lần tiếp theo ngay lập tức
            viewModelScope.launch {
                delay(700)
                _navigationEvent.emit(LockNavigationEvent.ClearPattern)
                _uiState.value = _uiState.value.copy(
                    patternViewState = PatternLockView.PatternViewState.NORMAL,
                    isInputEnabled = true
                )
            }
        }
    }

    // Xử lý khi user bấm nút Redraw
    fun onRedrawClicked() {
        if (_uiState.value.mode == LockMode.CONFIRM_NEW) {
            firstPatternDrawn = null
            _uiState.value = LockUiState(
                mode = LockMode.SETUP_NEW,
                hintResId = R.string.draw_pattern_to_lock,
                showRedrawButton = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isError = false,
                hintResId = if (_uiState.value.mode == LockMode.UNLOCK) {
                    R.string.draw_pattern_to_unlock
                } else {
                    R.string.draw_pattern_to_lock
                },
                patternViewState = PatternLockView.PatternViewState.NORMAL,
                showRedrawButton = false
            )
        }
    }
}
