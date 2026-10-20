package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * High-level grouping for the 25 video editor transitions.
 */
enum class TransitionCategory(val label: String) {
    ALL("All (25)"),
    DISSOLVE("Dissolve"),
    SLIDE_PUSH("Slide & Push"),
    WIPE("Geometric Wipe"),
    IRIS_SHAPE("Iris & Shape"),
    ZOOM_SPIN("Zoom & Spin"),
    STYLIZED_3D("3D & Stylized")
}

/**
 * Mathematical easing curves applied to the normalized [0f..1f] transition progress.
 */
enum class TransitionEasing(val label: String) {
    APPLE_SMOOTH("Smooth"),
    LINEAR("Linear"),
    CUBIC_IN_OUT("Cubic"),
    CINEMATIC_SNAP("Snap")
}

/**
 * Evaluates the selected [TransitionEasing] for a raw progress value in [0f..1f].
 */
fun TransitionEasing.transform(rawProgress: Float): Float {
    val t = rawProgress.coerceIn(0f, 1f)
    return when (this) {
        TransitionEasing.LINEAR -> t
        TransitionEasing.APPLE_SMOOTH -> {
            // Smooth S-curve (Hermite interpolation)
            t * t * (3f - 2f * t)
        }
        TransitionEasing.CUBIC_IN_OUT -> {
            if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
        }
        TransitionEasing.CINEMATIC_SNAP -> {
            // Fast acceleration in the middle with gentle landings
            val shifted = (t - 0.5f) * 2f
            0.5f + 0.5f * (shifted * shifted * shifted * 0.4f + shifted * 0.6f)
        }
    }
}

/**
 * Complete catalog of 25 real-time video transition effects for an Android video editor.
 */
