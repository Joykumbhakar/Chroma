package com.chroma.studio.ui.screens

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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chroma.studio.R
import com.chroma.studio.ui.components.OscellionBrand
import kotlinx.coroutines.launch

/**
 * Official Primary Oscellion Cinematic Splash Screen.
 * Exact 1:1 realization of oscellion_reveal_variation_v2.html:
 * "LIQUID APERTURE + PRECISION BEVEL IGNITION + MASKED SHEEN"
 *
 * Sequence:
 * 1. Studio Canvas: Pure pitch-black studio background (#000000). Zero ghost fibers or clutter.
 * 2. Subtle Camera Pull-Back: 1.06x -> 1.0x subtle settle (0ms - 2400ms, cubic-bezier(0.16, 1, 0.3, 1))
 * 3. Emblem Aperture Glide: Center emergence (-42° -> +4° -> 0°), scaling (0.35x -> 1.16x -> 1.12x),
 *    then glides left to settle alongside "scellion" (0ms - 2200ms, cubic-bezier(0.22, 1, 0.36, 1))
 * 4. Perimeter Wireframe Trace: Precision 2-pass stroke trace ignition (80ms - 1230ms, cubic-bezier(0.4, 0, 0.2, 1))
 * 5. Progressive Bevel Ignition: Top-left highlight, bottom-right depth, and metallic face fade in (420ms - 1160ms)
 * 6. Directional Wordmark Reveal: "scellion" unmasks from left to right, tracking and sliding into position (960ms - 1860ms)
 * 7. Continuous Specular Light Sweep: Diagonal metallic sheen sweeps sequentially across emblem and letters (1560ms - 2560ms)
 * 8. Responsive Geometry: Fluidly responsive across all phones, tablets, foldables, portrait & landscape.
 */
