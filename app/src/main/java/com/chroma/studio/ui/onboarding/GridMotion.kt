package com.chroma.studio.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

sealed interface GridMotionTile {
    data class ImageRes(
        @param:DrawableRes val resId: Int,
        val contentDescription: String = ""
    ) : GridMotionTile

    data class StudioText(
        val text: String,
        val subtitle: String? = null,
        val accent: Color = Color(0xFF38BDF8)
    ) : GridMotionTile

    data class Custom(
        val content: @Composable BoxScope.() -> Unit
    ) : GridMotionTile

    data object Empty : GridMotionTile
}

enum class GridMotionDirection {
    Alternate,
    Left,
    Right
}

private val RowInertia = floatArrayOf(0.42f, 0.32f, 0.26f, 0.22f)
private val RowPace = floatArrayOf(1f, 0.86f, 1.12f, 0.94f)

private fun wrapOffset(value: Float, total: Float): Float {
    if (total <= 0f) return 0f
    return ((value % total) + total) % total
}

private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Jetpack Compose port of React Bits' <GridMotion /> (Showcase + 3D Tilt capable):
 * Diagonal 3D-tilted gallery wall with alternating row velocity, touch-drag parallax inertia,
 * 3D-projected spotlight brightening, dim contrast scrim, and soft feathered edge masks.
 */