enum class VideoTransitionEffect(
    val id: String,
    val displayName: String,
    val badgeCode: String,
    val category: TransitionCategory,
    val defaultDurationMs: Int = 800
) {
    // --- 1. DISSOLVE & FADE (4 Effects) ---
    CROSS_DISSOLVE(
        id = "cross_dissolve",
        displayName = "Cross Dissolve",
        badgeCode = "XD",
        category = TransitionCategory.DISSOLVE,
        defaultDurationMs = 800
    ),
    DIP_TO_BLACK(
        id = "dip_to_black",
        displayName = "Fade to Black",
        badgeCode = "FB",
        category = TransitionCategory.DISSOLVE,
        defaultDurationMs = 1000
    ),
    DIP_TO_WHITE(
        id = "dip_to_white",
        displayName = "Dip to White",
        badgeCode = "FW",
        category = TransitionCategory.DISSOLVE,
        defaultDurationMs = 900
    ),
    FILM_BLOOM_FADE(
        id = "film_bloom_fade",
        displayName = "Light Bloom",
        badgeCode = "LB",
        category = TransitionCategory.DISSOLVE,
        defaultDurationMs = 900
    ),

    // --- 2. SLIDE & PUSH (6 Effects) ---
    PUSH_LEFT(
        id = "push_left",
        displayName = "Push Left",
        badgeCode = "PL",
        category = TransitionCategory.SLIDE_PUSH,
        defaultDurationMs = 700
    ),
    PUSH_RIGHT(
        id = "push_right",
        displayName = "Push Right",
        badgeCode = "PR",
        category = TransitionCategory.SLIDE_PUSH,
        defaultDurationMs = 700
    ),
    PUSH_UP(
        id = "push_up",
        displayName = "Push Up",
        badgeCode = "PU",
        category = TransitionCategory.SLIDE_PUSH,
        defaultDurationMs = 700
    ),
    PUSH_DOWN(
        id = "push_down",
        displayName = "Push Down",
        badgeCode = "PD",
        category = TransitionCategory.SLIDE_PUSH,
        defaultDurationMs = 700
    ),
    COVER_SLIDE_LEFT(
        id = "cover_slide_left",
        displayName = "Cover Slide",
        badgeCode = "CS",
        category = TransitionCategory.SLIDE_PUSH,
        defaultDurationMs = 750
    ),
    REVEAL_PARALLAX(
        id = "reveal_parallax",
        displayName = "Parallax Reveal",
        badgeCode = "PX",
        category = TransitionCategory.SLIDE_PUSH,
        defaultDurationMs = 850
    ),

    // --- 3. GEOMETRIC WIPES (5 Effects) ---
    WIPE_HORIZONTAL(
        id = "wipe_horizontal",
        displayName = "Linear Wipe",
        badgeCode = "LW",
        category = TransitionCategory.WIPE,
        defaultDurationMs = 750
    ),
    WIPE_DIAGONAL(
        id = "wipe_diagonal",
        displayName = "Diagonal Wipe",
        badgeCode = "DW",
        category = TransitionCategory.WIPE,
        defaultDurationMs = 800
    ),
    CLOCK_RADIAL_WIPE(
        id = "clock_radial_wipe",
        displayName = "Clock Sweep",
        badgeCode = "CW",
        category = TransitionCategory.WIPE,
        defaultDurationMs = 900
    ),
    BARN_DOORS_SPLIT(
        id = "barn_doors_split",
        displayName = "Barn Doors",
        badgeCode = "BD",
        category = TransitionCategory.WIPE,
        defaultDurationMs = 800
    ),
    VENETIAN_BLINDS(
        id = "venetian_blinds",
        displayName = "Venetian Blinds",
        badgeCode = "VB",
        category = TransitionCategory.WIPE,
        defaultDurationMs = 850
    ),

    // --- 4. IRIS & SHAPE APERTURES (4 Effects) ---
    CIRCLE_IRIS_OPEN(
        id = "circle_iris_open",
        displayName = "Circle Iris In",
        badgeCode = "CI",
        category = TransitionCategory.IRIS_SHAPE,
        defaultDurationMs = 850
    ),
    CIRCLE_PINHOLE_CLOSE(
        id = "circle_pinhole_close",
        displayName = "Pinhole Snap",
        badgeCode = "PS",
        category = TransitionCategory.IRIS_SHAPE,
        defaultDurationMs = 1000
    ),
    DIAMOND_SHUTTER(
        id = "diamond_shutter",
        displayName = "Diamond Iris",
        badgeCode = "DI",
        category = TransitionCategory.IRIS_SHAPE,
        defaultDurationMs = 850
    ),
    CORNER_QUAD_SPLIT(
        id = "corner_quad_split",
        displayName = "Quad Shutter",
        badgeCode = "QS",
        category = TransitionCategory.IRIS_SHAPE,
        defaultDurationMs = 900
    ),

    // --- 5. ZOOM & SPIN WARP (3 Effects) ---
    CROSS_ZOOM_IN(
        id = "cross_zoom_in",
        displayName = "Crash Zoom In",
        badgeCode = "ZI",
        category = TransitionCategory.ZOOM_SPIN,
        defaultDurationMs = 650
    ),
    CROSS_ZOOM_OUT(
        id = "cross_zoom_out",
        displayName = "Pull Zoom Out",
        badgeCode = "ZO",
        category = TransitionCategory.ZOOM_SPIN,
        defaultDurationMs = 650
    ),
    WHIP_SPIN_ROTATE(
        id = "whip_spin_rotate",
        displayName = "Whip Spin",
        badgeCode = "WS",
        category = TransitionCategory.ZOOM_SPIN,
        defaultDurationMs = 750
    ),

    // --- 6. 3D & STYLIZED EFFECTS (3 Effects) ---
    CUBE_ROTATE_3D(
        id = "cube_rotate_3d",
        displayName = "3D Cube Spin",
        badgeCode = "3D",
        category = TransitionCategory.STYLIZED_3D,
        defaultDurationMs = 900
    ),
    FLIP_CARD_3D(
        id = "flip_card_3d",
        displayName = "3D Page Flip",
        badgeCode = "PF",
        category = TransitionCategory.STYLIZED_3D,
        defaultDurationMs = 900
    ),
    GLITCH_SLICE_SHIFT(
        id = "glitch_slice_shift",
        displayName = "Glitch Slice",
        badgeCode = "GL",
        category = TransitionCategory.STYLIZED_3D,
        defaultDurationMs = 650
    )
}

/**
 * Builds the geometric [Path] mask for wipe, iris, and shutter transitions.
 */
