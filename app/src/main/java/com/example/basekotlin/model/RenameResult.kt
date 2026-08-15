// model/RenameResult.kt
package com.example.basekotlin.model

import android.content.IntentSender

sealed class RenameResult {
    object Success : RenameResult()
    data class NeedsUserConsent(val intentSender: IntentSender) : RenameResult()
    data class Failure(val error: Throwable) : RenameResult()
}