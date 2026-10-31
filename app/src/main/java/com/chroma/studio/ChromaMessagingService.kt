@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.chroma.studio

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Root package ChromaMessagingService delegate.
 */
class ChromaMessagingService : FirebaseMessagingService() {

    companion object {
        fun syncTokenForCurrentUser() {
            com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
        }
    }

    private val delegateService = com.chroma.studio.services.ChromaMessagingService()

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        delegateService.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        delegateService.onMessageReceived(remoteMessage)
    }
}