fun buildGeometricMaskPath(
    effect: VideoTransitionEffect,
    progress: Float,
    size: Size
): Path {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    return Path().apply {
        when (effect) {
            VideoTransitionEffect.WIPE_HORIZONTAL -> {
                addRect(Rect(left = 0f, top = 0f, right = w * progress, bottom = h))
            }

            VideoTransitionEffect.WIPE_DIAGONAL -> {
                // Sweeps a 45-degree diagonal polygon from top-left to bottom-right
                val sweep = (w + h) * progress
                moveTo(0f, 0f)
                lineTo(sweep, 0f)
                lineTo(0f, sweep)
                close()
            }

            VideoTransitionEffect.CLOCK_RADIAL_WIPE -> {
                val maxRadius = hypot(cx, cy) * 1.25f
                val ovalRect = Rect(
                    left = cx - maxRadius,
                    top = cy - maxRadius,
                    right = cx + maxRadius,
                    bottom = cy + maxRadius
                )
                moveTo(cx, cy)
                arcTo(
                    rect = ovalRect,
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 360f * progress,
                    forceMoveTo = false
                )
                close()
            }

            VideoTransitionEffect.BARN_DOORS_SPLIT -> {
                val halfSpan = (w / 2f) * progress
                addRect(Rect(left = cx - halfSpan, top = 0f, right = cx + halfSpan, bottom = h))
            }

            VideoTransitionEffect.VENETIAN_BLINDS -> {
                val slatCount = 6
                val slatHeight = h / slatCount
                for (i in 0 until slatCount) {
                    val topY = i * slatHeight
                    // Stagger each slat slightly for a realistic mechanical blind motion
                    val localProgress = ((progress * 1.25f) - (i * 0.04f)).coerceIn(0f, 1f)
                    addRect(
                        Rect(
                            left = 0f,
                            top = topY,
                            right = w,
                            bottom = topY + slatHeight * localProgress
                        )
                    )
                }
            }

            VideoTransitionEffect.CIRCLE_IRIS_OPEN -> {
                val maxRadius = hypot(cx, cy)
                addOval(Rect(center = Offset(cx, cy), radius = maxRadius * progress))
            }

            VideoTransitionEffect.DIAMOND_SHUTTER -> {
                val maxSpan = (w + h) * 0.78f * progress
                moveTo(cx, cy - maxSpan)
                lineTo(cx + maxSpan, cy)
                lineTo(cx, cy + maxSpan)
                lineTo(cx - maxSpan, cy)
                close()
            }

            VideoTransitionEffect.CORNER_QUAD_SPLIT -> {
                val quadW = cx * progress
                val quadH = cy * progress
                // Top-left quadrant expanding from center outward
                addRect(Rect(left = cx - quadW, top = cy - quadH, right = cx, bottom = cy))
                // Top-right quadrant
                addRect(Rect(left = cx, top = cy - quadH, right = cx + quadW, bottom = cy))
                // Bottom-left quadrant
                addRect(Rect(left = cx - quadW, top = cy, right = cx, bottom = cy + quadH))
                // Bottom-right quadrant
                addRect(Rect(left = cx, top = cy, right = cx + quadW, bottom = cy + quadH))
            }

            else -> {
                addRect(Rect(0f, 0f, w * progress, h))
            }
        }
    }
}

/**
 * Generates horizontal scanline slices for [VideoTransitionEffect.GLITCH_SLICE_SHIFT].
 */
fun buildGlitchSliceMaskPath(progress: Float, size: Size): Path {
    val bandCount = 8
    val bandHeight = size.height / bandCount
    return Path().apply {
        for (i in 0 until bandCount) {
            val stagger = ((i * 37) % 10) / 25f
            val local = ((progress * 1.35f) - stagger).coerceIn(0f, 1f)
            if (local > 0f) {
                val yTop = i * bandHeight
                val sliceWidth = size.width * local
                val fromLeft = i % 2 == 0
                val left = if (fromLeft) 0f else size.width - sliceWidth
                addRect(
                    Rect(
                        left = left,
                        top = yTop,
                        right = left + sliceWidth,
                        bottom = yTop + bandHeight
                    )
                )
            }
        }
    }
}

