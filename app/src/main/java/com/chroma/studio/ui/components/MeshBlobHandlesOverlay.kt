package com.chroma.studio.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType

/**
 * Smooth, crash-free drag handles for BLOB / LIQUID / MESH layers.
 *
 * Architecture for zero-lag dragging:
 * - During drag: ONLY local state is updated via [onBlobDrag]. No ViewModel writes = no
 *   full recompose of the layer stack. The parent passes drag positions to CanvasPreview
 *   via blobDragOverrides for instant visual feedback.
 * - On drag end: [onBlobDragEnd] is called ONCE, writing the final position to the ViewModel.
 */
@Composable
fun MeshBlobHandlesOverlay(
    layer: GradientLayer,
    /** Called every frame during drag — update a local Map<Int,Offset> (% coords), NOT ViewModel */
    onBlobDrag: (index: Int, pctPos: Offset) -> Unit = { _, _ -> },
    /** Called once on finger lift — write final position to ViewModel */
    onBlobDragEnd: (index: Int, finalPctPos: Offset) -> Unit = { _, _ -> },
    /** Mesh-only point drag (still commits each frame — mesh doesn't have 60fps issue) */
    onPointDrag: (index: Int, newPercentPos: Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (layer.type != LayerType.MESH &&
        layer.type != LayerType.BLOB &&
        layer.type != LayerType.LIQUID
    ) return

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight

        if (layer.type == LayerType.BLOB || layer.type == LayerType.LIQUID) {
            BlobHandles(
                layer = layer,
                containerW = w,
                containerH = h,
                onBlobDrag = onBlobDrag,
                onBlobDragEnd = onBlobDragEnd
            )
        } else {
            MeshHandles(
                layer = layer,
                containerW = w,
                containerH = h,
                onPointDrag = onPointDrag
            )
        }
    }
}

@Composable
private fun BlobHandles(
    layer: GradientLayer,
    containerW: Dp,
    containerH: Dp,
    onBlobDrag: (Int, Offset) -> Unit,
    onBlobDragEnd: (Int, Offset) -> Unit
) {
    // Track which blob is being dragged for visual feedback (elevation, size)
    var draggingIdx by remember { mutableStateOf(-1) }

    layer.blobs.forEachIndexed { idx, blob ->
        key(layer.id, idx) {
            val stop = layer.stops.getOrNull(idx) ?: layer.stops.firstOrNull()
            val handleColor = stop?.color ?: Color.White
            val isActive = idx == layer.activeBlobIdx
            val isDragging = idx == draggingIdx

            // Live position in % — starts from model, updated by drag
            var livePct by remember(layer.id, idx, blob.x, blob.y) {
                mutableStateOf(Offset(blob.x, blob.y))
            }

            // Animate handle size for press feedback
            val handleSize by animateDpAsState(
                targetValue = if (isDragging) 32.dp else 24.dp,
                animationSpec = spring(stiffness = 600f),
                label = "blobHandleSize_$idx"
            )
            val ringW by animateDpAsState(
                targetValue = if (isDragging) (blob.width * 2.2f).dp else (blob.width * 2f).dp,
                animationSpec = spring(stiffness = 400f),
                label = "blobRingW_$idx"
            )
            val ringH by animateDpAsState(
                targetValue = if (isDragging) (blob.height * 2.2f).dp else (blob.height * 2f).dp,
                animationSpec = spring(stiffness = 400f),
                label = "blobRingH_$idx"
            )

            // Ring showing blob ellipse bounds
            val ringX = containerW * (livePct.x / 100f) - ringW / 2
            val ringY = containerH * (livePct.y / 100f) - ringH / 2
            Box(
                modifier = Modifier
                    .offset(x = ringX, y = ringY)
                    .size(width = ringW, height = ringH)
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isDragging) Color(0xFF4F46E5).copy(alpha = 0.8f)
                        else if (isActive) Color(0xFF4F46E5).copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.35f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                    )
            )

            // Draggable handle dot
            val handleX = containerW * (livePct.x / 100f) - handleSize / 2
            val handleY = containerH * (livePct.y / 100f) - handleSize / 2
            Box(
                modifier = Modifier
                    .offset(x = handleX, y = handleY)
                    .size(handleSize)
                    .shadow(
                        elevation = if (isDragging) 12.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = handleColor.copy(alpha = 0.4f),
                        spotColor = handleColor.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(handleColor.copy(alpha = if (isDragging) 1f else 0.9f))
                    .border(
                        width = if (isDragging) 3.dp else if (isActive) 2.dp else 1.5.dp,
                        color = if (isDragging) Color.White
                        else if (isActive) Color.White
                        else Color.White.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
                    .pointerInput(layer.id, idx, containerW, containerH) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                draggingIdx = idx
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val wPx = containerW.toPx().coerceAtLeast(1f)
                                val hPx = containerH.toPx().coerceAtLeast(1f)
                                val newX = (livePct.x + dragAmount.x / wPx * 100f).coerceIn(-50f, 150f)
                                val newY = (livePct.y + dragAmount.y / hPx * 100f).coerceIn(-50f, 150f)
                                livePct = Offset(newX, newY)
                                // Only update local canvas overlay — no ViewModel write here
                                onBlobDrag(idx, livePct)
                            },
                            onDragEnd = {
                                draggingIdx = -1
                                // Single ViewModel write on finger lift
                                onBlobDragEnd(idx, livePct)
                            },
                            onDragCancel = {
                                draggingIdx = -1
                                // Revert to model position on cancel
                                livePct = Offset(blob.x, blob.y)
                                onBlobDrag(idx, livePct)
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun MeshHandles(
    layer: GradientLayer,
    containerW: Dp,
    containerH: Dp,
    onPointDrag: (Int, Offset) -> Unit
) {
    layer.meshPoints.forEachIndexed { idx, point ->
        key(layer.id, idx) {
            var livePct by remember(layer.id, idx, point.x, point.y) {
                mutableStateOf(Offset(point.x, point.y))
            }

            val handleX = containerW * (livePct.x / 100f) - 12.dp
            val handleY = containerH * (livePct.y / 100f) - 12.dp

            Box(
                modifier = Modifier
                    .offset(x = handleX, y = handleY)
                    .size(24.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .border(2.dp, Color(0xFF4F46E5), CircleShape)
                    .pointerInput(layer.id, idx, containerW, containerH) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val wPx = containerW.toPx().coerceAtLeast(1f)
                            val hPx = containerH.toPx().coerceAtLeast(1f)
                            val newX = (livePct.x + dragAmount.x / wPx * 100f).coerceIn(0f, 100f)
                            val newY = (livePct.y + dragAmount.y / hPx * 100f).coerceIn(0f, 100f)
                            livePct = Offset(newX, newY)
                            onPointDrag(idx, livePct)
                        }
                    }
            )
        }
    }
}
