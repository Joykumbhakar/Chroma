package com.chroma.studio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import kotlin.math.cos
import kotlin.math.sin

/**
 * #canvas-container {
 *   aspect-ratio: 3/2; max-width: 600px; border-radius: 16px (rounded-2xl);
 *   box-shadow: 0 10px 40px rgba(0,0,0,.15); border: 1px solid var(--glass-border);
 * }
 * Shape switches with the mobile pills (#m-shape-rounded/circle/full/text) -> canvasShape.
 */
@Composable
fun CanvasPreview(
    layers: List<GradientLayer>,
    shape: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    // Loops the CSS aurora/animated-gradient keyframes at low intensity so the preview
    // never looks static, similar to the running animation engine in index.html.
    val infinite = rememberInfiniteTransition(label = "auroraDrift")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    val outerShape: Shape = when (shape) {
        "circle" -> CircleShape
        "full" -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(16.dp) // "rounded" / "text"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
            .shadow(elevation = 20.dp, shape = outerShape, ambientColor = Color(0x26000000), spotColor = Color(0x26000000))
            .clip(outerShape)
            .border(1.dp, borderColor, outerShape)
            .background(Color.White)
    ) {
        val w = size.width
        val h = size.height

        layers.filter { it.visible }.forEach { layer ->
            drawRect(
                brush = brushForLayer(layer, w, h, drift),
                size = size,
                alpha = layer.opacity,
                blendMode = layer.blendMode.compose
            )
        }
    }
}

/** Recreates each LayerType's gradient using Compose's built-in Brush constructors. */
private fun brushForLayer(layer: GradientLayer, w: Float, h: Float, drift: Float): Brush {
    val stops = layer.stops.sortedBy { it.position }
        .map { (it.position / 100f).coerceIn(0f, 1f) to it.color }
        .toTypedArray()
    if (stops.isEmpty()) return Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

    return when (layer.type) {
        LayerType.LINEAR -> {
            val rad = Math.toRadians(layer.angle.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val len = (w.coerceAtLeast(h))
            Brush.linearGradient(
                colorStops = stops,
                start = Offset(cx - dx * len / 2f, cy - dy * len / 2f),
                end = Offset(cx + dx * len / 2f, cy + dy * len / 2f)
            )
        }
        LayerType.RADIAL, LayerType.BLOB -> Brush.radialGradient(
            colorStops = stops,
            center = Offset(w * (layer.centerX / 100f), h * (layer.centerY / 100f)),
            radius = (w.coerceAtLeast(h)) * 0.75f
        )
        LayerType.CONIC -> Brush.sweepGradient(
            colorStops = stops,
            center = Offset(w * (layer.centerX / 100f), h * (layer.centerY / 100f))
        )
        LayerType.AURORA -> Brush.sweepGradient(
            colorStops = stops,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        LayerType.MESH -> Brush.radialGradient(
            colorStops = stops,
            center = Offset(w * (layer.centerX / 100f), h * (layer.centerY / 100f)),
            radius = w.coerceAtLeast(h)
        )
    }
}