/**
 * Renders any of the 25 [VideoTransitionEffect]s between [outgoingClip] (Clip A)
 * and [incomingClip] (Clip B) at the given normalized [progress] (`0f..1f`).
 *
 * Designed for direct integration into a Jetpack Compose video preview player or timeline renderer.
 *
 * @param effect The active [VideoTransitionEffect] to apply.
 * @param progress Normalized transition progress from `0.0f` (100% Clip A) to `1.0f` (100% Clip B).
 * @param modifier Layout modifier for the video viewport container.
 * @param easing Curve applied to [progress] for natural motion physics.
 * @param outgoingClip Composable content representing the ending frames of Clip A.
 * @param incomingClip Composable content representing the starting frames of Clip B.
 */
@Composable
fun VideoTransitionRenderer(
    effect: VideoTransitionEffect,
    progress: Float,
    modifier: Modifier = Modifier,
    easing: TransitionEasing = TransitionEasing.APPLE_SMOOTH,
    outgoingClip: @Composable BoxScope.() -> Unit,
    incomingClip: @Composable BoxScope.() -> Unit
) {
    val p = easing.transform(progress)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp))
            .background(Color.Black)
    ) {
        when (effect) {
            // =================================================================
            // 1. DISSOLVE & FADE EFFECTS
            // =================================================================
            VideoTransitionEffect.CROSS_DISSOLVE -> {
                Box(modifier = Modifier.fillMaxSize(), content = outgoingClip)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = p },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.DIP_TO_BLACK -> {
                if (p < 0.5f) {
                    val fadeOut = 1f - (p * 2f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fadeOut },
                        content = outgoingClip
                    )
                } else {
                    val fadeIn = (p - 0.5f) * 2f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fadeIn },
                        content = incomingClip
                    )
                }
            }

            VideoTransitionEffect.DIP_TO_WHITE -> {
                val whiteOverlayAlpha = 1f - abs(p * 2f - 1f)
                if (p < 0.5f) {
                    Box(modifier = Modifier.fillMaxSize(), content = outgoingClip)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), content = incomingClip)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = whiteOverlayAlpha))
                )
            }

            VideoTransitionEffect.FILM_BLOOM_FADE -> {
                val bloomIntensity = sin(p * PI.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1f + bloomIntensity * 0.06f
                            scaleY = 1f + bloomIntensity * 0.06f
                        },
                    content = outgoingClip
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = p
                            scaleX = 1.06f - p * 0.06f
                            scaleY = 1.06f - p * 0.06f
                        },
                    content = incomingClip
                )
                // Warm golden-white film flash overlay at midpoint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF8E7).copy(alpha = bloomIntensity * 0.72f),
                                    Color(0xFFFFB347).copy(alpha = bloomIntensity * 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // =================================================================
            // 2. SLIDE & PUSH EFFECTS
            // =================================================================
            VideoTransitionEffect.PUSH_LEFT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -size.width * p },
                    content = outgoingClip
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * (1f - p) },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.PUSH_RIGHT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * p },
                    content = outgoingClip
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -size.width * (1f - p) },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.PUSH_UP -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = -size.height * p },
                    content = outgoingClip
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = size.height * (1f - p) },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.PUSH_DOWN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = size.height * p },
                    content = outgoingClip
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = -size.height * (1f - p) },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.COVER_SLIDE_LEFT -> {
                // Outgoing recedes slightly with parallax dim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = -size.width * 0.25f * p
                            scaleX = 1f - 0.06f * p
                            scaleY = 1f - 0.06f * p
                            alpha = 1f - 0.35f * p
                        },
                    content = outgoingClip
                )
                // Incoming slides directly over from right edge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * (1f - p) },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.REVEAL_PARALLAX -> {
                // Incoming scales up from underneath
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val inv = 1f - p
                            scaleX = 0.88f + 0.12f * p
                            scaleY = 0.88f + 0.12f * p
                            translationX = -size.width * 0.15f * inv
                            alpha = 0.45f + 0.55f * p
                        },
                    content = incomingClip
                )
                // Outgoing slides off to the right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * p },
                    content = outgoingClip
                )
            }

            // =================================================================
            // 3. GEOMETRIC WIPE EFFECTS (Canvas Path Clipping)
            // =================================================================
            VideoTransitionEffect.WIPE_HORIZONTAL,
            VideoTransitionEffect.WIPE_DIAGONAL,
            VideoTransitionEffect.CLOCK_RADIAL_WIPE,
            VideoTransitionEffect.BARN_DOORS_SPLIT,
            VideoTransitionEffect.VENETIAN_BLINDS,
            VideoTransitionEffect.CIRCLE_IRIS_OPEN,
            VideoTransitionEffect.DIAMOND_SHUTTER,
            VideoTransitionEffect.CORNER_QUAD_SPLIT -> {
                Box(modifier = Modifier.fillMaxSize(), content = outgoingClip)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val maskPath = buildGeometricMaskPath(effect, p, size)
                            clipPath(maskPath) {
                                this@drawWithContent.drawContent()
                            }
                            // Subtle crisp white specular edge along the active wipe boundary
                            if (p > 0.02f && p < 0.98f) {
                                drawPath(
                                    path = maskPath,
                                    color = Color.White.copy(alpha = 0.35f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.CIRCLE_PINHOLE_CLOSE -> {
                // Contracts Clip A to a center pinhole (0..0.5), then expands Clip B (0.5..1.0)
                val isFirstHalf = p < 0.5f
                val apertureFraction = if (isFirstHalf) {
                    1f - (p * 2f)
                } else {
                    (p - 0.5f) * 2f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val maxRadius = hypot(size.width / 2f, size.height / 2f)
                            val radius = (maxRadius * apertureFraction).coerceAtLeast(1f)
                            val circlePath = Path().apply {
                                addOval(
                                    Rect(
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = radius
                                    )
                                )
                            }
                            clipPath(circlePath) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    content = if (isFirstHalf) outgoingClip else incomingClip
                )
            }

            // =================================================================
            // 5. ZOOM & SPIN WARP EFFECTS
            // =================================================================
            VideoTransitionEffect.CROSS_ZOOM_IN -> {
                // Outgoing zooms aggressively toward camera and fades out
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val zoomOutScale = 1f + (p * p * 2.2f)
                            scaleX = zoomOutScale
                            scaleY = zoomOutScale
                            alpha = (1f - p * 1.25f).coerceIn(0f, 1f)
                        },
                    content = outgoingClip
                )
                // Incoming starts zoomed in and settles cleanly to 1.0x
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val inv = 1f - p
                            val zoomInScale = 1f + (inv * inv * 1.6f)
                            scaleX = zoomInScale
                            scaleY = zoomInScale
                            alpha = p
                        },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.CROSS_ZOOM_OUT -> {
                // Outgoing shrinks away into the center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val shrink = (1f - p * 0.65f).coerceAtLeast(0.2f)
                            scaleX = shrink
                            scaleY = shrink
                            alpha = 1f - p
                        },
                    content = outgoingClip
                )
                // Incoming pulls down from an oversized frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val expand = 1.85f - 0.85f * p
                            scaleX = expand
                            scaleY = expand
                            alpha = p
                        },
                    content = incomingClip
                )
            }

            VideoTransitionEffect.WHIP_SPIN_ROTATE -> {
                if (p < 0.5f) {
                    val halfT = p * 2f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = halfT * 180f
                                val sc = 1f + halfT * 0.45f
                                scaleX = sc
                                scaleY = sc
                                alpha = 1f - halfT * 0.5f
                            },
                        content = outgoingClip
                    )
                } else {
                    val halfT = (1f - p) * 2f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = -halfT * 180f
                                val sc = 1f + halfT * 0.45f
                                scaleX = sc
                                scaleY = sc
                                alpha = 1f - halfT * 0.5f
                            },
                        content = incomingClip
                    )
                }
            }

            // =================================================================
            // 6. 3D PERSPECTIVE & STYLIZED EFFECTS
            // =================================================================
            VideoTransitionEffect.CUBE_ROTATE_3D -> {
                // Simulated 3D Y-axis perspective cube rotation
                if (p < 0.99f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 14f * density
                                transformOrigin = TransformOrigin(1f, 0.5f)
                                translationX = -size.width * p
                                rotationY = -90f * p
                                alpha = (1f - p * 0.45f).coerceIn(0f, 1f)
                            },
                        content = outgoingClip
                    )
                }
                if (p > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 14f * density
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                translationX = size.width * (1f - p)
                                rotationY = 90f * (1f - p)
                                alpha = (0.55f + p * 0.45f).coerceIn(0f, 1f)
                            },
                        content = incomingClip
                    )
                }
            }

            VideoTransitionEffect.FLIP_CARD_3D -> {
                if (p < 0.5f) {
                    val t = p * 2f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 16f * density
                                rotationY = 90f * t
                                val scale = 1f - 0.12f * t
                                scaleX = scale
                                scaleY = scale
                            },
                        content = outgoingClip
                    )
                } else {
                    val t = (1f - p) * 2f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                cameraDistance = 16f * density
                                rotationY = -90f * t
                                val scale = 1f - 0.12f * t
                                scaleX = scale
                                scaleY = scale
                            },
                        content = incomingClip
                    )
                }
            }

            VideoTransitionEffect.GLITCH_SLICE_SHIFT -> {
                val glitchEnvelope = sin(p * PI.toFloat())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = glitchEnvelope * 14f * sin(p * 36f)
                        },
                    content = if (p < 0.5f) outgoingClip else incomingClip
                )
                // Horizontal displaced slice bands from incoming clip
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = -glitchEnvelope * 22f * cos(p * 28f)
                        }
                        .drawWithContent {
                            val slicePath = buildGlitchSliceMaskPath(p, size)
                            clipPath(slicePath) {
                                this@drawWithContent.drawContent()
                            }
                            // Cyan/Magenta chromatic aberration scanlines during peak glitch
                            if (glitchEnvelope > 0.15f) {
                                drawRect(
                                    color = Color(0xFF00F0FF).copy(alpha = glitchEnvelope * 0.18f),
                                    topLeft = Offset(0f, size.height * ((p * 1.7f) % 0.8f)),
                                    size = Size(size.width, size.height * 0.08f)
                                )
                                drawRect(
                                    color = Color(0xFFFF2D55).copy(alpha = glitchEnvelope * 0.18f),
                                    topLeft = Offset(0f, size.height * ((1f - p * 1.3f).coerceIn(0f, 0.85f))),
                                    size = Size(size.width, size.height * 0.06f)
                                )
                            }
                        },
                    content = incomingClip
                )
            }
        }
    }
}

