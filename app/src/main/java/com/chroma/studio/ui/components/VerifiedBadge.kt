package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal Verification Tiers for creators, VIPs, organizations, and partners.
 */
enum class VerifiedBadgeTier {
    BLUE,       // Official Creator / Verified (Signature Royal Cupertino Blue)
    GOLD,       // Official Organization / VIP Studio Partner (Radiant Warm Gold)
    CHROMA,     // Chroma Original / Premium (Signature Prism Holographic)
    SILVER      // Certified Partner / Pro (Titanium Silver)
}

// Official Signature Verified Blue (Apple Cupertino / Instagram)
val VerifiedBadgeBlueGradient = listOf(
    Color(0xFF2F80ED),
    Color(0xFF007AFF),
    Color(0xFF0056D6)
)

val VerifiedBadgeGoldGradient = listOf(
    Color(0xFFFFD700),
    Color(0xFFFFA500),
    Color(0xFFE67300)
)

val VerifiedBadgeChromaGradient = listOf(
    Color(0xFF00F5FF),
    Color(0xFF7C3AED),
    Color(0xFFFF2E93)
)

val VerifiedBadgeSilverGradient = listOf(
    Color(0xFFF1F5F9),
    Color(0xFF94A3B8),
    Color(0xFF64748B)
)

val IosBadgeGradients = listOf(
    VerifiedBadgeBlueGradient
)

// 12-lobed scalloped rosette seal in 24x24 canonical coordinate space
private const val SEAL_PATH_STRING =
    "M22.5 12.5c0-1.58-.875-2.95-2.148-3.6.154-.435.238-.905.238-1.4 0-2.21-1.71-3.998-3.918-3.998-.47 0-.92.084-1.336.25C14.818 2.415 13.51 1.5 12 1.5s-2.816.917-3.337 2.25c-.416-.165-.866-.25-1.336-.25-2.21 0-3.918 1.79-3.918 4 0 .495.084.965.238 1.4-1.273.65-2.148 2.02-2.148 3.6 0 1.46.74 2.746 1.865 3.45-.06.31-.09.63-.09.95 0 2.21 1.71 4 3.918 4 .47 0 .92-.085 1.336-.25.52 1.335 1.826 2.25 3.337 2.25s2.816-.915 3.337-2.25c.416.165.866.25 1.336.25 2.21 0 3.918-1.79 3.918-4 0-.32-.03-.64-.09-.95 1.125-.705 1.865-1.99 1.865-3.45z"

/**
 * Universal Professional Verified Badge used across all screens in ChromaStudio.
 * Renders an iconic scalloped rosette seal with multi-tiered specular gradients,
 * bevel lighting, subtle drop shadow, and a crisp mathematically centered white checkmark.
 */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE,
    badgeColor: Color? = null,
    userId: String? = null,
    animated: Boolean = false,
    showGlow: Boolean = false
) {
    val sealPath = remember { PathParser().parsePathString(SEAL_PATH_STRING).toPath() }
    val checkPath = remember {
        Path().apply {
            moveTo(7.2f, 12.0f)
            lineTo(10.4f, 15.2f)
            lineTo(17.2f, 8.4f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgeGleam")
    val gleamProgress by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gleamShift"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val primaryBrush = remember(tier, badgeColor) {
        if (badgeColor != null) {
            Brush.verticalGradient(listOf(badgeColor, badgeColor))
        } else {
            when (tier) {
                VerifiedBadgeTier.BLUE -> Brush.linearGradient(VerifiedBadgeBlueGradient)
                VerifiedBadgeTier.GOLD -> Brush.linearGradient(VerifiedBadgeGoldGradient)
                VerifiedBadgeTier.CHROMA -> Brush.linearGradient(VerifiedBadgeChromaGradient)
                VerifiedBadgeTier.SILVER -> Brush.linearGradient(VerifiedBadgeSilverGradient)
            }
        }
    }

    val glowColor = remember(tier, badgeColor) {
        badgeColor?.copy(alpha = 0.35f) ?: when (tier) {
            VerifiedBadgeTier.BLUE -> Color(0xFF007AFF).copy(alpha = 0.35f)
            VerifiedBadgeTier.GOLD -> Color(0xFFFFA500).copy(alpha = 0.35f)
            VerifiedBadgeTier.CHROMA -> Color(0xFF7C3AED).copy(alpha = 0.35f)
            VerifiedBadgeTier.SILVER -> Color(0xFF94A3B8).copy(alpha = 0.35f)
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 24f

        // 0. Optional Ambient Glow
        if (showGlow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = this.size.width * 0.75f
                ),
                radius = this.size.width * 0.75f
            )
        }

        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawIntoCanvas { canvas ->
                // 1. Soft Seal Drop Shadow for realistic depth
                val sealShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = 0.22f)
                }
                sealShadowPaint.asFrameworkPaint().maskFilter =
                    android.graphics.BlurMaskFilter(1f, android.graphics.BlurMaskFilter.Blur.NORMAL)

                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(0f, 0.45f)
                canvas.nativeCanvas.drawPath(sealPath.asAndroidPath(), sealShadowPaint.asFrameworkPaint())
                canvas.nativeCanvas.restore()

                // 2. Rosette Seal Body
                drawPath(
                    path = sealPath,
                    brush = primaryBrush,
                    style = Fill
                )

                // 3. Specular Gleam Sweep (if animated)
                if (animated) {
                    val gleamWidth = 8f
                    val gleamStart = gleamProgress * 24f
                    drawPath(
                        path = sealPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            start = Offset(gleamStart, 0f),
                            end = Offset(gleamStart + gleamWidth, 24f)
                        ),
                        style = Fill
                    )
                }

                // 4. Subtle Apple-Style Bevel Highlight (Hairline Top Rim)
                drawPath(
                    path = sealPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.18f)
                        ),
                        startY = 0f,
                        endY = 24f
                    ),
                    style = Stroke(width = 0.65f)
                )

                // 5. Checkmark Drop Shadow
                val checkShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = 0.22f)
                }
                checkShadowPaint.asFrameworkPaint().strokeWidth = 2.4f
                checkShadowPaint.asFrameworkPaint().style = android.graphics.Paint.Style.STROKE
                checkShadowPaint.asFrameworkPaint().strokeCap = android.graphics.Paint.Cap.ROUND
                checkShadowPaint.asFrameworkPaint().strokeJoin = android.graphics.Paint.Join.ROUND
                checkShadowPaint.asFrameworkPaint().maskFilter =
                    android.graphics.BlurMaskFilter(0.8f, android.graphics.BlurMaskFilter.Blur.NORMAL)

                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(0f, 0.35f)
                canvas.nativeCanvas.drawPath(checkPath.asAndroidPath(), checkShadowPaint.asFrameworkPaint())
                canvas.nativeCanvas.restore()

                // 6. Crisp White Checkmark with Rounded Caps & Join
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = 2.4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Backward compatibility alias for IosVerificationBadge.
 */
@Composable
fun IosVerificationBadge(
    modifier: Modifier = Modifier,
    userId: String? = null,
    size: Dp = 14.dp,
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE,
    animated: Boolean = false
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        tier = tier,
        userId = userId,
        animated = animated
    )
}

/**
 * Backward compatibility alias for ScallopedVerifiedBadge.
 */
@Composable
fun ScallopedVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    badgeColor: Color = Color(0xFF007AFF),
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE,
    animated: Boolean = false
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        badgeColor = badgeColor,
        tier = tier,
        animated = animated
    )
}
