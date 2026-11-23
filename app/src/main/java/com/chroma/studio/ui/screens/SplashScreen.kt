package com.chroma.studio.ui.screens

import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chroma.studio.R
import com.chroma.studio.ui.components.GhostFibers
import com.chroma.studio.ui.components.GhostFibersConfig
import com.chroma.studio.ui.components.OscellionBrand
import kotlinx.coroutines.launch

/**
 * Official Cinematic Oscellion Splash Screen.
 * Faithful implementation of variation 2:
 * "LIQUID APERTURE + PRECISION BEVEL IGNITION + MASKED SHEEN"
 *
 * Sequence:
 * 1. Camera Pull-Back: 1.06x -> 1.0x subtle settle
 * 2. Emblem Aperture Glide: Center emergence (-42° -> +4° -> 0°), scaling (0.35x -> 1.16x -> 1.12x), then glides to the left
 * 3. Perimeter Wireframe Trace: Precision 2-pass path trace ignition
 * 4. Progressive Bevel Ignition: Top-left highlight, bottom-right depth, and metallic face fade in
 * 5. Directional Wordmark Reveal: "scellion" unmasks from left to right, tracking and sliding into position
 * 6. Continuous Specular Light Sweep: Diagonal metallic sheen sweeps across emblem and letters
 */