/**
 * Synthetic "Clip A" (Golden Sunset Horizon) used inside the interactive transition preview monitor.
 */
@Composable
fun DemoOutgoingVideoClip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1D2B64),
                        Color(0xFFBA5370),
                        Color(0xFFF4E2D8)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Glowing sun disc
            drawCircle(
                color = Color(0xFFFFE599).copy(alpha = 0.92f),
                radius = size.minDimension * 0.18f,
                center = Offset(size.width * 0.35f, size.height * 0.42f)
            )
            // Layered mountain silhouettes
            val mountainBack = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * 0.65f)
                lineTo(size.width * 0.28f, size.height * 0.45f)
                lineTo(size.width * 0.58f, size.height * 0.68f)
                lineTo(size.width * 0.82f, size.height * 0.48f)
                lineTo(size.width, size.height * 0.62f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(mountainBack, color = Color(0xFF2C1E38).copy(alpha = 0.75f))

            val mountainFront = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * 0.78f)
                lineTo(size.width * 0.42f, size.height * 0.56f)
                lineTo(size.width * 0.75f, size.height * 0.76f)
                lineTo(size.width, size.height * 0.66f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(mountainFront, color = Color(0xFF171123))
        }

        // Clip A Badge
        Box(
            modifier = Modifier
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicText(
                text = "CLIP A • 01:12",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Synthetic "Clip B" (Alpine Blue Cyber Peak) used inside the interactive transition preview monitor.
 */
@Composable
fun DemoIncomingVideoClip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF004E92),
                        Color(0xFF007AFF),
                        Color(0xFF70D6FF)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Crisp geometric snow peaks & stars
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = size.minDimension * 0.13f,
                center = Offset(size.width * 0.72f, size.height * 0.32f)
            )

            val peakPath = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * 0.72f)
                lineTo(size.width * 0.25f, size.height * 0.42f)
                lineTo(size.width * 0.52f, size.height * 0.70f)
                lineTo(size.width * 0.78f, size.height * 0.36f)
                lineTo(size.width, size.height * 0.64f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(peakPath, color = Color(0xFF0A192F).copy(alpha = 0.88f))
        }

        // Clip B Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF007AFF).copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicText(
                text = "CLIP B • 01:16",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Complete Apple-style Video Transition Picker & Live Preview Studio that pairs directly
 * with [AppleBeveledTransitionButton].
 *
 * Allows users to:
 * 1. Toggle the transition picker open/closed via the beveled `+` button on a timeline seam.
 * 2. Preview all 25 transitions live in real time on a dual-clip monitor.
 * 3. Filter by category (`All`, `Dissolve`, `Slide & Push`, `Geometric Wipe`, `Iris & Shape`, `Zoom & Spin`, `3D & Stylized`).
 * 4. Adjust transition duration (`0.4s`, `0.8s`, `1.2s`, `1.6s`) and easing curve (`Smooth`, `Linear`, `Cubic`, `Snap`).
 */
