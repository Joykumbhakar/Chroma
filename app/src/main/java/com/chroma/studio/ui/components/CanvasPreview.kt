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
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.ColorBlindMode
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
 * colorBlindMode simulates #cb-select; halftone/dither mirror the Post FX toggles.
 */
@Composable
fun CanvasPreview(
    layers: List<GradientLayer>,
    shape: String,
    borderColor: Color,
    colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    halftoneEnabled: Boolean = false,
    ditherEnabled: Boolean = false,
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

    val textMeasurer = rememberTextMeasurer()
    val cbFilter = colorBlindColorFilter(colorBlindMode)

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

        if (shape == "text") {
            // Draw the whole gradient stack into an offscreen layer, then mask it down to
            // just the glyphs with BlendMode.DstIn — matching background-clip: text.
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), Paint())
                layers.filter { it.visible }.forEach { layer ->
                    drawRect(
                        brush = brushForLayer(layer, w, h, drift),
                        size = size,
                        alpha = layer.opacity,
                        blendMode = layer.blendMode.compose,
                        colorFilter = cbFilter
                    )
                }
                val layout = textMeasurer.measure(
                    text = "JOY",
                    style = TextStyle(fontSize = (h * 0.5f).toSp(), fontWeight = FontWeight.Black)
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset((w - layout.size.width) / 2f, (h - layout.size.height) / 2f),
                    blendMode = BlendMode.DstIn
                )
                canvas.restore()
            }
        } else {
            layers.filter { it.visible }.forEach { layer ->
                drawRect(
                    brush = brushForLayer(layer, w, h, drift),
                    size = size,
                    alpha = layer.opacity,
                    blendMode = layer.blendMode.compose,
                    colorFilter = cbFilter
                )
            }
        }

        if (halftoneEnabled) drawHalftoneOverlay(w, h)
        if (ditherEnabled) drawDitherOverlay(w, h)
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
        LayerType.MESH -> {
            // Approximates the mesh warp by radial-blending from the average of all
            // meshPoints rather than a true per-point mesh interpolation (see README).
            val avgX = layer.meshPoints.map { it.x }.average().toFloat().let { if (it.isNaN()) layer.centerX else it }
            val avgY = layer.meshPoints.map { it.y }.average().toFloat().let { if (it.isNaN()) layer.centerY else it }
            Brush.radialGradient(
                colorStops = stops,
                center = Offset(w * (avgX / 100f), h * (avgY / 100f)),
                radius = w.coerceAtLeast(h)
            )
        }
    }
}

/** Approximate color-blindness simulation matrices (simplified Brettel/Viénot-style). */
private fun colorBlindColorFilter(mode: ColorBlindMode): ColorFilter? {
    val matrix = when (mode) {
        ColorBlindMode.NONE -> return null
        ColorBlindMode.PROTANOPIA -> floatArrayOf(
            0.567f, 0.433f, 0f, 0f, 0f,
            0.558f, 0.442f, 0f, 0f, 0f,
            0f, 0.242f, 0.758f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        ColorBlindMode.DEUTERANOPIA -> floatArrayOf(
            0.625f, 0.375f, 0f, 0f, 0f,
            0.7f, 0.3f, 0f, 0f, 0f,
            0f, 0.3f, 0.7f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        ColorBlindMode.TRITANOPIA -> floatArrayOf(
            0.95f, 0.05f, 0f, 0f, 0f,
            0f, 0.433f, 0.567f, 0f, 0f,
            0f, 0.475f, 0.525f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
    return ColorFilter.colorMatrix(ColorMatrix(matrix))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHalftoneOverlay(w: Float, h: Float) {
    val spacing = 10.dp.toPx()
    var y = spacing / 2f
    var row = 0
    while (y < h) {
        var x = if (row % 2 == 0) spacing / 2f else spacing
        while (x < w) {
            drawCircle(color = Color.Black.copy(alpha = 0.12f), radius = spacing * 0.28f, center = Offset(x, y))
            x += spacing
        }
        y += spacing
        row++
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDitherOverlay(w: Float, h: Float) {
    val rng = kotlin.random.Random(42) // fixed seed so it doesn't shimmer every recomposition
    val dotCount = ((w * h) / 90f).toInt().coerceIn(200, 6000)
    repeat(dotCount) {
        val x = rng.nextFloat() * w
        val y = rng.nextFloat() * h
        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = 1.2f, center = Offset(x, y))
    }
}