@Composable
fun OscellionSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 3000L
) {
    val coroutineScope = rememberCoroutineScope()
    val animTime = remember { Animatable(0f) }

    fun startAnimation() {
        coroutineScope.launch {
            animTime.snapTo(0f)
            animTime.animateTo(
                targetValue = durationMillis.toFloat(),
                animationSpec = tween(durationMillis = durationMillis.toInt(), easing = LinearEasing)
            )
            onSplashFinished()
        }
    }

    LaunchedEffect(Unit) {
        startAnimation()
    }

    val t = animTime.value

    // Easing curves matching CSS keyframes
    val easeDecel = remember { CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) }
    val easeAperture = remember { CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f) }
    val easeTrace = remember { CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f) }
    val easeSheen = remember { CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f) }

    // ─── 1. OVERALL CAMERA PULL-BACK (0ms - 2400ms) ───────────
    val cameraScale = 1.06f - 0.06f * easeDecel.transform((t / 2400f).coerceIn(0f, 1f))

    // ─── 2. EMBLEM APERTURE GLIDE (0ms - 2200ms) ───────────────
    val emblemOpacity: Float
    val emblemScale: Float
    val emblemRotation: Float
    val emblemGlideProgress: Float // 0f = optical center, 1f = settled on left

    if (t < 396f) { // 0% - 18%
        val f = (t / 396f).coerceIn(0f, 1f)
        emblemOpacity = f
        emblemScale = 0.35f + (1.16f - 0.35f) * f
        emblemRotation = -42f + 46f * f
        emblemGlideProgress = 0f
    } else if (t < 748f) { // 18% - 34%
        val f = ((t - 396f) / 352f).coerceIn(0f, 1f)
        emblemOpacity = 1f
        emblemScale = 1.16f - 0.04f * f
        emblemRotation = 4f - 4f * f
        emblemGlideProgress = 0f
    } else if (t < 968f) { // 34% - 44%
        emblemOpacity = 1f
        emblemScale = 1.12f
        emblemRotation = 0f
        emblemGlideProgress = 0f
    } else if (t < 1804f) { // 44% - 82% (Glide to left)
        val f = ((t - 968f) / 836f).coerceIn(0f, 1f)
        val ease = easeAperture.transform(f)
        emblemOpacity = 1f
        emblemScale = 1.12f - 0.12f * ease
        emblemRotation = 0f
        emblemGlideProgress = ease
    } else { // 82% - 100%
        emblemOpacity = 1f
        emblemScale = 1.0f
        emblemRotation = 0f
        emblemGlideProgress = 1f
    }

    // ─── 3. WIREFRAME PERIMETER TRACE (80ms - 1230ms) ──────────
    val wireframeProgress: Float
    val wireframeAlpha: Float
    if (t in 80f..710f) {
        val f = ((t - 80f) / 630f).coerceIn(0f, 1f)
        wireframeProgress = easeTrace.transform(f)
        wireframeAlpha = 0.95f + 0.05f * wireframeProgress
    } else if (t in 710f..1230f) {
        val f = ((t - 710f) / 520f).coerceIn(0f, 1f)
        wireframeProgress = 1f
        wireframeAlpha = (1f - f) * 1.0f
    } else {
        wireframeProgress = if (t < 80f) 0f else 1f
        wireframeAlpha = 0f
    }

    // ─── 4. PROGRESSIVE BEVEL IGNITION (420ms - 1160ms) ────────
    val topHighlightAlpha = if (t < 420f) 0f else easeDecel.transform(((t - 420f) / 550f).coerceIn(0f, 1f))
    val bottomDepthAlpha = if (t < 500f) 0f else easeDecel.transform(((t - 500f) / 550f).coerceIn(0f, 1f))
    val mainFaceAlpha = if (t < 560f) 0f else easeDecel.transform(((t - 560f) / 600f).coerceIn(0f, 1f))

    // ─── 5. WORDMARK REVEAL (960ms - 1860ms) ───────────────────
    val wordmarkRevealProgress: Float
    val wordmarkSlideX: Float
    val wordmarkAlpha: Float
    if (t < 960f) {
        wordmarkRevealProgress = 0f
        wordmarkSlideX = -22f
        wordmarkAlpha = 0f
    } else if (t < 1860f) {
        val f = ((t - 960f) / 900f).coerceIn(0f, 1f)
        val ease = easeDecel.transform(f)
        wordmarkRevealProgress = ease
        wordmarkSlideX = -22f * (1f - ease)
        wordmarkAlpha = (ease * 1.25f).coerceIn(0f, 1f)
    } else {
        wordmarkRevealProgress = 1f
        wordmarkSlideX = 0f
        wordmarkAlpha = 1f
    }

    // ─── 6. SPECULAR SHEEN SWEEPS ──────────────────────────────
    // Emblem sheen: 1560ms - 2240ms (duration 680ms)
    val logoSheenProgress: Float
    val logoSheenAlpha: Float
    if (t in 1560f..2240f) {
        val f = ((t - 1560f) / 680f).coerceIn(0f, 1f)
        logoSheenProgress = easeSheen.transform(f)
        logoSheenAlpha = if (f < 0.18f) f / 0.18f else if (f > 0.82f) (1f - f) / 0.18f else 1f
    } else {
        logoSheenProgress = 0f
        logoSheenAlpha = 0f
    }

    // Wordmark sheen: 1740ms - 2560ms (duration 820ms)
    val wordmarkSheenProgress: Float
    val wordmarkSheenAlpha: Float
    if (t in 1740f..2560f) {
        val f = ((t - 1740f) / 820f).coerceIn(0f, 1f)
        wordmarkSheenProgress = easeSheen.transform(f)
        wordmarkSheenAlpha = if (f < 0.15f) f / 0.15f else if (f > 0.85f) (1f - f) / 0.15f else 1f
    } else {
        wordmarkSheenProgress = 0f
        wordmarkSheenAlpha = 0f
    }

    // Dimensions
    val lockupHeight = 58.dp
    val emblemWidth = lockupHeight * (98f / 95f) // ~60dp
    val wordmarkWidth = lockupHeight * (356f / 95f) // ~217dp
    val totalLockupWidth = emblemWidth + wordmarkWidth // ~277dp

    // At start (emblemGlideProgress = 0), emblem is at center of lockup:
    val centerOffsetDp = wordmarkWidth / 2f
    val currentGlideOffsetDp = centerOffsetDp * (1f - emblemGlideProgress)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { startAnimation() },
        contentAlignment = Alignment.Center
    ) {
        // Deep cyber ribbon atmospheric background
        GhostFibers(
            modifier = Modifier.fillMaxSize(),
            config = GhostFibersConfig(
                glowColor = Color(0xFF001550),
                lineColor = Color(0xFF0A3075),
                secondaryColor = Color(0xFF001F60),
                layers = 2,
                twist = 1.0f,
                threads = 48,
                depthOfField = 0.85f,
                particles = 0.35f,
                sparkle = 0.35f,
                glow = 0.65f,
                brightness = 0.75f,
                intro = true
            )
        )

        // Subtle ambient radial glow behind logo lockup
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f * (topHighlightAlpha + mainFaceAlpha) * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Master lockup container with subtle camera scale settle
        Row(
            modifier = Modifier
                .width(totalLockupWidth)
                .height(lockupHeight)
                .graphicsLayer {
                    scaleX = cameraScale
                    scaleY = cameraScale
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ─── EMBLEM (SYMBOL "O") ───────────────────────────────────
            Box(
                modifier = Modifier
                    .width(emblemWidth)
                    .height(lockupHeight)
                    .offset(x = currentGlideOffsetDp)
                    .graphicsLayer {
                        alpha = emblemOpacity
                        scaleX = emblemScale
                        scaleY = emblemScale
                        rotationZ = emblemRotation
                    },
                contentAlignment = Alignment.Center
            ) {
                // Vector Canvas Emblem with Wireframe Trace + Bevel Ignition + Light Sheen
                EmblemVectorCanvas(
                    modifier = Modifier.fillMaxSize(),
                    wireframeProgress = wireframeProgress,
                    wireframeAlpha = wireframeAlpha,
                    topHighlightAlpha = topHighlightAlpha,
                    bottomDepthAlpha = bottomDepthAlpha,
                    mainFaceAlpha = mainFaceAlpha,
                    sheenProgress = logoSheenProgress,
                    sheenAlpha = logoSheenAlpha
                )
            }

            // ─── WORDMARK ("scellion") ─────────────────────────────────
            Box(
                modifier = Modifier
                    .width(wordmarkWidth)
                    .height(lockupHeight)
                    .graphicsLayer {
                        translationX = wordmarkSlideX.dp.toPx()
                        alpha = wordmarkAlpha
                        // Directional left-to-right unmasking clip
                        clip = true
                        shape = GenericShape { size, _ ->
                            val revealW = size.width * wordmarkRevealProgress
                            addRect(Rect(0f, 0f, revealW, size.height))
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Render crisp metallic wordmark
                Image(
                    painter = painterResource(R.drawable.oscellion_wordmark),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                // Masked specular light sheen across letters
                if (wordmarkSheenAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                val sweepX = size.width * 2.2f * wordmarkSheenProgress - size.width * 0.6f
                                val sheenBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.85f * wordmarkSheenAlpha),
                                        Color.Transparent
                                    ),
                                    start = Offset(sweepX - 45.dp.toPx(), 0f),
                                    end = Offset(sweepX + 45.dp.toPx(), size.height)
                                )
                                drawRect(brush = sheenBrush, blendMode = ComposeBlendMode.Screen)
                            }
                    )
                }
            }
        }
    }
}

