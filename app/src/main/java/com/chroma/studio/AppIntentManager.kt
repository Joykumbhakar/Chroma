package com.chroma.studio

import com.chroma.studio.model.SharedIncomingData

/**
 * Global bus for forwarding external Android system intents (Share Target, App Widgets, Deep Links)
 * into the active HomeActivity Compose UI lifecycle.
 */
object AppIntentManager {
    var onIncomingShare: ((SharedIncomingData) -> Unit)? = null
    var onWidgetAction: ((String) -> Unit)? = null
}