@Composable
fun OscellionSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2850L
) {
    val coroutineScope = rememberCoroutineScope()
    val animTime = remember { Animatable(0f) }
    val isLuxuryBrand by com.chroma.studio.ui.components.OscellionLuxuryBrandController.isLuxuryEnabled.collectAsState()

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

    // Exact CSS cubic-bezier easing curves
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

    if (t < 396f) { // 0% - 18% (0ms - 396ms)
        val f = (t / 396f).coerceIn(0f, 1f)
        emblemOpacity = f
        emblemScale = 0.35f + (1.16f - 0.35f) * f
        emblemRotation = -42f + 46f * f
        emblemGlideProgress = 0f
    } else if (t < 748f) { // 18% - 34% (396ms - 748ms)
        val f = ((t - 396f) / 352f).coerceIn(0f, 1f)
        emblemOpacity = 1f
        emblemScale = 1.16f - 0.04f * f
        emblemRotation = 4f - 4f * f
        emblemGlideProgress = 0f
    } else if (t < 968f) { // 34% - 44% (748ms - 968ms)
        emblemOpacity = 1f
        emblemScale = 1.12f
        emblemRotation = 0f
        emblemGlideProgress = 0f
    } else if (t < 1804f) { // 44% - 82% (968ms - 1804ms) Glide left
        val f = ((t - 968f) / 836f).coerceIn(0f, 1f)
        val ease = easeAperture.transform(f)
        emblemOpacity = 1f
        emblemScale = 1.12f - 0.12f * ease
        emblemRotation = 0f
        emblemGlideProgress = ease
    } else { // 82% - 100% (1804ms - 2200ms+)
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
    val wordmarkSlideNorm: Float
    val wordmarkAlpha: Float
    if (t < 960f) {
        wordmarkRevealProgress = 0f
        wordmarkSlideNorm = 1f
        wordmarkAlpha = 0f
    } else if (t < 1860f) {
        val f = ((t - 960f) / 900f).coerceIn(0f, 1f)
        val ease = easeDecel.transform(f)
        wordmarkRevealProgress = ease
        wordmarkSlideNorm = 1f - ease
        wordmarkAlpha = (f * 5f).coerceIn(0f, 1f) // Settles opacity quickly by 20%
    } else {
        wordmarkRevealProgress = 1f
        wordmarkSlideNorm = 0f
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

    // ─── PURE PITCH-BLACK STUDIO CANVAS & RESPONSIVE CONTAINER ──
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { startAnimation() },
        contentAlignment = Alignment.Center
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH
        val referenceDim = if (isLandscape) screenH * 0.82f else screenW

        // Fluid responsive sizing: clamps between 240dp on compact screens up to 380dp on large displays
        val targetLockupWidth = (referenceDim * 0.74f).coerceIn(240.dp, 380.dp)
        val scaleRatio = targetLockupWidth.value / 454f

        val lockupHeight = (95f * scaleRatio).dp
        val emblemWidth = lockupHeight * (740f / 660f)
        val wordmarkWidth = lockupHeight * (356f / 95f)
        val gap = (-4f * scaleRatio).dp
        val totalLockupWidth = emblemWidth + wordmarkWidth + gap

        // Precision optical center offset: emblem is perfectly centered at start, then glides into lockup
        val centerOffsetDp = (wordmarkWidth + gap) / 2f
        val currentGlideOffsetDp = centerOffsetDp * (1f - emblemGlideProgress)
        val wordmarkSlideX = (-22f * scaleRatio * wordmarkSlideNorm).dp

        // Master brand lockup container with subtle camera scale settle
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
                EmblemVectorCanvas(
                    modifier = Modifier.fillMaxSize(),
                    wireframeProgress = wireframeProgress,
                    wireframeAlpha = wireframeAlpha,
                    topHighlightAlpha = topHighlightAlpha,
                    bottomDepthAlpha = bottomDepthAlpha,
                    mainFaceAlpha = mainFaceAlpha,
                    sheenProgress = logoSheenProgress,
                    sheenAlpha = logoSheenAlpha,
                    isLuxuryBrand = isLuxuryBrand
                )
            }

            Spacer(modifier = Modifier.width(gap))

            // ─── WORDMARK ("scellion") ─────────────────────────────────
            Box(
                modifier = Modifier
                    .width(wordmarkWidth)
                    .height(lockupHeight)
                    .graphicsLayer {
                        translationX = wordmarkSlideX.toPx()
                        alpha = wordmarkAlpha
                        clip = true
                        shape = GenericShape { size, _ ->
                            val revealW = size.width * wordmarkRevealProgress
                            addRect(Rect(0f, 0f, revealW, size.height))
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // High-resolution metallic wordmark (24K Champagne-Gold tint for Verified Pro users)
                Image(
                    painter = painterResource(R.drawable.oscellion_wordmark),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = if (isLuxuryBrand) ColorFilter.tint(Color(0xFFF7E3A1), ComposeBlendMode.Modulate) else null,
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
                                        (if (isLuxuryBrand) Color(0xFFFFFDF5) else Color.White).copy(alpha = 0.88f * wordmarkSheenAlpha),
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
 * Faithfully mirrors the SVG structure from oscellion_reveal_variation_v2.html:
 * viewBox: 100 180 740 660
 * - 2-pass wireframe perimeter trace ignition
 * - 3-layer bevel ignition (top-left highlight, bottom-right depth, vertical silver or 24K gold gradient face)
 * - Inner specular rim stroke
 * - Masked diagonal specular sheen sweep
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
    sheenAlpha: Float,
    isLuxuryBrand: Boolean = false
) {
    val parsedPath = remember(isLuxuryBrand) {
        val rawPath = if (isLuxuryBrand) OscellionBrand.LUXURY_PATH_DATA else OscellionBrand.PATH_DATA
        PathParser().parsePathString(rawPath).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    // Standard vertical metallic gradient vs 24K Champagne-Gold gradient for Verified Pro users
    val faceGradient = remember(isLuxuryBrand) {
        if (isLuxuryBrand) {
            Brush.linearGradient(
                colorStops = OscellionBrand.LuxuryGoldFillStops,
                start = Offset(122.88f, 81.92f),
                end = Offset(901.12f, 942.08f)
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF7F7F9),
                    Color(0xFFD4D4DA),
                    Color(0xFFA8A8B0)
                )
            )
        }
    }

    Canvas(modifier = modifier) {
        // SVG viewBox: x=100, y=180, width=740, height=660 (or fitted 1024x1024 for luxury path)
        val scale = if (isLuxuryBrand) size.height / 820f else size.height / 660f
        val offsetX = if (isLuxuryBrand) (size.width - 1024f * scale) / 2f else -100f * scale
        val offsetY = if (isLuxuryBrand) (size.height - 1024f * scale) / 2f else -180f * scale

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // ─── 1. SOLID BEVEL LAYERS (PROGRESSIVE IGNITION) ────────
            if (bottomDepthAlpha > 0f) {
                // Bottom-right crisp dark bevel edge (+7px in viewBox space)
                canvas.save()
                canvas.translate(7f, 7f)
                val depthPaint = Paint().apply {
                    color = (if (isLuxuryBrand) Color(0xFF6E4E10) else Color(0xFF5A5A60)).copy(alpha = 0.9f * bottomDepthAlpha)
                    isAntiAlias = true
                    style = PaintingStyle.Fill
                }
                canvas.drawPath(parsedPath, depthPaint)
                canvas.restore()
            }

            if (topHighlightAlpha > 0f) {
                // Top-left crisp bright bevel edge (-7px in viewBox space)
                canvas.save()
                canvas.translate(-7f, -7f)
                val highlightPaint = Paint().apply {
                    color = (if (isLuxuryBrand) Color(0xFFFFFDF5) else Color.White).copy(alpha = 0.95f * topHighlightAlpha)
                    isAntiAlias = true
                    style = PaintingStyle.Fill
                }
                canvas.drawPath(parsedPath, highlightPaint)
                canvas.restore()
            }

            if (mainFaceAlpha > 0f) {
                // Main metallic vertical / 24K gold gradient face
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
                            (if (isLuxuryBrand) Color(0xFFFFF2C6) else Color.White).copy(alpha = 0.98f * sheenAlpha),
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
                    strokeWidth = if (isLuxuryBrand) 14f else 5f
                    color = (if (isLuxuryBrand) Color(0xFFFCEBB6) else Color.White).copy(alpha = 0.55f * mainFaceAlpha)
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
                    color = (if (isLuxuryBrand) Color(0xFFF7E3A1) else Color.White).copy(alpha = wireframeAlpha)
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
