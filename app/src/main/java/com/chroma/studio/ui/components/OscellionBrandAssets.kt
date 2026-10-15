package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.chroma.studio.ui.theme.InterFontFamily

/**
 * Official Oscellion Brand Identity System.
 * Geometry: M 512 212 A 300 300 0 0 1 812 512 A 300 300 0 0 1 512 812 A 300 300 0 0 1 299.87 724.13 L 148.5 771 Q 120 780 136.5 755.1 L 230.24 615 A 300 300 0 0 1 512 212 Z M 512 342 A 170 170 0 0 0 512 682 A 170 170 0 0 0 512 342 Z
 */
object OscellionBrand {
    const val PATH_DATA = "M 512 212 A 300 300 0 0 1 812 512 A 300 300 0 0 1 512 812 A 300 300 0 0 1 299.87 724.13 L 148.5 771 Q 120 780 136.5 755.1 L 230.24 615 A 300 300 0 0 1 512 212 Z M 512 342 A 170 170 0 0 0 512 682 A 170 170 0 0 0 512 342 Z"

    // Metallic silver gradient stops per brand guidelines
    val MetalGradientStops = listOf(
        0.00f to Color(0xFFFFFFFF),
        0.25f to Color(0xFFF5F5F7),
        0.50f to Color(0xFFD1D1D6),
        0.85f to Color(0xFF8E8E93),
        1.00f to Color(0xFF48484A)
    )

    val AppIconBgGradient = Brush.verticalGradient(
        listOf(Color(0xFF2C2C2E), Color(0xFF121213))
    )

    val DynamicIslandTileBg = Color(0xFF121213)
}

enum class OscellionLogoVariant {
    APP_ICON,            // 1. Main App Icon Squircle
    TRANSPARENT_METALLIC,// 2. Web Logo (Transparent)
    SPLASH,              // 3. Android Splash Screen Mockup & Asset
    FAVICON,             // 4. Compact / Favicon High Contrast
    NOTIFICATION,        // 5. Monochromatic Pure White
    MONOCHROME_BLACK,    // 6. Monochromatic Solid Black
    DYNAMIC_ISLAND,      // 7. Dynamic Island Alert Badge
    WATCH_CIRCULAR,      // 8. Watch OS Circular Complication
    WATERMARK_OUTLINE,   // 9. Minimalist Wireframe Outline
    SPINNER              // 10. Animated In-App Loader
}

/**
 * Universal Oscellion Logo Component that renders any brand variation.
 */
@Composable
fun OscellionLogo(
    variant: OscellionLogoVariant,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color? = null
) {
    when (variant) {
        OscellionLogoVariant.APP_ICON -> OscellionAppIcon(modifier = modifier, size = size)
        OscellionLogoVariant.TRANSPARENT_METALLIC -> OscellionWebLogo(modifier = modifier, size = size)
        OscellionLogoVariant.SPLASH -> OscellionSplashBrandView(modifier = modifier, size = size)
        OscellionLogoVariant.FAVICON -> OscellionFavicon(modifier = modifier, size = size)
        OscellionLogoVariant.NOTIFICATION -> OscellionNotificationBadge(modifier = modifier, size = size, tint = tint ?: Color.White)
        OscellionLogoVariant.MONOCHROME_BLACK -> OscellionMonochromeLogo(modifier = modifier, size = size, color = tint ?: Color.Black)
        OscellionLogoVariant.DYNAMIC_ISLAND -> OscellionDynamicIslandBadge(modifier = modifier, size = size)
        OscellionLogoVariant.WATCH_CIRCULAR -> OscellionWatchComplicationBadge(modifier = modifier, size = size)
        OscellionLogoVariant.WATERMARK_OUTLINE -> OscellionWatermarkView(modifier = modifier, size = size, color = tint ?: Color.White)
        OscellionLogoVariant.SPINNER -> OscellionSpinner(modifier = modifier, size = size, color = tint ?: Color.White)
    }
}

/**
 * Variation 1: Main App Icon Squircle
 */
@Composable
fun OscellionAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val squircleShape = RoundedCornerShape(size * (226f / 1024f))

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = size * 0.12f,
                shape = squircleShape,
                spotColor = Color.Black.copy(alpha = 0.65f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(squircleShape)
            .background(OscellionBrand.AppIconBgGradient)
            .border(
                width = (size * 0.015f).coerceAtLeast(1.dp),
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF48484A).copy(alpha = 0.8f),
                        Color(0xFF2C2C2E).copy(alpha = 0.2f),
                        Color(0xFF000000).copy(alpha = 0.9f)
                    )
                ),
                shape = squircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Milled Metallic Symbol
        OscellionVectorSymbol(
            modifier = Modifier.size(size * 0.72f),
            brush = Brush.linearGradient(
                colorStops = OscellionBrand.MetalGradientStops.toTypedArray(),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            edgeHighlight = true
        )

        // Top Glass Gloss Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Variation 2: Web Logo (Transparent 3D Metallic Symbol)
 */
@Composable
fun OscellionWebLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        OscellionVectorSymbol(
            modifier = Modifier.fillMaxSize(),
            brush = Brush.linearGradient(
                colorStops = OscellionBrand.MetalGradientStops.toTypedArray(),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            edgeHighlight = true
        )
    }
}

/**
 * Variation 3: Android Splash Screen Cold-Boot Mockup & Composable
 */
