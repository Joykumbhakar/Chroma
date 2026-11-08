@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.chroma.studio

/**
 * Root package ChromaMessagingService extending the services package implementation.
 * Ensures consistent behavior and valid context regardless of manifest reference.
 */
class ChromaMessagingService : com.chroma.studio.services.ChromaMessagingService() {
    companion object {
        fun syncTokenForCurrentUser() {
            com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
        }
    }
}
