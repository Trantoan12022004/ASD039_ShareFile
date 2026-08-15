// model/DeleteResult.kt
package com.example.basekotlin.model

import android.content.IntentSender

sealed class DeleteResult {
    object Success : DeleteResult()
    data class NeedsUserConsent(val intentSender: IntentSender) : DeleteResult()
    data class Failure(val error: Throwable) : DeleteResult()
}