@Composable
fun GridMotion(
    items: List<GridMotionTile>,
    modifier: Modifier = Modifier,
    rows: Int = 4,
    aspectRatio: Float = 1.33f,
    gap: Dp = 16.dp,
    radius: Dp = 14.dp,
    angle: Float = -12f,
    tilt: Float = 0f,
    speedDpPerSec: Float = 24f,
    direction: GridMotionDirection = GridMotionDirection.Alternate,
    parallax: Float = 0.5f,
    spotlight: Float = 0.6f,
    dim: Float = 0.35f,
    fade: Float = 0.5f,
    tileColor: Color = Color(0xFF16161D),
    textColor: Color = Color.White.copy(alpha = 0.84f),
    borderColor: Color = Color.White.copy(alpha = 0.08f)
) {
    val density = LocalDensity.current
    val safeRows = rows.coerceIn(1, 12)
    val safeAspect = aspectRatio.coerceIn(0.3f, 4f)
    val safeTilt = tilt.coerceIn(0f, 75f)
    val gapPx = with(density) { gap.toPx().coerceAtLeast(0f) }
    val speedPx = with(density) { speedDpPerSec.dp.toPx() }
    val safeParallax = parallax.coerceIn(0f, 1f)
    val safeSpotlight = spotlight.coerceIn(0f, 1f)
    val safeDim = dim.coerceIn(0f, 1f)
    val safeFade = fade.coerceIn(0f, 1f)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val itemsSizeOrDefault = if (items.isNotEmpty()) items.size else 6

    val layout = remember(containerSize, safeRows, safeAspect, gapPx, angle, safeTilt, itemsSizeOrDefault) {
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        if (w <= 0f || h <= 0f) {
            null
        } else {
            val turn = (angle * PI / 180.0).toFloat()
            val lean = (safeTilt * PI / 180.0).toFloat()
            val c = abs(cos(turn))
            val s = abs(sin(turn))
            val planeH = ((w * s + h * c) / max(0.35f, cos(lean))) * 1.25f
            val planeW = (w * c + h * s) * (1f + sin(lean) * 0.9f) * 1.25f
            val tileH = max(18f, (planeH - gapPx * (safeRows - 1)) / safeRows)
            val tileW = tileH * safeAspect
            val step = tileW + gapPx
            val perRow = max(itemsSizeOrDefault, ceil(planeW / step).toInt() + 2)
            val period = perRow * step
            val top = (planeH - safeRows * tileH - (safeRows - 1) * gapPx) / 2f
            val perspective = max(w, h) * 1.6f
            GridMotionLayout(
                planeW = planeW,
                planeH = planeH,
                tileW = tileW,
                tileH = tileH,
                step = step,
                perRow = perRow,
                period = period,
                copies = 3,
                top = top,
                perspective = perspective
            )
        }
    }

    val rowOffsets = remember(safeRows) { FloatArray(safeRows) }
    val rowShifts = remember(safeRows) { FloatArray(safeRows) }
    val rowXState = remember(safeRows) { List(safeRows) { mutableFloatStateOf(0f) } }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var pointerActive by remember { mutableStateOf(false) }
    var dragNormalized by remember { mutableFloatStateOf(0f) }
    var introAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(layout, speedPx, direction, safeParallax) {
        val l = layout ?: return@LaunchedEffect
        for (r in 0 until safeRows) {
            if (rowOffsets[r] == 0f) {
                rowOffsets[r] = r * l.step * 0.37f
            }
        }
        var lastNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (lastNs == 0L) lastNs = frameNs
                val dt = ((frameNs - lastNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNs = frameNs

                if (introAlpha < 1f) {
                    introAlpha = (introAlpha + dt / 0.85f).coerceAtMost(1f)
                }

                if (!pointerActive && abs(dragNormalized) > 0.001f) {
                    dragNormalized *= exp(-dt / 0.6f)
                }

                for (r in 0 until safeRows) {
                    val dirSign = when (direction) {
                        GridMotionDirection.Left -> -1f
                        GridMotionDirection.Right -> 1f
                        GridMotionDirection.Alternate -> if (r % 2 == 1) 1f else -1f
                    }
                    val pace = RowPace[r % RowPace.size]
                    rowOffsets[r] += speedPx * dirSign * pace * dt

                    val targetShift = safeParallax * l.tileW * 1.15f * dragNormalized * (if (r % 2 == 1) -1f else 1f)
                    val inertia = RowInertia[r % RowInertia.size]
                    rowShifts[r] += (targetShift - rowShifts[r]) * (1f - exp(-dt / inertia))

                    rowXState[r].floatValue = -l.period + wrapOffset(rowOffsets[r] + rowShifts[r], l.period)
                }
            }
        }
    }

    val horizontalEdgeZone = (safeFade * 0.34f).coerceIn(0f, 0.45f)
    val verticalEdgeZone = (safeFade * 0.44f).coerceIn(0f, 0.48f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .pointerInput(containerSize) {
                detectDragGestures(
                    onDragStart = { start ->
                        pointerActive = true
                        pointerPos = start
                    },
                    onDragEnd = { pointerActive = false },
                    onDragCancel = { pointerActive = false },
                    onDrag = { change, dragAmount ->
                        pointerPos = change.position
                        val halfW = (containerSize.width / 2f).coerceAtLeast(1f)
                        dragNormalized = (dragNormalized + dragAmount.x / halfW).coerceIn(-1f, 1f)
                    }
                )
            }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                if (horizontalEdgeZone > 0.01f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            horizontalEdgeZone to Color.Black,
                            (1f - horizontalEdgeZone) to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                if (verticalEdgeZone > 0.01f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            (verticalEdgeZone * 0.45f) to Color.Black,
                            (1f - verticalEdgeZone) to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) {
        if (layout != null) {
            val planeWidthDp = with(density) { layout.planeW.toDp() }
            val planeHeightDp = with(density) { layout.planeH.toDp() }
            val tileWidthDp = with(density) { layout.tileW.toDp() }
            val tileHeightDp = with(density) { layout.tileH.toDp() }
            val fontSizeSp = with(density) {
                (layout.tileH * 0.13f).toSp().let { spVal ->
                    if (spVal.value < 11f) 11.sp else if (spVal.value > 22f) 22.sp else spVal
                }
            }

            // 3D un-projection matching React Bits flatX / flatY
            val activeSpotPoint = if (pointerActive) {
                pointerPos
            } else {
                Offset(containerSize.width * 0.5f, containerSize.height * 0.34f)
            }
            val turn = (angle * PI / 180.0).toFloat()
            val lean = (safeTilt * PI / 180.0).toFloat()
            val px = activeSpotPoint.x - containerSize.width / 2f
            val py = activeSpotPoint.y - containerSize.height / 2f
            var flatY = py
            var flatX = px
            if (lean > 0.0001f) {
                val depth = layout.perspective
                val denom = (depth * cos(lean) + py * sin(lean)).coerceAtLeast(depth * 0.15f)
                flatY = (py * depth) / denom
                flatX = (px * (depth - flatY * sin(lean))) / depth
            }
            val planeSpotX = flatX * cos(turn) + flatY * sin(turn)
            val planeSpotY = -flatX * sin(turn) + flatY * cos(turn)
            val reach = (0.6f + 2.2f * safeSpotlight) * max(layout.tileW, layout.tileH)

            // Outer wrapper applies 3D tilt (rotationX + deep cameraDistance) before inner Z rotation
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredSize(planeWidthDp, planeHeightDp)
                    .graphicsLayer {
                        val t = introAlpha.coerceIn(0f, 1f)
                        alpha = 1f - (1f - t) * (1f - t) * (1f - t)
                        rotationX = safeTilt
                        cameraDistance = 32f * density.density
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(planeWidthDp, planeHeightDp)
                        .graphicsLayer {
                            rotationZ = angle
                        }
                ) {
                    val totalPerRow = layout.copies * layout.perRow
                    val tileShape = RoundedCornerShape(radius)

                    for (r in 0 until safeRows) {
                        val topDp = with(density) { (layout.top + r * (layout.tileH + gapPx)).toDp() }
                        val rowY = layout.top + r * (layout.tileH + gapPx) + layout.tileH / 2f - layout.planeH / 2f

                        Row(
                            modifier = Modifier
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .offset(y = topDp)
                                .offset { IntOffset(rowXState[r].floatValue.roundToInt(), 0) },
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (k in 0 until totalPerRow) {
                                val tileIdx = if (items.isEmpty()) 0 else ((r * 3 + (k % layout.perRow)) % items.size)
                                val tile = if (items.isEmpty()) GridMotionTile.Empty else items[tileIdx]

                                Box(
                                    modifier = Modifier
                                        .size(tileWidthDp, tileHeightDp)
                                        .graphicsLayer {
                                            val rowX = rowXState[r].floatValue
                                            val tileCenterX = rowX + k * layout.step - layout.planeW / 2f + layout.tileW / 2f
                                            val spotFactor = if (safeSpotlight > 0f) {
                                                val dist = hypot(tileCenterX - planeSpotX, rowY - planeSpotY) / reach
                                                1f - smoothStep(0.25f, 1f, dist)
                                            } else 0f
                                            alpha = 1f - safeDim * (1f - spotFactor)
                                        }
                                        .clip(tileShape)
                                        .background(tileColor)
                                        .border(1.dp, borderColor, tileShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (tile) {
                                        is GridMotionTile.ImageRes -> {
                                            Image(
                                                painter = painterResource(id = tile.resId),
                                                contentDescription = tile.contentDescription,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        is GridMotionTile.StudioText -> {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(16.dp)
                                                        .height(2.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(tile.accent)
                                                )
                                                Text(
                                                    text = tile.text,
                                                    color = textColor,
                                                    fontSize = fontSizeSp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center,
                                                    letterSpacing = (-0.3).sp
                                                )
                                                if (tile.subtitle != null) {
                                                    Text(
                                                        text = tile.subtitle,
                                                        color = textColor.copy(alpha = 0.55f),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                        is GridMotionTile.Custom -> tile.content(this)
                                        GridMotionTile.Empty -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
private data class GridMotionLayout(
    val planeW: Float,
    val planeH: Float,
    val tileW: Float,
    val tileH: Float,
    val step: Float,
    val perRow: Int,
    val period: Float,
    val copies: Int,
    val top: Float,
    val perspective: Float
)
