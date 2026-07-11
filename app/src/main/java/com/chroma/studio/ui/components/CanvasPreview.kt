package com.chroma.studio.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.ColorBlindMode
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import com.chroma.studio.model.PostProcessingFx
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.toArgb

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.platform.LocalDensity

@Composable
fun CanvasPreview(
    layers: List<GradientLayer>,
    shape: String,
    borderColor: Color,
    colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    postFxMode: PostProcessingFx = PostProcessingFx.NONE,
    reactOffset: Offset = Offset.Zero,
    blobDragOverrides: Map<Int, Offset> = emptyMap(),  // live drag positions (% units) per blob index
    textContent: String = "CHROMA",
    onTextContentChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
        else -> RoundedCornerShape(16.dp)
    }

    val textMeasurer = rememberTextMeasurer()
    val cbFilter = colorBlindColorFilter(colorBlindMode)

    val postFxModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && postFxMode != PostProcessingFx.NONE) {
        val shader = remember(postFxMode) { android.graphics.RuntimeShader(ShaderEngines.POST_FX_SHADER) }
        val modeInt = when (postFxMode) {
            PostProcessingFx.GRAIN -> 1
            PostProcessingFx.HALFTONE -> 2
            PostProcessingFx.DITHER -> 3
            else -> 0
        }
        Modifier.graphicsLayer {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", drift)
            shader.setIntUniform("mode", modeInt)
            renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .run {
                when (shape) {
                    "circle" -> aspectRatio(1f)
                    "full" -> fillMaxHeight()
                    "text" -> aspectRatio(3f / 1f)
                    else -> aspectRatio(3f / 2f)
                }
            }
            .run {
                if (shape == "full") this else shadow(elevation = 20.dp, shape = outerShape, ambientColor = Color(0x26000000), spotColor = Color(0x26000000))
            }
            .clip(outerShape)
            .run {
                if (shape == "full") this else border(1.dp, borderColor, outerShape)
            }
            .background(Color.White)
            .then(postFxModifier)
            .run {
                if (shape == "text") graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else this
            }
    ) {
        layers.filter { it.visible }.forEach { layer ->
            val liquidModifier = if (layer.type == LayerType.LIQUID && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.graphicsLayer {
                    val matrix = floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 18f, -7f * 255f
                    )
                    val blur = android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.CLAMP)
                    val colorMatrix = android.graphics.RenderEffect.createColorFilterEffect(android.graphics.ColorMatrixColorFilter(matrix))
                    renderEffect = android.graphics.RenderEffect.createChainEffect(colorMatrix, blur).asComposeRenderEffect()
                }
            } else Modifier

            Canvas(modifier = Modifier.fillMaxSize().then(liquidModifier)) {
                drawChromaLayer(layer, size.width, size.height, drift, cbFilter, reactOffset, blobDragOverrides)
            }
        }

        val density = LocalDensity.current.density
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (shape == "text") {
                val layout = textMeasurer.measure(
                    text = textContent.ifEmpty { " " },
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = (h / density * 0.5f).sp,
                        fontWeight = FontWeight.Black
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset((w - layout.size.width) / 2f, (h - layout.size.height) / 2f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }

            // Fallback for older devices without AGSL support
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (postFxMode == PostProcessingFx.HALFTONE) drawHalftoneOverlay(w, h)
                if (postFxMode == PostProcessingFx.DITHER) drawDitherOverlay(w, h)
            }
        }

        if (shape == "text") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = textContent,
                    onValueChange = onTextContentChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = (this@BoxWithConstraints.maxHeight.value * 0.5f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Transparent,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    singleLine = true
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChromaLayer(
    layer: GradientLayer, w: Float, h: Float, drift: Float,
    cbFilter: ColorFilter?, reactOffset: Offset = Offset.Zero,
    blobDragOverrides: Map<Int, Offset> = emptyMap()
) {
    val stops = layer.stops.sortedBy { it.position }.map { (it.position / 100f).coerceIn(0f, 1f) to it.color }
    if (stops.isEmpty()) return
    val stopPairs = stops.toTypedArray()

    val paint = androidx.compose.ui.graphics.Paint().apply {
        alpha = layer.opacity
        blendMode = layer.blendMode.compose
        colorFilter = cbFilter
    }

    drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, w, h), paint)
    
    // Apply reactivity offset if blob
    if (layer.type == LayerType.BLOB || layer.type == LayerType.LIQUID) {
        drawContext.transform.translate(reactOffset.x, reactOffset.y)
    }

    when (layer.type) {
        LayerType.LINEAR -> {
            val rad = Math.toRadians(layer.angle.toDouble())
            val dx = kotlin.math.cos(rad).toFloat()
            val dy = kotlin.math.sin(rad).toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val len = maxOf(w, h)
            val brush = Brush.linearGradient(colorStops = stopPairs, start = Offset(cx - dx * len / 2f, cy - dy * len / 2f), end = Offset(cx + dx * len / 2f, cy + dy * len / 2f))
            drawRect(brush)
        }
        LayerType.RADIAL -> {
            val rw = w * (layer.width / 100f)
            val rh = h * (layer.height / 100f)
            val center = Offset(w * (layer.centerX / 100f), h * (layer.centerY / 100f))
            withTransform({
                translate(center.x, center.y)
                scale(scaleX = 1f, scaleY = rh / rw.coerceAtLeast(1f))
                translate(-center.x, -center.y)
            }) {
                val radius = rw.coerceAtLeast(1f)
                val brush = Brush.radialGradient(colorStops = stopPairs, center = center, radius = radius)
                drawRect(brush, topLeft = Offset(-w * 2, -h * 2), size = androidx.compose.ui.geometry.Size(w * 5, h * 5))
            }
        }
        LayerType.CONIC -> {
            withTransform({
                rotate(degrees = layer.angle, pivot = Offset(w * (layer.centerX / 100f), h * (layer.centerY / 100f)))
            }) {
                val brush = Brush.sweepGradient(colorStops = stopPairs, center = Offset(w * (layer.centerX / 100f), h * (layer.centerY / 100f)))
                drawRect(brush, topLeft = Offset(-w * 2, -h * 2), size = androidx.compose.ui.geometry.Size(w * 5, h * 5))
            }
        }
        LayerType.MESH -> {
            val c = layer.columns.coerceAtLeast(1)
            val r = layer.rows.coerceAtLeast(1)
            for (i in layer.stops.indices) {
                val stop = layer.stops[i]
                val col = i % c
                val row = i / c
                val x = if (c > 1) (col.toFloat() / (c - 1)) * w else w / 2f
                val y = if (r > 1) (row.toFloat() / (r - 1)) * h else h / 2f
                val sz = (150f / maxOf(c, r)) / 100f * maxOf(w, h)
                val brush = Brush.radialGradient(colors = listOf(stop.color, Color.Transparent), center = Offset(x, y), radius = sz.coerceAtLeast(1f))
                drawRect(brush, topLeft = Offset(x - sz, y - sz), size = androidx.compose.ui.geometry.Size(sz * 2, sz * 2))
            }
        }
        LayerType.BLOB, LayerType.LIQUID -> {
            if (layer.hasBaseBackground) drawRect(layer.blobBgColor)
            layer.blobs.forEachIndexed { i, blob ->
                val stop = layer.stops.getOrNull(i) ?: layer.stops.firstOrNull() ?: return@forEachIndexed
                // Use drag override position if actively dragging this blob
                val overridePos = blobDragOverrides[i]
                val bx = (overridePos?.x ?: blob.x) / 100f * w
                val by = (overridePos?.y ?: blob.y) / 100f * h
                val bw = w * (blob.width / 100f)
                val bh = h * (blob.height / 100f)
                val hardStopFrac = ((100f - blob.feather) / 100f).coerceIn(0f, 0.99f)
                val effectiveAlpha = stop.color.alpha * blob.opacity
                val blobColor = stop.color.copy(alpha = effectiveAlpha.coerceIn(0f, 1f))
                val colorStops = if (hardStopFrac > 0.01f) {
                    arrayOf(0f to blobColor, hardStopFrac to blobColor, 1f to Color.Transparent)
                } else {
                    arrayOf(0f to blobColor, 1f to Color.Transparent)
                }
                val radius = bw.coerceAtLeast(1f)
                val brush = Brush.radialGradient(
                    colorStops = colorStops,
                    center = Offset(bx, by),
                    radius = radius
                )
                // Apply rotation + elliptical scaling centered on the blob
                val needsRotate = blob.rotation != 0f
                val needsScale = kotlin.math.abs(bw - bh) >= 1f
                if (!needsRotate && !needsScale) {
                    drawRect(brush, topLeft = Offset(-w * 2, -h * 2), size = androidx.compose.ui.geometry.Size(w * 5, h * 5))
                } else {
                    withTransform({
                        translate(bx, by)
                        if (needsRotate) rotate(degrees = blob.rotation, pivot = Offset.Zero)
                        if (needsScale) scale(scaleX = 1f, scaleY = bh / radius)
                        translate(-bx, -by)
                    }) {
                        drawRect(brush, topLeft = Offset(-w * 2, -h * 2), size = androidx.compose.ui.geometry.Size(w * 5, h * 5))
                    }
                }
            }
        }

        LayerType.AURORA -> {
            val nW = minOf(4, layer.stops.size)
            val bri = layer.brightness / 100f
            val cmpl = layer.complexity / 10f
            val time = drift * layer.animSpeed / 100f
            for (wi in 0 until nW) {
                val stop = layer.stops.getOrNull(wi % layer.stops.size) ?: continue
                val phase = wi * (Math.PI * 2 / nW) + time * (0.5 + wi * 0.3)
                val yBase = h * (0.15 + wi * 0.14)
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        val xn = x / w
                        var y = yBase.toFloat()
                        for (k in 1..cmpl.toInt().coerceAtLeast(1)) {
                            y += (kotlin.math.sin(xn * Math.PI * 2 * k + phase + k * 0.5) * h * (0.08 + 0.04 / k)).toFloat()
                            y += (kotlin.math.cos(xn * Math.PI * 3 * k + time * (0.3 + k * 0.1)) * h * (0.04 / k)).toFloat()
                        }
                        lineTo(x, y)
                        x += 3f
                    }
                    lineTo(w, h)
                    close()
                }
                val brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to stop.color.copy(alpha = 0f),
                        0.3f to stop.color.copy(alpha = (bri * 0.85f).coerceIn(0f, 1f)),
                        0.7f to stop.color.copy(alpha = (bri * 0.35f).coerceIn(0f, 1f)),
                        1f to stop.color.copy(alpha = 0f)
                    ),
                    startY = (yBase - h * 0.3).toFloat(),
                    endY = (yBase + h * 0.5).toFloat()
                )
                drawPath(path, brush, blendMode = BlendMode.Screen)
            }
        }
    }
    
    if (layer.type == LayerType.BLOB || layer.type == LayerType.LIQUID) {
        drawContext.transform.translate(-reactOffset.x, -reactOffset.y)
    }
    drawContext.canvas.restore()
}

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
    val rng = kotlin.random.Random(42)
    val dotCount = ((w * h) / 90f).toInt().coerceIn(200, 6000)
    repeat(dotCount) {
        val x = rng.nextFloat() * w
        val y = rng.nextFloat() * h
        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = 1.2f, center = Offset(x, y))
    }
}