@Composable
fun OscellionSplashBrandView(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient radial glow behind symbol
            Box(
                modifier = Modifier
                    .size(size * 1.5f)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.12f * pulseAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Scaled metallic symbol
            OscellionVectorSymbol(
                modifier = Modifier.size(size),
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFF888888))
                ),
                edgeHighlight = true
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "OSCELLION",
            fontSize = (size.value * 0.14f).coerceAtLeast(13f).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color.White.copy(alpha = pulseAlpha),
            letterSpacing = 4.sp
        )
    }
}

/**
 * Variation 4: Favicon / Compact High-Contrast Symbol
 */
@Composable
fun OscellionFavicon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    OscellionVectorSymbol(
        modifier = modifier.size(size),
        brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFF8E8E93))),
        edgeHighlight = true,
        edgeStrokeColor = Color(0xFF48484A)
    )
}

/**
 * Variation 5: Monochromatic Pure White Notification Silhouette
 */
@Composable
fun OscellionNotificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color.White
) {
    OscellionVectorSymbol(
        modifier = modifier.size(size),
        color = tint,
        edgeHighlight = false
    )
}

/**
 * Variation 6: Monochromatic Solid Black Print Logo
 */
@Composable
fun OscellionMonochromeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color.Black
) {
    OscellionVectorSymbol(
        modifier = modifier.size(size),
        color = color,
        edgeHighlight = false
    )
}

/**
 * Variation 7: Dynamic Island Alert Miniature Squircle Badge
 */
@Composable
fun OscellionDynamicIslandBadge(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val squircleRadius = size * (226f / 1024f)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(squircleRadius))
            .background(OscellionBrand.DynamicIslandTileBg)
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(squircleRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        OscellionVectorSymbol(
            modifier = Modifier.size(size * 0.70f),
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFF8E8E93))
            ),
            edgeHighlight = true
        )
    }
}

/**
 * Variation 8: Watch OS Circular Wearable Complication
 */
@Composable
fun OscellionWatchComplicationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2C2C2E), Color(0xFF0A0A0B))
                )
            )
            .border(
                (size * 0.016f).coerceAtLeast(1.dp),
                Color(0x993A3A3C),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        OscellionVectorSymbol(
            modifier = Modifier.size(size * 0.68f),
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFFD1D1D6), Color(0xFF48484A))
            ),
            edgeHighlight = true
        )
    }
}

/**
 * Variation 9: Minimalist Wireframe Watermark (Stroke Only)
 */
@Composable
fun OscellionWatermarkView(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color.White.copy(alpha = 0.5f),
    strokeWidthDp: Dp = 2.dp
) {
    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 1024f
        val strokePx = (strokeWidthDp.toPx() * (1024f / size.toPx())).coerceAtLeast(2f)

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.scale(scale, scale)
            canvas.drawPath(
                parsedPath,
                Paint().apply {
                    this.color = color
                    this.style = PaintingStyle.Stroke
                    this.strokeWidth = strokePx
                    this.strokeCap = StrokeCap.Round
                    this.strokeJoin = StrokeJoin.Round
                    this.isAntiAlias = true
                }
            )
            canvas.restore()
        }
    }
}

/**
 * Variation 10: In-App Animated Loader / Spinner (Exact implementation of HTML Section 10)
 */
@Composable
fun OscellionSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OscellionLoader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "osc_spin"
    )

    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.toPx() / 1024f
            val strokePx = 48f * scale

            // 1. Faint background track
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.scale(scale, scale)
                canvas.drawPath(
                    parsedPath,
                    Paint().apply {
                        this.color = color.copy(alpha = 0.12f)
                        this.style = PaintingStyle.Stroke
                        this.strokeWidth = 48f
                        this.strokeCap = StrokeCap.Round
                        this.strokeJoin = StrokeJoin.Round
                        this.isAntiAlias = true
                    }
                )
                canvas.restore()
            }

            // 2. Solid animated outer trace rotating around center
            rotate(degrees = rotation, pivot = center) {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.scale(scale, scale)
                    canvas.drawPath(
                        parsedPath,
                        Paint().apply {
                            this.color = color
                            this.style = PaintingStyle.Stroke
                            this.strokeWidth = 48f
                            this.strokeCap = StrokeCap.Round
                            this.strokeJoin = StrokeJoin.Round
                            this.isAntiAlias = true
                        }
                    )
                    canvas.restore()
                }
            }
        }
    }
}

/**
 * Low-level vector renderer for the official Oscellion symbol path.
 */
@Composable
private fun OscellionVectorSymbol(
    modifier: Modifier = Modifier,
    color: Color? = null,
    brush: Brush? = null,
    edgeHighlight: Boolean = false,
    edgeStrokeColor: Color = Color.White.copy(alpha = 0.6f)
) {
    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 1024f
        val offsetX = (size.width - 1024f * scale) / 2f
        val offsetY = (size.height - 1024f * scale) / 2f

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // Fill
            val paint = Paint().apply {
                this.isAntiAlias = true
                this.style = PaintingStyle.Fill
            }
            if (brush != null) {
                brush.applyTo(Size(1024f, 1024f), paint, 1.0f)
            } else {
                paint.color = color ?: Color.White
            }
            canvas.drawPath(parsedPath, paint)

            // Optional specular highlight stroke
            if (edgeHighlight) {
                val strokePaint = Paint().apply {
                    this.isAntiAlias = true
                    this.style = PaintingStyle.Stroke
                    this.color = edgeStrokeColor
                    this.strokeWidth = 6f
                }
                canvas.drawPath(parsedPath, strokePaint)
            }

            canvas.restore()
        }
    }
}
