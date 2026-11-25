package com.chroma.studio

import com.chroma.studio.model.SharedIncomingData

/**
 * Global bus for forwarding external Android system intents (Share Target, App Widgets, Deep Links)
 * into the active HomeActivity Compose UI lifecycle, with pending queues so cold-start intents
 * are never lost before Compose attaches listeners.
 */
object AppIntentManager {
    private var pendingIncomingShare: SharedIncomingData? = null
    private var pendingWidgetAction: String? = null

    var onIncomingShare: ((SharedIncomingData) -> Unit)? = null
    var onWidgetAction: ((String) -> Unit)? = null

    fun dispatchIncomingShare(data: SharedIncomingData) {
        val listener = onIncomingShare
        if (listener != null) {
            listener.invoke(data)
        } else {
            pendingIncomingShare = data
        }
    }

    fun dispatchWidgetAction(action: String) {
        if (action.isBlank()) return
        val listener = onWidgetAction
        if (listener != null) {
            listener.invoke(action)
        } else {
            pendingWidgetAction = action
        }
    }

    fun consumePendingIncomingShare(): SharedIncomingData? {
        val next = pendingIncomingShare
        pendingIncomingShare = null
        return next
    }

    fun consumePendingWidgetAction(): String? {
        val next = pendingWidgetAction
        pendingWidgetAction = null
        return next
    }
}