/**
 * Precision Vector Canvas for the Oscellion Emblem.
 * Draws SVG path geometry, 2-pass wireframe perimeter trace, 3-layer beveled ignition,
 * and masked specular light sheen bar.
 */
@Composable
private fun EmblemVectorCanvas(
    modifier: Modifier = Modifier,
    wireframeProgress: Float,
    wireframeAlpha: Float,
    topHighlightAlpha: Float,
    bottomDepthAlpha: Float,
    mainFaceAlpha: Float,
    sheenProgress: Float,
    sheenAlpha: Float
) {
    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    // Cached metallic face gradient
    val faceGradient = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF7F7F9),
                Color(0xFFD4D4DA),
                Color(0xFFA8A8B0)
            )
        )
    }

    Canvas(modifier = modifier) {
        // SVG viewBox: x=100, y=180, width=740, height=660
        val scale = size.height / 660f
        val offsetX = -100f * scale
        val offsetY = -180f * scale

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // ─── 1. SOLID BEVEL LAYERS (PROGRESSIVE IGNITION) ────────
            if (bottomDepthAlpha > 0f) {
                // Bottom-right crisp dark bevel edge (+7px in 1024-space)
                canvas.save()
                canvas.translate(7f, 7f)
                val depthPaint = Paint().apply {
                    color = Color(0xFF5A5A60).copy(alpha = 0.9f * bottomDepthAlpha)
                    isAntiAlias = true
                    style = PaintingStyle.Fill
                }
                canvas.drawPath(parsedPath, depthPaint)
                canvas.restore()
            }

            if (topHighlightAlpha > 0f) {
                // Top-left crisp bright bevel edge (-7px in 1024-space)
                canvas.save()
                canvas.translate(-7f, -7f)
                val highlightPaint = Paint().apply {
                    color = Color.White.copy(alpha = 0.95f * topHighlightAlpha)
                    isAntiAlias = true
                    style = PaintingStyle.Fill
                }
                canvas.drawPath(parsedPath, highlightPaint)
                canvas.restore()
            }

            if (mainFaceAlpha > 0f) {
                // Main metallic vertical gradient face
                canvas.save()
                canvas.clipPath(parsedPath)

                val facePaint = Paint().apply {
                    isAntiAlias = true
                    style = PaintingStyle.Fill
                    alpha = mainFaceAlpha
                }
                faceGradient.applyTo(Size(1024f, 1024f), facePaint, mainFaceAlpha)
                canvas.drawRect(Rect(0f, 0f, 1024f, 1024f), facePaint)

                // Masked specular light sheen bar (Stage 5)
                if (sheenAlpha > 0f) {
                    val sheenX = 100f + (940f - 100f) * sheenProgress - 240f
                    val sheenBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.98f * sheenAlpha),
                            Color.Transparent
                        ),
                        start = Offset(sheenX, 150f),
                        end = Offset(sheenX + 240f, 870f)
                    )
                    val sheenPaint = Paint().apply {
                        isAntiAlias = true
                        style = PaintingStyle.Fill
                        blendMode = ComposeBlendMode.Screen
                    }
                    sheenBrush.applyTo(Size(1024f, 1024f), sheenPaint, sheenAlpha)
                    canvas.drawRect(Rect(0f, 0f, 1024f, 1024f), sheenPaint)
                }

                canvas.restore()

                // Thin specular inner rim stroke
                val strokePaint = Paint().apply {
                    isAntiAlias = true
                    style = PaintingStyle.Stroke
                    strokeWidth = 5f
                    color = Color.White.copy(alpha = 0.45f * mainFaceAlpha)
                }
                canvas.drawPath(parsedPath, strokePaint)
            }

            // ─── 2. WIREFRAME PERIMETER TRACE (STAGE 2) ─────────────
            if (wireframeAlpha > 0f && wireframeProgress > 0f) {
                val wireframePaint = Paint().apply {
                    isAntiAlias = true
                    style = PaintingStyle.Stroke
                    strokeWidth = 11f
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    color = Color.White.copy(alpha = wireframeAlpha)
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(2600f, 2600f),
                        2600f * (1f - wireframeProgress)
                    )
                }
                canvas.drawPath(parsedPath, wireframePaint)
            }

            canvas.restore()
        }
    }
}
