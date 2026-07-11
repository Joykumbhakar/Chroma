package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.chroma.studio.model.ColorStop
import com.chroma.studio.ui.theme.LocalChromaColors

/**
 * .stops-track-container { height: 32px; cursor: crosshair; }
 * .stops-track-bg { checkerboard behind the gradient preview, radius: --radius-sm }
 * .stop-thumb { positioned via left: <pos>%, draggable, tap opens the color picker;
 *   double-tap on empty track inserts a new stop — matching the JS drag/insert behavior. }
 */
@Composable
fun ColorStopEditor(
    stops: List<ColorStop>,
    onStopsChange: (List<ColorStop>) -> Unit
) {
    val colors = LocalChromaColors.current
    val sorted = stops.sortedBy { it.position }
    val shape = RoundedCornerShape(8.dp)
    var editingStopId by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(shape)
            .background(checkerboardBrush())
            .border(1.dp, colors.glassBorder, shape)
    ) {
        val trackWidth = maxWidth

        // gradient preview strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    Brush.linearGradient(
                        colorStops = sorted.map { (it.position / 100f) to it.color }.toTypedArray()
                    )
                )
                // double-tap empty space to insert a new stop at that position
                .pointerInput(stops) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            val pos = (tap.x / size.width * 100f).coerceIn(0f, 100f)
                            val nearestColor = sorted.minByOrNull { kotlin.math.abs(it.position - pos) }?.color ?: Color.White
                            onStopsChange(stops + ColorStop(color = nearestColor, position = pos))
                        }
                    )
                }
        )

        sorted.forEach { stop ->
            val stopOffset = trackWidth * (stop.position / 100f) - 9.dp
            Box(
                modifier = Modifier
                    .offset(x = stopOffset, y = 7.dp)
                    .size(18.dp)
                    .pointerInput(stop.id, trackWidth) {
                        val trackWidthPx = trackWidth.toPx()
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaPct = (dragAmount.x / trackWidthPx) * 100f
                            val newPos = (stop.position + deltaPct).coerceIn(0f, 100f)
                            onStopsChange(stops.map { if (it.id == stop.id) it.copy(position = newPos) else it })
                        }
                    }
                    .pointerInput(stop.id) {
                        detectTapGestures(
                            onTap = { editingStopId = stop.id },
                            onLongPress = {
                                // long-press removes a stop, mirroring the delete affordance in index.html,
                                // as long as at least 2 stops remain
                                if (stops.size > 2) onStopsChange(stops.filterNot { it.id == stop.id })
                            }
                        )
                    }
                    .clip(CircleShape)
                    .background(stop.color)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }

    val editing = sorted.find { it.id == editingStopId }
    if (editing != null) {
        ColorPickerDialog(
            initialColor = editing.color,
            onDismiss = { editingStopId = null },
            onConfirm = { newColor ->
                onStopsChange(stops.map { if (it.id == editing.id) it.copy(color = newColor) else it })
                editingStopId = null
            }
        )
    }
}

private fun checkerboardBrush(): Brush {
    // approximates the repeating-conic-gradient checker used behind alpha/stop tracks
    return Brush.linearGradient(
        listOf(Color(0xFFCBD5E1), Color(0xFFF8FAFC))
    )
}