@Composable
fun AppleVideoTransitionStudio(
    selectedEffect: VideoTransitionEffect?,
    onSelectEffect: (VideoTransitionEffect?) -> Unit,
    durationMs: Int = 800,
    onDurationChange: (Int) -> Unit = {},
    selectedEasing: TransitionEasing = TransitionEasing.APPLE_SMOOTH,
    onEasingChange: (TransitionEasing) -> Unit = {},
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    clipAName: String = "Clip 01",
    clipBName: String = "Clip 02",
    customOutgoingClip: (@Composable BoxScope.() -> Unit)? = null,
    customIncomingClip: (@Composable BoxScope.() -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf(TransitionCategory.ALL) }
    val effectiveEffect = selectedEffect ?: VideoTransitionEffect.CROSS_DISSOLVE

    // Continuous looping progress for live previewing the selected transition
    val infiniteTransition = rememberInfiniteTransition(label = "TransitionLoop")
    val liveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMs.coerceIn(200, 3000),
                delayMillis = 380,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "LiveTransitionProgress"
    )

    val filteredEffects = remember(selectedCategory) {
        if (selectedCategory == TransitionCategory.ALL) {
            VideoTransitionEffect.entries
        } else {
            VideoTransitionEffect.entries.filter { it.category == selectedCategory }
        }
    }

    val cardBg = if (isDark) Color(0xFF1C1D24) else Color.White
    val cardBorder = if (isDark) Color(0xFF2E3346) else Color.Black.copy(alpha = 0.08f)
    val textPrimary = if (isDark) Color.White else Color(0xFF1D1D1F)
    val textSecondary = if (isDark) Color(0xFF8E95A5) else Color(0xFF86868B)
    val pillInactiveBg = if (isDark) Color(0xFF282C3C) else Color(0xFFF5F5F7)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. LIVE PREVIEW MONITOR CARD + DUAL CLIP SEAM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    BasicText(
                        text = if (selectedEffect == null) "Direct Cut (No Transition)" else effectiveEffect.displayName,
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    BasicText(
                        text = if (selectedEffect == null) "Instant boundary splice" else "${effectiveEffect.category.label} • ${"%.1f".format(durationMs / 1000f)}s • ${selectedEasing.label}",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Live Progress Pill / Cut indicator
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selectedEffect == null) Color.Gray.copy(alpha = 0.2f) else Color(0xFF007AFF).copy(alpha = 0.15f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    BasicText(
                        text = if (selectedEffect == null) "CUT" else "${(liveProgress * 100).toInt()}%",
                        style = TextStyle(
                            color = if (selectedEffect == null) textSecondary else Color(0xFF007AFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 16:9 Video Transition Viewport
            if (selectedEffect == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                ) {
                    if (liveProgress < 0.5f) {
                        customOutgoingClip?.invoke(this) ?: DemoOutgoingVideoClip()
                    } else {
                        customIncomingClip?.invoke(this) ?: DemoIncomingVideoClip()
                    }
                }
            } else {
                VideoTransitionRenderer(
                    effect = effectiveEffect,
                    progress = liveProgress,
                    easing = selectedEasing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    outgoingClip = { customOutgoingClip?.invoke(this) ?: DemoOutgoingVideoClip() },
                    incomingClip = { customIncomingClip?.invoke(this) ?: DemoIncomingVideoClip() }
                )
            }

            // Dual Clip Seam Bar with Cut indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2C1E38), Color(0xFFBA5370))
                            )
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicText(
                        text = clipAName,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Apple Beveled Transition "+" Seam Button
                AppleBeveledTransitionButton(
                    isActive = selectedEffect != null,
                    onClick = {
                        if (selectedEffect != null) onSelectEffect(null) else onSelectEffect(VideoTransitionEffect.CROSS_DISSOLVE)
                    },
                    buttonSize = 34.dp,
                    activeRotationDegrees = if (selectedEffect != null) 0f else 90f
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF004E92), Color(0xFF007AFF))
                            )
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    BasicText(
                        text = clipBName,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. DURATION & EASING CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Duration Presets & Easing Curves
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration Presets
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(400, 600, 800, 1200, 1600).forEach { ms ->
                        val isSelected = durationMs == ms
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF007AFF) else pillInactiveBg
                                )
                                .clickable { onDurationChange(ms) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                text = "${"%.1f".format(ms / 1000f)}s",
                                style = TextStyle(
                                    color = if (isSelected) Color.White else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Easing Curves
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TransitionEasing.entries.forEach { easingItem ->
                        val isSelected = selectedEasing == easingItem
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFF1D1D1F) else pillInactiveBg
                                )
                                .clickable { onEasingChange(easingItem) }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                text = easingItem.label,
                                style = TextStyle(
                                    color = if (isSelected) Color.White else textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Category Filter Horizontal Scroll Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "None / Cut" Pill
                val isNoneSelected = selectedEffect == null
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isNoneSelected) Color(0xFFE53935) else pillInactiveBg)
                        .clickable { onSelectEffect(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = "None (Cut)",
                        style = TextStyle(
                            color = if (isNoneSelected) Color.White else textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                TransitionCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF007AFF) else pillInactiveBg,
                        animationSpec = tween(150),
                        label = "CategoryBg"
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            text = category.label,
                            style = TextStyle(
                                color = if (isSelected) Color.White else textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Grid of 25 Transition Cards with Animated Schematic Mini-Thumbnails
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredEffects,
                    key = { it.id }
                ) { effectItem ->
                    TransitionEffectGridCard(
                        effect = effectItem,
                        isSelected = effectItem == selectedEffect,
                        liveProgress = liveProgress,
                        isDark = isDark,
                        onClick = {
                            onSelectEffect(effectItem)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual card inside the Transition Picker grid showing a miniature animated
 * visual diagram of the transition effect and its name.
 */
@Composable
fun TransitionEffectGridCard(
    effect: VideoTransitionEffect,
    isSelected: Boolean,
    liveProgress: Float,
    isDark: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF007AFF) else if (isDark) Color(0xFF2E3346) else Color(0xFFE5E5EA),
        animationSpec = tween(150),
        label = "CardBorderColor"
    )
    val containerBg by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
        } else {
            if (isDark) Color(0xFF222533) else Color(0xFFF9F9FB)
        },
        animationSpec = tween(150),
        label = "CardContainerBg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Miniature Real-Time Transition Preview Thumbnail
        VideoTransitionRenderer(
            effect = effect,
            progress = if (isSelected) liveProgress else 0.48f,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            outgoingClip = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3A3A3C), Color(0xFF8E8E93))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "A",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            incomingClip = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF148EFF), Color(0xFF0062CC))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "B",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )

        // Transition Title
        BasicText(
            text = effect.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = if (isSelected) Color(0xFF007AFF) else if (isDark) Color.White else Color(0xFF1D1D1F),
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview(
    name = "25+ Video Transitions Studio (Interactive)",
    showBackground = true,
    backgroundColor = 0xFFF5F5F7,
    widthDp = 412,
    heightDp = 760
)
@Composable
fun AppleVideoTransitionStudioPreview() {
    var currentEffect by remember { mutableStateOf<VideoTransitionEffect?>(VideoTransitionEffect.CIRCLE_IRIS_OPEN) }
    var durationMs by remember { mutableIntStateOf(800) }
    var easing by remember { mutableStateOf(TransitionEasing.APPLE_SMOOTH) }

    AppleVideoTransitionStudio(
        selectedEffect = currentEffect,
        onSelectEffect = { currentEffect = it },
        durationMs = durationMs,
        onDurationChange = { durationMs = it },
        selectedEasing = easing,
        onEasingChange = { easing = it }
    )
}
