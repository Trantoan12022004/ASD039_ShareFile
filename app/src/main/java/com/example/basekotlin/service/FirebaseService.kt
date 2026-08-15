package com.example.basekotlin.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
        }

        if (remoteMessage.notification != null) {
        }
    }

    companion object {
        private const val TAG = "SERVICE"
    }
}