package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType

/**
 * Draggable handles for MESH / BLOB layers — a simplified stand-in for index.html's full
 * mesh-warp gizmo (which also exposes per-point feather/opacity/width/height). Each handle
 * is a % position within the canvas; dragging one calls back with its new position so the
 * gradient center (mesh) or blend point can react live. See README for what's simplified.
 */
@Composable
fun MeshBlobHandlesOverlay(
    layer: GradientLayer,
    onPointDrag: (index: Int, newPercentPos: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    if (layer.type != LayerType.MESH && layer.type != LayerType.BLOB) return

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight

        layer.meshPoints.forEachIndexed { index, point ->
            val handleX = w * (point.x / 100f) - 12.dp
            val handleY = h * (point.y / 100f) - 12.dp

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .offset(x = handleX, y = handleY)
                    .size(24.dp)
                    .pointerInput(layer.id, index, w, h) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val trackWidthPx = w.toPx()
                            val trackHeightPx = h.toPx()
                            val deltaXPct = (dragAmount.x / trackWidthPx) * 100f
                            val deltaYPct = (dragAmount.y / trackHeightPx) * 100f
                            val newPos = Offset(
                                (point.x + deltaXPct).coerceIn(0f, 100f),
                                (point.y + deltaYPct).coerceIn(0f, 100f)
                            )
                            onPointDrag(index, newPos)
                        }
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(2.dp, Color(0xFF4F46E5), CircleShape)
            )
        }
    }
}
