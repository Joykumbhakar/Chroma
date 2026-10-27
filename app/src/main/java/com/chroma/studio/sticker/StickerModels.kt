package com.chroma.studio.sticker

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import java.util.UUID

@Immutable
data class StickerItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String = "Sticker",
    val isCustom: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class StickerOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    var offset: Offset = Offset.Zero,
    var scale: Float = 1.0f,
    var rotation: Float = 0.0f,
    val borderPx: Float = 5f
)
