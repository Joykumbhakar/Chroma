package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.chroma.studio.model.ColorStop
import com.chroma.studio.ui.theme.LocalChromaColors

/**
 * .stops-track-container { height: 32px; }
 * .stops-track-bg { checkerboard background for alpha preview, radius: --radius-sm }
 * .stop-thumb { positioned via left: <pos>%, 24x42 grab handle }
 */
@Composable
fun ColorStopEditor(stops: List<ColorStop>) {
    val colors = LocalChromaColors.current
    val sorted = stops.sortedBy { it.position }
    val shape = RoundedCornerShape(8.dp)

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
        )

        sorted.forEach { stop ->
            val stopOffset = trackWidth * (stop.position / 100f) - 9.dp
            Box(
                modifier = Modifier
                    .offset(x = stopOffset, y = 7.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(stop.color)
                    .border(2.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
            )
        }
    }
}

private fun checkerboardBrush(): Brush {
    // approximates the repeating-conic-gradient checker used behind alpha/stop tracks
    return Brush.linearGradient(
        listOf(
            androidx.compose.ui.graphics.Color(0xFFCBD5E1),
            androidx.compose.ui.graphics.Color(0xFFF8FAFC)
        )
    )
}
